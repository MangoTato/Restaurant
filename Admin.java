import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Admin extends JPanel {
    private final Frame mainFrame;
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final EmployeePanel operationsHost;

    @Override
    public void removeNotify() {
        if (operationsHost != null) {
            operationsHost.detachAdminPanelListeners();
        }
        super.removeNotify();
    }

    public Admin(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;
        operationsHost = new EmployeePanel(mainFrame, username);

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.BACKGROUND);
        setBorder(new EmptyBorder(18, 28, 28, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));
        JLabel pageTitle = mainFrame.createLabel("Pâques • Administrator • " + username, 22, Frame.NAVY);
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.add(pageTitle, BorderLayout.WEST);

        JButton logout = new JButton("Sign out");
        styleActionButton(logout, false, true);
        logout.addActionListener(e -> mainFrame.signOut());
        header.add(logout, BorderLayout.EAST);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setBackground(Frame.ACCENT);
        actions.setPreferredSize(new Dimension(220, 0));
        actions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 226, 220)),
                new EmptyBorder(8, 8, 8, 8)));

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 0, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        List<String> actionNames = List.of("Overview", "Manage menu", "Staff", "Customer", "Reports", "Tables",
                "Front Desk", "Kitchen", "Cashier");

        for (String action : actionNames) {
            JButton button = new JButton(action);
            styleNavigationButton(button, false);
            button.setIcon(new NavigationIcon(action));
            button.addActionListener(e -> {
                setActiveNavigation(button);
                handleAdminAction(action);
            });
            navigationButtons.add(button);
            buttonPanel.add(button);
        }
        JPanel buttonHolder = new JPanel(new BorderLayout());
        buttonHolder.setBackground(Frame.ACCENT);
        buttonHolder.setBorder(new EmptyBorder(4, 4, 4, 4));
        buttonHolder.add(buttonPanel, BorderLayout.NORTH);
        actions.add(buttonHolder, BorderLayout.NORTH);
        setActiveNavigation(navigationButtons.get(0));

        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 226, 220)),
                new EmptyBorder(0, 0, 0, 0)));
        contentPanel.add(overviewPanel(), "Home");
        contentLayout.show(contentPanel, "Home");

        JPanel mainArea = new JPanel(new BorderLayout(16, 0));
        mainArea.setOpaque(false);
        mainArea.add(actions, BorderLayout.WEST);
        mainArea.add(contentPanel, BorderLayout.CENTER);

        JLabel status = new JLabel("Administrator  ·  " + username
                + "     |     Choose a section to manage today's restaurant operations.");
        status.setFont(new Font("SansSerif", Font.PLAIN, 12));
        status.setForeground(new Color(100, 105, 110));
        status.setBorder(new EmptyBorder(10, 8, 0, 8));

        add(header, BorderLayout.NORTH);
        add(mainArea, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private void handleAdminAction(String action) {
        operationsHost.detachAdminPanelListeners();
        if ("Manage menu".equals(action)) {
            ManageMenu manageMenu = new ManageMenu(mainFrame);

            contentPanel.removeAll();
            contentPanel.add(manageMenu.ShowMenuPanel(), "Manage menu");
            contentLayout.show(contentPanel, "Manage menu");

        } else if ("Staff".equals(action)) {
            Staff staff = new Staff(mainFrame);
            contentPanel.removeAll();
            contentPanel.add(staff.showStaff(), "Staff");
            contentLayout.show(contentPanel, "Staff");
        } else if ("Customer".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(new CustomerLog(mainFrame), "Customer");
            contentLayout.show(contentPanel, "Customer");
        } else if ("Tables".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(new TableServicePanel(mainFrame), "Tables");
            contentLayout.show(contentPanel, "Tables");
        } else if ("Front Desk".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(new Registration(mainFrame), "Front Desk");
            contentLayout.show(contentPanel, "Front Desk");
        } else if ("Kitchen".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(operationsHost.createAdminKitchenPanel(), "Kitchen");
            contentLayout.show(contentPanel, "Kitchen");
        } else if ("Cashier".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(operationsHost.createAdminCashierPanel(), "Cashier");
            contentLayout.show(contentPanel, "Cashier");
        } else if ("Reports".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(createReportsPanel(), "Reports");
            contentLayout.show(contentPanel, "Reports");
        } else if ("Overview".equals(action)) {
            contentPanel.removeAll();
            contentPanel.add(overviewPanel(), "Home");
            contentLayout.show(contentPanel, "Home");
        } else {
            contentPanel.removeAll();
            contentPanel.add(overviewPanel(), action);
            contentLayout.show(contentPanel, action);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel overviewPanel() {
        return new Overview(mainFrame);
    }

    private void setActiveNavigation(JButton activeButton) {
        for (JButton button : navigationButtons) {
            boolean selected = button == activeButton;
            styleNavigationButton(button, selected);
        }
    }

    private void styleNavigationButton(JButton button, boolean selected) {
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(12);
        button.setPreferredSize(new Dimension(180, 44));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Color.WHITE : Frame.ACCENT),
                new EmptyBorder(8, 12, 8, 12)));
        Frame.styleButtonState(button, selected);
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 14));
    }

    private void styleReportStatusButton(JButton button, boolean selected) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, selected);
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Frame.ACCENT : Frame.BORDER),
                new EmptyBorder(7, 10, 7, 10)));
    }

    private void styleActionButton(JButton button, boolean selected, boolean accent) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(8, 14, 8, 14)));
        Frame.styleButtonState(button, selected);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
    }

    private static final class NavigationIcon implements Icon {
        private final String action;

        private NavigationIcon(String action) {
            this.action = action;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setColor(component.getForeground());
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (action) {
            case "Overview":
                g.drawLine(2, 8, 9, 2);
                g.drawLine(9, 2, 16, 8);
                g.drawRect(4, 8, 10, 8);
                g.drawRect(8, 11, 3, 5);
                break;
            case "Manage menu":
                g.drawRoundRect(2, 2, 14, 14, 2, 2);
                g.drawLine(5, 6, 13, 6);
                g.drawLine(5, 9, 13, 9);
                g.drawLine(5, 12, 11, 12);
                break;
            case "Staff":
                g.drawOval(6, 2, 6, 6);
                g.drawArc(3, 9, 12, 9, 0, 180);
                break;
            case "Customer":
                g.drawOval(6, 2, 6, 6);
                g.drawArc(3, 9, 12, 9, 0, 180);
                g.drawOval(1, 5, 4, 4);
                g.drawArc(0, 11, 6, 6, 0, 180);
                break;
            case "Reports":
                g.drawRect(3, 2, 12, 14);
                g.drawLine(6, 12, 6, 9);
                g.drawLine(9, 12, 9, 6);
                g.drawLine(12, 12, 12, 8);
                break;
            case "Tables":
                g.drawRect(2, 3, 14, 5);
                g.drawLine(4, 8, 4, 15);
                g.drawLine(14, 8, 14, 15);
                break;
            default:
                g.drawOval(3, 3, 12, 12);
                g.drawLine(9, 5, 9, 9);
                g.drawLine(9, 9, 12, 11);
                break;
            }
            g.dispose();
        }
    }

    private static final class NavigationButtonCard extends JPanel {
        private NavigationButtonCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g.setColor(new Color(232, 229, 223));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.dispose();
        }
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(Frame.BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Sales reports");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Frame.NAVY);
        JLabel subtitle = new JLabel("Review, filter, and export customer transactions");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Frame.MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(3));
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);

        JPanel invoicePanel = new JPanel(new BorderLayout(8, 10));
        invoicePanel.setBackground(Frame.CARD);
        invoicePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(14, 14, 12, 14)));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        controls.setOpaque(false);
        JComboBox<String> period = new JComboBox<>(new String[] { "Last 7 days", "This month", "All time" });
        period.setBackground(Color.WHITE);
        period.setForeground(Frame.NAVY);
        JTextField fromDate = new JTextField(LocalDate.now().minusDays(6).toString(), 10);
        JTextField toDate = new JTextField(LocalDate.now().toString(), 10);
        JButton loadButton = new JButton("Load");
        JTextField search = new JTextField(16);
        JButton exportButton = new JButton("Export");
        JButton detailsButton = new JButton("View receipt");
        styleActionButton(loadButton, false, true);
        styleActionButton(exportButton, false, false);
        styleActionButton(detailsButton, false, false);
        controls.add(period);
        controls.add(new JLabel("From"));
        controls.add(fromDate);
        controls.add(new JLabel("To"));
        controls.add(toDate);
        controls.add(loadButton);
        controls.add(new JLabel("Filter"));
        controls.add(search);
        controls.add(exportButton);
        controls.add(detailsButton);

        String[] columns = { "Transaction ID", "Customer", "Email", "Status", "Amount",
                "Transaction Date", "Invoice Data" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable invoicesTable = new JTable(model);
        invoicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        invoicesTable.setRowHeight(34);
        invoicesTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        invoicesTable.setAutoCreateRowSorter(true);
        invoicesTable.getTableHeader().setReorderingAllowed(false);
        invoicesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        invoicesTable.getTableHeader().setBackground(Frame.BACKGROUND);
        invoicesTable.getTableHeader().setForeground(Frame.NAVY);
        invoicesTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        invoicesTable.setGridColor(Frame.BORDER);
        invoicesTable.setShowVerticalLines(false);
        invoicesTable.setIntercellSpacing(new Dimension(0, 1));
        invoicesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        invoicesTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        invoicesTable.getColumnModel().getColumn(1).setPreferredWidth(145);
        invoicesTable.getColumnModel().getColumn(2).setPreferredWidth(175);
        invoicesTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        invoicesTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        invoicesTable.getColumnModel().getColumn(5).setPreferredWidth(145);
        invoicesTable.removeColumn(invoicesTable.getColumnModel().getColumn(6));
        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        invoicesTable.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        invoicesTable.getColumnModel().getColumn(3).setCellRenderer(centeredRenderer);
        invoicesTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        invoicesTable.getColumnModel().getColumn(5).setCellRenderer(centeredRenderer);
        invoicesTable.setFillsViewportHeight(true);

        JPanel top = new JPanel(new BorderLayout(4, 8));
        top.setOpaque(false);
        top.add(controls, BorderLayout.NORTH);
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusBar.setOpaque(false);
        String[] statusNames = { "All", "Draft", "Outstanding", "Past due", "Paid" };
        JButton[] statusButtons = new JButton[statusNames.length];
        JLabel summary = new JLabel(" ");
        for (int i = 0; i < statusNames.length; i++) {
            JButton button = new JButton(statusNames[i]);
            styleReportStatusButton(button, i == 0);
            button.putClientProperty("statusFilter", statusNames[i]);
            statusButtons[i] = button;
            statusBar.add(button);
        }
        top.add(statusBar, BorderLayout.CENTER);
        invoicePanel.add(top, BorderLayout.NORTH);
        JScrollPane invoiceScroll = new JScrollPane(invoicesTable);
        invoiceScroll.setBorder(BorderFactory.createLineBorder(Frame.BORDER));
        invoiceScroll.getViewport().setBackground(Color.WHITE);
        invoicePanel.add(invoiceScroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setForeground(Frame.NAVY);
        footer.add(summary, BorderLayout.WEST);
        JLabel help = new JLabel("Double-click a transaction row to view the receipt.");
        help.setFont(new Font("SansSerif", Font.PLAIN, 11));
        help.setForeground(Frame.MUTED);
        footer.add(help, BorderLayout.EAST);
        invoicePanel.add(footer, BorderLayout.SOUTH);
        panel.add(invoicePanel, BorderLayout.CENTER);

        List<InvoiceService.Invoice> allInvoices = new java.util.ArrayList<>();
        final String[] selectedStatus = { "All" };
        Runnable refreshRows = () -> {
            model.setRowCount(0);
            String query = search.getText().trim().toLowerCase(Locale.ROOT);
            BigDecimal visibleTotal = BigDecimal.ZERO;
            int visibleCount = 0;
            for (InvoiceService.Invoice invoice : allInvoices) {
                if (!"All".equals(selectedStatus[0]) && !selectedStatus[0].equals(invoice.getStatus())) {
                    continue;
                }
                String searchable = (invoice.transactionId + " " + invoice.email + " "
                        + invoice.getCustomerName() + " " + invoice.customerId + " " + invoice.getStatus() + " "
                        + invoice.amount + " " + invoice.created).toLowerCase(Locale.ROOT);
                if (!query.isEmpty() && !searchable.contains(query)) {
                    continue;
                }
                String amount = invoice.amount == null ? "₱0.00"
                        : "₱" + invoice.amount.setScale(2, java.math.RoundingMode.HALF_UP);
                String transactionDate = invoice.created == null ? "" : invoice.created.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String customerName = invoice.getCustomerName().trim();
                if (customerName.isEmpty()) {
                    customerName = "Customer " + invoice.customerId;
                }
                model.addRow(new Object[] { invoice.transactionId == null ? "" : invoice.transactionId,
                        customerName, invoice.email == null ? "" : invoice.email, invoice.getStatus(),
                        amount, transactionDate, invoice });
                if (invoice.amount != null) {
                    visibleTotal = visibleTotal.add(invoice.amount);
                }
                visibleCount++;
            }
            summary.setText(visibleCount + " transactions  ·  Total: ₱"
                    + visibleTotal.setScale(2, java.math.RoundingMode.HALF_UP));
            for (JButton button : statusButtons) {
                String name = (String) button.getClientProperty("statusFilter");
                int count = "All".equals(name) ? allInvoices.size()
                        : (int) allInvoices.stream().filter(invoice -> name.equals(invoice.getStatus())).count();
                button.setText(name + "  " + count);
                button.setEnabled(count > 0 || name.equals(selectedStatus[0]));
                styleReportStatusButton(button, name.equals(selectedStatus[0]));
            }
            invoicesTable.clearSelection();
        };

        for (JButton button : statusButtons) {
            button.addActionListener(event -> {
                selectedStatus[0] = (String) button.getClientProperty("statusFilter");
                refreshRows.run();
            });
        }
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshRows.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshRows.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshRows.run();
            }
        });
        period.addActionListener(event -> {
            LocalDate today = LocalDate.now();
            String selection = (String) period.getSelectedItem();
            if ("Last 7 days".equals(selection)) {
                fromDate.setText(today.minusDays(6).toString());
                toDate.setText(today.toString());
            } else if ("This month".equals(selection)) {
                fromDate.setText(today.withDayOfMonth(1).toString());
                toDate.setText(today.toString());
            } else if ("All time".equals(selection)) {
                fromDate.setText("1000-01-01");
                toDate.setText(today.toString());
            }
        });

        loadButton.addActionListener(event -> {
            final LocalDate start;
            final LocalDate end;
            try {
                start = LocalDate.parse(fromDate.getText().trim());
                end = LocalDate.parse(toDate.getText().trim());
                if (end.isBefore(start)) {
                    throw new IllegalArgumentException("The end date must be on or after the start date.");
                }
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(mainFrame, "Enter a valid date range using YYYY-MM-DD.",
                        "Invalid Date Range", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadButton.setEnabled(false);
            summary.setText("Loading invoices from the database...");
            new SwingWorker<List<InvoiceService.Invoice>, Void>() {
                @Override
                protected List<InvoiceService.Invoice> doInBackground() throws SQLException {
                    return InvoiceService.loadInvoices(start, end);
                }

                @Override
                protected void done() {
                    loadButton.setEnabled(true);
                    try {
                        allInvoices.clear();
                        allInvoices.addAll(get());
                        refreshRows.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        summary.setText("Invoice loading was interrupted.");
                        JOptionPane.showMessageDialog(mainFrame, "Invoice loading was interrupted.",
                                "Report Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        summary.setText("Invoices could not be loaded.");
                        JOptionPane.showMessageDialog(mainFrame, "Unable to load sales invoices:\n"
                                + cause.getMessage(), "Report Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        Runnable showSelectedInvoice = () -> {
            int viewRow = invoicesTable.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(mainFrame, "Select an invoice first.", "Invoice Required",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int modelRow = invoicesTable.convertRowIndexToModel(viewRow);
            InvoiceService.Invoice invoice = (InvoiceService.Invoice) model.getValueAt(modelRow, 6);
            InvoiceService.showDetails(mainFrame, invoice);
        };
        detailsButton.addActionListener(event -> showSelectedInvoice.run());
        invoicesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)) {
                    showSelectedInvoice.run();
                }
            }
        });
        exportButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("sales-invoices.csv"));
            if (chooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = chooser.getSelectedFile();
            if (file.exists() && JOptionPane.showConfirmDialog(mainFrame,
                    "Replace " + file.getName() + "?", "Confirm Export", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) {
                return;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                for (int column = 0; column < invoicesTable.getColumnCount(); column++) {
                    if (column > 0) {
                        writer.write(',');
                    }
                    writer.write(csvValue(invoicesTable.getColumnName(column)));
                }
                writer.newLine();
                for (int row = 0; row < invoicesTable.getRowCount(); row++) {
                    for (int column = 0; column < invoicesTable.getColumnCount(); column++) {
                        if (column > 0) {
                            writer.write(',');
                        }
                        Object value = invoicesTable.getValueAt(row, column);
                        writer.write(csvValue(value == null ? "" : value.toString()));
                    }
                    writer.newLine();
                }
                JOptionPane.showMessageDialog(mainFrame, "Exported " + invoicesTable.getRowCount()
                        + " invoices to:\n" + file.getAbsolutePath(), "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainFrame, "Unable to export invoices:\n" + e.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        loadButton.doClick();
        return panel;
    }

    private String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private JPanel createTablesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 35, 30, 35));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        JLabel title = new JLabel("Table Availability");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Frame.NAVY);
        header.add(title, BorderLayout.NORTH);

        JLabel stateLegend = new JLabel(
                "States: Available  |  Reserved  |  In Progress  |  Checkout Requested  |  Completed");
        stateLegend.setFont(new Font("SansSerif", Font.PLAIN, 14));
        stateLegend.setForeground(Color.DARK_GRAY);
        header.add(stateLegend, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        JPanel tableGrid = new JPanel(new GridLayout(0, 2, 12, 12));
        tableGrid.setBackground(Color.WHITE);
        tableGrid.setBorder(new EmptyBorder(4, 4, 4, 4));
        List<TableCard> cards = new java.util.ArrayList<>();
        for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
            TableCard card = new TableCard(table);
            cards.add(card);
            tableGrid.add(card);
        }

        JScrollPane scrollPane = new JScrollPane(tableGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        panel.add(scrollPane, BorderLayout.CENTER);

        Runnable refresh = () -> cards.forEach(TableCard::refresh);
        mainFrame.tableService.addListener(refresh);
        refresh.run();
        return panel;
    }

    private final class TableCard extends JPanel {
        private final int tableNumber;
        private final JLabel statusLabel = new JLabel();
        private final JLabel customerDetails = new JLabel("No customer selected.");
        private final JTextField customerIdField = new JTextField();
        private final JButton openButton = new JButton("Open");
        private final JButton closeButton = new JButton("Close");
        private final JButton startButton = new JButton("Start Table");
        private CustomerDetails selectedCustomer;
        private boolean lookupInProgress;

        private TableCard(int tableNumber) {
            this.tableNumber = tableNumber;
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    new EmptyBorder(12, 12, 12, 12)));

            JPanel heading = new JPanel(new BorderLayout(6, 6));
            heading.setOpaque(false);
            JLabel tableLabel = new JLabel("Table " + tableNumber + " · "
                    + TableService.capacityFor(tableNumber) + " people");
            tableLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            heading.add(tableLabel, BorderLayout.WEST);
            heading.add(statusLabel, BorderLayout.EAST);
            add(heading);
            add(Box.createVerticalStrut(10));

            JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            tableActions.setOpaque(false);
            tableActions.add(openButton);
            tableActions.add(closeButton);
            add(tableActions);
            add(Box.createVerticalStrut(8));

            JLabel customerLabel = new JLabel("Customer ID");
            customerLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            add(customerLabel);

            JPanel lookup = new JPanel(new BorderLayout(6, 0));
            lookup.setOpaque(false);
            lookup.add(customerIdField, BorderLayout.CENTER);
            JButton findButton = new JButton("Find");
            lookup.add(findButton, BorderLayout.EAST);
            add(lookup);
            add(Box.createVerticalStrut(8));

            customerDetails.setFont(new Font("SansSerif", Font.PLAIN, 12));
            customerDetails.setVerticalAlignment(SwingConstants.TOP);
            add(customerDetails);
            add(Box.createVerticalStrut(8));

            startButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(startButton);

            findButton.addActionListener(e -> findCustomer());
            startButton.addActionListener(e -> startTable());
            openButton.addActionListener(e -> openTable());
            closeButton.addActionListener(e -> closeTable());
            Long assignedCustomerId = mainFrame.tableService.getCustomerId(tableNumber);
            if (assignedCustomerId != null) {
                customerIdField.setText(String.valueOf(assignedCustomerId));
                findCustomer();
            }
            refresh();
        }

        private void findCustomer() {
            String customerIdText = customerIdField.getText().trim();
            long customerId;
            try {
                customerId = Long.parseLong(customerIdText);
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
                            return new CustomerDetails(result.getLong("customer_id"),
                                    result.getString("first_name"), result.getString("last_name"),
                                    result.getString("email"), result.getString("phone_number"),
                                    result.getString("status"), result.getTimestamp("scheduled"));
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
                            customerDetails.setText(selectedCustomer.toHtml());
                            if (!mainFrame.tableService.isOccupied(tableNumber)) {
                                mainFrame.tableService.setCustomer(tableNumber, selectedCustomer.id,
                                        selectedCustomer.status);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        customerDetails.setText("Customer lookup was interrupted.");
                        clearUnoccupiedCustomer();
                        JOptionPane.showMessageDialog(mainFrame, "Customer lookup was interrupted.",
                                "Customer Lookup Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        customerDetails.setText("Unable to load customer details.");
                        clearUnoccupiedCustomer();
                        JOptionPane.showMessageDialog(mainFrame,
                                "Unable to load customer details:\n" + cause.getMessage(),
                                "Customer Lookup Error", JOptionPane.ERROR_MESSAGE);
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

        private void startTable() {
            if (selectedCustomer == null
                    || !String.valueOf(selectedCustomer.id).equals(customerIdField.getText().trim())) {
                JOptionPane.showMessageDialog(mainFrame, "Find a valid customer before starting this table.",
                        "Customer Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!mainFrame.tableService.occupy(tableNumber)) {
                JOptionPane.showMessageDialog(mainFrame, "This table is already open.", "Table Unavailable",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            mainFrame.tableService.setCustomer(tableNumber, selectedCustomer.id, selectedCustomer.status);
        }

        private void openTable() {
            if (selectedCustomer != null) {
                mainFrame.tableService.setCustomer(tableNumber, selectedCustomer.id, selectedCustomer.status);
            }
            mainFrame.tableService.occupy(tableNumber);
        }

        private void closeTable() {
            mainFrame.orderService.clearTableOrders(tableNumber);
            mainFrame.tableService.clear(tableNumber);
            selectedCustomer = null;
            customerIdField.setText("");
            customerDetails.setText("No customer selected.");
            refresh();
        }

        private void refresh() {
            boolean occupied = mainFrame.tableService.isOccupied(tableNumber);
            String state;
            Color stateColor;
            if (mainFrame.tableService.isCheckoutRequested(tableNumber)) {
                state = "Status: Checkout requested";
                stateColor = Frame.SUCCESS;
            } else if (occupied) {
                state = "Status: In progress";
                stateColor = Frame.ACCENT_DARK;
            } else if ("Reserved".equalsIgnoreCase(mainFrame.tableService.getCustomerStatus(tableNumber))) {
                state = "Status: Reserved";
                stateColor = Frame.ACCENT_DARK;
            } else {
                state = "Status: Available";
                stateColor = Frame.SUCCESS;
            }
            statusLabel.setText(state);
            statusLabel.setForeground(stateColor);
            openButton.setEnabled(!occupied && !lookupInProgress);
            closeButton.setEnabled(occupied || mainFrame.tableService.getCustomerId(tableNumber) != null);
            startButton.setEnabled(!occupied && selectedCustomer != null && !lookupInProgress);
        }
    }

    private static final class CustomerDetails {
        private final long id;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String phone;
        private final String status;
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

    public void ShowAddUserDialog() {
        new AddUser(mainFrame).ShowAddUserDialog();
    }

}
