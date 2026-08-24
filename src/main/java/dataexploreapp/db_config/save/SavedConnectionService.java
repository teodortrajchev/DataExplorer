package dataexploreapp.db_config.save;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SavedConnectionService {

    private static Path connections_file = Path.of(System.getProperty("user.home"), ".dataexplorer", "connections.json");

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private SavedConnectionService() {
    }

    public static List<SavedConnection> loadConnections() {

        try {

            File file = connections_file.toFile();

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
    public static void setConnectionsFile(Path path) {
        connections_file = path;
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

            Files.createDirectories(connections_file.getParent());

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

            mapper.writeValue(connections_file.toFile(), connections);

        } catch (Exception e) {

            throw new RuntimeException("Failed to save database connection.", e);
        }
    }

    public static void deleteConnection(String applicationUser, String connectionName) {

        try {

            List<SavedConnection> connections = loadConnections();

            connections.removeIf(connection -> connection.getApplicationUser().equalsIgnoreCase(applicationUser) && connection.getName().equalsIgnoreCase(connectionName));

            mapper.writeValue(connections_file.toFile(), connections);

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete saved connection.", e);
        }
    }
    public static void change_default(String name) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File file = connections_file.toFile();
        // Read JSON
        ArrayNode connections = (ArrayNode) mapper.readTree(file);
        for (JsonNode node : connections) {
            ObjectNode connection = (ObjectNode) node;
            if (connection.get("name").asText().equals(name)) {
                connection.put("defaultConnection", true);
            } else {
                connection.put("defaultConnection", false);
            }
        }
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(file, connections);
    }
}