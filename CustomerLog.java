import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class CustomerLog extends JPanel {
  private static final String CONFIRMED = "Confirmed";
  private static final String RESERVED = "Reserved";
  private static final String CANCELLED = "Cancelled";

  private final Frame mainFrame;
  private final JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
  private JTable table;
  private DefaultTableModel tableModel;
  private String selectedCategory = "Customer List";
  private int loadGeneration;

  public CustomerLog(Frame mainFrame) {
    this.mainFrame = mainFrame;
    setLayout(new BorderLayout());
    setBackground(Color.WHITE);
    setBorder(new EmptyBorder(16, 18, 16, 18));
    add(createTabs(), BorderLayout.NORTH);
    add(contentPanel, BorderLayout.CENTER);
    showCategory("Customer List");
  }

  private JPanel createTabs() {
    JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    tabs.setOpaque(false);
    for (String category :
        new String[] {"Customer List", CONFIRMED, RESERVED, "Completed", CANCELLED}) {
      JButton button = new JButton(category);
      button.addActionListener(e -> showCategory(category));
      tabs.add(button);
    }
    return tabs;
  }

  private void showCategory(String category) {
    selectedCategory = category;
    loadGeneration++;
    contentPanel.removeAll();

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    toolbar.setOpaque(false);
    if ("Customer List".equals(category)) {
      JButton addButton = new JButton("Add");
      addButton.addActionListener(e -> showAddCustomerDialog());
      toolbar.add(addButton);
    }

    JLabel heading = new JLabel(category, SwingConstants.LEFT);
    heading.setFont(new Font("SansSerif", Font.BOLD, 20));
    heading.setForeground(Frame.NAVY);
    toolbar.add(heading);
    contentPanel.add(toolbar, BorderLayout.NORTH);

    String[] columns = columnsFor(category);
    tableModel =
        new DefaultTableModel(columns, 0) {
          @Override
          public boolean isCellEditable(int row, int column) {
            return "Customer List".equals(selectedCategory) && column == getColumnCount() - 1;
          }
        };
    table = new JTable(tableModel);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowHeight(32);
    table.getTableHeader().setReorderingAllowed(false);
    if ("Customer List".equals(category)) {
      table
          .getColumnModel()
          .getColumn(tableModel.getColumnCount() - 1)
          .setCellRenderer(new EditButtonRenderer());
      table
          .getColumnModel()
          .getColumn(tableModel.getColumnCount() - 1)
          .setCellEditor(new EditButtonEditor());
    }
    contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);
    contentPanel.revalidate();
    contentPanel.repaint();

    loadCategory(category, ++loadGeneration);
  }

  private String[] columnsFor(String category) {
    if ("Customer List".equals(category)) {
      return new String[] {
        "Customer ID",
        "First Name",
        "Last Name",
        "Email",
        "Phone Number",
        "Birthdate",
        "Status",
        "Scheduled",
        "Actions"
      };
    }
    if ("Completed".equals(category)) {
      return new String[] {
        "Transaction ID", "Transaction Date/Time", "Customer ID", "First Name", "Amount"
      };
    }
    if (RESERVED.equals(category)) {
      return new String[] {
        "Customer ID",
        "First Name",
        "Last Name",
        "Email",
        "Phone Number",
        "Birthdate",
        "Scheduled",
        "Time"
      };
    }
    if (CANCELLED.equals(category)) {
      return new String[] {
        "Customer ID",
        "First Name",
        "Last Name",
        "Email",
        "Phone Number",
        "Birthdate",
        "Scheduled",
        "Status"
      };
    }
    return new String[] {
      "Customer ID", "First Name", "Last Name", "Email", "Phone Number", "Birthdate", "Scheduled"
    };
  }

  private void loadCategory(String category, int generation) {
    new SwingWorker<List<Object[]>, Void>() {
      @Override
      protected List<Object[]> doInBackground() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = database.getConnection()) {
          ensureCancelledStatus(connection);
          if ("Customer List".equals(category)) {
            String sql =
                "SELECT customer_id, first_name, last_name, email, phone_number, "
                    + "birthdate, status, scheduled FROM customer "
                    + "WHERE status IS NULL OR status <> 'Completed' ORDER BY customer_id";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
              while (results.next()) {
                rows.add(
                    new Object[] {
                      results.getObject("customer_id"),
                      results.getString("first_name"),
                      results.getString("last_name"),
                      results.getString("email"),
                      results.getString("phone_number"),
                      results.getDate("birthdate"),
                      results.getString("status"),
                      results.getTimestamp("scheduled"),
                      ""
                    });
              }
            }
          } else if ("Completed".equals(category)) {
            String sql =
                "SELECT t.transaction_id, t.transaction_datetime, c.customer_id, "
                    + "c.first_name, t.amount FROM customer c "
                    + "LEFT JOIN `transaction` t ON t.customer_id = c.customer_id "
                    + "WHERE c.status = 'Completed' "
                    + "ORDER BY COALESCE(t.transaction_datetime, c.scheduled) DESC, c.customer_id";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) {
              while (results.next()) {
                rows.add(
                    new Object[] {
                      results.getObject("transaction_id"),
                      results.getTimestamp("transaction_datetime"),
                      results.getObject("customer_id"),
                      results.getString("first_name"),
                      results.getString("amount")
                    });
              }
            }
          } else {
            String sql =
                "SELECT customer_id, first_name, last_name, email, phone_number, birthdate, "
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
                  row.add(
                      scheduled == null
                          ? null
                          : Date.valueOf(scheduled.toLocalDateTime().toLocalDate()));
                  if (RESERVED.equals(category)) {
                    row.add(
                        scheduled == null
                            ? null
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
          for (Object[] row : get()) {
            tableModel.addRow(row);
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

  private void ensureCancelledStatus(Connection connection) throws SQLException {
    String columnType;
    String sql =
        "SELECT COLUMN_TYPE FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer' "
            + "AND COLUMN_NAME = 'status'";
    try (PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet result = statement.executeQuery()) {
      if (!result.next()) {
        throw new SQLException("The customer.status column was not found.");
      }
      columnType = result.getString("COLUMN_TYPE");
    }

    if (!columnType.contains("'Cancelled'")) {
      try (java.sql.Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            "ALTER TABLE customer MODIFY COLUMN status "
                + "ENUM('Confirmed', 'In Progress', 'Completed', 'Reserved', 'Cancelled') NULL");
      }
    }
  }

  private void showAddCustomerDialog() {
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
    DateFields birthdate = new DateFields(LocalDate.of(2000, 1, 1), LocalDate.now().getYear());
    DateFields scheduled = new DateFields(LocalDate.now(), LocalDate.now().getYear() + 10);
    JComboBox<String> hour = new JComboBox<>(numberOptions(0, 23));
    JComboBox<String> minute = new JComboBox<>(numberOptions(0, 59));
    JComboBox<String> status =
        new JComboBox<>(new String[] {CONFIRMED, "In Progress", "Completed", RESERVED, CANCELLED});

    if (customerId != null) {
      String sql =
          "SELECT first_name, last_name, email, phone_number, birthdate, scheduled, status "
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
          Date birth = result.getDate("birthdate");
          if (birth != null) {
            birthdate.setDate(birth.toLocalDate());
          }
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

    JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
    form.add(new JLabel("First name:"));
    form.add(firstName);
    form.add(new JLabel("Last name:"));
    form.add(lastName);
    form.add(new JLabel("Email:"));
    form.add(email);
    form.add(new JLabel("Phone number:"));
    form.add(phone);
    form.add(new JLabel("Birthdate (year / month / day):"));
    form.add(birthdate);
    form.add(new JLabel("Scheduled (year / month / day):"));
    form.add(scheduled);
    JPanel timeFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    timeFields.add(hour);
    timeFields.add(new JLabel(":"));
    timeFields.add(minute);
    form.add(new JLabel("Scheduled time (24-hour):"));
    form.add(timeFields);
    form.add(new JLabel("Status:"));
    form.add(status);

    int result =
        JOptionPane.showConfirmDialog(
            mainFrame,
            form,
            customerId == null ? "Add Customer" : "Edit Customer",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
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
    if (first.length() > 25
        || last.length() > 25
        || emailValue.length() > 50
        || phoneValue.length() > 12) {
      showMessage(
          "Names must be 25 characters or fewer, email 50 or fewer, and phone number 12 or fewer.");
      return;
    }

    LocalDate birthDateValue = birthdate.getDate();
    LocalDate scheduledValue = scheduled.getDate();
    LocalTime scheduledTime =
        LocalTime.of(
            Integer.parseInt((String) hour.getSelectedItem()),
            Integer.parseInt((String) minute.getSelectedItem()));
    Timestamp scheduledDateTime =
        Timestamp.valueOf(LocalDateTime.of(scheduledValue, scheduledTime));
    String statusValue = (String) status.getSelectedItem();
    String sql =
        customerId == null
            ? "INSERT INTO customer "
                + "(first_name, last_name, email, phone_number, birthdate, scheduled, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)"
            : "UPDATE customer SET first_name = ?, last_name = ?, email = ?, phone_number = ?, "
                + "birthdate = ?, scheduled = ?, status = ? WHERE customer_id = ?";
    runUpdate(
        sql,
        statement -> {
          statement.setString(1, first);
          statement.setString(2, last);
          statement.setString(3, emailValue);
          statement.setString(4, phoneValue);
          statement.setDate(5, Date.valueOf(birthDateValue));
          statement.setTimestamp(6, scheduledDateTime);
          statement.setString(7, statusValue);
          if (customerId != null) {
            statement.setLong(8, customerId);
          }
        },
        "Customer List");
  }

  private final class EditButtonRenderer extends JPanel implements TableCellRenderer {
    private final JButton editButton = new JButton("Edit");

    private EditButtonRenderer() {
      super(new FlowLayout(FlowLayout.CENTER, 0, 2));
      add(editButton);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable source, Object value, boolean selected, boolean focused, int row, int column) {
      setBackground(selected ? source.getSelectionBackground() : source.getBackground());
      return this;
    }
  }

  private final class EditButtonEditor extends DefaultCellEditor {
    private final JButton editButton = new JButton("Edit");
    private int editingRow;

    private EditButtonEditor() {
      super(new JTextField());
      editButton.addActionListener(
          event -> {
            fireEditingStopped();
            editCustomer(editingRow);
          });
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable source, Object value, boolean selected, int row, int column) {
      editingRow = source.convertRowIndexToModel(row);
      return editButton;
    }

    @Override
    public Object getCellEditorValue() {
      return "";
    }
  }

  private void runUpdate(String sql, SqlBinder binder, String category) {
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
    JOptionPane.showMessageDialog(mainFrame, message);
  }

  private void showError(String action, Throwable error) {
    JOptionPane.showMessageDialog(
        mainFrame,
        action + "\n" + error.getMessage(),
        "Customer Log Error",
        JOptionPane.ERROR_MESSAGE);
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
      return LocalDate.of(
          (Integer) year.getSelectedItem(),
          (Integer) month.getSelectedItem(),
          (Integer) day.getSelectedItem());
    }

    private void updateDays(ActionEvent event) {
      if (year.getSelectedItem() == null || month.getSelectedItem() == null) {
        return;
      }
      Integer selectedDay = (Integer) day.getSelectedItem();
      int numberOfDays =
          LocalDate.of((Integer) year.getSelectedItem(), (Integer) month.getSelectedItem(), 1)
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
