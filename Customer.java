import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Customer extends JPanel {
    private final Frame mainFrame;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    public Customer(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.LIGHT);
        setBorder(new EmptyBorder(28, 38, 28, 38));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        header.add(
            mainFrame.createLabel(
                "Good day, " + username,
                25,
                Frame.NAVY
            ),
            BorderLayout.WEST
        );

        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> confirmCustomerSignOut());
        header.add(logout, BorderLayout.EAST);

        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)
        );

        contentPanel.add(createLandingPanel(), "Home");
        contentLayout.show(contentPanel, "Home");

        JTextArea status = new JTextArea(
            "You are signed in as a customer.\n"
            + "Start your order to view the restaurant menu."
        );
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(new Font("SansSerif", Font.PLAIN, 15));
        status.setBackground(Color.WHITE);
        status.setBorder(new EmptyBorder(20, 20, 20, 20));

        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(new JScrollPane(status), BorderLayout.SOUTH);
    }

    private JPanel createLandingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.NAVY, 2),
                new EmptyBorder(45, 70, 45, 70)
            )
        );

        JLabel title = mainFrame.createLabel(
            "Welcome to Our Restaurant",
            28,
            Frame.NAVY
        );
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel message = new JLabel("Ready to place your order?");
        message.setFont(new Font("SansSerif", Font.PLAIN, 17));
        message.setForeground(Color.DARK_GRAY);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tablePrompt = new JLabel("Choose your table:");
        tablePrompt.setFont(new Font("SansSerif", Font.PLAIN, 15));
        tablePrompt.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<Integer> tableSelector = new JComboBox<>();
        for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
            tableSelector.addItem(table);
        }
        tableSelector.setMaximumSize(new Dimension(180, 30));
        tableSelector.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton startOrderButton = new JButton("Start Order");
        startOrderButton.setPreferredSize(new Dimension(180, 55));
        startOrderButton.setMaximumSize(new Dimension(180, 55));
        startOrderButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        startOrderButton.addActionListener(e -> {
            Integer selectedTable = (Integer) tableSelector.getSelectedItem();
            if (selectedTable == null) return;

            if (mainFrame.tableService.isOccupied(selectedTable)) {
                int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Table " + selectedTable + " is currently occupied. Are you sure this is the right table?",
                    "Confirm Occupied Table",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (choice != JOptionPane.YES_OPTION) return;
            } else {
                mainFrame.tableService.occupy(selectedTable);
            }
            // An occupied table is reopened with its saved, table-specific order.
            showMenu(selectedTable);
        });

        card.add(title);
        card.add(Box.createVerticalStrut(15));
        card.add(message);
        card.add(Box.createVerticalStrut(18));
        card.add(tablePrompt);
        card.add(Box.createVerticalStrut(8));
        card.add(tableSelector);
        card.add(Box.createVerticalStrut(30));
        card.add(startOrderButton);

        panel.add(card);

        return panel;
    }

    private void showMenu(int tableNumber) {
    ViewMenu viewMenu = new ViewMenu(mainFrame, true, tableNumber);

    contentPanel.add(
        viewMenu.ShowViewMenuPanel(),
        "Menu"
    );

    contentLayout.show(contentPanel, "Menu");

    contentPanel.revalidate();
    contentPanel.repaint();
}

    private void confirmCustomerSignOut() {
        JPasswordField passwordField = new JPasswordField(14);
        int choice = JOptionPane.showConfirmDialog(
            this,
            passwordField,
            "Enter password to sign out",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) return;

        if ("adm123".equals(new String(passwordField.getPassword()))) {
            mainFrame.signOut();
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Incorrect password. You remain signed in.",
                "Sign out denied",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
