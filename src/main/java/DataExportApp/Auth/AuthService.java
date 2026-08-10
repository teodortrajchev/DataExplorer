package DataExportApp.Auth;

import io.github.cdimascio.dotenv.Dotenv;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    private static final Dotenv dotenv = Dotenv.load();

    public static final String DB_URL = dotenv.get("DB_URL");
    public static final String DB_USER = dotenv.get("DB_USER");
    public static final String DB_PASS = dotenv.get("DB_PASS");

    public AuthService() {
    }

    public enum AuthResult { SUCCESS, WRONG_PASSWORD, USER_NOT_FOUND, DB_ERROR }

    public AuthResult authenticate(String username, String plainPassword) {
        String sql = "SELECT password_hash FROM SRB.app_users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return AuthResult.USER_NOT_FOUND;
                }
                String storedHash = rs.getString("password_hash");
                boolean matches = BCrypt.checkpw(plainPassword, storedHash);
                return matches ? AuthResult.SUCCESS : AuthResult.WRONG_PASSWORD;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return AuthResult.DB_ERROR;
        }
    }
}