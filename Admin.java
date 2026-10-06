import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Admin extends JPanel {
    private final Frame mainFrame;
    private final BufferedImage backgroundImage;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);

    public Admin(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;
        backgroundImage = loadBackgroundImage();

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.LIGHT);
        setBorder(new EmptyBorder(18, 38, 28, 38));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(
            mainFrame.createLabel(" " , 25, Frame.NAVY),
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
            "Front Desk"
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
        gbc.insets = new Insets(120, 0, 0, 0);
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
                contentLayout.show(contentPanel, "Staff");
                } else if ("Customer".equals(action)) {
                    contentPanel.removeAll();
                    contentPanel.add(new CustomerLog(mainFrame), "Customer");
                    contentLayout.show(contentPanel, "Customer");
                } else if ("Tables".equals(action)) {
                    contentPanel.removeAll();
                    contentPanel.add(createTablesPanel(), "Tables");
                    contentLayout.show(contentPanel, "Tables");
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

    private JPanel createTablesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JLabel title = new JLabel("Table Availability");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Frame.NAVY);
        panel.add(title, BorderLayout.NORTH);

        JPanel tableList = new JPanel();
        tableList.setLayout(new BoxLayout(tableList, BoxLayout.Y_AXIS));
        tableList.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(tableList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        panel.add(scrollPane, BorderLayout.CENTER);

        Runnable refresh = () -> {
            tableList.removeAll();
            for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
                final int tableNumber = table;
                boolean occupied = mainFrame.tableService.isOccupied(tableNumber);
                JPanel row = new JPanel(new BorderLayout(15, 8));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
                row.setBackground(Color.WHITE);
                row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY), new EmptyBorder(10, 12, 10, 12)));

                JLabel tableLabel = new JLabel("Table " + tableNumber);
                tableLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
                JLabel status = new JLabel(occupied ? "Occupied" : "Open");
                status.setFont(new Font("SansSerif", Font.BOLD, 15));
                status.setForeground(occupied ? new Color(190, 45, 45) : new Color(0, 130, 80));
                JPanel labels = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
                labels.setOpaque(false);
                labels.add(tableLabel);
                labels.add(status);
                row.add(labels, BorderLayout.WEST);

                JButton done = new JButton("Table Done");
                done.setVisible(occupied);
                done.setEnabled(occupied);
                done.addActionListener(e -> {
                    mainFrame.orderService.clearTableOrders(tableNumber);
                    mainFrame.tableService.clear(tableNumber);
                });
                row.add(done, BorderLayout.EAST);
                tableList.add(row);
                tableList.add(Box.createVerticalStrut(8));
            }
            tableList.revalidate();
            tableList.repaint();
        };
        mainFrame.tableService.addListener(refresh);
        refresh.run();
        return panel;
    }

    public void ShowAddUserDialog() {
        new AddUser(mainFrame).ShowAddUserDialog();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (backgroundImage != null) {
            graphics.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private BufferedImage loadBackgroundImage() {
        try {
            return ImageIO.read(new File("Images", "5.png"));
        } catch (IOException e) {
            return null;
        }
    }
}
