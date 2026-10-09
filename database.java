import java.sql.*;

public class database {
    private static final String URL = "jdbc:mysql://localhost:3306/restaurant";
    private static final String USER = "root";
    private static final String PASSWORD = "M@ngoT@to123";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J is missing from the project classpath. "
                    + "Reload the project in VS Code and try again.", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void ensureOverviewSchema(Connection connection) throws SQLException {
        ensureMenuColumn(connection, "image_path", "VARCHAR(500) NULL");
        ensureMenuColumn(connection, "cooking_time_minutes", "INT NOT NULL DEFAULT 20");
    }

    private static void ensureMenuColumn(Connection connection, String column, String definition) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menu' AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, column);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Unable to inspect the menu table schema.");
                }
                if (result.getInt(1) == 0) {
                    try (Statement alter = connection.createStatement()) {
                        alter.executeUpdate("ALTER TABLE menu ADD COLUMN " + column + " " + definition);
                    }
                }
            }
        }
    }

    public static void ensureCustomerStatusOptions(Connection connection) throws SQLException {
        String columnType;
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'status'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("The customer.status column was not found.");
            }
            columnType = result.getString("COLUMN_TYPE");
        }

        if (!columnType.contains("'Cancelled'")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE customer MODIFY COLUMN status "
                        + "ENUM('Confirmed', 'In Progress', 'Completed', 'Reserved', 'Cancelled') NULL");
            }
        }
    }

    public static String markCustomerInProgress(long customerId) throws SQLException {
        String updateSql = "UPDATE customer SET status = 'In Progress' WHERE customer_id = ? "
                + "AND (status IS NULL OR status NOT IN ('Cancelled', 'Completed'))";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(updateSql)) {
            statement.setLong(1, customerId);
            if (statement.executeUpdate() > 0) {
                return null;
            }
        }

        String lookupSql = "SELECT status FROM customer WHERE customer_id = ?";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(lookupSql)) {
            statement.setLong(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return "Not Found";
                }
                String status = result.getString("status");
                return "In Progress".equalsIgnoreCase(status) ? null : status;
            }
        }
    }

    public static void clearCustomerInProgress(long customerId) throws SQLException {
        String sql = "UPDATE customer SET status = NULL WHERE customer_id = ? AND status = 'In Progress'";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.executeUpdate();
        }
    }

}
