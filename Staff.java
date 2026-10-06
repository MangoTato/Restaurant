import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

public class Staff extends JPanel {

  private final Frame mainFrame;

  private JPanel contentPanel;
  private JTable staffTable;
  private DefaultTableModel tableModel;
  private JTable archiveTable;
  private DefaultTableModel archiveTableModel;
  private JTextField searchField;
  private JComboBox<String> typeFilter;
  private JComboBox<String> rowsCombo;

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
    card.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Frame.NAVY, 2), new EmptyBorder(20, 25, 20, 25)));

    card.setPreferredSize(new Dimension(1100, 680));

    JLabel title = mainFrame.createLabel("Staff", 25, Frame.NAVY);
    
    JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    titlePanel.setOpaque(false);
    titlePanel.add(title);

    card.add(titlePanel, BorderLayout.NORTH);

    contentPanel = new JPanel(new BorderLayout(0, 10));
    contentPanel.setBackground(Color.WHITE);

    card.add(contentPanel, BorderLayout.CENTER);

    showStaffList();

    return card;
  }

  private void showStaffList() {

    contentPanel.removeAll();

    JPanel topArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    topArea.setOpaque(false);

    JButton staffListButton = createTopButton("Staff List");
    JButton archiveButton = createTopButton("Archive");

    staffListButton.setBackground(Frame.TEAL);
    topArea.add(staffListButton);
    topArea.add(archiveButton);
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

    String sql =
        "SELECT * FROM employee "
            + (archived
                ? "WHERE LOWER(status) = 'archived' "
                : "WHERE status IS NULL OR LOWER(status) <> 'archived' ")
            + "ORDER BY employee_id";

    new SwingWorker<java.util.List<StaffRecord>, Void>() {
      @Override
      protected java.util.List<StaffRecord> doInBackground() throws SQLException {
        java.util.List<StaffRecord> records = new ArrayList<>();
        try (Connection connection = database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet results = statement.executeQuery()) {
          while (results.next()) {
            Object id = results.getObject("employee_id");
            Object[] row = {
              id,
              fullName(results.getString("first_name"), results.getString("last_name")),
              results.getString("role"),
              results.getString("phone_number")
            };
            if (!archived) {
              row = new Object[] {row[0], row[1], row[2], row[3], results.getString("status"), ""};
            }
            String email = results.getString("email");
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
      return new String[] {trimmedName, ""};
    }
    return new String[] {
      trimmedName.substring(0, separator), trimmedName.substring(separator + 1).trim()
    };
  }

  private void showDatabaseError(String action, SQLException error) {
    JOptionPane.showMessageDialog(
        mainFrame,
        "Unable to " + action + " in the database:\n" + error.getMessage(),
        "Database Error",
        JOptionPane.ERROR_MESSAGE);
  }

  private JPanel createToolbar() {

    JPanel toolbar = new JPanel(new BorderLayout());
    toolbar.setOpaque(false);

    JButton addButton = new JButton("+");
    addButton.setFont(new Font("SansSerif", Font.BOLD, 15));
    addButton.setForeground(Color.WHITE);
    addButton.setBackground(Frame.TEAL);
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

    rowsCombo = new JComboBox<>(new String[] {"10", "25", "50", "100"});
    rowsCombo.setPreferredSize(new Dimension(55, 30));

    typeFilter = new JComboBox<>(new String[] {"All", "Cashier", "Waiter", "Manager", "Chef"});
    typeFilter.setPreferredSize(new Dimension(100, 30));

    searchField = new JTextField();
    searchField.setPreferredSize(new Dimension(160, 30));
    searchField.setToolTipText("Search staff");

    JButton searchButton = new JButton("Search");
    searchButton.setPreferredSize(new Dimension(75, 30));
    searchButton.setFocusPainted(false);

    searchPanel.add(rowsCombo);
    searchPanel.add(typeFilter);
    searchPanel.add(searchField);
    searchPanel.add(searchButton);

    toolbar.add(searchPanel, BorderLayout.EAST);

    searchButton.addActionListener(e -> searchStaff());
    searchField.addActionListener(e -> searchStaff());
    typeFilter.addActionListener(e -> filterStaff());
    rowsCombo.addActionListener(e -> updateRowLimit());

    return toolbar;
  }

  private void createStaffTable() {

    String[] columns = {"ID#", "Staff Name", "Type", "Phone", "Status", "Actions"};

    tableModel =
        new DefaultTableModel(columns, 0) {

          @Override
          public boolean isCellEditable(int row, int column) {

            return column == 5;
          }
        };

    loadStaff(false);

    staffTable = new JTable(tableModel);

    staffTable.setRowHeight(38);

    staffTable.setFont(new Font("SansSerif", Font.PLAIN, 12));

    staffTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

    staffTable.getTableHeader().setBackground(new Color(235, 237, 242));

    staffTable.getTableHeader().setForeground(Color.DARK_GRAY);

    staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    staffTable.setGridColor(new Color(225, 225, 225));

    staffTable.setShowVerticalLines(false);

    staffTable.setBackground(Color.WHITE);

    TableColumnModel columnModel = staffTable.getColumnModel();

    columnModel.getColumn(0).setPreferredWidth(50);

    columnModel.getColumn(1).setPreferredWidth(250);

    columnModel.getColumn(2).setPreferredWidth(150);

    columnModel.getColumn(3).setPreferredWidth(180);

    columnModel.getColumn(4).setPreferredWidth(150);

    columnModel.getColumn(5).setPreferredWidth(160);

    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();

    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

    columnModel.getColumn(0).setCellRenderer(centerRenderer);

    columnModel.getColumn(2).setCellRenderer(centerRenderer);

    columnModel.getColumn(3).setCellRenderer(centerRenderer);

    columnModel
        .getColumn(4)
        .setCellRenderer(
            new DefaultTableCellRenderer() {

              @Override
              public Component getTableCellRendererComponent(
                  JTable table,
                  Object value,
                  boolean isSelected,
                  boolean hasFocus,
                  int row,
                  int column) {

                JLabel label =
                    (JLabel)
                        super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);

                label.setHorizontalAlignment(SwingConstants.CENTER);

                if ("Active".equals(value)) {

                  label.setForeground(new Color(30, 150, 80));

                } else {

                  label.setForeground(Color.RED);
                }

                return label;
              }
            });

    columnModel.getColumn(5).setCellRenderer(new ActionRenderer());

    columnModel.getColumn(5).setCellEditor(new ActionEditor());
  }

  private class ActionRenderer extends JPanel implements TableCellRenderer {

    private final JButton editButton;

    private final JButton archiveButton;

    public ActionRenderer() {

      setLayout(new FlowLayout(FlowLayout.CENTER, 5, 4));

      setBackground(Color.WHITE);

      editButton = new JButton("Edit");

      archiveButton = new JButton("Archive");

      styleActionButton(editButton, new Color(255, 193, 7));

      styleActionButton(archiveButton, new Color(70, 145, 210));

      add(editButton);

      add(archiveButton);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

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

      styleActionButton(editButton, new Color(255, 193, 7));

      styleActionButton(archiveButton, new Color(70, 145, 210));

      panel.add(editButton);

      panel.add(archiveButton);

      editButton.addActionListener(
          e -> {
            fireEditingStopped();

            editStaff(currentRow);
          });

      archiveButton.addActionListener(
          e -> {
            fireEditingStopped();

            archiveStaff(currentRow);
          });
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected, int row, int column) {

      currentRow = row;

      return panel;
    }

    @Override
    public Object getCellEditorValue() {

      return "";
    }
  }

  private void styleActionButton(JButton button, Color color) {

    button.setFont(new Font("SansSerif", Font.BOLD, 11));

    button.setForeground(Color.WHITE);

    button.setBackground(color);

    button.setFocusPainted(false);

    button.setBorderPainted(false);

    button.setMargin(new Insets(2, 7, 2, 7));
  }

  private JButton createTopButton(String text) {

    JButton button = new JButton(text);

    button.setFont(new Font("SansSerif", Font.BOLD, 13));

    button.setForeground(Color.WHITE);

    button.setBackground(Frame.NAVY);

    button.setFocusPainted(false);

    button.setBorderPainted(false);

    button.setPreferredSize(new Dimension(125, 38));

    return button;
  }

  private void addStaff() {

    JTextField nameField = new JTextField();

    JComboBox<String> typeBox =
        new JComboBox<>(new String[] {"Waiter", "Cashier", "Manager", "Chef"});

    JTextField phoneField = new JTextField();

    JTextField emailField = new JTextField();

    JComboBox<String> statusBox = new JComboBox<>(new String[] {"Active", "Inactive"});

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

    int result =
        JOptionPane.showConfirmDialog(
            mainFrame, form, "Add Staff", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (result != JOptionPane.OK_OPTION) {

      return;
    }

    String name = nameField.getText().trim();

    String phone = phoneField.getText().trim();

    String email = emailField.getText().trim();

    if (name.isEmpty() || email.isEmpty()) {

      JOptionPane.showMessageDialog(
          mainFrame,
          "Please enter the staff name and email.",
          "Invalid Input",
          JOptionPane.WARNING_MESSAGE);

      return;
    }

    String[] names = splitName(name);
    String sql =
        "INSERT INTO employee "
            + "(first_name, last_name, email, phone_number, role, status) "
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

    JComboBox<String> typeBox =
        new JComboBox<>(new String[] {"Waiter", "Cashier", "Manager", "Chef"});

    typeBox.setSelectedItem(tableModel.getValueAt(row, 2));

    JTextField phoneField = new JTextField(String.valueOf(tableModel.getValueAt(row, 3)));

    JTextField emailField =
        new JTextField(emailByStaffId.getOrDefault(tableModel.getValueAt(row, 0), ""));

    JComboBox<String> statusBox = new JComboBox<>(new String[] {"Active", "Inactive"});

    statusBox.setSelectedItem(tableModel.getValueAt(row, 4));

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

    int result =
        JOptionPane.showConfirmDialog(
            mainFrame,
            form,
            "Modify Staff",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

    if (result != JOptionPane.OK_OPTION) {

      return;
    }

    String name = nameField.getText().trim();

    String email = emailField.getText().trim();

    if (name.isEmpty() || email.isEmpty()) {

      JOptionPane.showMessageDialog(
          mainFrame,
          "Staff name and email cannot be empty.",
          "Invalid Input",
          JOptionPane.WARNING_MESSAGE);

      return;
    }

    String[] names = splitName(name);
    Object id = tableModel.getValueAt(row, 0);
    String sql =
        "UPDATE employee SET first_name = ?, last_name = ?, "
            + "email = ?, phone_number = ?, role = ?, status = ? "
            + "WHERE employee_id = ?";
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
        JOptionPane.showMessageDialog(
            mainFrame,
            "This staff record no longer exists. Refresh the staff list and try again.",
            "Staff Not Found",
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

    int result =
        JOptionPane.showConfirmDialog(
            mainFrame,
            "Are you sure you want to Archive " + name + "?",
            "Archive Staff",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

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
        JOptionPane.showMessageDialog(
            mainFrame,
            "This staff record no longer exists. Refresh the staff list and try again.",
            "Staff Not Found",
            JOptionPane.WARNING_MESSAGE);
        loadStaff(false);
        return;
      }
      loadStaff(false);
    } catch (SQLException e) {
      showDatabaseError("archive staff", e);
      return;
    }

    JOptionPane.showMessageDialog(
        mainFrame, name + " has been archived.", "Archive Staff", JOptionPane.INFORMATION_MESSAGE);
  }

  private void showArchive() {

    contentPanel.removeAll();

    JPanel panel = new JPanel(new BorderLayout(0, 10));
    panel.setBackground(Color.WHITE);

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    top.setOpaque(false);

    JButton staffListButton = createTopButton("Staff List");
    JButton archiveButton = createTopButton("Archive");

    archiveButton.setBackground(Frame.TEAL);

    top.add(staffListButton);
    top.add(archiveButton);

    panel.add(top, BorderLayout.NORTH);

    createArchiveTable();
    loadStaff(true);

    JScrollPane scrollPane = new JScrollPane(archiveTable);
    scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

    panel.add(scrollPane, BorderLayout.CENTER);

    staffListButton.addActionListener(e -> showStaffList());

    contentPanel.add(panel, BorderLayout.CENTER);
    contentPanel.revalidate();
    contentPanel.repaint();
  }

  private void createArchiveModel() {

    String[] columns = {"ID#", "Staff Name", "Type", "Phone", "Status"};

    archiveTableModel = new DefaultTableModel(columns, 0);
  }

  private void createArchiveTable() {

    if (archiveTableModel == null) {

      createArchiveModel();
    }

    archiveTable = new JTable(archiveTableModel);
    archiveTable.setRowHeight(38);
    archiveTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
    archiveTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
    archiveTable.getTableHeader().setBackground(new Color(235, 237, 242));
    archiveTable.getTableHeader().setForeground(Color.DARK_GRAY);
    archiveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    archiveTable.setGridColor(new Color(225, 225, 225));
    archiveTable.setShowVerticalLines(false);
    archiveTable.setBackground(Color.WHITE);

    TableColumnModel columnModel = archiveTable.getColumnModel();
    columnModel.getColumn(0).setPreferredWidth(50);
    columnModel.getColumn(1).setPreferredWidth(250);
    columnModel.getColumn(2).setPreferredWidth(150);
    columnModel.getColumn(3).setPreferredWidth(180);
    columnModel.getColumn(4).setPreferredWidth(150);

    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

    columnModel.getColumn(0).setCellRenderer(centerRenderer);
    columnModel.getColumn(2).setCellRenderer(centerRenderer);
    columnModel.getColumn(3).setCellRenderer(centerRenderer);
    columnModel
        .getColumn(4)
        .setCellRenderer(
            new DefaultTableCellRenderer() {

              @Override
              public Component getTableCellRendererComponent(
                  JTable table,
                  Object value,
                  boolean isSelected,
                  boolean hasFocus,
                  int row,
                  int column) {

                JLabel label =
                    (JLabel)
                        super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);

                label.setHorizontalAlignment(SwingConstants.CENTER);

                label.setForeground(Color.RED);

                return label;
              }
            });
  }

  private void searchStaff() {

    String search = searchField.getText().trim().toLowerCase();

    String selectedType = String.valueOf(typeFilter.getSelectedItem());

    if (search.isEmpty() && "All".equals(selectedType)) {

      staffTable.clearSelection();

      return;
    }

    for (int i = 0; i < tableModel.getRowCount(); i++) {

      String name = String.valueOf(tableModel.getValueAt(i, 1)).toLowerCase();
      String type = String.valueOf(tableModel.getValueAt(i, 2));
      String phone = String.valueOf(tableModel.getValueAt(i, 3)).toLowerCase();

      boolean typeMatch = "All".equals(selectedType) || type.equals(selectedType);

      boolean searchMatch =
          search.isEmpty()
              || name.contains(search)
              || type.toLowerCase().contains(search)
              || phone.contains(search);

      if (typeMatch && searchMatch) {

        staffTable.setRowSelectionInterval(i, i);

        staffTable.scrollRectToVisible(staffTable.getCellRect(i, 0, true));

        return;
      }
    }

    staffTable.clearSelection();

    JOptionPane.showMessageDialog(
        mainFrame, "No staff member found.", "Search", JOptionPane.INFORMATION_MESSAGE);
  }

  private void filterStaff() {

    String selectedType = String.valueOf(typeFilter.getSelectedItem());

    if ("All".equals(selectedType)) {

      staffTable.clearSelection();

      return;
    }

    for (int i = 0; i < tableModel.getRowCount(); i++) {
      String type = String.valueOf(tableModel.getValueAt(i, 2));

      if (type.equals(selectedType)) {
        staffTable.setRowSelectionInterval(i, i);
        staffTable.scrollRectToVisible(staffTable.getCellRect(i, 0, true));

        return;
      }
    }

    staffTable.clearSelection();

    JOptionPane.showMessageDialog(
        mainFrame,
        "No " + selectedType + " staff found.",
        "Staff Type",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private void updateRowLimit() {

    if (rowsCombo == null) {
      return;
    }

    String selectedRows = String.valueOf(rowsCombo.getSelectedItem());
    System.out.println("Rows per page: " + selectedRows);
  }
}
