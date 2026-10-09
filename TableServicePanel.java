import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Shared table board for staff roles. */
public class TableServicePanel extends JPanel {
    private final Frame mainFrame;
    private final Runnable backAction;
    private final List<TableCard> cards = new ArrayList<>();
    private final Runnable tableListener = this::refresh;
    private boolean listening;

    public TableServicePanel(Frame mainFrame) {
        this(mainFrame, null);
    }

    public TableServicePanel(Frame mainFrame, Runnable backAction) {
        this.mainFrame = mainFrame;
        this.backAction = backAction;
        setLayout(new BorderLayout(10, 15));
        setBackground(Frame.BACKGROUND);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Table service");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Frame.NAVY);
        JLabel subtitle = new JLabel("Live availability and table reservations");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Frame.MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.WEST);
        if (backAction != null) {
            JButton backButton = new JButton("← Back to Reservations");
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            Frame.styleButtonState(backButton, false);
            backButton.setFont(new Font("SansSerif", Font.BOLD, 12));
            backButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.ACCENT), new EmptyBorder(7, 11, 7, 11)));
            backButton.addActionListener(event -> backAction.run());
            header.add(backButton, BorderLayout.EAST);
        }
        JLabel legend = new JLabel("Available  ·  Reserved  ·  In progress  ·  Cleaning  ·  Checkout requested");
        legend.setFont(new Font("SansSerif", Font.PLAIN, 12));
        legend.setForeground(Frame.MUTED);
        header.add(legend, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setBackground(Frame.BACKGROUND);
        grid.setBorder(new EmptyBorder(4, 4, 4, 4));
        for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
            TableCard card = new TableCard(table);
            cards.add(card);
            grid.add(card);
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Frame.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        add(scroll, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!listening) {
            mainFrame.tableService.addListener(tableListener);
            listening = true;
        }
    }

    @Override
    public void removeNotify() {
        if (listening) {
            mainFrame.tableService.removeListener(tableListener);
            listening = false;
        }
        super.removeNotify();
    }

    private void refresh() {
        cards.forEach(TableCard::refresh);
    }

    private final class TableCard extends JPanel {
        private final int tableNumber;
        private final JLabel stateLabel = new JLabel();
        private final JLabel customerDetails = new JLabel("No customer selected.");
        private final JTextField customerIdField = new JTextField();
        private final JButton findButton = new JButton("Find");
        private final JButton openButton = new JButton("Open");
        private final JButton closeButton = new JButton("Close");
        private final JButton cleaningButton = new JButton("Mark Cleaning");
        private final JButton startButton = new JButton("Start Table");
        private CustomerDetails selectedCustomer;
        private boolean lookupInProgress;
        private boolean startInProgress;

        private TableCard(int tableNumber) {
            this.tableNumber = tableNumber;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Frame.CARD);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Frame.BORDER),
                    new EmptyBorder(12, 12, 12, 12)));

            JPanel heading = new JPanel(new BorderLayout(6, 6));
            heading.setOpaque(false);
            JLabel tableLabel = new JLabel("Table " + tableNumber + " · "
                    + TableService.capacityFor(tableNumber) + " people");
            tableLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            stateLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            heading.add(tableLabel, BorderLayout.WEST);
            heading.add(stateLabel, BorderLayout.EAST);
            add(heading);
            add(Box.createVerticalStrut(8));

            JPanel tableActions = new JPanel(new GridLayout(0, 2, 6, 6));
            tableActions.setOpaque(false);
            tableActions.add(openButton);
            tableActions.add(closeButton);
            tableActions.add(cleaningButton);
            tableActions.add(new JLabel());
            styleTableButton(openButton, true);
            styleTableButton(closeButton, false);
            styleTableButton(cleaningButton, false);
            styleTableButton(findButton, true);
            styleTableButton(startButton, false);
            add(tableActions);
            add(Box.createVerticalStrut(6));

            JLabel customerLabel = new JLabel("Customer ID");
            customerLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            customerLabel.setForeground(Frame.NAVY);

            customerIdField.setColumns(12);
            customerIdField.setPreferredSize(new Dimension(105, 32));
            customerIdField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(5, 7, 5, 7)));
            JPanel lookup = new JPanel(new GridBagLayout());
            lookup.setOpaque(false);
            GridBagConstraints lookupConstraints = new GridBagConstraints();
            lookupConstraints.gridy = 0;
            lookupConstraints.insets = new Insets(0, 0, 0, 6);
            lookupConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
            lookupConstraints.gridx = 0;
            lookup.add(customerLabel, lookupConstraints);
            lookupConstraints.gridx = 1;
            lookupConstraints.weightx = 1;
            lookupConstraints.fill = GridBagConstraints.HORIZONTAL;
            lookup.add(customerIdField, lookupConstraints);
            lookupConstraints.gridx = 2;
            lookupConstraints.weightx = 0;
            lookupConstraints.fill = GridBagConstraints.NONE;
            lookupConstraints.insets = new Insets(0, 0, 0, 0);
            lookup.add(findButton, lookupConstraints);
            add(lookup);
            add(Box.createVerticalStrut(6));

            customerDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
            customerDetails.setVerticalAlignment(SwingConstants.TOP);
            customerDetails.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(7, 8, 7, 8)));
            customerDetails.setOpaque(true);
            customerDetails.setBackground(Color.WHITE);
            add(customerDetails);
            add(Box.createVerticalStrut(6));

            JPanel startRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            startRow.setOpaque(false);
            startRow.add(startButton);
            add(startRow);

            findButton.addActionListener(e -> findCustomer());
            startButton.addActionListener(e -> startTable());
            openButton.addActionListener(e -> openTable());
            closeButton.addActionListener(e -> closeTable());
            cleaningButton.addActionListener(e -> {
                if (mainFrame.tableService.getStatus(tableNumber) == TableService.Status.CLEANING) {
                    mainFrame.tableService.markAvailable(tableNumber);
                } else {
                    mainFrame.tableService.markCleaning(tableNumber);
                }
            });

            Long assignedId = mainFrame.tableService.getCustomerId(tableNumber);
            if (assignedId != null) {
                customerIdField.setText(String.valueOf(assignedId));
                findCustomer();
            }
            refresh();
        }

        private void styleTableButton(JButton button, boolean primary) {
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            Frame.styleButtonState(button, false);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.ACCENT),
                    new EmptyBorder(6, 9, 6, 9)));
            button.setFont(new Font("SansSerif", Font.BOLD, 11));
        }

        private void findCustomer() {
            String text = customerIdField.getText().trim();
            long customerId;
            try {
                customerId = Long.parseLong(text);
                if (customerId <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                selectedCustomer = null;
                customerDetails.setText("Enter a valid customer ID.");
                clearUnoccupiedCustomer();
                refresh();
                return;
            }

            lookupInProgress = true;
            selectedCustomer = null;
            customerDetails.setText("Looking up customer...");
            refresh();
            new SwingWorker<CustomerDetails, Void>() {
                @Override
                protected CustomerDetails doInBackground() throws SQLException {
                    String sql = "SELECT customer_id, first_name, last_name, email, phone_number, status, scheduled "
                            + "FROM customer WHERE customer_id = ?";
                    try (Connection connection = database.getConnection();
                            PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, customerId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                return null;
                            }
                            return new CustomerDetails(result.getLong("customer_id"), result.getString("first_name"),
                                    result.getString("last_name"), result.getString("email"),
                                    result.getString("phone_number"), result.getString("status"),
                                    result.getTimestamp("scheduled"));
                        }
                    }
                }

                @Override
                protected void done() {
                    lookupInProgress = false;
                    try {
                        selectedCustomer = get();
                        if (selectedCustomer == null) {
                            customerDetails.setText("No customer found for that ID.");
                            clearUnoccupiedCustomer();
                        } else {
                            Long assignedId = mainFrame.tableService.getCustomerId(tableNumber);
                            if (assignedId != null && assignedId.longValue() != selectedCustomer.id) {
                                customerDetails.setText("This table is assigned to customer " + assignedId + ".");
                                selectedCustomer = null;
                                refresh();
                                return;
                            }
                            customerDetails.setText(selectedCustomer.toHtml());
                            if (!mainFrame.tableService.isOccupied(tableNumber)) {
                                mainFrame.tableService.setCustomer(tableNumber, selectedCustomer.id,
                                        selectedCustomer.status);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        clearUnoccupiedCustomer();
                        showLookupError("Customer lookup was interrupted.");
                    } catch (ExecutionException e) {
                        clearUnoccupiedCustomer();
                        Throwable cause = e.getCause();
                        showLookupError("Unable to load customer details:\n" + cause.getMessage());
                    }
                    refresh();
                }
            }.execute();
        }

        private void clearUnoccupiedCustomer() {
            if (!mainFrame.tableService.isOccupied(tableNumber)) {
                mainFrame.tableService.setCustomer(tableNumber, null, null);
            }
        }

        private void showLookupError(String message) {
            customerDetails.setText("Unable to load customer details.");
            JOptionPane.showMessageDialog(mainFrame, message, "Customer Lookup Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        private void startTable() {
            if (selectedCustomer == null
                    || !String.valueOf(selectedCustomer.id).equals(customerIdField.getText().trim())) {
                JOptionPane.showMessageDialog(mainFrame, "Find a valid customer before starting this table.",
                        "Customer Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if ("Cancelled".equalsIgnoreCase(selectedCustomer.status)
                    || "Completed".equalsIgnoreCase(selectedCustomer.status)) {
                JOptionPane.showMessageDialog(mainFrame, "A " + selectedCustomer.status.toLowerCase()
                        + " customer cannot start a table.", "Customer Cannot Proceed",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (lookupInProgress || startInProgress) {
                return;
            }
            if (mainFrame.tableService.isOccupied(tableNumber)) {
                JOptionPane.showMessageDialog(mainFrame, "This table is already in progress.",
                        "Table Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            final CustomerDetails customerToStart = selectedCustomer;
            final long customerId = customerToStart.id;
            if (!mainFrame.tableService.isAvailableTo(tableNumber, customerId)) {
                JOptionPane.showMessageDialog(mainFrame, "This table is reserved for someone else or needs cleaning.",
                        "Table Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Long assignedCustomerId = mainFrame.tableService.getCustomerId(tableNumber);
            if (assignedCustomerId != null && assignedCustomerId.longValue() != customerId) {
                JOptionPane.showMessageDialog(mainFrame, "This table is assigned to another customer.",
                        "Table Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            startInProgress = true;
            refresh();
            startButton.setEnabled(false);
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
                            JOptionPane.showMessageDialog(mainFrame,
                                    "This customer cannot start a table. Current status: " + blockedStatus + ".",
                                    "Customer Cannot Proceed", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        if (!mainFrame.tableService.occupy(tableNumber, customerId)) {
                            JOptionPane.showMessageDialog(mainFrame, "This table is already in progress.",
                                    "Table Unavailable", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        customerToStart.status = "In Progress";
                        mainFrame.tableService.setCustomer(tableNumber, customerId, "In Progress");
                        if (selectedCustomer == customerToStart) {
                            customerDetails.setText(customerToStart.toHtml());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        JOptionPane.showMessageDialog(mainFrame, "Starting the table was interrupted.",
                                "Table Start Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        JOptionPane.showMessageDialog(mainFrame,
                                "Unable to start this table:\n" + cause.getMessage(), "Table Start Error",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        startInProgress = false;
                        refresh();
                    }
                }
            }.execute();
        }

        private void openTable() {
            if (selectedCustomer != null) {
                startTable();
                return;
            }
            if (!mainFrame.tableService.occupy(tableNumber)) {
                JOptionPane.showMessageDialog(mainFrame, "This table is reserved or needs cleaning.",
                        "Table Unavailable", JOptionPane.WARNING_MESSAGE);
            }
        }

        private void closeTable() {
            Long customerId = mainFrame.tableService.getCustomerId(tableNumber);
            boolean occupied = mainFrame.tableService.isOccupied(tableNumber);
            if (occupied && !mainFrame.orderService.getOrders(tableNumber).isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame,
                        "This table has active orders. Complete checkout before closing it.", "Checkout Required",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            closeButton.setEnabled(false);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws SQLException {
                    mainFrame.orderService.clearTableOrders(tableNumber);
                    if (occupied && customerId != null) {
                        database.clearCustomerInProgress(customerId);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        mainFrame.tableService.clear(tableNumber);
                        if (occupied) {
                            mainFrame.tableService.markCleaning(tableNumber);
                        }
                        selectedCustomer = null;
                        customerIdField.setText("");
                        customerDetails.setText("No customer selected.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        showCloseError("Closing the table was interrupted.");
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        showCloseError("Unable to close the table:\n" + cause.getMessage());
                    } finally {
                        refresh();
                    }
                }
            }.execute();
        }

        private void showCloseError(String message) {
            JOptionPane.showMessageDialog(mainFrame, message, "Unable to Close Table",
                    JOptionPane.ERROR_MESSAGE);
        }

        private void refresh() {
            boolean occupied = mainFrame.tableService.isOccupied(tableNumber);
            boolean checkoutRequested = mainFrame.tableService.isCheckoutRequested(tableNumber);
            String state;
            Color color;
            if (checkoutRequested) {
                state = "Status: Checkout requested";
                color = Frame.SUCCESS;
            } else if (occupied) {
                state = "Status: In progress";
                color = Frame.ACCENT_DARK;
            } else if ("Reserved".equalsIgnoreCase(mainFrame.tableService.getCustomerStatus(tableNumber))) {
                state = "Status: Reserved";
                color = Frame.ACCENT_DARK;
            } else if (mainFrame.tableService.getStatus(tableNumber) == TableService.Status.CLEANING) {
                state = "Status: Cleaning";
                color = Frame.MUTED;
            } else {
                state = "Status: Available";
                color = Frame.SUCCESS;
            }
            stateLabel.setText(state);
            stateLabel.setForeground(color);
            boolean cleaning = mainFrame.tableService.getStatus(tableNumber) == TableService.Status.CLEANING;
            openButton.setEnabled(mainFrame.tableService.getStatus(tableNumber) == TableService.Status.AVAILABLE
                    && !checkoutRequested && !lookupInProgress && !startInProgress);
            findButton.setEnabled(!startInProgress);
            closeButton.setEnabled(occupied || mainFrame.tableService.getCustomerId(tableNumber) != null);
            cleaningButton.setText(cleaning ? "Mark Available" : "Mark Cleaning");
            cleaningButton.setEnabled(cleaning || (!occupied && !checkoutRequested
                    && mainFrame.tableService.getStatus(tableNumber) != TableService.Status.RESERVED));
            startButton.setEnabled(!occupied && !cleaning && !checkoutRequested && selectedCustomer != null
                    && !lookupInProgress && !startInProgress && !"Cancelled".equalsIgnoreCase(selectedCustomer.status)
                    && !"Completed".equalsIgnoreCase(selectedCustomer.status));
        }
    }

    private static final class CustomerDetails {
        private final long id;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String phone;
        private String status;
        private final Timestamp scheduled;

        private CustomerDetails(long id, String firstName, String lastName, String email, String phone, String status,
                Timestamp scheduled) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.status = status;
            this.scheduled = scheduled;
        }

        private String toHtml() {
            String scheduledText = scheduled == null ? "Not scheduled" : scheduled.toString();
            return "<html>" + escape(firstName) + " " + escape(lastName) + "<br>" + escape(email) + "<br>"
                    + escape(phone) + "<br>" + (status == null ? "Status: None" : "Status: " + escape(status))
                    + "<br>" + escape(scheduledText) + "</html>";
        }

        private static String escape(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
        }
    }
}
