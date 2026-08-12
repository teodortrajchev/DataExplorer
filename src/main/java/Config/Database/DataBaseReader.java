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

    public DataTable loadTable(String schemaOwner, String tableName) throws SQLException {
        DataTable table = runQuery("SELECT * FROM " + schemaOwner + "." + tableName);
        table.setTitle(tableName);
        return table;
    }

    /** Loads a procedure with no parameters (backward-compatible convenience overload). */
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