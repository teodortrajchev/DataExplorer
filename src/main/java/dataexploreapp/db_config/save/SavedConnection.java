package dataexploreapp.db_config.save;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dataexploreapp.encryption.PasswordEncryptionService;

public class SavedConnection {

    private String name;

    @JsonProperty("applicationUser")
    private String applicationUser;

    private String jdbcUrl;
    private String databaseUser;
    private String encryptedPassword;
    private String schema;
    private boolean defaultConnection;

    public SavedConnection() {
    }

    public SavedConnection(String name, String applicationUser, String jdbcUrl, String databaseUser, String encryptedPassword, String schema, boolean defaultConnection) {
        this.name = name;
        this.applicationUser = applicationUser;
        this.jdbcUrl = jdbcUrl;
        this.databaseUser = databaseUser;
        this.encryptedPassword = encryptedPassword;
        this.schema = schema;
        this.defaultConnection = defaultConnection;
    }

    public void setName(String name) {
        this.name = name;
    }


    @JsonIgnore
    public void setApplicationUser(String applicationUser) {
        this.applicationUser = applicationUser;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setDefaultConnection(boolean defaultConnection) {
        this.defaultConnection = defaultConnection;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getName() {
        return name;
    }

    /**
     * Public API: returns the DECRYPTED application user.
     * Ignored by Jackson - the field annotation above handles JSON binding,
     * so this never gets invoked during writeValue()/readValue().
     */
    @JsonIgnore
    public String getApplicationUser() {
        try {
            return PasswordEncryptionService.decrypt(applicationUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDatabaseUser() {
        return databaseUser;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getSchema() {
        return schema;
    }

    public boolean isDefaultConnection() {
        return defaultConnection;
    }
}