package Config.Database;


import oracle.jdbc.internal.OracleTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public DataTable loadTable(String schemaOwner, String tableName) throws SQLException {
        DataTable table = runQuery("SELECT * FROM " + schemaOwner + "." + tableName);
        table.setTitle(tableName);
        return table;
    }
    public DataTable loadProcedure(String schemaOwner, String procedureName) throws SQLException {
        DataTable table = runQuery("CALL " + schemaOwner + "." + procedureName + "(?)");
        table.setTitle(procedureName);
        return table;
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