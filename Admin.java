import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Admin extends JPanel {
    private final Frame mainFrame;

    public Admin(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.LIGHT);
        setBorder(new EmptyBorder(28, 38, 28, 38));

        // Header Section
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(mainFrame.createLabel("Good day, " + username, 25, Frame.NAVY), BorderLayout.WEST);

        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> mainFrame.signOut());
        header.add(logout, BorderLayout.EAST);

        // Actions Section
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        actions.setOpaque(false);

        List<String> actionNames = List.of("Manage menu", "View staff", "Add user", "Daily reports");
        for (String action : actionNames) {
            JButton button = new JButton(action);
            button.setPreferredSize(new Dimension(150, 62));
            button.addActionListener(e -> handleAdminAction(action));
            actions.add(button);
        }

        // Status Text Area
        JTextArea status = new JTextArea("You are signed in as an administrator.\nChoose an option to begin managing today's restaurant operations.");
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(new Font("SansSerif", Font.PLAIN, 15));
        status.setBackground(Color.WHITE);
        status.setBorder(new EmptyBorder(20, 20, 20, 20));

        add(header, BorderLayout.NORTH);
        add(actions, BorderLayout.CENTER);
        add(new JScrollPane(status), BorderLayout.SOUTH);
    }

    private void handleAdminAction(String action) {
        if ("Add user".equals(action)) {
            showAddUserDialog();
        } else {
            JOptionPane.showMessageDialog(this, action + " Soon.", "Administrator", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showAddUserDialog() {
        JTextField newUsername = new JTextField();
        JPasswordField newPassword = new JPasswordField();
        JCheckBox makeAdmin = new JCheckBox("Administrator privileges");

        Object[] message = {
            "Username:", newUsername,
            "Password:", newPassword,
            "", makeAdmin
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add New Account", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String user = newUsername.getText().trim();
            String pass = new String(newPassword.getPassword());
            boolean isAdmin = makeAdmin.isSelected();

            if (!user.isEmpty() && !pass.isEmpty()) {
                if (mainFrame.addUser(user, pass, isAdmin)) {
                    JOptionPane.showMessageDialog(this, "Account created for " + user + "!");
                } else {
                    JOptionPane.showMessageDialog(this, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Fields cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}