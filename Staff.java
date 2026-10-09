import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

public class Staff extends JPanel {
    private static final Color DIRECTORY_ACCENT = Frame.ACCENT;

    private final Frame mainFrame;

    private JPanel contentPanel;
    private JTable staffTable;
    private DefaultTableModel tableModel;
    private JTable archiveTable;
    private DefaultTableModel archiveTableModel;
    private JTextField searchField;
    private JComboBox<String> typeFilter;
    private JLabel staffCount;
    private JLabel archiveCount;
    private TableRowSorter<DefaultTableModel> staffSorter;

    private final Map<Object, String> emailByStaffId = new HashMap<>();
    private int staffLoadGeneration;
    private int archiveLoadGeneration;

    private static final class StaffRecord {
        private final Object id;
        private final String email;
        private final Object[] values;

        private StaffRecord(Object id, String email, Object[] values) {
            this.id = id;
            this.email = email;
            this.values = values;
        }
    }

    public Staff(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public JPanel showStaff() {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 220, 239)), new EmptyBorder(20, 24, 20, 24)));

        card.setMinimumSize(new Dimension(0, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Staff directory");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(DIRECTORY_ACCENT);
        JLabel subtitle = new JLabel("Manage staff profiles, roles, and access status");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Frame.MUTED);
        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(subtitle);
        titlePanel.setBorder(new EmptyBorder(0, 0, 12, 0));

        card.add(titlePanel, BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout(0, 12));
        contentPanel.setBackground(Color.WHITE);

        card.add(contentPanel, BorderLayout.CENTER);

        showStaffList();

        return card;
    }

    private void showStaffList() {

        contentPanel.removeAll();

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.setOpaque(false);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        JButton staffListButton = createTopButton("Staff list");
        JButton archiveButton = createTopButton("Archived");
        styleStaffTabButton(staffListButton, true);
        styleStaffTabButton(archiveButton, false);
        tabs.add(staffListButton);
        tabs.add(archiveButton);
        topArea.add(tabs, BorderLayout.WEST);
        staffCount = new JLabel("Loading staff...");
        staffCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
        staffCount.setForeground(Frame.MUTED);
        topArea.add(staffCount, BorderLayout.EAST);
        contentPanel.add(topArea, BorderLayout.NORTH);

        JPanel tableArea = new JPanel(new BorderLayout(0, 10));
        tableArea.setBackground(Color.WHITE);

        JPanel toolbar = createToolbar();
        tableArea.add(toolbar, BorderLayout.NORTH);

        createStaffTable();

        JScrollPane scrollPane = new JScrollPane(staffTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        tableArea.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(tableArea, BorderLayout.CENTER);

        staffListButton.addActionListener(e -> showStaffList());
        archiveButton.addActionListener(e -> showArchive());

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void loadStaff(boolean archived) {
        DefaultTableModel destination;
        int generation;

        if (archived) {
            if (archiveTableModel == null) {
                createArchiveModel();
            }
            destination = archiveTableModel;
            generation = ++archiveLoadGeneration;
        } else {
            destination = tableModel;
            generation = ++staffLoadGeneration;
            emailByStaffId.clear();
        }
        destination.setRowCount(0);

        String sql = "SELECT * FROM employee " + (archived ? "WHERE LOWER(status) = 'archived' "
                : "WHERE status IS NULL OR LOWER(status) <> 'archived' ") + "ORDER BY employee_id";

        new SwingWorker<java.util.List<StaffRecord>, Void>() {
            @Override
            protected java.util.List<StaffRecord> doInBackground() throws SQLException {
                java.util.List<StaffRecord> records = new ArrayList<>();
                try (Connection connection = database.getConnection();
                        PreparedStatement statement = connection.prepareStatement(sql);
                        ResultSet results = statement.executeQuery()) {
                    while (results.next()) {
                        Object id = results.getObject("employee_id");
                        String email = results.getString("email");
                        Object[] row = { id, fullName(results.getString("first_name"), results.getString("last_name")),
                                results.getString("role"), results.getString("phone_number"), email };
                        if (!archived) {
                            row = new Object[] { row[0], row[1], row[2], row[3], row[4],
                                    results.getString("status"), "" };
                        } else {
                            row = new Object[] { row[0], row[1], row[2], row[3], row[4],
                                    results.getString("status"), "" };
                        }
                        records.add(new StaffRecord(id, email == null ? "" : email, row));
                    }
                }
                return records;
            }

            @Override
            protected void done() {
                int currentGeneration = archived ? archiveLoadGeneration : staffLoadGeneration;
                if (generation != currentGeneration) {
                    return;
                }

                try {
                    for (StaffRecord record : get()) {
                        destination.addRow(record.values);
                        if (!archived) {
                            emailByStaffId.put(record.id, record.email);
                        }
                    }
                    if (!archived && staffCount != null) {
                        staffCount.setText(destination.getRowCount()
                                + (destination.getRowCount() == 1 ? " staff member" : " staff members"));
                    } else if (archived && archiveCount != null) {
                        archiveCount.setText(destination.getRowCount()
                                + (destination.getRowCount() == 1 ? " archived staff member"
                                        : " archived staff members"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Staff loading was interrupted.", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof SQLException) {
                        showDatabaseError("load staff", (SQLException) cause);
                    } else {
                        throw new IllegalStateException("Unable to load staff.", cause);
                    }
                }
            }
        }.execute();
    }

    private String fullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    private String[] splitName(String name) {
        String trimmedName = name.trim();
        int separator = trimmedName.indexOf(' ');
        if (separator < 0) {
            return new String[] { trimmedName, "" };
        }
        return new String[] { trimmedName.substring(0, separator), trimmedName.substring(separator + 1).trim() };
    }

    private void showDatabaseError(String action, SQLException error) {
        JOptionPane.showMessageDialog(mainFrame, "Unable to " + action + " in the database:\n" + error.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createToolbar() {

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JButton addButton = new JButton("+");
        addButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        Frame.styleButtonState(addButton, false);
        addButton.setFocusPainted(false);
        addButton.setBorderPainted(false);
        addButton.setPreferredSize(new Dimension(42, 42));
        addButton.addActionListener(e -> addStaff());

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        addPanel.setOpaque(false);

        addPanel.add(addButton);

        toolbar.add(addPanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        searchPanel.setOpaque(false);

        typeFilter = new JComboBox<>(new String[] { "All", "Cashier", "Waiter", "Manager", "Chef" });
        typeFilter.setPreferredSize(new Dimension(125, 32));
        typeFilter.setBackground(Color.WHITE);
        typeFilter.setForeground(DIRECTORY_ACCENT);

        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(190, 32));
        searchField.setToolTipText("Search staff");

        JButton searchButton = createTopButton("Search");
        searchButton.setPreferredSize(new Dimension(82, 32));

        searchPanel.add(typeFilter);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        toolbar.add(searchPanel, BorderLayout.EAST);

        searchButton.addActionListener(e -> searchStaff());
        searchField.addActionListener(e -> searchStaff());
        typeFilter.addActionListener(e -> filterStaff());

        return toolbar;
    }

    private void createStaffTable() {

        String[] columns = { "ID#", "Staff Name", "Type", "Phone", "Email", "Status", "Actions" };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {

                return column == getColumnCount() - 1;
            }
        };

        loadStaff(false);

        staffTable = new JTable(tableModel);

        staffTable.setRowHeight(38);

        staffTable.setFont(new Font("SansSerif", Font.PLAIN, 12));

        staffTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        staffTable.getTableHeader().setBackground(new Color(244, 240, 231));

        staffTable.getTableHeader().setForeground(DIRECTORY_ACCENT);
        staffTable.getTableHeader().setPreferredSize(new Dimension(0, 36));

        staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        staffTable.setGridColor(Frame.BORDER);

        staffTable.setShowVerticalLines(false);
        staffTable.setIntercellSpacing(new Dimension(0, 1));
        staffTable.setFillsViewportHeight(true);

        staffTable.setBackground(Color.WHITE);
        staffSorter = new TableRowSorter<>(tableModel);
        staffTable.setRowSorter(staffSorter);

        TableColumnModel columnModel = staffTable.getColumnModel();

        columnModel.getColumn(0).setPreferredWidth(50);

        columnModel.getColumn(1).setPreferredWidth(250);

        columnModel.getColumn(2).setPreferredWidth(150);

        columnModel.getColumn(3).setPreferredWidth(180);

        columnModel.getColumn(4).setPreferredWidth(220);
        columnModel.getColumn(5).setPreferredWidth(120);
        columnModel.getColumn(6).setPreferredWidth(160);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        columnModel.getColumn(0).setCellRenderer(centerRenderer);

        columnModel.getColumn(2).setCellRenderer(centerRenderer);

        columnModel.getColumn(3).setCellRenderer(centerRenderer);

        columnModel.getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);

                label.setHorizontalAlignment(SwingConstants.CENTER);

                if ("Active".equals(value)) {

                    label.setForeground(new Color(35, 130, 80));

                } else {

                    label.setForeground(new Color(184, 78, 78));
                }

                return label;
            }
        });

        columnModel.getColumn(6).setCellRenderer(new ActionRenderer());

        columnModel.getColumn(6).setCellEditor(new ActionEditor());
    }

    private class ActionRenderer extends JPanel implements TableCellRenderer {

        private final JButton editButton;

        private final JButton archiveButton;

        public ActionRenderer() {

            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 4));

            setBackground(Color.WHITE);

            editButton = new JButton("Edit");

            archiveButton = new JButton("Archive");

            styleActionButton(editButton, Frame.ACCENT_DARK);

            styleActionButton(archiveButton, Frame.NAVY);

            add(editButton);

            add(archiveButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {

            setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            return this;
        }
    }

    private class ActionEditor extends DefaultCellEditor {

        private final JPanel panel;

        private final JButton editButton;

        private final JButton archiveButton;

        private int currentRow;

        public ActionEditor() {

            super(new JTextField());

            setClickCountToStart(1);

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));

            panel.setBackground(Color.WHITE);

            editButton = new JButton("Edit");

            archiveButton = new JButton("Archive");

            styleActionButton(editButton, Frame.ACCENT_DARK);

            styleActionButton(archiveButton, Frame.NAVY);

            panel.add(editButton);

            panel.add(archiveButton);

            editButton.addActionListener(e -> {
                fireEditingStopped();

                editStaff(currentRow);
            });

            archiveButton.addActionListener(e -> {
                fireEditingStopped();

                archiveStaff(currentRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {

            currentRow = table.convertRowIndexToModel(row);

            return panel;
        }

        @Override
        public Object getCellEditorValue() {

            return "";
        }
    }

    private void styleActionButton(JButton button, Color color) {

        button.setFont(new Font("SansSerif", Font.BOLD, 11));

        Frame.styleButtonState(button, false);

        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(4, 8, 4, 8)));
    }

    private JButton createTopButton(String text) {

        JButton button = new JButton(text);

        button.setFont(new Font("SansSerif", Font.BOLD, 13));

        Frame.styleButtonState(button, false);

        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(7, 12, 7, 12)));

        button.setPreferredSize(new Dimension(125, 38));

        return button;
    }

    private void styleStaffTabButton(JButton button, boolean selected) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, selected);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Frame.ACCENT : Frame.BORDER),
                new EmptyBorder(7, 12, 7, 12)));
    }

    private void addStaff() {

        JTextField nameField = new JTextField();

        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Waiter", "Cashier", "Manager", "Chef" });

        JTextField phoneField = new JTextField();

        JTextField emailField = new JTextField();

        JComboBox<String> statusBox = new JComboBox<>(new String[] { "Active", "Inactive" });

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));

        form.add(new JLabel("Staff Name:"));

        form.add(nameField);

        form.add(new JLabel("Type:"));

        form.add(typeBox);

        form.add(new JLabel("Email:"));

        form.add(emailField);

        form.add(new JLabel("Phone:"));

        form.add(phoneField);

        form.add(new JLabel("Status:"));

        form.add(statusBox);

        int result = JOptionPane.showConfirmDialog(mainFrame, form, "Add Staff", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {

            return;
        }

        String name = nameField.getText().trim();

        String phone = phoneField.getText().trim();

        String email = emailField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {

            JOptionPane.showMessageDialog(mainFrame, "Please enter the staff name and email.", "Invalid Input",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        String[] names = splitName(name);
        String sql = "INSERT INTO employee " + "(first_name, last_name, email, phone_number, role, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, names[0]);
            statement.setString(2, names[1]);
            statement.setString(3, email);
            statement.setString(4, phone);
            statement.setString(5, String.valueOf(typeBox.getSelectedItem()));
            statement.setString(6, String.valueOf(statusBox.getSelectedItem()));
            statement.executeUpdate();
            loadStaff(false);
        } catch (SQLException e) {
            showDatabaseError("add staff", e);
        }
    }

    private void editStaff(int row) {

        if (row < 0 || row >= tableModel.getRowCount()) {

            return;
        }

        JTextField nameField = new JTextField(String.valueOf(tableModel.getValueAt(row, 1)));

        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Waiter", "Cashier", "Manager", "Chef" });

        typeBox.setSelectedItem(tableModel.getValueAt(row, 2));

        JTextField phoneField = new JTextField(String.valueOf(tableModel.getValueAt(row, 3)));

        JTextField emailField = new JTextField(emailByStaffId.getOrDefault(tableModel.getValueAt(row, 0), ""));

        JComboBox<String> statusBox = new JComboBox<>(new String[] { "Active", "Inactive" });

        statusBox.setSelectedItem(tableModel.getValueAt(row, 5));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));

        form.add(new JLabel("Staff Name:"));

        form.add(nameField);

        form.add(new JLabel("Type:"));

        form.add(typeBox);

        form.add(new JLabel("Email:"));

        form.add(emailField);

        form.add(new JLabel("Phone:"));

        form.add(phoneField);

        form.add(new JLabel("Status:"));

        form.add(statusBox);

        int result = JOptionPane.showConfirmDialog(mainFrame, form, "Modify Staff", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {

            return;
        }

        String name = nameField.getText().trim();

        String email = emailField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {

            JOptionPane.showMessageDialog(mainFrame, "Staff name and email cannot be empty.", "Invalid Input",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        String[] names = splitName(name);
        Object id = tableModel.getValueAt(row, 0);
        String sql = "UPDATE employee SET first_name = ?, last_name = ?, "
                + "email = ?, phone_number = ?, role = ?, status = ? " + "WHERE employee_id = ?";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, names[0]);
            statement.setString(2, names[1]);
            statement.setString(3, email);
            statement.setString(4, phoneField.getText().trim());
            statement.setString(5, String.valueOf(typeBox.getSelectedItem()));
            statement.setString(6, String.valueOf(statusBox.getSelectedItem()));
            statement.setObject(7, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                JOptionPane.showMessageDialog(mainFrame,
                        "This staff record no longer exists. Refresh the staff list and try again.", "Staff Not Found",
                        JOptionPane.WARNING_MESSAGE);
                loadStaff(false);
                return;
            }
            loadStaff(false);
        } catch (SQLException e) {
            showDatabaseError("update staff", e);
        }
    }

    private void archiveStaff(int row) {

        if (row < 0 || row >= tableModel.getRowCount()) {

            return;
        }

        String name = String.valueOf(tableModel.getValueAt(row, 1));

        int result = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to Archive " + name + "?",
                "Archive Staff", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {

            return;
        }

        Object id = tableModel.getValueAt(row, 0);

        String sql = "UPDATE employee SET status = ? WHERE employee_id = ?";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "Archived");
            statement.setObject(2, id);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                JOptionPane.showMessageDialog(mainFrame,
                        "This staff record no longer exists. Refresh the staff list and try again.", "Staff Not Found",
                        JOptionPane.WARNING_MESSAGE);
                loadStaff(false);
                return;
            }
            loadStaff(false);
        } catch (SQLException e) {
            showDatabaseError("archive staff", e);
            return;
        }

        JOptionPane.showMessageDialog(mainFrame, name + " has been archived.", "Archive Staff",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showArchive() {

        contentPanel.removeAll();

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        JButton staffListButton = createTopButton("Staff list");
        JButton archiveButton = createTopButton("Archived");
        styleStaffTabButton(staffListButton, false);
        styleStaffTabButton(archiveButton, true);
        tabs.add(staffListButton);
        tabs.add(archiveButton);
        top.add(tabs, BorderLayout.WEST);
        JLabel archivedCount = new JLabel("Archived staff");
        archiveCount = archivedCount;
        archivedCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
        archivedCount.setForeground(Frame.MUTED);
        top.add(archivedCount, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        createArchiveTable();
        loadStaff(true);

        JScrollPane scrollPane = new JScrollPane(archiveTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Frame.BORDER));

        panel.add(scrollPane, BorderLayout.CENTER);

        staffListButton.addActionListener(e -> showStaffList());

        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void createArchiveModel() {

        String[] columns = { "ID#", "Staff Name", "Type", "Phone", "Email", "Status", "Actions" };
        archiveTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == getColumnCount() - 1;
            }
        };
    }

    private void createArchiveTable() {

        if (archiveTableModel == null) {

            createArchiveModel();
        }

        archiveTable = new JTable(archiveTableModel);
        archiveTable.setRowHeight(38);
        archiveTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        archiveTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        archiveTable.getTableHeader().setBackground(new Color(244, 240, 231));
        archiveTable.getTableHeader().setForeground(DIRECTORY_ACCENT);
        archiveTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        archiveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        archiveTable.setGridColor(Frame.BORDER);
        archiveTable.setShowVerticalLines(false);
        archiveTable.setIntercellSpacing(new Dimension(0, 1));
        archiveTable.setFillsViewportHeight(true);
        archiveTable.setBackground(Color.WHITE);

        TableColumnModel columnModel = archiveTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(250);
        columnModel.getColumn(2).setPreferredWidth(150);
        columnModel.getColumn(3).setPreferredWidth(180);
        columnModel.getColumn(4).setPreferredWidth(220);
        columnModel.getColumn(5).setPreferredWidth(120);
        columnModel.getColumn(6).setPreferredWidth(130);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        columnModel.getColumn(0).setCellRenderer(centerRenderer);
        columnModel.getColumn(2).setCellRenderer(centerRenderer);
        columnModel.getColumn(3).setCellRenderer(centerRenderer);
        columnModel.getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);

                label.setHorizontalAlignment(SwingConstants.CENTER);

                label.setForeground(new Color(184, 78, 78));

                return label;
            }
        });
        columnModel.getColumn(6).setCellRenderer(new UnarchiveRenderer());
        columnModel.getColumn(6).setCellEditor(new UnarchiveEditor());
    }

    private final class UnarchiveRenderer extends JPanel implements TableCellRenderer {
        private final JButton button = new JButton("Unarchive");

        private UnarchiveRenderer() {
            super(new FlowLayout(FlowLayout.CENTER, 0, 3));
            styleActionButton(button, Frame.SUCCESS);
            add(button);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int row, int column) {
            setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }

    private final class UnarchiveEditor extends DefaultCellEditor {
        private final JButton button = new JButton("Unarchive");
        private int editingRow;

        private UnarchiveEditor() {
            super(new JTextField());
            setClickCountToStart(1);
            styleActionButton(button, Frame.SUCCESS);
            button.addActionListener(event -> {
                fireEditingStopped();
                unarchiveStaff(editingRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row,
                int column) {
            editingRow = table.convertRowIndexToModel(row);
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private void unarchiveStaff(int row) {
        if (archiveTableModel == null || row < 0 || row >= archiveTableModel.getRowCount()) {
            return;
        }
        Object id = archiveTableModel.getValueAt(row, 0);
        String name = String.valueOf(archiveTableModel.getValueAt(row, 1));
        int result = JOptionPane.showConfirmDialog(mainFrame, "Restore " + name + " as active staff?",
                "Unarchive Staff", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "UPDATE employee SET status = 'Active' WHERE employee_id = ? AND LOWER(status) = 'archived'";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            if (statement.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(mainFrame, "This staff record is no longer archived.",
                        "Staff Not Found", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, name + " has been restored as active staff.",
                        "Unarchive Staff", JOptionPane.INFORMATION_MESSAGE);
            }
            loadStaff(true);
            loadStaff(false);
        } catch (SQLException e) {
            showDatabaseError("unarchive staff", e);
        }
    }

    private void searchStaff() {
        applyStaffFilters();
    }

    private void filterStaff() {
        applyStaffFilters();
    }

    private void applyStaffFilters() {
        if (staffSorter == null || searchField == null || typeFilter == null) {
            return;
        }
        String search = searchField.getText().trim().toLowerCase();
        String role = String.valueOf(typeFilter.getSelectedItem());
        staffSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean matchesSearch = search.isEmpty();
                for (int column = 0; column < entry.getValueCount() - 1 && !matchesSearch; column++) {
                    Object value = entry.getValue(column);
                    matchesSearch = value != null && value.toString().toLowerCase().contains(search);
                }
                boolean matchesRole = "All".equals(role)
                        || role.equalsIgnoreCase(String.valueOf(entry.getValue(2)));
                return matchesSearch && matchesRole;
            }
        });
    }
}
