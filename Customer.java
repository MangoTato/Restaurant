import  java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Customer extends Frame.BackgroundPanel {
    private static final String CUSTOMER_SIGN_OUT_PASSWORD = "adm123";

    private final Frame mainFrame;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new Frame.BackgroundPanel();
    private final Map<Integer, JButton> tableCards = new LinkedHashMap<>();
    private final Map<Integer, JFrame> tableFrames = new LinkedHashMap<>();
    private final boolean[] startingTables = new boolean[TableService.TABLE_COUNT + 1];
    private JPanel tableCardsPanel;
    private JButton backToLoginButton;
    private final Runnable tableListener = this::refreshTableCards;
    private boolean listening;

    public Customer(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 20));
        setDecorativeBackground(true);
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(18, 28, 28, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel pageTitle = mainFrame.createLabel("Good day, " + username, 22, Frame.NAVY);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(pageTitle, BorderLayout.WEST);

        backToLoginButton = new JButton("Back to login");
        styleActionButton(backToLoginButton, true);
        backToLoginButton.addActionListener(e -> confirmBackToLogin());
        header.add(backToLoginButton, BorderLayout.EAST);

        contentPanel.setLayout(contentLayout);
        contentPanel.setBackground(Frame.BACKGROUND);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER),
                new EmptyBorder(0, 0, 0, 0)));

        contentPanel.add(createLandingPanel(), "Home");
        contentLayout.show(contentPanel, "Home");

        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        JLabel status = new JLabel("Tap a table card to open its menu and start ordering.");
        status.setFont(new Font("SansSerif", Font.PLAIN, 12));
        status.setForeground(Frame.MUTED);
        status.setBorder(new EmptyBorder(8, 4, 0, 4));
        add(status, BorderLayout.SOUTH);
    }

    private JPanel createLandingPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(Frame.BACKGROUND);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        JLabel title = mainFrame.createLabel("Welcome to Pâques", 27, Frame.NAVY);
        title.setFont(new Font("SansSerif", Font.BOLD, 27));
        JLabel subtitle = new JLabel(TableService.TABLE_COUNT
                + " tables · Tap a table card to browse its menu.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Frame.MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(7));
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);

        tableCardsPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        tableCardsPanel.setBackground(Frame.BACKGROUND);
        for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
            final int tableNumber = table;
            JButton tableCard = new JButton();
            tableCard.setHorizontalAlignment(SwingConstants.LEFT);
            tableCard.setVerticalAlignment(SwingConstants.CENTER);
            tableCard.setFocusPainted(false);
            tableCard.setOpaque(true);
            tableCard.setContentAreaFilled(true);
            tableCard.setCursor(new Cursor(Cursor.HAND_CURSOR));
            Frame.styleButtonState(tableCard, false);
            tableCard.setForeground(Frame.NAVY);
            tableCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.BORDER),
                    new EmptyBorder(18, 20, 18, 20)));
            tableCard.setPreferredSize(new Dimension(200, 120));
            tableCard.addActionListener(event -> startTableOrder(tableNumber));
            tableCards.put(table, tableCard);
            tableCardsPanel.add(tableCard);
        }

        JScrollPane tableScrollPane = new JScrollPane(tableCardsPanel);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(Frame.BACKGROUND);
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        refreshTableCards();
        return panel;
    }

    private void startTableOrder(int tableNumber) {
        JFrame openFrame = tableFrames.get(tableNumber);
        if (openFrame != null && openFrame.isDisplayable()) {
            openFrame.setState(JFrame.NORMAL);
            openFrame.toFront();
            openFrame.requestFocus();
            return;
        }
        if (startingTables[tableNumber]) {
            return;
        }
        Long customerId = mainFrame.tableService.getCustomerId(tableNumber);
        if (customerId == null) {
            JOptionPane.showMessageDialog(this,
                    "Table " + tableNumber
                            + " has no customer assigned. Please ask staff to assign your reservation first.",
                    "Customer Assignment Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (mainFrame.tableService.isCheckoutRequested(tableNumber)) {
            JOptionPane.showMessageDialog(this, "Table " + tableNumber + " is waiting for checkout.",
                    "Table Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!mainFrame.tableService.isAvailableTo(tableNumber, customerId)) {
            JOptionPane.showMessageDialog(this,
                    "Table " + tableNumber + " is not available to its assigned customer.",
                    "Table Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        startCustomerTable(tableNumber, customerId);
    }

    private void startCustomerTable(int tableNumber, long customerId) {
        startingTables[tableNumber] = true;
        refreshTableCards();
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws SQLException {
                return database.markCustomerInProgress(customerId);
            }

            @Override
            protected void done() {
                try {
                    String blockedStatus = get();
                    if (blockedStatus != null) {
                        JOptionPane.showMessageDialog(Customer.this,
                                "This customer cannot proceed with Table " + tableNumber
                                        + ". Current status: " + blockedStatus + ".",
                                "Customer Cannot Proceed", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    Long assignedCustomer = mainFrame.tableService.getCustomerId(tableNumber);
                    boolean occupied = mainFrame.tableService.isOccupied(tableNumber);
                    if (assignedCustomer != null && !Long.valueOf(customerId).equals(assignedCustomer)) {
                        JOptionPane.showMessageDialog(Customer.this,
                                "This table is already assigned to another customer.", "Table Unavailable",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (!mainFrame.tableService.isAvailableTo(tableNumber, customerId)) {
                        JOptionPane.showMessageDialog(Customer.this, "This table is reserved or needs cleaning.",
                                "Table Unavailable", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (!occupied && !mainFrame.tableService.occupy(tableNumber, customerId)) {
                        JOptionPane.showMessageDialog(Customer.this, "This table is no longer available.",
                                "Table Unavailable", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    mainFrame.tableService.setCustomer(tableNumber, customerId, "In Progress");
                    showMenu(tableNumber);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(Customer.this, "Starting the table was interrupted.",
                            "Table Start Error", JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    JOptionPane.showMessageDialog(Customer.this,
                            "Unable to start the table:\n" + cause.getMessage(), "Table Start Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    startingTables[tableNumber] = false;
                    refreshTableCards();
                }
            }
        }.execute();
    }

    private void refreshTableCards() {
        if (tableCardsPanel == null) {
            return;
        }
        for (Map.Entry<Integer, JButton> entry : tableCards.entrySet()) {
            int tableNumber = entry.getKey();
            JButton tableCard = entry.getValue();
            String status = getTableStatus(tableNumber);
            Color statusColor = getTableStatusColor(tableNumber);
            Long customerId = mainFrame.tableService.getCustomerId(tableNumber);
            String assignment = customerId == null ? "Not assigned" : "Assigned";
            tableCard.setText("<html><div style='width:150px'>"
                    + "<span style='font-size:16pt;font-weight:bold;color:#"
                    + toHex(Frame.NAVY) + "'>Table " + tableNumber + "</span><br>"
                    + "<span style='color:#" + toHex(Frame.MUTED) + "'>"
                    + TableService.capacityFor(tableNumber) + " seats · " + assignment + "</span><br>"
                    + "<span style='font-weight:bold;color:#" + toHex(statusColor) + "'>"
                    + "Status: " + status + "</span></div></html>");
            tableCard.setEnabled(mainFrame.orderService.isLoaded() && !startingTables[tableNumber]);
            tableCard.setToolTipText("Open ordering for Table " + tableNumber);
        }
        tableCardsPanel.revalidate();
        tableCardsPanel.repaint();
    }

    private Color getTableStatusColor(int tableNumber) {
        if (mainFrame.tableService.isCheckoutRequested(tableNumber)
                || mainFrame.tableService.getStatus(tableNumber) == TableService.Status.OCCUPIED) {
            return Frame.ACCENT_DARK;
        }
        if (mainFrame.tableService.getStatus(tableNumber) == TableService.Status.RESERVED) {
            return Frame.ACCENT;
        }
        return Frame.SUCCESS;
    }

    private String toHex(Color color) {
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String getTableStatus(int tableNumber) {
        if (mainFrame.tableService.isCheckoutRequested(tableNumber)) {
            return "Checkout requested";
        }
        switch (mainFrame.tableService.getStatus(tableNumber)) {
        case OCCUPIED:
            return "In progress";
        case RESERVED:
            return "Reserved";
        case CLEANING:
            return "Cleaning";
        default:
            return "Available";
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!listening) {
            mainFrame.tableService.addListener(tableListener);
            listening = true;
        }
        Timer readyTimer = new Timer(500, event -> {
            if (mainFrame.orderService.isLoaded()) {
                refreshTableCards();
                ((Timer) event.getSource()).stop();
            } else if (!mainFrame.orderService.isLoading()) {
                tableCards.values().forEach(button -> button.setEnabled(false));
                ((Timer) event.getSource()).stop();
            } else if (!isDisplayable()) {
                ((Timer) event.getSource()).stop();
            }
        });
        readyTimer.start();
    }

    @Override
    public void removeNotify() {
        if (listening) {
            mainFrame.tableService.removeListener(tableListener);
            listening = false;
        }
        super.removeNotify();
    }

    private void styleActionButton(JButton button, boolean accent) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(8, 14, 8, 14)));
        Frame.styleButtonState(button, false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
    }

    private void showMenu(int tableNumber) {
        JFrame existingFrame = tableFrames.get(tableNumber);
        if (existingFrame != null && existingFrame.isDisplayable()) {
            existingFrame.setState(JFrame.NORMAL);
            existingFrame.toFront();
            existingFrame.requestFocus();
            return;
        }

        ViewMenu viewMenu = new ViewMenu(mainFrame, true, tableNumber);
        JPanel menuScreenPanel = new JPanel();
        menuScreenPanel.setLayout(new BorderLayout(0, 8));
        menuScreenPanel.setBackground(Color.WHITE);
        menuScreenPanel.setBorder(new EmptyBorder(10, 12, 12, 12));

        JButton backToTables = new JButton("Back to tables");
        styleActionButton(backToTables, false);
        JFrame tableFrame = new JFrame("Table " + tableNumber + " - Ordering");
        backToTables.addActionListener(event -> confirmBackToTables(tableFrame));
        menuScreenPanel.add(backToTables, BorderLayout.NORTH);
        menuScreenPanel.add(viewMenu.ShowViewMenuPanel(), BorderLayout.CENTER);

        tableFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        tableFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                confirmBackToTables(tableFrame);
            }

            @Override
            public void windowClosed(WindowEvent event) {
                tableFrames.remove(tableNumber, tableFrame);
            }
        });
        tableFrame.setContentPane(Frame.createWorkspaceCanvas(menuScreenPanel));
        tableFrame.setMinimumSize(new Dimension(900, 650));
        tableFrame.setSize(1180, 780);
        tableFrame.setLocationRelativeTo(mainFrame);
        tableFrames.put(tableNumber, tableFrame);
        tableFrame.setVisible(true);
    }

    private void confirmBackToTables(JFrame tableFrame) {
        JPasswordField passwordField = new JPasswordField(16);
        int passwordChoice = JOptionPane.showConfirmDialog(tableFrame, passwordField,
                "Enter the password to return to table selection.", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (passwordChoice != JOptionPane.OK_OPTION) {
            return;
        }

        if (!CUSTOMER_SIGN_OUT_PASSWORD.equals(new String(passwordField.getPassword()))) {
            JOptionPane.showMessageDialog(tableFrame, "Incorrect password. You remain on this table.",
                    "Return denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableFrame.dispose();
    }

    private void confirmBackToLogin() {
        int choice = JOptionPane.showConfirmDialog(this, "Return to the login screen?", "Back to login",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            mainFrame.signOut();
        }
    }
}
