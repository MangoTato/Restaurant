import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuData {
    public static final List<MenuItem> menuItems = new ArrayList<>();

    public static void loadFromDatabase() throws SQLException {
        List<MenuItem> loadedItems = new ArrayList<>();
        try (Connection connection = database.getConnection()) {
            ensureImagePathColumn(connection);
            String sql = "SELECT item_id, item_name, item_description, item_category, price, image_path "
                    + "FROM menu ORDER BY item_id";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String name = results.getString("item_name");
                    MenuItem item = new MenuItem(name == null ? "" : name, results.getDouble("price"));
                    item.setDatabaseId(results.getInt("item_id"));
                    item.setCategory(fromDatabaseCategory(results.getString("item_category")));
                    String description = results.getString("item_description");
                    item.setDescription(description == null ? "" : description);
                    String imagePath = results.getString("image_path");
                    item.setImagePath(imagePath == null ? "" : imagePath);
                    loadedItems.add(item);
                }
            }
        }

        menuItems.clear();
        menuItems.addAll(loadedItems);
    }

    public static void insert(MenuItem item) throws SQLException {
        try (Connection connection = database.getConnection()) {
            ensureImagePathColumn(connection);
            String sql = "INSERT INTO menu (item_name, item_description, item_category, price, image_path) "
                    + "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindItem(statement, item);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("The database did not return a menu item ID.");
                    }
                    item.setDatabaseId(keys.getInt(1));
                }
            }
        }
        menuItems.add(item);
    }

    public static void update(MenuItem item, String name, double price, String category, String description,
            String imagePath) throws SQLException {
        if (item.getDatabaseId() == null) {
            throw new SQLException("This menu item has no database ID and cannot be updated.");
        }

        try (Connection connection = database.getConnection()) {
            ensureImagePathColumn(connection);
            String sql = "UPDATE menu SET item_name = ?, item_description = ?, item_category = ?, "
                    + "price = ?, image_path = ? WHERE item_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                statement.setString(2, description);
                statement.setString(3, toDatabaseCategory(category));
                statement.setDouble(4, price);
                statement.setString(5, imagePath);
                statement.setInt(6, item.getDatabaseId());
                if (statement.executeUpdate() == 0) {
                    throw new SQLException("The menu item no longer exists in the database.");
                }
            }
        }

        item.setName(name);
        item.setPrice(price);
        item.setCategory(category);
        item.setDescription(description);
        item.setImagePath(imagePath);
    }

    public static void delete(MenuItem item) throws SQLException {
        if (item.getDatabaseId() == null) {
            throw new SQLException("This menu item has no database ID and cannot be deleted.");
        }

        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM menu WHERE item_id = ?")) {
            statement.setInt(1, item.getDatabaseId());
            if (statement.executeUpdate() == 0) {
                throw new SQLException("The menu item no longer exists in the database.");
            }
        }

        menuItems.remove(item);
    }

    private static void bindItem(PreparedStatement statement, MenuItem item) throws SQLException {
        statement.setString(1, item.getName());
        statement.setString(2, item.getDescription());
        statement.setString(3, toDatabaseCategory(item.getCategory()));
        statement.setDouble(4, item.getPrice());
        statement.setString(5, item.getImagePath());
    }

    private static String fromDatabaseCategory(String category) throws SQLException {
        if (category == null) {
            throw new SQLException("A menu item has no category.");
        }
        switch (category.trim().toUpperCase(Locale.ROOT)) {
        case "MAIN DISH":
            return "Main Dish";
        case "SIDE DISH":
            return "Side Dish";
        case "BEVERAGES":
            return "Beverages";
        default:
            throw new SQLException("Unsupported menu category in database: " + category);
        }
    }

    private static String toDatabaseCategory(String category) throws SQLException {
        if (category == null) {
            throw new SQLException("A menu item category is required.");
        }
        switch (category.trim().toLowerCase(Locale.ROOT)) {
        case "main dish":
            return "MAIN DISH";
        case "side dish":
            return "SIDE DISH";
        case "beverages":
            return "BEVERAGES";
        default:
            throw new SQLException("Unsupported menu category: " + category);
        }
    }

    private static void ensureImagePathColumn(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'menu' " + "AND COLUMN_NAME = 'image_path'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Unable to inspect the menu table schema.");
            }
            if (result.getInt(1) == 0) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE menu ADD COLUMN image_path VARCHAR(500) NULL");
                }
            }
        }
    }
}
