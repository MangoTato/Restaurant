import java.sql.*;

public class database {
    private static final String URL = "jdbc:mysql://localhost:3306/restaurant";
    private static final String USER = "root";
    private static final String PASSWORD = "M@ngoT@to123";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL Connector/J is missing from the project classpath. "
                    + "Reload the project in VS Code and try again.",
                e
            );
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void database() {
        try (
            Connection connection = getConnection();
            PreparedStatement statement =
                connection.prepareStatement("SELECT 1 FROM employee LIMIT 1");
            ResultSet resultSet = statement.executeQuery()
        ) {
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read the employee table.", e);
        }
    }
}
