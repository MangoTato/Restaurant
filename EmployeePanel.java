import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class EmployeePanel extends JPanel {
    private static final String WINDOW_SELECTION_PASSWORD = "adm123";

    private final Frame mainFrame;
    private final Map<String, JFrame> operationWindows = new LinkedHashMap<>();
    private Runnable cashierRefreshListener;
    private Runnable kitchenRefreshListener;
    private Runnable cashierOrderListener;
    private final Set<Integer> completedKitchenTables = new HashSet<>();
    private boolean kitchenRefreshed;

    public EmployeePanel(Frame mainFrame, String username) {

        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.BACKGROUND);
        setBorder(new EmptyBorder(18, 28, 28, 28));

        // =====================================================
        // HEADER
        // =====================================================

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));
        JLabel pageTitle = mainFrame.createLabel("Pâques • Employee • " + username, 22, Frame.NAVY);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(pageTitle, BorderLayout.WEST);

        JButton logout = new JButton("Sign out");
        styleActionButton(logout, true);
        logout.addActionListener(e -> requestSelectionSignOut());
        header.add(logout, BorderLayout.EAST);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel welcome = new JLabel("Your workspaces");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 25));
        welcome.setForeground(Frame.NAVY);
        JLabel instruction = new JLabel("Choose a workspace. Each section opens in its own window.");
        instruction.setFont(new Font("SansSerif", Font.PLAIN, 13));
        instruction.setForeground(Frame.MUTED);
        heading.add(welcome);
        heading.add(Box.createVerticalStrut(5));
        heading.add(instruction);

        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 18));
        cards.setOpaque(false);
        cards.add(createOperationCard("KITCHEN", "Kitchen", "Prepare incoming table orders.", Frame.ACCENT));
        cards.add(createOperationCard("CASHIER", "Cashier", "Review bills and process checkout.", Frame.SUCCESS));
        cards.add(createOperationCard("FRONT DESK", "Registration", "Manage guest bookings and schedules.",
                Frame.ACCENT_DARK));
        JPanel launcher = new JPanel(new BorderLayout(0, 24));
        launcher.setOpaque(false);
        launcher.setBorder(new EmptyBorder(36, 18, 36, 18));
        launcher.add(heading, BorderLayout.NORTH);
        launcher.add(cards, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(launcher, BorderLayout.CENTER);
    }

    private JPanel createOperationCard(String eyebrow, String section, String description, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 18));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(20, 20, 18, 20)));

        JLabel icon = new JLabel(section.substring(0, 1), SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(54, 54));
        icon.setOpaque(true);
        icon.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
        icon.setForeground(accent);
        icon.setFont(new Font("SansSerif", Font.BOLD, 24));
        icon.setBorder(BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(),
                accent.getBlue(), 90)));
        JPanel iconRow = new JPanel(new BorderLayout());
        iconRow.setOpaque(false);
        iconRow.add(icon, BorderLayout.WEST);
        JLabel tag = new JLabel(eyebrow);
        tag.setFont(new Font("SansSerif", Font.BOLD, 10));
        tag.setForeground(accent);
        iconRow.add(tag, BorderLayout.EAST);
        card.add(iconRow, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(section);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Frame.NAVY);
        JLabel copy = new JLabel("<html><div style='width:190px'>" + description + "</div></html>");
        copy.setFont(new Font("SansSerif", Font.PLAIN, 12));
        copy.setForeground(Frame.MUTED);
        details.add(title);
        details.add(Box.createVerticalStrut(8));
        details.add(copy);
        card.add(details, BorderLayout.CENTER);

        JButton open = new JButton("Open workspace  →");
        styleWorkspaceButton(open, accent);
        open.addActionListener(event -> openOperationWindow(section));
        card.add(open, BorderLayout.SOUTH);
        return card;
    }

    private void styleWorkspaceButton(JButton button, Color accent) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(10, 12, 10, 12)));
    }

    private void openOperationWindow(String section) {
        JFrame open = operationWindows.get(section);
        if (open != null && open.isDisplayable()) {
            open.setState(JFrame.NORMAL);
            open.toFront();
            open.requestFocus();
            return;
        }
        JPanel operationPanel;
        switch (section) {
            case "Kitchen":
                operationPanel = createKitchenPanel();
                break;
            case "Cashier":
                operationPanel = createCashierPanel();
                break;
            case "Registration":
                operationPanel = new Registration(mainFrame);
                break;
            default:
                throw new IllegalArgumentException("Unknown employee workspace: " + section);
        }
        JFrame window = new JFrame("Pâques • " + section);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(Frame.BACKGROUND);
        content.setBorder(new EmptyBorder(12, 14, 14, 14));
        JPanel windowHeader = new JPanel(new BorderLayout(12, 0));
        windowHeader.setOpaque(false);
        JLabel title = new JLabel("Pâques  /  " + section);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Frame.NAVY);
        JButton back = new JButton("← Employee selection");
        styleActionButton(back, false);
        back.setForeground(Frame.NAVY);
        back.addActionListener(event -> requestBackToSelection(window));
        windowHeader.add(title, BorderLayout.WEST);
        windowHeader.add(back, BorderLayout.EAST);
        content.add(windowHeader, BorderLayout.NORTH);
        content.add(operationPanel, BorderLayout.CENTER);
        window.setContentPane(content);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestBackToSelection(window);
            }

            @Override
            public void windowClosed(WindowEvent event) {
                operationWindows.remove(section, window);
                if ("Kitchen".equals(section)) {
                    detachKitchenListener();
                } else if ("Cashier".equals(section)) {
                    detachCashierListeners();
                }
            }
        });
        window.setMinimumSize(new Dimension(1000, 700));
        window.setSize(1280, 850);
        window.setLocationRelativeTo(mainFrame);
        operationWindows.put(section, window);
        window.setVisible(true);
    }

    private void requestBackToSelection(JFrame sourceWindow) {
        JPasswordField password = new JPasswordField(16);
        int result = JOptionPane.showConfirmDialog(sourceWindow == null ? mainFrame : sourceWindow, password,
                "Enter the password to return to employee selection.", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        if (!WINDOW_SELECTION_PASSWORD.equals(new String(password.getPassword()))) {
            JOptionPane.showMessageDialog(sourceWindow == null ? mainFrame : sourceWindow,
                    "Incorrect password. This workspace remains open.",
                    "Return denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (sourceWindow == null) {
            mainFrame.signOut();
            return;
        }
        sourceWindow.dispose();
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainFrame.toFront();
        mainFrame.requestFocus();
    }

    private void requestSelectionSignOut() {
        JPasswordField password = new JPasswordField(16);
        int result = JOptionPane.showConfirmDialog(mainFrame, password,
                "Enter the password to sign out.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        if (!WINDOW_SELECTION_PASSWORD.equals(new String(password.getPassword()))) {
            JOptionPane.showMessageDialog(mainFrame, "Incorrect password. You remain signed in.",
                    "Sign out denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (JFrame window : operationWindows.values().toArray(new JFrame[0])) {
            window.dispose();
        }
        operationWindows.clear();
        detachLivePanelListeners();
        mainFrame.signOut();
    }

    private String toHex(Color color) {
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void detachLivePanelListeners() {
        detachCashierListeners();
        detachKitchenListener();
    }

    private void detachCashierListeners() {
        if (cashierRefreshListener != null) {
            mainFrame.tableService.removeListener(cashierRefreshListener);
            cashierRefreshListener = null;
        }
        if (cashierOrderListener != null) {
            mainFrame.orderService.removeListener(cashierOrderListener);
            cashierOrderListener = null;
        }
    }

    private void detachKitchenListener() {
        if (kitchenRefreshListener != null) {
            mainFrame.orderService.removeListener(kitchenRefreshListener);
            kitchenRefreshListener = null;
        }
    }

    public JPanel createAdminKitchenPanel() {
        return createKitchenPanel();
    }

    public JPanel createAdminCashierPanel() {
        return createCashierPanel();
    }

    public void detachAdminPanelListeners() {
        detachLivePanelListeners();
    }

    private JPanel createCashierPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 12));
        panel.setBackground(Frame.BACKGROUND);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel title = new JLabel("Cashier checkout");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Frame.NAVY);
        JLabel subtitle = new JLabel("Select a table to review its bill and process checkout.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Frame.MUTED);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(3));
        heading.add(subtitle);

        JPanel ticketContent = new JPanel(new BorderLayout());
        ticketContent.setBackground(Frame.BACKGROUND);
        ticketContent.setPreferredSize(new Dimension(420, 360));
        ticketContent.setMinimumSize(new Dimension(0, 0));
        ticketContent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(12, 12, 12, 12)));
        int[] selectedTable = { 1 };
        java.util.List<JButton> tableButtons = new java.util.ArrayList<>();
        JPanel navigation = createTableNavigation("CASHIER", selectedTable, tableButtons,
                table -> refreshCashierTickets(ticketContent, table));
        JPanel workspace = new JPanel(new BorderLayout(14, 0));
        workspace.setOpaque(false);
        workspace.add(navigation, BorderLayout.WEST);
        JScrollPane scroll = new JScrollPane(ticketContent);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Frame.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        workspace.add(scroll, BorderLayout.CENTER);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(workspace, BorderLayout.CENTER);

        Runnable refresh = () -> refreshCashierTickets(ticketContent, selectedTable[0]);
        cashierRefreshListener = refresh;
        cashierOrderListener = refresh;
        mainFrame.tableService.addListener(cashierRefreshListener);
        mainFrame.orderService.addListener(cashierOrderListener);
        refresh.run();
        return panel;
    }

    private JPanel createTableNavigation(String eyebrow, int[] selectedTable,
            java.util.List<JButton> tableButtons, java.util.function.IntConsumer tableSelected) {
        JPanel navigation = new JPanel(new BorderLayout(0, 10));
        navigation.setBackground(Color.WHITE);
        navigation.setPreferredSize(new Dimension(146, 0));
        navigation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(9, 8, 9, 8)));
        JLabel label = new JLabel(eyebrow);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(Frame.MUTED);
        JPanel tableList = new JPanel(new GridLayout(0, 1, 0, 5));
        tableList.setOpaque(false);
        for (int index = 0; index < TableService.TABLE_COUNT; index++) {
            final int tableNumber = index + 1;
            JButton button = new JButton("Table " + tableNumber);
            styleEmployeeTableButton(button, tableNumber == selectedTable[0]);
            button.addActionListener(event -> {
                selectedTable[0] = tableNumber;
                for (int buttonIndex = 0; buttonIndex < tableButtons.size(); buttonIndex++) {
                    styleEmployeeTableButton(tableButtons.get(buttonIndex),
                            tableNumber == buttonIndex + 1);
                }
                tableSelected.accept(tableNumber);
            });
            tableButtons.add(button);
            tableList.add(button);
        }
        navigation.add(label, BorderLayout.NORTH);
        navigation.add(tableList, BorderLayout.CENTER);
        return navigation;
    }

    private void styleEmployeeTableButton(JButton button, boolean selected) {
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, selected);
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Frame.ACCENT : Frame.BORDER),
                new EmptyBorder(6, 9, 6, 9)));
    }

    private void refreshCashierTickets(JPanel grid, int selectedTable) {
        grid.removeAll();
        int tableNumber = selectedTable;
        boolean checkoutRequested = mainFrame.tableService.isCheckoutRequested(tableNumber);
        boolean occupied = mainFrame.tableService.isOccupied(tableNumber);
        java.util.List<KitchenOrder> orders = mainFrame.orderService.getOrders(tableNumber);
        JPanel card = new JPanel(new BorderLayout(8, 10));
        card.setBackground(Color.WHITE);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(checkoutRequested ? Frame.SUCCESS : Frame.BORDER,
                        checkoutRequested ? 2 : 1),
                new EmptyBorder(14, 14, 14, 14)));

        JPanel cardHeader = new JPanel(new BorderLayout(8, 0));
        cardHeader.setOpaque(false);
        JLabel tableLabel = new JLabel("Table " + tableNumber);
        tableLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        tableLabel.setForeground(Frame.NAVY);
        String stateText = checkoutRequested ? "CHECKOUT REQUESTED" : occupied ? "IN PROGRESS" : "AVAILABLE";
        JLabel state = new JLabel(stateText);
        state.setFont(new Font("SansSerif", Font.BOLD, 10));
        state.setForeground(checkoutRequested || !occupied ? Frame.SUCCESS : Frame.ACCENT_DARK);
        cardHeader.add(tableLabel, BorderLayout.WEST);
        cardHeader.add(state, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);

        JPanel items = new JPanel();
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setOpaque(false);
        if (orders.isEmpty()) {
            JLabel emptyItems = new JLabel("No active order for this table.");
            emptyItems.setForeground(Frame.MUTED);
            items.add(emptyItems);
        } else {
            for (KitchenOrder order : orders) {
                JPanel itemRow = new JPanel(new BorderLayout(6, 0));
                itemRow.setOpaque(false);
                itemRow.setBorder(new EmptyBorder(5, 0, 5, 0));
                JLabel itemName = new JLabel(order.getQuantity() + " × " + order.getItem().getName());
                itemName.setFont(new Font("SansSerif", Font.PLAIN, 12));
                itemName.setForeground(Frame.NAVY);
                BigDecimal amount = BigDecimal.valueOf(order.getItem().getPrice())
                        .multiply(BigDecimal.valueOf(order.getQuantity()));
                JLabel lineTotal = new JLabel("₱" + amount.setScale(2, java.math.RoundingMode.HALF_UP));
                lineTotal.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lineTotal.setForeground(Frame.NAVY);
                itemRow.add(itemName, BorderLayout.CENTER);
                itemRow.add(lineTotal, BorderLayout.EAST);
                items.add(itemRow);
                items.add(new JSeparator());
            }
        }
        card.add(items, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setOpaque(false);
        JLabel total = new JLabel("Total  ₱"
                + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)));
        total.setFont(new Font("SansSerif", Font.BOLD, 16));
        total.setForeground(Frame.NAVY);
        JButton details = new JButton("See Details");
        styleActionButton(details, false);
        details.addActionListener(event -> showCashierDetails(tableNumber));
        details.setEnabled(!orders.isEmpty());
        JButton payment = new JButton(checkoutRequested ? "Complete Payment" : "Awaiting checkout");
        styleActionButton(payment, checkoutRequested);
        payment.setEnabled(checkoutRequested && !orders.isEmpty());
        payment.addActionListener(event -> completeCheckout(tableNumber));
        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        actions.setOpaque(false);
        actions.add(details);
        actions.add(payment);
        footer.add(total, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        grid.add(card, BorderLayout.CENTER);
        grid.revalidate();
        grid.repaint();
    }

    private void showCashierDetails(int tableNumber) {
        JPanel details = new JPanel(new BorderLayout(0, 10));
        details.setBackground(Color.WHITE);
        details.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel heading = new JLabel("Pâques  ·  Table " + tableNumber);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(Frame.NAVY);
        details.add(heading, BorderLayout.NORTH);
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(Color.WHITE);
        for (KitchenOrder order : mainFrame.orderService.getOrders(tableNumber)) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(7, 2, 7, 2));
            JLabel item = new JLabel(order.getQuantity() + " × " + order.getItem().getName());
            item.setForeground(Frame.NAVY);
            JLabel amount = new JLabel("₱" + String.format("%.2f",
                    order.getItem().getPrice() * order.getQuantity()));
            amount.setForeground(Frame.NAVY);
            row.add(item, BorderLayout.CENTER);
            row.add(amount, BorderLayout.EAST);
            rows.add(row);
            rows.add(new JSeparator());
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setPreferredSize(new Dimension(420, 260));
        scroll.setBorder(BorderFactory.createLineBorder(Frame.BORDER));
        details.add(scroll, BorderLayout.CENTER);
        JLabel total = new JLabel("TOTAL  ₱" + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)),
                SwingConstants.RIGHT);
        total.setFont(new Font("SansSerif", Font.BOLD, 17));
        total.setForeground(Frame.ACCENT_DARK);
        details.add(total, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(mainFrame, details, "Bill Details", JOptionPane.PLAIN_MESSAGE);
    }

    private void completeCheckout(int tableNumber) {
        Long customerId = mainFrame.tableService.getCustomerId(tableNumber);
        if (customerId == null) {
            JOptionPane.showMessageDialog(mainFrame,
                    "This table has no linked customer. Assign a customer from Tables before completing payment.",
                    "Customer Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal total = BigDecimal.valueOf(mainFrame.orderService.getTableTotal(tableNumber))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        int choice = JOptionPane.showConfirmDialog(mainFrame,
                "Record payment of ₱" + total.toPlainString() + " for Table " + tableNumber + "?",
                "Complete Checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                try (Connection connection = database.getConnection()) {
                    database.ensureCustomerStatusOptions(connection);
                    InvoiceService.ensureInvoiceSchema(connection);
                    connection.setAutoCommit(false);
                    try {
                        String updateCustomer = "UPDATE customer SET status = 'Completed' WHERE customer_id = ?";
                        try (PreparedStatement statement = connection.prepareStatement(updateCustomer)) {
                            statement.setLong(1, customerId);
                            if (statement.executeUpdate() == 0) {
                                throw new SQLException("The linked customer record no longer exists.");
                            }
                        }
                        String insertTransaction = "INSERT INTO `transaction` "
                                + "(transaction_datetime, customer_id, amount) VALUES (?, ?, ?)";
                        long transactionId;
                        try (PreparedStatement statement = connection.prepareStatement(insertTransaction,
                                Statement.RETURN_GENERATED_KEYS)) {
                            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                            statement.setLong(2, customerId);
                            statement.setBigDecimal(3, total);
                            statement.executeUpdate();
                            try (ResultSet keys = statement.getGeneratedKeys()) {
                                if (!keys.next()) {
                                    throw new SQLException("The database did not return an invoice ID.");
                                }
                                transactionId = keys.getLong(1);
                            }
                        }
                        String finishReceipt = "UPDATE receipt SET transaction_id = ?, "
                                + "chef_received_at = COALESCE(chef_received_at, CURRENT_TIMESTAMP) "
                                + "WHERE customer_id = ? AND table_number = ? AND transaction_id IS NULL";
                        try (PreparedStatement statement = connection.prepareStatement(finishReceipt)) {
                            statement.setLong(1, transactionId);
                            statement.setLong(2, customerId);
                            statement.setInt(3, tableNumber);
                            statement.executeUpdate();
                        }
                        connection.commit();
                    } catch (SQLException e) {
                        connection.rollback();
                        throw e;
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    mainFrame.orderService.completeTableCheckout(tableNumber);
                    mainFrame.tableService.clear(tableNumber);
                    mainFrame.tableService.markCleaning(tableNumber);
                    JOptionPane.showMessageDialog(mainFrame, "Payment recorded successfully.", "Checkout Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showCheckoutError("Recording payment was interrupted.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    showCheckoutError("Unable to complete checkout:\n" + cause.getMessage());
                }
            }
        }.execute();
    }

    private void showCheckoutError(String message) {
        JOptionPane.showMessageDialog(mainFrame, message, "Checkout Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createKitchenPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Frame.BACKGROUND);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel title = new JLabel("Kitchen orders");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Frame.NAVY);
        JLabel subtitle = new JLabel("Live orders grouped by table.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Frame.MUTED);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(title);
        heading.add(Box.createVerticalStrut(3));
        heading.add(subtitle);

        JPanel ticketContent = new JPanel(new GridLayout(0, 2, 12, 12));
        ticketContent.setBackground(Frame.BACKGROUND);
        ticketContent.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane scroll = new JScrollPane(ticketContent);
        scroll.setBorder(BorderFactory.createLineBorder(Frame.BORDER));
        scroll.getViewport().setBackground(Frame.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        Runnable refresh = () -> refreshKitchenTickets(mainFrame, ticketContent);
        kitchenRefreshListener = refresh;
        mainFrame.orderService.addListener(kitchenRefreshListener);
        Timer kitchenClock = new Timer(1000, e -> {
            if (!panel.isDisplayable()) {
                ((Timer) e.getSource()).stop();
            } else {
                refresh.run();
            }
        });
        kitchenClock.start();
        refresh.run();
        return panel;
    }

    private void refreshKitchenTickets(Frame frame, JPanel ticketGrid) {
        ticketGrid.removeAll();
        java.util.List<KitchenOrder> orders = frame.orderService.getKitchenOrders();
        java.util.Map<Integer, java.util.List<KitchenOrder>> ordersByTable = new java.util.TreeMap<>();
        for (KitchenOrder order : orders) {
            ordersByTable.computeIfAbsent(order.getTableNumber(), ignored -> new java.util.ArrayList<>()).add(order);
        }
        Set<Integer> currentlyCompleted = new HashSet<>();
        for (java.util.Map.Entry<Integer, java.util.List<KitchenOrder>> entry : ordersByTable.entrySet()) {
            int tableNumber = entry.getKey();
            java.util.List<KitchenOrder> tableOrders = entry.getValue();
            boolean allComplete = tableOrders.stream()
                    .allMatch(order -> order.isComplete() || order.isReceived());
            if (allComplete) {
                currentlyCompleted.add(tableNumber);
                if (kitchenRefreshed && !completedKitchenTables.contains(tableNumber)) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
            Color cardColor = allComplete ? new Color(226, 242, 229) : Color.WHITE;
            JPanel card = new JPanel(new BorderLayout(8, 10));
            card.setBackground(cardColor);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(allComplete ? Frame.SUCCESS : Frame.BORDER, allComplete ? 2 : 1),
                    new EmptyBorder(14, 14, 14, 14)));

            JPanel ticketHeader = new JPanel(new BorderLayout(8, 6));
            ticketHeader.setOpaque(false);
            JPanel tableBlock = new JPanel();
            tableBlock.setOpaque(false);
            tableBlock.setLayout(new BoxLayout(tableBlock, BoxLayout.Y_AXIS));
            JLabel tableLabel = new JLabel("Table " + tableNumber);
            tableLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            tableLabel.setForeground(Frame.NAVY);
            JLabel itemCount = new JLabel(tableOrders.size()
                    + (tableOrders.size() == 1 ? " item" : " items"));
            itemCount.setFont(new Font("SansSerif", Font.PLAIN, 11));
            itemCount.setForeground(Frame.MUTED);
            tableBlock.add(tableLabel);
            tableBlock.add(Box.createVerticalStrut(2));
            tableBlock.add(itemCount);
            JLabel ticketStatus = new JLabel(allComplete ? "COMPLETED" : "IN PROGRESS");
            ticketStatus.setFont(new Font("SansSerif", Font.BOLD, 11));
            ticketStatus.setForeground(allComplete ? Frame.SUCCESS : Frame.ACCENT_DARK);
            ticketHeader.add(tableBlock, BorderLayout.WEST);
            ticketHeader.add(ticketStatus, BorderLayout.EAST);
            card.add(ticketHeader, BorderLayout.NORTH);

            JPanel itemList = new JPanel();
            itemList.setLayout(new BoxLayout(itemList, BoxLayout.Y_AXIS));
            itemList.setOpaque(false);
            for (KitchenOrder order : tableOrders) {
                String status = order.isPending() ? "Waiting"
                        : order.isReceived() ? "Received"
                                : order.isComplete() ? "Ready to serve"
                                : "Cooking · " + formatRemaining(order.getSecondsRemaining());
                String note = order.getNote() == null || order.getNote().trim().isEmpty()
                        ? "No special instructions" : order.getNote();
                JPanel itemRow = new JPanel(new BorderLayout(8, 3));
                itemRow.setOpaque(false);
                itemRow.setBorder(new EmptyBorder(7, 0, 7, 0));
                JPanel itemDetails = new JPanel();
                itemDetails.setOpaque(false);
                itemDetails.setLayout(new BoxLayout(itemDetails, BoxLayout.Y_AXIS));
                JLabel itemName = new JLabel(order.getQuantity() + " × " + order.getItem().getName());
                itemName.setFont(new Font("SansSerif", Font.BOLD, 13));
                itemName.setForeground(Frame.NAVY);
                JLabel itemNote = new JLabel(note);
                itemNote.setFont(new Font("SansSerif", Font.PLAIN, 11));
                itemNote.setForeground(Frame.MUTED);
                itemDetails.add(itemName);
                itemDetails.add(Box.createVerticalStrut(3));
                itemDetails.add(itemNote);
                JLabel itemStatus = new JLabel(status);
                itemStatus.setFont(new Font("SansSerif", Font.PLAIN, 11));
                itemStatus.setForeground(allComplete ? Frame.SUCCESS : Frame.MUTED);
                itemRow.add(itemDetails, BorderLayout.CENTER);
                itemRow.add(itemStatus, BorderLayout.SOUTH);
                itemList.add(itemRow);
                itemList.add(new JSeparator());
            }
            card.add(itemList, BorderLayout.CENTER);

            ticketGrid.add(card);
        }
        if (ordersByTable.isEmpty()) {
            JLabel empty = new JLabel("No kitchen orders yet.", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 15));
            empty.setForeground(Frame.MUTED);
            ticketGrid.setLayout(new BorderLayout());
            ticketGrid.add(empty, BorderLayout.CENTER);
        } else {
            ticketGrid.setLayout(new GridLayout(0, 2, 12, 12));
        }
        completedKitchenTables.clear();
        completedKitchenTables.addAll(currentlyCompleted);
        kitchenRefreshed = true;
        ticketGrid.revalidate();
        ticketGrid.repaint();
    }

    private String formatRemaining(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private void styleActionButton(JButton button, boolean accent) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(8, 12, 8, 12)));
        Frame.styleButtonState(button, false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
    }
}
