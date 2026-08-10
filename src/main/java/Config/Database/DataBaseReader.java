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

    private DataTable runSelect(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return toDataTable(rs);
        }
    }

    /**
     * Calls a stored procedure whose single parameter is an OUT REF CURSOR,
     * e.g. Oracle proc: PROCEDURE get_all_emps(p_cursor OUT SYS_REFCURSOR)
     * Called as: CALL SRB.GET_ALL_EMPS(?)
     */
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