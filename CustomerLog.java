import java.awt.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableCellRenderer;

public class CustomerLog extends JPanel {
    private static final String CONFIRMED = "Confirmed";
    private static final String RESERVED = "Reserved";
    private static final String CANCELLED = "Cancelled";

    private final Frame mainFrame;
    private final boolean showAddCustomerAction;
    private final boolean frontDeskCustomerDirectory;
    private final JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
    private final List<JButton> categoryButtons = new ArrayList<>();
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JLabel recordCount;
    private TableRowSorter<DefaultTableModel> tableSorter;
    private String selectedCategory = "Customer List";
    private LocalDate selectedReservationDate = LocalDate.now();
    private java.util.function.Consumer<LocalDate> reservationChangedListener;
    private Runnable backNavigationListener;
    private Component dialogParent;
    private int loadGeneration;
    private static final Color DIRECTORY_ACCENT = Frame.ACCENT_DARK;
    private static final Color DIRECTORY_LIGHT = Frame.BACKGROUND;

    public CustomerLog(Frame mainFrame) {
        this(mainFrame, true, false);
    }

    public CustomerLog(Frame mainFrame, boolean showAddCustomerAction) {
        this(mainFrame, showAddCustomerAction, false);
    }

    public CustomerLog(Frame mainFrame, boolean showAddCustomerAction, boolean frontDeskCustomerDirectory) {
        this.mainFrame = mainFrame;
        this.showAddCustomerAction = showAddCustomerAction;
        this.frontDeskCustomerDirectory = frontDeskCustomerDirectory;
        setLayout(new BorderLayout());
        setBackground(DIRECTORY_LIGHT);
        setBorder(new EmptyBorder(18, 20, 20, 20));
        JPanel tabs = createTabs();
        if (showAddCustomerAction || frontDeskCustomerDirectory) {
            add(tabs, BorderLayout.NORTH);
        }
        add(contentPanel, BorderLayout.CENTER);
        showCategory(frontDeskCustomerDirectory ? CONFIRMED : "Customer List");
    }

    void setSelectedReservationDate(LocalDate date) {
        if (date != null) {
            selectedReservationDate = date;
        }
    }

    void setReservationChangedListener(java.util.function.Consumer<LocalDate> listener) {
        reservationChangedListener = listener;
    }

    void setBackNavigationListener(Runnable listener) {
        backNavigationListener = listener;
    }

    void setDialogParent(Component parent) {
        dialogParent = parent;
    }

    private void openSelectedInvoice() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showMessage("Select a completed customer invoice first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        Object value = tableModel.getValueAt(modelRow, tableModel.getColumnCount() - 1);
        if (!(value instanceof InvoiceService.Invoice)) {
            showMessage("The selected row has no invoice details.");
            return;
        }
        InvoiceService.showDetails(mainFrame, (InvoiceService.Invoice) value);
    }

