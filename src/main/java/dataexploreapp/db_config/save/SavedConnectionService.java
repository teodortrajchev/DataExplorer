package dataexploreapp.db_config.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SavedConnectionService {

    private static final Path CONNECTIONS_FILE =
            Path.of(System.getProperty("user.home"), ".dataexplorer", "connections.json");

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SavedConnectionService() {
    }

    public static List<SavedConnection> loadConnections() {

        try {

            File file = CONNECTIONS_FILE.toFile();

            if (!file.exists()) {
                return new ArrayList<>();
            }

            SavedConnection[] connections = mapper.readValue(file, SavedConnection[].class);

            return new ArrayList<>(List.of(connections));

        } catch (Exception e) {

            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    public static List<SavedConnection> getConnectionsForUser(String applicationUser) {

        return loadConnections()
                .stream().filter(connection -> connection.getApplicationUser().equalsIgnoreCase(applicationUser)).toList();
    }

    public static Optional<SavedConnection> getDefaultConnection(String applicationUser) {

        return getConnectionsForUser(applicationUser)
                .stream()
                .filter(SavedConnection::isDefaultConnection)
                .findFirst();
    }

    public static void saveConnection(SavedConnection connection) {

        try {

            Files.createDirectories(CONNECTIONS_FILE.getParent());

            List<SavedConnection> connections = loadConnections();

            /*
             * If this connection is marked default,
             * remove default status from all other
             * connections belonging to this user.
             */
            if (connection.isDefaultConnection()) {

                for (SavedConnection existing : connections) {

                    if (existing.getApplicationUser().equalsIgnoreCase(connection.getApplicationUser())) {
                        existing.setDefaultConnection(false);
                    }
                }
            }

            /*
             * Replace an existing connection with the
             * same name for this user.
             */
            connections.removeIf(existing -> existing.getApplicationUser().equalsIgnoreCase(connection.getApplicationUser())&& existing.getName().equalsIgnoreCase(connection.getName()));

            connections.add(connection);

            mapper.writeValue(CONNECTIONS_FILE.toFile(), connections);

        } catch (Exception e) {

            throw new RuntimeException("Failed to save database connection.", e);
        }
    }

    public static void deleteConnection(String applicationUser, String connectionName) {

        try {

            List<SavedConnection> connections = loadConnections();

            connections.removeIf(connection -> connection.getApplicationUser().equalsIgnoreCase(applicationUser) && connection.getName().equalsIgnoreCase(connectionName));

            mapper.writeValue(CONNECTIONS_FILE.toFile(), connections);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete saved connection.", e);
        }
    }
}