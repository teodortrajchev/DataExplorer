package Config.Database;


import oracle.jdbc.internal.OracleTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataBaseReader {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DataBaseReader(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public DataTable runQuery(String sql) throws SQLException {
        String trimmed = sql.trim();
        boolean isCall = trimmed.toUpperCase().startsWith("CALL")
                || trimmed.toUpperCase().startsWith("{CALL")
                || trimmed.toUpperCase().startsWith("BEGIN");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            if (isCall) {
                return runProcedureWithRefCursor(conn, trimmed);
            } else {
                return runSelect(conn, trimmed);
            }
        }
    }
    public void testConnection() throws SQLException {

        try (Connection conn = DriverManager.getConnection(
                jdbcUrl,
                username,
                password)) {

            if (!conn.isValid(5)) {
                throw new SQLException("Connection is not valid.");
            }
        }
    }

    public List<String> listTables(String schemaOwner) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT table_name FROM all_tables WHERE owner = ? ORDER BY table_name";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaOwner.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        }
        return tables;
    }
    public List<String> listProcedures(String schemaOwner) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = """
        SELECT object_name
        FROM all_objects
        WHERE owner = ?
          AND object_type = 'PROCEDURE'
        ORDER BY object_name
        """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaOwner.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("object_name"));
                }
            }
        }
        return tables;
    }
    public List<ForeignKeyInfo> getForeignKeys(String schemaOwner, String tableName) throws SQLException {
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();
        String sql = """
            SELECT a.constraint_name,
                   a.column_name AS fk_column,
                   c_pk.owner AS referenced_owner,
                   c_pk.table_name AS referenced_table,
                   b.column_name AS referenced_column
            FROM all_cons_columns a
            JOIN all_constraints c
              ON a.constraint_name = c.constraint_name AND a.owner = c.owner
            JOIN all_constraints c_pk
              ON c.r_constraint_name = c_pk.constraint_name AND c.r_owner = c_pk.owner
            JOIN all_cons_columns b
              ON c_pk.constraint_name = b.constraint_name AND c_pk.owner = b.owner AND a.position = b.position
            WHERE c.constraint_type = 'R'
              AND a.owner = ?
              AND a.table_name = ?
            ORDER BY a.constraint_name, a.position
            """;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaOwner.toUpperCase());
            stmt.setString(2, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    foreignKeys.add(new ForeignKeyInfo(
                            rs.getString("constraint_name"),
                            rs.getString("fk_column"),
                            rs.getString("referenced_owner"),
                            rs.getString("referenced_table"),
                            rs.getString("referenced_column")
                    ));
                }
            }
        }
        return foreignKeys;
    }
    public List<String> getColumnNames(String schemaOwner, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String sql = "SELECT column_name FROM all_tab_columns WHERE owner = ? AND table_name = ? ORDER BY column_id";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaOwner.toUpperCase());
            stmt.setString(2, tableName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("column_name"));
                }
            }
        }
        return columns;
    }
    public List<ProcedureParameter> getProcedureParameters(String schemaOwner, String procedureName) throws SQLException {
        List<ProcedureParameter> params = new ArrayList<>();
        String sql = """
            SELECT argument_name, position, in_out, data_type
            FROM all_arguments
            WHERE owner = ?
              AND object_name = ?
              AND package_name IS NULL
            ORDER BY position
            """;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaOwner.toUpperCase());
            stmt.setString(2, procedureName.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String argName = rs.getString("argument_name");
                    int position = rs.getInt("position");
                    // Fall back to a synthetic name if Oracle reports none (rare, but defensive)
                    if (argName == null) {
                        argName = "param" + position;
                    }
                    params.add(new ProcedureParameter(
                            argName,
                            position,
                            rs.getString("in_out"),
                            rs.getString("data_type")
                    ));
                }
            }
        }
        return params;
    }

    public DataTable loadTableWithForeignKeys(String schemaOwner, String tableName, List<ForeignKeyInfo> enabledForeignKeys) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT t.*");
        List<String> joinClauses = new ArrayList<>();

        int i = 0;
        for (ForeignKeyInfo fk : enabledForeignKeys) {
            String alias = "j" + i;
            List<String> refColumns = getColumnNames(fk.getReferencedOwner(), fk.getReferencedTable());

            for (String col : refColumns) {
                sql.append(", ").append(alias).append(".").append(col)
                        .append(" AS ").append(fk.getReferencedTable()).append(i).append("_").append(col);
            }

            joinClauses.add("LEFT JOIN " + fk.getReferencedOwner() + "." + fk.getReferencedTable() + " " + alias
                    + " ON t." + fk.getFkColumn() + " = " + alias + "." + fk.getReferencedColumn());
            i++;
        }

        sql.append(" FROM ").append(schemaOwner).append(".").append(tableName).append(" t");
        for (String join : joinClauses) {
            sql.append(" ").append(join);
        }

        DataTable table = runQuery(sql.toString());
        table.setTitle(tableName);
        return table;
    }

    public DataTable loadTable(String schemaOwner, String tableName) throws SQLException {
        return loadTableWithForeignKeys(schemaOwner, tableName, List.of());
    }
    public DataTable loadProcedure(String schemaOwner, String procedureName) throws SQLException {
        return loadProcedure(schemaOwner, procedureName, Map.of());
    }


    public DataTable loadProcedure(String schemaOwner, String procedureName, Map<String, String> paramValues) throws SQLException {
        List<ProcedureParameter> params = getProcedureParameters(schemaOwner, procedureName);

        if (params.isEmpty()) {
            // No metadata found — fall back to the original single-OUT-cursor assumption.
            DataTable table = runQuery("CALL " + schemaOwner + "." + procedureName + "(?)");
            table.setTitle(procedureName);
            return table;
        }

        String placeholders = params.stream().map(p -> "?").collect(Collectors.joining(", "));
        String callSql = "{call " + schemaOwner + "." + procedureName + "(" + placeholders + ")}";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             CallableStatement stmt = conn.prepareCall(callSql)) {

            int cursorPosition = -1;
            for (ProcedureParameter param : params) {
                if (param.isOutCursor()) {
                    stmt.registerOutParameter(param.getPosition(), OracleTypes.CURSOR);
                    cursorPosition = param.getPosition();
                } else if (param.isInput()) {
                    stmt.setString(param.getPosition(), paramValues.get(param.getName()));
                }
            }

            if (cursorPosition == -1) {
                throw new SQLException("Procedure " + procedureName + " has no OUT REF CURSOR parameter — cannot load as a table.");
            }

            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(cursorPosition)) {
                DataTable table = toDataTable(rs);
                table.setTitle(procedureName);
                return table;
            }
        }
    }


    private DataTable runSelect(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return toDataTable(rs);
        }
    }

    private DataTable runProcedureWithRefCursor(Connection conn, String sql) throws SQLException {
        // Normalize "CALL X(?)" into JDBC escape syntax "{call X(?)}" if not already wrapped
        String callSql = sql.startsWith("{") ? sql : "{" + sql + "}";

        try (CallableStatement stmt = conn.prepareCall(callSql)) {
            stmt.registerOutParameter(1, OracleTypes.CURSOR);
            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                return toDataTable(rs);
            }
        }
    }

    private DataTable toDataTable(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> columnNames = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(meta.getColumnLabel(i));
        }

        DataTable table = new DataTable(columnNames);

        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            table.addRow(row);
        }

        return table;
    }
}