    private JPanel createTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setBackground(Frame.CARD);
        tabs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(5, 6, 5, 6)));
        categoryButtons.clear();
        String[] categories = frontDeskCustomerDirectory
                ? new String[] { CONFIRMED, RESERVED, "In Progress", CANCELLED }
                : showAddCustomerAction
                        ? new String[] { "Customer List", "Completed", CANCELLED }
                        : new String[] { "Customer List" };
        for (String category : categories) {
            JButton button = new JButton(category);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            categoryButtons.add(button);
            button.addActionListener(e -> showCategory(category));
            tabs.add(button);
        }
        return tabs;
    }

    private void showCategory(String category) {
        selectedCategory = category;
        for (JButton button : categoryButtons) {
            boolean selected = category.equals(button.getText());
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            Frame.styleButtonState(button, selected);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? Frame.ACCENT : Frame.BORDER),
                    new EmptyBorder(8, 12, 8, 12)));
        }
        loadGeneration++;
        contentPanel.removeAll();

        JPanel directoryHeader = new JPanel(new BorderLayout(12, 8));
        directoryHeader.setOpaque(false);
        directoryHeader.setBorder(new EmptyBorder(8, 0, 12, 0));
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        JLabel heading = new JLabel(category);
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        heading.setForeground(DIRECTORY_ACCENT);
        JLabel description = new JLabel("Manage customer records and restaurant visits");
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(Frame.MUTED);
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(description);
        directoryHeader.add(titleBlock, BorderLayout.WEST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);
        if ("Customer List".equals(category) || frontDeskCustomerDirectory) {
            if (!showAddCustomerAction) {
                JButton backButton = createDirectoryButton("← Back to Front Desk", false);
                backButton.addActionListener(event -> {
                    if (backNavigationListener != null) {
                        backNavigationListener.run();
                    }
                });
                toolbar.add(backButton);
            }
            if (frontDeskCustomerDirectory) {
                recordCount = new JLabel("Loading customers...");
                recordCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
                recordCount.setForeground(Frame.MUTED);
                toolbar.add(recordCount);
            } else {
                recordCount = new JLabel("0 customers");
                recordCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
                recordCount.setForeground(Frame.MUTED);
                toolbar.add(recordCount);
                searchField = new JTextField(14);
                searchField.setToolTipText("Search customers");
                statusFilter = new JComboBox<>(new String[] { "All statuses", CONFIRMED, RESERVED, "In Progress" });
                styleFilter(statusFilter);
                JButton searchButton = createDirectoryButton("Search", false);
                searchButton.addActionListener(e -> applyCustomerFilters());
                searchField.addActionListener(e -> applyCustomerFilters());
                statusFilter.addActionListener(e -> applyCustomerFilters());
                toolbar.add(searchField);
                toolbar.add(statusFilter);
                toolbar.add(searchButton);
            }
            if (showAddCustomerAction) {
                JButton addButton = createDirectoryButton("+ Add customer", true);
                addButton.addActionListener(e -> showAddCustomerDialog());
                toolbar.add(addButton);
            }
        } else if ("Completed".equals(category)) {
            JButton invoiceButton = createDirectoryButton("View Receipt", true);
            invoiceButton.addActionListener(e -> openSelectedInvoice());
            toolbar.add(invoiceButton);
        }
        directoryHeader.add(toolbar, BorderLayout.EAST);
        contentPanel.add(directoryHeader, BorderLayout.NORTH);

        String[] columns = columnsFor(category);
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return "Customer List".equals(selectedCategory) && column == getColumnCount() - 1;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(38);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setGridColor(Frame.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(244, 240, 231));
        table.getTableHeader().setForeground(DIRECTORY_ACCENT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        if ("Completed".equals(category)) {
            table.setAutoCreateRowSorter(true);
            table.removeColumn(table.getColumnModel().getColumn(tableModel.getColumnCount() - 1));
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                        openSelectedInvoice();
                    }
                }
            });
        }
        if ("Customer List".equals(category) || frontDeskCustomerDirectory) {
            tableSorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(tableSorter);
            if ("Customer List".equals(category)) {
                table.getColumnModel().getColumn(tableModel.getColumnCount() - 1)
                        .setCellRenderer(new EditButtonRenderer());
                table.getColumnModel().getColumn(tableModel.getColumnCount() - 1)
                        .setCellEditor(new EditButtonEditor());
                int statusColumn = 6;
                table.getColumnModel().getColumn(statusColumn).setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                            boolean focused, int row, int column) {
                        JLabel label = (JLabel) super.getTableCellRendererComponent(source, value, selected, focused,
                                row, column);
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setForeground("In Progress".equalsIgnoreCase(String.valueOf(value))
                                ? new Color(35, 130, 80)
                                : DIRECTORY_ACCENT);
                        return label;
                    }
                });
            }
        }
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(Frame.BORDER));
        contentPanel.add(tableScroll, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();

        loadCategory(category, ++loadGeneration);
    }

    private String[] columnsFor(String category) {
        if ("Customer List".equals(category)) {
            return new String[] { "Customer ID", "First Name", "Last Name", "Email", "Phone Number", "Birthdate",
                    "Status", "Scheduled", "Actions" };
        }
        if (frontDeskCustomerDirectory) {
            return new String[] { "Customer ID", "First Name", "Last Name", "Email", "Phone Number", "Birthdate",
                    "Scheduled", "Status" };
        }
        if ("Completed".equals(category)) {
            return new String[] { "Transaction ID", "Customer ID", "Amount", "Transaction Date/Time",
                    "Invoice Data" };
        }
        if (RESERVED.equals(category)) {
            return new String[] { "Customer ID", "First Name", "Last Name", "Email", "Phone Number", "Birthdate",
                    "Scheduled", "Time" };
        }
        if (CANCELLED.equals(category)) {
            return new String[] { "Customer ID", "First Name", "Last Name", "Email", "Phone Number", "Birthdate",
                    "Scheduled", "Status" };
        }
        return new String[] { "Customer ID", "First Name", "Last Name", "Email", "Phone Number", "Birthdate",
                "Scheduled" };
    }

    private void loadCategory(String category, int generation) {
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws SQLException {
                List<Object[]> rows = new ArrayList<>();
                try (Connection connection = database.getConnection()) {
                    ensureCancelledStatus(connection);
                    if ("Customer List".equals(category)) {
                        String sql = "SELECT customer_id, first_name, last_name, email, phone_number, "
                                + "birthdate, status, scheduled FROM customer "
                                + "WHERE status IS NULL OR status NOT IN ('Completed', 'Cancelled') "
                                + "ORDER BY customer_id";
                        try (PreparedStatement statement = connection.prepareStatement(sql);
                                ResultSet results = statement.executeQuery()) {
                            while (results.next()) {
                                rows.add(new Object[] { results.getObject("customer_id"),
                                        results.getString("first_name"), results.getString("last_name"),
                                        results.getString("email"), results.getString("phone_number"),
                                        results.getDate("birthdate"), results.getString("status"),
                                        results.getTimestamp("scheduled"), "" });
                            }
                        }
                    } else if ("Completed".equals(category)) {
                        for (InvoiceService.Invoice invoice : InvoiceService.loadCompletedCustomerInvoices()) {
                            String transactionDate = invoice.created == null ? ""
                                    : invoice.created.toLocalDateTime()
                                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            String amount = invoice.amount == null ? ""
                                    : "₱" + invoice.amount.setScale(2, java.math.RoundingMode.HALF_UP);
                            rows.add(new Object[] { invoice.transactionId == null ? "" : invoice.transactionId,
                                    invoice.customerId, amount, transactionDate, invoice });
                        }
                    } else {
                        String sql = "SELECT customer_id, first_name, last_name, email, phone_number, birthdate, "
                                + "scheduled, status FROM customer "
                                + "WHERE status = ? ORDER BY scheduled, customer_id";
                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            statement.setString(1, category);
                            try (ResultSet results = statement.executeQuery()) {
                                while (results.next()) {
                                    List<Object> row = new ArrayList<>();
                                    row.add(results.getObject("customer_id"));
                                    row.add(results.getString("first_name"));
                                    row.add(results.getString("last_name"));
                                    row.add(results.getString("email"));
                                    row.add(results.getString("phone_number"));
                                    row.add(results.getDate("birthdate"));
                                    Timestamp scheduled = results.getTimestamp("scheduled");
                                    row.add(scheduled == null ? null
                                            : Date.valueOf(scheduled.toLocalDateTime().toLocalDate()));
                                    if (frontDeskCustomerDirectory) {
                                        row.add(results.getString("status"));
                                    } else if (RESERVED.equals(category)) {
                                        row.add(scheduled == null ? null
                                                : Time.valueOf(scheduled.toLocalDateTime().toLocalTime()));
                                    } else if (CANCELLED.equals(category)) {
                                        row.add(results.getString("status"));
                                    }
                                    rows.add(row.toArray());
                                }
                            }
                        }
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                if (generation != loadGeneration || !category.equals(selectedCategory)) {
                    return;
                }
                try {
                    List<Object[]> rows = get();
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                    if (("Customer List".equals(category) || frontDeskCustomerDirectory) && recordCount != null) {
                        recordCount.setText(rows.size() + (rows.size() == 1 ? " customer" : " customers"));
                        if ("Customer List".equals(category)) {
                            applyCustomerFilters();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Loading was interrupted.", e);
                } catch (ExecutionException e) {
                    showError("Unable to load " + category.toLowerCase() + " records.", e.getCause());
                }
            }
        }.execute();
    }

    private JButton createDirectoryButton(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(8, 12, 8, 12)));
        return button;
    }

    private void styleFilter(JComboBox<String> filter) {
        filter.setBackground(Color.WHITE);
        filter.setForeground(DIRECTORY_ACCENT);
        filter.setPreferredSize(new Dimension(125, 32));
    }

    private void applyCustomerFilters() {
        if (tableSorter == null || searchField == null || statusFilter == null) {
            return;
        }
        String search = searchField.getText().trim().toLowerCase();
        String status = String.valueOf(statusFilter.getSelectedItem());
        tableSorter.setRowFilter(new javax.swing.RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean matchesSearch = search.isEmpty();
                for (int column = 0; column < entry.getValueCount() - 1 && !matchesSearch; column++) {
                    Object value = entry.getValue(column);
                    matchesSearch = value != null && value.toString().toLowerCase().contains(search);
                }
                boolean matchesStatus = "All statuses".equals(status)
                        || status.equalsIgnoreCase(String.valueOf(entry.getValue(6)));
                return matchesSearch && matchesStatus;
            }
        });
    }

    private void ensureCancelledStatus(Connection connection) throws SQLException {
        database.ensureCustomerStatusOptions(connection);
    }

    void showAddCustomerDialog() {
        showCustomerDialog(null);
    }

    private void editCustomer(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        Object value = tableModel.getValueAt(row, 0);
        if (!(value instanceof Number)) {
            showMessage("The selected customer has an invalid ID.");
            return;
        }
        showCustomerDialog(((Number) value).longValue());
    }

    private void showCustomerDialog(Long customerId) {
        JTextField firstName = new JTextField();
        JTextField lastName = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        DateFields scheduled = new DateFields(selectedReservationDate, LocalDate.now().getYear() + 10);
        JComboBox<String> hour = new JComboBox<>(numberOptions(0, 23));
        JComboBox<String> minute = new JComboBox<>(numberOptions(0, 59));
        JComboBox<String> status = new JComboBox<>(
                new String[] { CONFIRMED, "In Progress", "Completed", RESERVED, CANCELLED });

        if (customerId != null) {
            String sql = "SELECT first_name, last_name, email, phone_number, scheduled, status "
                    + "FROM customer WHERE customer_id = ?";
            try (Connection connection = database.getConnection();
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, customerId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        showMessage("This customer no longer exists.");
                        return;
                    }
                    firstName.setText(result.getString("first_name"));
                    lastName.setText(result.getString("last_name"));
                    email.setText(result.getString("email"));
                    phone.setText(result.getString("phone_number"));
                    Timestamp scheduledDateTime = result.getTimestamp("scheduled");
                    if (scheduledDateTime != null) {
                        LocalDateTime scheduledValue = scheduledDateTime.toLocalDateTime();
                        scheduled.setDate(scheduledValue.toLocalDate());
                        hour.setSelectedItem(String.format("%02d", scheduledValue.getHour()));
                        minute.setSelectedItem(String.format("%02d", scheduledValue.getMinute()));
                    }
                    status.setSelectedItem(result.getString("status"));
                }
            } catch (SQLException e) {
                showError("Unable to load this customer.", e);
                return;
            }
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel customerSection = new JLabel("CUSTOMER DETAILS");
        customerSection.setFont(new Font("SansSerif", Font.BOLD, 11));
        customerSection.setForeground(Frame.ACCENT_DARK);
        form.add(customerSection, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        styleFormField(firstName);
        styleFormField(lastName);
        styleFormField(email);
        styleFormField(phone);
        addFormField(form, gbc, "First name", firstName);
        addFormField(form, gbc, "Last name", lastName);
        addFormField(form, gbc, "Email address", email);
        addFormField(form, gbc, "Phone number", phone);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 5, 6);
        JLabel reservationSection = new JLabel("RESERVATION DETAILS");
        reservationSection.setFont(new Font("SansSerif", Font.BOLD, 11));
        reservationSection.setForeground(Frame.ACCENT_DARK);
        form.add(reservationSection, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 6, 5, 6);
        addFormField(form, gbc, "Date", scheduled);
        JPanel timeFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        timeFields.setOpaque(false);
        hour.setPreferredSize(new Dimension(76, 34));
        minute.setPreferredSize(new Dimension(76, 34));
        timeFields.add(hour);
        timeFields.add(new JLabel(":"));
        timeFields.add(minute);
        addFormField(form, gbc, "Time", timeFields);
        addFormField(form, gbc, "Status", status);

        JPanel dialogContent = new JPanel(new BorderLayout(0, 8));
        dialogContent.setBackground(Color.WHITE);
        JLabel dialogHeading = new JLabel(customerId == null ? "New reservation" : "Edit customer");
        dialogHeading.setFont(new Font("SansSerif", Font.BOLD, 20));
        dialogHeading.setForeground(Frame.NAVY);
        dialogHeading.setBorder(new EmptyBorder(14, 18, 0, 18));
        dialogContent.add(dialogHeading, BorderLayout.NORTH);
        dialogContent.add(form, BorderLayout.CENTER);
        Component owner = dialogParent == null ? mainFrame : dialogParent;
        int result = JOptionPane.showConfirmDialog(owner, dialogContent,
                customerId == null ? "New reservation" : "Edit customer",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String first = firstName.getText().trim();
        String last = lastName.getText().trim();
        String emailValue = email.getText().trim();
        String phoneValue = phone.getText().trim();
        if (first.isEmpty() || last.isEmpty() || emailValue.isEmpty() || phoneValue.isEmpty()) {
            showMessage("Please fill in all customer details.");
            return;
        }
        if (first.length() > 25 || last.length() > 25 || emailValue.length() > 50 || phoneValue.length() > 12) {
            showMessage("Names must be 25 characters or fewer, email 50 or fewer, and phone number 12 or fewer.");
            return;
        }

        LocalDate scheduledValue = scheduled.getDate();
        LocalTime scheduledTime = LocalTime.of(Integer.parseInt((String) hour.getSelectedItem()),
                Integer.parseInt((String) minute.getSelectedItem()));
        Timestamp scheduledDateTime = Timestamp.valueOf(LocalDateTime.of(scheduledValue, scheduledTime));
        String statusValue = (String) status.getSelectedItem();
        String sql = customerId == null
                ? "INSERT INTO customer "
                        + "(first_name, last_name, email, phone_number, birthdate, scheduled, status) "
                        + "VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?)"
                : "UPDATE customer SET first_name = ?, last_name = ?, email = ?, phone_number = ?, "
                        + "scheduled = ?, status = ? WHERE customer_id = ?";
        runUpdate(sql, statement -> {
            statement.setString(1, first);
            statement.setString(2, last);
            statement.setString(3, emailValue);
            statement.setString(4, phoneValue);
            if (customerId != null) {
                statement.setTimestamp(5, scheduledDateTime);
                statement.setString(6, statusValue);
                statement.setLong(7, customerId);
            } else {
                statement.setTimestamp(5, scheduledDateTime);
                statement.setString(6, statusValue);
            }
        }, "Customer List", scheduledValue);
    }

    private void addFormField(JPanel form, GridBagConstraints gbc, String label, Component field) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        JLabel caption = new JLabel(label);
        caption.setFont(new Font("SansSerif", Font.BOLD, 12));
        caption.setForeground(Frame.NAVY);
        form.add(caption, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
        gbc.gridy++;
    }

    private void styleFormField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(220, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(6, 8, 6, 8)));
    }

    private final class EditButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton editButton = new JButton("Edit");

        private EditButtonRenderer() {
            super(new FlowLayout(FlowLayout.CENTER, 0, 2));
            editButton.setForeground(DIRECTORY_ACCENT);
            editButton.setFocusPainted(false);
            add(editButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable source, Object value, boolean selected, boolean focused,
                int row, int column) {
            setBackground(selected ? source.getSelectionBackground() : source.getBackground());
            return this;
        }
    }

    private final class EditButtonEditor extends DefaultCellEditor {
        private final JButton editButton = new JButton("Edit");
        private int editingRow;

        private EditButtonEditor() {
            super(new JTextField());
            editButton.addActionListener(event -> {
                fireEditingStopped();
                editCustomer(editingRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable source, Object value, boolean selected, int row,
                int column) {
            editingRow = source.convertRowIndexToModel(row);
            return editButton;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private void runUpdate(String sql, SqlBinder binder, String category, LocalDate scheduledValue) {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws SQLException {
                try (Connection connection = database.getConnection()) {
                    ensureCancelledStatus(connection);
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        binder.bind(statement);
                        return statement.executeUpdate();
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    int updated = get();
                    if (updated == 0) {
                        showMessage("The customer record was not changed. Refresh and try again.");
                    }
                    if (category.equals(selectedCategory)) {
                        showCategory(category);
                    }
                    selectedReservationDate = scheduledValue;
                    if (reservationChangedListener != null) {
                        reservationChangedListener.accept(scheduledValue);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Saving was interrupted.", e);
                } catch (ExecutionException e) {
                    showError("Unable to save the customer record.", e.getCause());
                }
            }
        }.execute();
    }

    private String[] numberOptions(int minimum, int maximum) {
        String[] values = new String[maximum - minimum + 1];
        for (int i = minimum; i <= maximum; i++) {
            values[i - minimum] = String.format("%02d", i);
        }
        return values;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(dialogParent == null ? mainFrame : dialogParent, message);
    }

    private void showError(String action, Throwable error) {
        JOptionPane.showMessageDialog(dialogParent == null ? mainFrame : dialogParent,
                action + "\n" + error.getMessage(), "Customer Log Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private static final class DateFields extends JPanel {
        private final JComboBox<Integer> year;
        private final JComboBox<Integer> month;
        private final JComboBox<Integer> day = new JComboBox<>();

        private DateFields(LocalDate initialDate, int maximumYear) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            year = new JComboBox<>();
            for (int value = maximumYear; value >= 1900; value--) {
                year.addItem(value);
            }
            month = new JComboBox<>();
            for (int value = 1; value <= 12; value++) {
                month.addItem(value);
            }
            year.addActionListener(this::updateDays);
            month.addActionListener(this::updateDays);
            add(year);
            add(month);
            add(day);
            setDate(initialDate);
        }

        private void setDate(LocalDate date) {
            year.setSelectedItem(date.getYear());
            month.setSelectedItem(date.getMonthValue());
            updateDays(null);
            day.setSelectedItem(date.getDayOfMonth());
        }

        private LocalDate getDate() {
            return LocalDate.of((Integer) year.getSelectedItem(), (Integer) month.getSelectedItem(),
                    (Integer) day.getSelectedItem());
        }

        private void updateDays(ActionEvent event) {
            if (year.getSelectedItem() == null || month.getSelectedItem() == null) {
                return;
            }
            Integer selectedDay = (Integer) day.getSelectedItem();
            int numberOfDays = LocalDate.of((Integer) year.getSelectedItem(), (Integer) month.getSelectedItem(), 1)
                    .lengthOfMonth();
            day.removeAllItems();
            for (int value = 1; value <= numberOfDays; value++) {
                day.addItem(value);
            }
            if (selectedDay != null) {
                day.setSelectedItem(Math.min(selectedDay, numberOfDays));
            }
        }
    }
}
