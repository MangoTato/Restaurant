import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class Frame extends JFrame {
    public static final Color NAVY = new Color(24, 42, 62);
    public static final Color TEAL = new Color(0, 145, 150);
    public static final Color LIGHT = new Color(245, 248, 250);

    final Map<String, User> userMap = new HashMap<>();

    final CardLayout cardLayout = new CardLayout();
    final JPanel content = new JPanel(cardLayout);
    final JTextField usernameField = new JTextField(18);
    final JPasswordField passwordField = new JPasswordField(18);
    final JRadioButton adminRole = new JRadioButton("Administrator", true);
    final JRadioButton employeeRole = new JRadioButton("Employee");
    final JLabel messageLabel = new JLabel(" ");

    public Frame() {
        userMap.put("admin", new User("admin", "admin123", true));
        userMap.put("employee", new User("employee", "employee123", false));

        setTitle("Restaurant Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setSize(900, 560);
        setLocationRelativeTo(null);

        content.add(new Login(this).LoginPanel(), "login");
        setContentPane(content);
    }

    public boolean addUser(String username, String password, boolean isAdmin) {
        if (userMap.containsKey(username)) {
            return false;
        }
        userMap.put(username, new User(username, password, isAdmin));
        return true;
    }

    public JLabel createLabel(String text, int size, Color color) {
        JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        label.setFont(new Font("SansSerif", Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    public void addField(JPanel form, GridBagConstraints gbc, int row, String labelText, Component comp) {
        gbc.gridy = row;
        if (labelText != null) {
            JLabel lbl = new JLabel(labelText);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            form.add(lbl, gbc);
            gbc.gridy = row + 1;
        }
        if (comp != null) {
            form.add(comp, gbc);
        }
    }

    public void attemptLogin() {
        new attemptlogin(this).attemptLogin();
    }

    public void showDashboard(String username, boolean isAdmin) {
        JPanel dashboard;
        if (isAdmin) {
            dashboard = new Admin(this, username);
        } else {
            dashboard = new EmployeePanel(this, username);
        }

        content.add(dashboard, "dashboard");
        cardLayout.show(content, "dashboard");
    }

    public void signOut() {
        usernameField.setText("");
        passwordField.setText("");
        messageLabel.setText(" ");
        cardLayout.show(content, "login");
    }
}