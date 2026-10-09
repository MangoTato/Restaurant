import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Database access and shared presentation for completed restaurant invoices. */
public final class InvoiceService {
    private InvoiceService() {
    }

    public static void ensureInvoiceSchema(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'receipt' "
                + "AND COLUMN_NAME = 'transaction_id'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Unable to inspect the receipt table schema.");
            }
            if (result.getInt(1) == 0) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("ALTER TABLE receipt ADD COLUMN transaction_id BIGINT NULL");
                    alter.executeUpdate("UPDATE receipt r SET r.transaction_id = ("
                            + "SELECT t.transaction_id FROM `transaction` t "
                            + "WHERE t.customer_id = r.customer_id "
                            + "AND t.transaction_datetime <= r.chef_received_at "
                            + "AND t.transaction_datetime >= DATE_SUB(r.chef_received_at, INTERVAL 5 MINUTE) "
                            + "ORDER BY ABS(TIMESTAMPDIFF(MICROSECOND, t.transaction_datetime, "
                            + "r.chef_received_at)) LIMIT 1) "
                            + "WHERE r.transaction_id IS NULL AND r.chef_received_at IS NOT NULL");
                }
            }
        }

        String indexSql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'receipt' "
                + "AND INDEX_NAME = 'idx_receipt_transaction_id'";
        try (PreparedStatement statement = connection.prepareStatement(indexSql);
                ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Unable to inspect receipt indexes.");
            }
            if (result.getInt(1) == 0) {
                try (Statement alter = connection.createStatement()) {
                    alter.executeUpdate("CREATE INDEX idx_receipt_transaction_id ON receipt (transaction_id)");
                }
            }
        }
    }

    public static List<Invoice> loadInvoices(LocalDate start, LocalDate end) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT t.transaction_id, t.customer_id, t.transaction_datetime, t.amount, "
                + "c.first_name, c.last_name, c.email "
                + "FROM `transaction` t JOIN customer c ON c.customer_id = t.customer_id "
                + "WHERE t.transaction_datetime >= ? AND t.transaction_datetime < ? "
                + "ORDER BY t.transaction_datetime DESC, t.transaction_id DESC";
        try (Connection connection = database.getConnection()) {
            ensureInvoiceSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
                statement.setTimestamp(2, Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        long transactionId = result.getLong("transaction_id");
                        Timestamp created = result.getTimestamp("transaction_datetime");
                        invoices.add(new Invoice(transactionId, result.getLong("customer_id"),
                                result.getString("first_name"), result.getString("last_name"),
                                result.getString("email"), result.getBigDecimal("amount"), created));
                    }
                }
            }
        }
        return invoices;
    }

    public static List<Invoice> loadCompletedCustomerInvoices() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT t.transaction_id, t.customer_id, t.transaction_datetime, t.amount, "
                + "c.first_name, c.last_name, c.email "
                + "FROM customer c LEFT JOIN `transaction` t ON t.customer_id = c.customer_id "
                + "WHERE c.status = 'Completed' "
                + "ORDER BY COALESCE(t.transaction_datetime, c.scheduled) DESC, c.customer_id";
        try (Connection connection = database.getConnection()) {
            ensureInvoiceSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Object transactionIdValue = result.getObject("transaction_id");
                    Timestamp created = result.getTimestamp("transaction_datetime");
                    Long transactionId = transactionIdValue == null ? null : ((Number) transactionIdValue).longValue();
                    BigDecimal amount = result.getBigDecimal("amount");
                    invoices.add(new Invoice(transactionId, result.getLong("customer_id"),
                            result.getString("first_name"), result.getString("last_name"),
                            result.getString("email"), amount, created));
                }
            }
        }
        return invoices;
    }

    public static void showDetails(Component parent, Invoice invoice) {
        if (invoice.transactionId == null) {
            JOptionPane.showMessageDialog(parent,
                    "There is no transaction record for this completed customer.", "Invoice Not Available",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws SQLException {
                List<Object[]> rows = new ArrayList<>();
                String sql = "SELECT ri.item_name, ri.quantity, ri.unit_price, ri.note "
                        + "FROM receipt r JOIN receipt_item ri ON ri.receipt_id = r.receipt_id "
                        + "WHERE r.transaction_id = ? ORDER BY ri.receipt_item_id";
                try (Connection connection = database.getConnection()) {
                    ensureInvoiceSchema(connection);
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, invoice.transactionId);
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                rows.add(new Object[] { result.getString("item_name"), result.getInt("quantity"),
                                        result.getBigDecimal("unit_price"), result.getString("note") });
                            }
                        }
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    showInvoiceDialog(parent, invoice, get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError(parent, "Loading the invoice was interrupted.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    showError(parent, "Unable to load the invoice:\n" + cause.getMessage());
                }
            }
        }.execute();
    }

    private static void showInvoiceDialog(Component parent, Invoice invoice, List<Object[]> lines) {
        Color accent = Frame.ACCENT;
        Color muted = Frame.MUTED;
        JPanel paper = new JPanel(new BorderLayout(0, 0));
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createLineBorder(new Color(215, 215, 215)));
        JPanel topStripe = new JPanel();
        topStripe.setBackground(accent);
        topStripe.setPreferredSize(new Dimension(1, 8));
        paper.add(topStripe, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 26, 16, 26));

        JLabel restaurantName = new JLabel("Pâques", SwingConstants.CENTER);
        restaurantName.setFont(new Font("SansSerif", Font.BOLD, 24));
        restaurantName.setForeground(Frame.NAVY);
        JLabel receiptTitle = new JLabel("PAYMENT RECEIPT", SwingConstants.CENTER);
        receiptTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        receiptTitle.setForeground(accent);
        JPanel brand = new JPanel(new GridLayout(2, 1, 0, 3));
        brand.setOpaque(false);
        brand.add(restaurantName);
        brand.add(receiptTitle);
        body.add(brand, BorderLayout.NORTH);

        String customerName = invoice.getCustomerName().trim();
        JPanel billTo = new JPanel(new GridLayout(0, 1, 0, 4));
        billTo.setOpaque(false);
        JLabel billTitle = new JLabel("BILL TO");
        billTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        billTitle.setForeground(accent);
        billTo.add(billTitle);
        billTo.add(new JLabel(customerName.isEmpty() ? "Customer" : customerName));
        billTo.add(new JLabel("Customer ID: " + invoice.customerId));
        if (invoice.email != null && !invoice.email.trim().isEmpty()) {
            billTo.add(new JLabel(invoice.email));
        }

        JPanel transactionDetails = new JPanel(new GridLayout(0, 1, 0, 4));
        transactionDetails.setOpaque(false);
        transactionDetails.add(new JLabel("Transaction ID: "
                + (invoice.transactionId == null ? "Unavailable" : invoice.transactionId)));
        transactionDetails.add(new JLabel("Transaction Date: " + (invoice.created == null ? "—"
                : invoice.created.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));
        transactionDetails.add(new JLabel("Payment Status: " + invoice.getStatus().toUpperCase()));
        JPanel parties = new JPanel(new BorderLayout(16, 0));
        parties.setOpaque(false);
        parties.add(billTo, BorderLayout.WEST);
        parties.add(transactionDetails, BorderLayout.EAST);

        DefaultTableModel model = new DefaultTableModel(
                new String[] { "NO.", "DESCRIPTION", "QTY", "UNIT PRICE", "AMOUNT" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        BigDecimal subtotal = BigDecimal.ZERO;
        int lineNumber = 1;
        for (Object[] line : lines) {
            BigDecimal unitPrice = (BigDecimal) line[2];
            int quantity = (Integer) line[1];
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);
            String description = String.valueOf(line[0]);
            if (line[3] != null && !line[3].toString().trim().isEmpty()) {
                description += " — " + line[3].toString().trim();
            }
            model.addRow(new Object[] { lineNumber++, description, quantity,
                    formatReceiptAmount(unitPrice), formatReceiptAmount(lineTotal) });
        }
        if (lines.isEmpty()) {
            model.addRow(new Object[] { "", "No purchased item details are linked to this transaction.",
                    "", "", "" });
        }
        JTable items = new JTable(model);
        items.setFillsViewportHeight(true);
        items.setRowHeight(30);
        items.setFont(new Font("SansSerif", Font.PLAIN, 12));
        items.setGridColor(new Color(226, 226, 226));
        items.setShowVerticalLines(false);
        JTableHeader tableHeader = items.getTableHeader();
        tableHeader.setReorderingAllowed(false);
        tableHeader.setBackground(Frame.BACKGROUND);
        tableHeader.setForeground(Frame.NAVY);
        tableHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        JScrollPane itemScroll = new JScrollPane(items);
        itemScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(220, 220, 220)));
        JPanel lineItems = new JPanel(new BorderLayout(0, 8));
        lineItems.setOpaque(false);
        lineItems.add(parties, BorderLayout.NORTH);
        lineItems.add(itemScroll, BorderLayout.CENTER);

        BigDecimal amountPaid = invoice.amount == null ? subtotal : invoice.amount;
        JPanel totals = new JPanel(new GridLayout(0, 1, 0, 5));
        totals.setOpaque(false);
        JLabel subtotalLabel = new JLabel("Subtotal: " + formatReceiptAmount(subtotal), SwingConstants.RIGHT);
        subtotalLabel.setForeground(muted);
        JLabel totalLabel = new JLabel("TOTAL PAID: " + formatReceiptAmount(amountPaid), SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(accent);
        totalLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        totals.add(subtotalLabel);
        totals.add(totalLabel);
        JLabel thanks = new JLabel("Thank you for dining with Pâques!", SwingConstants.CENTER);
        thanks.setFont(new Font("SansSerif", Font.ITALIC, 17));
        JPanel receiptFooter = new JPanel(new BorderLayout(0, 12));
        receiptFooter.setOpaque(false);
        receiptFooter.add(totals, BorderLayout.NORTH);
        receiptFooter.add(thanks, BorderLayout.SOUTH);
        JPanel receiptContent = new JPanel(new BorderLayout(0, 18));
        receiptContent.setOpaque(false);
        receiptContent.add(lineItems, BorderLayout.CENTER);
        receiptContent.add(receiptFooter, BorderLayout.SOUTH);
        body.add(receiptContent, BorderLayout.CENTER);

        JPanel bottomStripe = new JPanel();
        bottomStripe.setBackground(accent);
        bottomStripe.setPreferredSize(new Dimension(1, 7));
        paper.add(body, BorderLayout.CENTER);
        paper.add(bottomStripe, BorderLayout.SOUTH);

        JPanel dialogContent = new JPanel(new BorderLayout(0, 10));
        dialogContent.setBackground(Frame.BACKGROUND);
        dialogContent.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        JScrollPane receiptScroll = new JScrollPane(paper);
        receiptScroll.setPreferredSize(new Dimension(760, 540));
        receiptScroll.setBorder(BorderFactory.createEmptyBorder());
        receiptScroll.getViewport().setBackground(Frame.BACKGROUND);
        dialogContent.add(receiptScroll, BorderLayout.CENTER);

        JButton printButton = new JButton("Print");
        JButton closeButton = new JButton("Close");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(printButton);
        actions.add(closeButton);
        dialogContent.add(actions, BorderLayout.SOUTH);

        String dialogTitle = invoice.transactionId == null ? "Receipt"
                : "Receipt - Transaction " + invoice.transactionId;
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, dialogTitle, Dialog.ModalityType.APPLICATION_MODAL);
        printButton.addActionListener(event -> JOptionPane.showMessageDialog(
                dialog, "Printing is not available yet.", "Print Receipt", JOptionPane.INFORMATION_MESSAGE));
        closeButton.addActionListener(event -> dialog.dispose());
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(dialogContent);
        dialog.getRootPane().setDefaultButton(closeButton);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String formatReceiptAmount(BigDecimal amount) {
        return "₱" + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Invoice Error", JOptionPane.ERROR_MESSAGE);
    }

    public static final class Invoice {
        public final Long transactionId;
        public final long customerId;
        public final String firstName;
        public final String lastName;
        public final String email;
        public final BigDecimal amount;
        public final Timestamp created;

        private Invoice(Long transactionId, long customerId, String firstName, String lastName,
                String email, BigDecimal amount, Timestamp created) {
            this.transactionId = transactionId;
            this.customerId = customerId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.amount = amount;
            this.created = created;
        }

        public String getStatus() {
            return transactionId == null ? "No transaction" : "Paid";
        }

        public String getCustomerName() {
            return ((firstName == null) ? "" : firstName) + " " + ((lastName == null) ? "" : lastName);
        }
    }
}
