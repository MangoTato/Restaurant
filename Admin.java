import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Admin extends JPanel {
    private final Frame mainFrame;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    public Admin(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.LIGHT);
        setBorder(new EmptyBorder(28, 38, 28, 38));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(
            mainFrame.createLabel("Good day, " + username, 25, Frame.NAVY),
            BorderLayout.WEST
        );

        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> mainFrame.signOut());
        header.add(logout, BorderLayout.EAST);

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 14));
        buttonPanel.setOpaque(false);

        List<String> actionNames = List.of(
            "Dashboard",
            "Manage menu",
            "Staff",
            "Customer",
            "Reports",
            "Tables",
            "Kitchen",
            "Front Desk",
            "Cashier"
        );

        for (String action : actionNames) {
            JButton button = new JButton(action);
            button.setPreferredSize(new Dimension(180, 55));
            button.addActionListener(e -> handleAdminAction(action));
            buttonPanel.add(button);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        actions.add(buttonPanel, gbc);

        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)
        );
        contentPanel.add(dashboardPanel(), "Home");
        contentLayout.show(contentPanel, "Home");

        JPanel mainArea = new JPanel(new BorderLayout(20, 0));
        mainArea.setOpaque(false);
        mainArea.add(actions, BorderLayout.WEST);
        mainArea.add(contentPanel, BorderLayout.CENTER);

        JTextArea status = new JTextArea(
            "You are signed in as an administrator.\n"
            + "Choose an option to begin managing "
            + "today's restaurant operations."
        );
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(new Font("SansSerif", Font.PLAIN, 15));
        status.setBackground(Color.WHITE);
        status.setBorder(new EmptyBorder(20, 20, 20, 20));

        add(header, BorderLayout.NORTH);
        add(mainArea, BorderLayout.CENTER);
        add(new JScrollPane(status), BorderLayout.SOUTH);
    }

    private void handleAdminAction(String action) {
        if ("Manage menu".equals(action)) {
            ManageMenu manageMenu = new ManageMenu(mainFrame);

            contentPanel.removeAll();
            contentPanel.add(manageMenu.ShowMenuPanel(),"Manage menu");
            contentLayout.show(contentPanel, "Manage menu");

            } else if ("Staff".equals(action)){
                Staff staff = new Staff(mainFrame);
                contentPanel.removeAll();
                contentPanel.add(staff.showStaff(),"Staff");
                
                } else {
                    contentPanel.removeAll();
                    contentPanel.add(ConstructionPanel(action),action);
                    contentLayout.show(contentPanel, action);
                }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel dashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel title = new JLabel("Administrator Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Frame.NAVY);
        panel.add(title, BorderLayout.NORTH);

        JTextArea text = new JTextArea(
            "\nSelect an option from the left "
            + "to begin managing the restaurant."
        );
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("SansSerif", Font.PLAIN, 16));
        text.setBackground(Color.WHITE);
        panel.add(text, BorderLayout.CENTER);

        return panel;
    }

    private JPanel ConstructionPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 25));
        label.setForeground(Frame.NAVY);
        panel.add(label, BorderLayout.NORTH);

        JLabel message = new JLabel(
            title + " is Under Construction."
        );
        message.setFont(new Font("SansSerif", Font.PLAIN, 16));
        panel.add(message, BorderLayout.CENTER);

        return panel;
    }

    public void ShowAddUserDialog() {
        new AddUser(mainFrame).ShowAddUserDialog();
    }
}
