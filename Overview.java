import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Live restaurant overview for administrators. */
public class Overview extends Frame.BackgroundPanel {
    private static final Color[] CHART_COLORS = {
            new Color(59, 143, 94), Frame.ACCENT,
            new Color(244, 218, 192), new Color(180, 183, 181)
    };
    private final Frame mainFrame;
    private final JLabel loadStatus = new JLabel("Loading overview data...");
    private final JPanel popularDishes = new JPanel(new GridLayout(1, 3, 10, 0));
    private final JLabel salesTotal = new JLabel("Loading...");
    private final JLabel salesChange = new JLabel(" ");
    private final JLabel overallIncome = new JLabel("Loading...");
    private final JLabel categorySummary = new JLabel(" ");
    private final JLabel staffCount = new JLabel(" ");
    private final JPanel tableLegend = new JPanel(new GridLayout(0, 1, 2, 2));
    private final JPanel categoryLegend = new JPanel();
    private final JComboBox<String> categoryPeriod = new JComboBox<>(
            new String[] { "Today", "Yesterday", "Last 7 days", "Last Month", "Last Year" });
    private final JPanel staffCards = new JPanel(new GridLayout(1, 3, 12, 0));
    private final TableStatusChart tableChart = new TableStatusChart();
    private final CategoryChart categoryChart = new CategoryChart();
    private final SalesChart salesChart = new SalesChart();
    private final Runnable tableListener = this::refreshTableStatus;
    private final Runnable orderListener = () -> SwingUtilities.invokeLater(() -> loadOverviewData(false));
    private final Timer overviewRefreshTimer = new Timer(30_000, event -> refreshOverviewData(false));
    private SwingWorker<OverviewData, Void> overviewWorker;
    private boolean overviewRefreshPending;
    private boolean pendingRefreshShowsErrors;
    private boolean listening;
    private int categoryLoadGeneration;

    public Overview(Frame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(0, 10));
        setBackground(Frame.BACKGROUND);
        setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 27));
        title.setForeground(Frame.NAVY);
        header.add(title, BorderLayout.WEST);
        loadStatus.setFont(new Font("SansSerif", Font.PLAIN, 13));
        loadStatus.setForeground(Frame.MUTED);
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(refreshButton, false);
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT), new EmptyBorder(7, 12, 7, 12)));
        refreshButton.addActionListener(event -> refreshOverviewData(true));
        JButton signOutButton = new JButton("Sign out");
        signOutButton.putClientProperty("pâques.force.filled", true);
        signOutButton.putClientProperty("pâques.danger", true);
        signOutButton.setFocusPainted(false);
        signOutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(signOutButton, false);
        signOutButton.putClientProperty("pâques.disable.button.hover", true);
        signOutButton.setBackground(new Color(196, 64, 64));
        signOutButton.setForeground(Color.WHITE);
        signOutButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        signOutButton.addActionListener(event -> mainFrame.signOut());
        headerActions.add(loadStatus);
        headerActions.add(refreshButton);
        headerActions.add(signOutButton);
        header.add(headerActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel dashboard = new JPanel(new GridBagLayout());
        dashboard.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 12, 12);
        gbc.weightx = 1;
        gbc.weighty = 0.46;
        gbc.gridx = 0;
        gbc.gridy = 0;
        dashboard.add(createPopularOrderCard(), gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        dashboard.add(createTableStatusCard(), gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0.46;
        gbc.insets = new Insets(0, 0, 12, 12);
        dashboard.add(createSalesCard(), gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        dashboard.add(createCategoryCard(), gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 0.08;
        gbc.insets = new Insets(0, 0, 0, 0);
        dashboard.add(createStaffCard(), gbc);
        JScrollPane scroll = new JScrollPane(dashboard);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(getBackground());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        refreshTableStatus();
        loadOverviewData(true);
        loadCategorySalesForSelectedPeriod();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!listening) {
            mainFrame.tableService.addListener(tableListener);
            mainFrame.orderService.addListener(orderListener);
            listening = true;
        }
        if (!overviewRefreshTimer.isRunning()) {
            overviewRefreshTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        overviewRefreshTimer.stop();
        if (listening) {
            mainFrame.tableService.removeListener(tableListener);
            mainFrame.orderService.removeListener(orderListener);
            listening = false;
        }
        super.removeNotify();
    }

    private JPanel createPopularOrderCard() {
        JPanel card = createCard("Popular");
        popularDishes.setOpaque(false);
        popularDishes.add(new JLabel("Loading popular dishes..."));
        card.add(popularDishes, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPopularDishCard(PopularDish dish) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Frame.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER),
                new EmptyBorder(7, 7, 7, 7)));

        JLabel image = new JLabel("Popular dish", SwingConstants.CENTER);
        image.setOpaque(true);
        image.setBackground(Frame.BACKGROUND);
        image.setPreferredSize(new Dimension(120, 94));
        image.setBorder(BorderFactory.createLineBorder(Frame.BORDER));
        if (dish != null) {
            setDishImage(image, dish.imagePath);
        }
        card.add(image, BorderLayout.NORTH);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(dish == null ? "Loading..." : dish.name);
        name.setFont(new Font("SansSerif", Font.BOLD, 14));
        name.setForeground(Frame.NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(name);

        if (dish != null) {
            JLabel price = new JLabel(String.format("₱%,.2f", dish.price));
            price.setFont(new Font("SansSerif", Font.BOLD, 14));
            price.setForeground(Frame.ACCENT);
            price.setAlignmentX(Component.LEFT_ALIGNMENT);
            details.add(price);

            JLabel sales = new JLabel(dish.unitsSold + " sold");
            sales.setFont(new Font("SansSerif", Font.BOLD, 12));
            sales.setForeground(new Color(59, 143, 94));
            sales.setAlignmentX(Component.LEFT_ALIGNMENT);
            details.add(sales);

            String description = dish.description == null || dish.description.trim().isEmpty()
                    ? "No description provided." : dish.description;
            JLabel descriptionLabel = new JLabel("<html><div style='width:105px'>"
                    + escapeHtml(description) + "</div></html>");
            descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            descriptionLabel.setForeground(new Color(75, 75, 75));
            descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            details.add(descriptionLabel);

            JLabel cookingTime = new JLabel(dish.cookingTime + " min");
            cookingTime.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cookingTime.setForeground(new Color(95, 95, 95));
            cookingTime.setAlignmentX(Component.LEFT_ALIGNMENT);
            details.add(Box.createVerticalStrut(3));
            details.add(cookingTime);
        }
        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableStatusCard() {
        JPanel card = createCard("Table Status");
        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setOpaque(false);
        tableChart.setPreferredSize(new Dimension(220, 205));
        content.add(tableChart, BorderLayout.CENTER);
        tableLegend.setOpaque(false);
        for (TableService.Status status : TableService.Status.values()) {
            JLabel entry = new JLabel();
            entry.setFont(new Font("SansSerif", Font.PLAIN, 13));
            entry.setName(status.name());
            tableLegend.add(entry);
        }
        content.add(tableLegend, BorderLayout.EAST);
        card.add(content, BorderLayout.CENTER);
        refreshTableStatus();
        return card;
    }

    private JPanel createSalesCard() {
        JPanel card = createCard("Sales Overview · Today");
        JPanel content = new JPanel(new BorderLayout(6, 8));
        content.setOpaque(false);
        JPanel totals = new JPanel(new BorderLayout(10, 0));
        totals.setOpaque(false);
        JPanel todayBlock = new JPanel();
        todayBlock.setOpaque(false);
        todayBlock.setLayout(new BoxLayout(todayBlock, BoxLayout.Y_AXIS));
        JLabel todayLabel = new JLabel("Today's sales");
        todayLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        todayLabel.setForeground(new Color(105, 105, 105));
        salesTotal.setFont(new Font("SansSerif", Font.BOLD, 25));
        salesTotal.setForeground(new Color(30, 30, 30));
        salesChange.setFont(new Font("SansSerif", Font.PLAIN, 13));
        todayBlock.add(todayLabel);
        todayBlock.add(salesTotal);
        todayBlock.add(salesChange);
        JPanel lifetimeBlock = new JPanel();
        lifetimeBlock.setOpaque(false);
        lifetimeBlock.setLayout(new BoxLayout(lifetimeBlock, BoxLayout.Y_AXIS));
        JLabel lifetimeLabel = new JLabel("Overall income");
        lifetimeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lifetimeLabel.setForeground(new Color(105, 105, 105));
        overallIncome.setFont(new Font("SansSerif", Font.BOLD, 18));
        overallIncome.setForeground(new Color(59, 143, 94));
        lifetimeBlock.add(lifetimeLabel);
        lifetimeBlock.add(overallIncome);
        totals.add(todayBlock, BorderLayout.CENTER);
        totals.add(lifetimeBlock, BorderLayout.EAST);
        content.add(totals, BorderLayout.NORTH);
        salesChart.setPreferredSize(new Dimension(400, 145));
        content.add(salesChart, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCategoryCard() {
        JPanel card = createCard("Sales by Category");
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        categoryPeriod.setFont(new Font("SansSerif", Font.PLAIN, 12));
        heading.add(categoryPeriod, BorderLayout.EAST);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(heading, BorderLayout.NORTH);
        categoryChart.setPreferredSize(new Dimension(190, 210));
        content.add(categoryChart, BorderLayout.WEST);
        categoryLegend.setOpaque(false);
        categoryLegend.setLayout(new BoxLayout(categoryLegend, BoxLayout.Y_AXIS));
        categoryLegend.add(new JLabel("Loading category sales..."));
        content.add(categoryLegend, BorderLayout.CENTER);
        categorySummary.setFont(new Font("SansSerif", Font.PLAIN, 12));
        categorySummary.setForeground(new Color(110, 110, 110));
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(categorySummary, BorderLayout.WEST);
        content.add(footer, BorderLayout.SOUTH);
        card.add(content, BorderLayout.CENTER);
        categoryPeriod.addActionListener(event -> loadCategorySalesForSelectedPeriod(true));
        return card;
    }

    private JPanel createStaffCard() {
        JPanel card = createCard("Staff Management");
        staffCount.setFont(new Font("SansSerif", Font.PLAIN, 13));
        staffCount.setForeground(new Color(90, 90, 90));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(staffCount, BorderLayout.NORTH);
        staffCards.setOpaque(false);
        content.add(staffCards, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(500, 150));
        card.setMinimumSize(new Dimension(230, 145));
        return card;
    }

    private JPanel createCard(String title) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 226, 220)),
                new EmptyBorder(10, 12, 10, 12)));
        card.setMinimumSize(new Dimension(230, 170));
        card.setPreferredSize(new Dimension(500, 235));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 16));
        heading.setForeground(new Color(45, 45, 45));
        heading.setBorder(new EmptyBorder(0, 0, 3, 0));
        card.add(heading, BorderLayout.NORTH);
        return card;
    }

    private void refreshTableStatus() {
        int[] counts = new int[TableService.Status.values().length];
        for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
            counts[mainFrame.tableService.getStatus(table).ordinal()]++;
        }
        tableChart.setCounts(counts);
        for (Component entry : tableLegend.getComponents()) {
            if (entry instanceof JLabel) {
                JLabel label = (JLabel) entry;
                TableService.Status status = TableService.Status.valueOf(label.getName());
                label.setText(statusLabel(status) + "  " + counts[status.ordinal()]);
                label.setForeground(CHART_COLORS[status.ordinal()]);
            }
        }
        tableChart.setToolTipText("Total tables: " + TableService.TABLE_COUNT);
    }

    private static String statusLabel(TableService.Status status) {
        switch (status) {
        case OCCUPIED:
            return "Occupied";
        case RESERVED:
            return "Reserved";
        case CLEANING:
            return "Cleaning";
        default:
            return "Available";
        }
    }

    private void loadOverviewData(boolean showErrors) {
        if (overviewWorker != null && !overviewWorker.isDone()) {
            overviewRefreshPending = true;
            pendingRefreshShowsErrors |= showErrors;
            return;
        }
        overviewWorker = new SwingWorker<OverviewData, Void>() {
            @Override
            protected OverviewData doInBackground() throws SQLException {
                OverviewData data = new OverviewData();
                try (Connection connection = database.getConnection()) {
                    database.ensureOverviewSchema(connection);
                    InvoiceService.ensureInvoiceSchema(connection);
                    loadPopularOrders(connection, data);
                    loadSales(connection, data);
                    loadStaff(connection, data);
                }
                return data;
            }

            @Override
            protected void done() {
                if (overviewWorker != this) {
                    return;
                }
                overviewWorker = null;
                try {
                    applyData(get());
                    loadStatus.setText("Updated just now");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    loadStatus.setText("Overview loading was interrupted.");
                    if (showErrors) {
                        showLoadError("Overview loading was interrupted.");
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    loadStatus.setText("Overview data could not be loaded.");
                    if (showErrors) {
                        showLoadError("Unable to load overview data:\n" + cause.getMessage());
                    }
                } finally {
                    if (overviewRefreshPending) {
                        boolean showPendingErrors = pendingRefreshShowsErrors;
                        overviewRefreshPending = false;
                        pendingRefreshShowsErrors = false;
                        loadOverviewData(showPendingErrors);
                    }
                }
            }
        };
        overviewWorker.execute();
    }

    private void refreshOverviewData(boolean showErrors) {
        loadOverviewData(showErrors);
        loadCategorySalesForSelectedPeriod(showErrors);
    }

    private void loadPopularOrders(Connection connection, OverviewData data) throws SQLException {
        String sql = "SELECT ri.item_name, COALESCE(m.price, ri.unit_price) AS price, "
                + "m.item_description, COALESCE(m.cooking_time_minutes, 20) AS cooking_time_minutes, "
                + "m.image_path, "
                + "SUM(ri.quantity) AS units_ordered FROM receipt_item ri "
                + "JOIN receipt r ON r.receipt_id = ri.receipt_id "
                + "LEFT JOIN menu m ON m.item_id = ri.item_id "
                + "GROUP BY ri.item_id, ri.item_name, ri.unit_price, m.price, m.item_description, "
                + "m.cooking_time_minutes, m.image_path ORDER BY units_ordered DESC, ri.item_name LIMIT 3";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                data.popularDishes.add(new PopularDish(result.getString("item_name"),
                        result.getBigDecimal("price").doubleValue(), result.getString("item_description"),
                        result.getInt("cooking_time_minutes"), result.getString("image_path"),
                        result.getLong("units_ordered")));
            }
        }
    }

    private void loadSales(Connection connection, OverviewData data) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        String sql = "SELECT transaction_datetime, amount FROM `transaction` "
                + "WHERE transaction_datetime >= ? AND transaction_datetime < ? "
                + "ORDER BY transaction_datetime";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(start));
            statement.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    double amount = result.getBigDecimal("amount").doubleValue();
                    int hour = result.getTimestamp("transaction_datetime").toLocalDateTime().getHour();
                    data.salesByHour[hour] += amount;
                    data.todaySales += amount;
                }
            }
        }

        String previousSql = "SELECT COALESCE(SUM(amount), 0) AS total FROM `transaction` "
                + "WHERE transaction_datetime >= ? AND transaction_datetime < ?";
        try (PreparedStatement statement = connection.prepareStatement(previousSql)) {
            statement.setTimestamp(1, Timestamp.valueOf(today.minusDays(1).atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(start));
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    data.yesterdaySales = result.getBigDecimal("total").doubleValue();
                }
            }
        }

        String overallSql = "SELECT COALESCE(SUM(amount), 0) AS total FROM `transaction`";
        try (PreparedStatement statement = connection.prepareStatement(overallSql);
                ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                data.overallIncome = result.getBigDecimal("total").doubleValue();
            }
        }
    }

    private void loadCategorySales(Connection connection, OverviewData data, LocalDate start, LocalDate end)
            throws SQLException {
        String sql = "SELECT m.item_category, SUM(ri.unit_price * ri.quantity) AS total "
                + "FROM receipt_item ri JOIN receipt r ON r.receipt_id = ri.receipt_id "
                + "JOIN `transaction` t ON t.transaction_id = r.transaction_id "
                + "JOIN menu m ON m.item_id = ri.item_id "
                + "JOIN customer c ON c.customer_id = r.customer_id "
                + "WHERE r.chef_received_at IS NOT NULL AND c.status = 'Completed' "
                + "AND t.transaction_datetime >= ? AND t.transaction_datetime < ? "
                + "GROUP BY m.item_category "
                + "ORDER BY total DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(end.atStartOfDay()));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    data.categorySales.put(displayCategory(result.getString("item_category")),
                            result.getBigDecimal("total").doubleValue());
                }
            }
        }
    }

    private void loadCategorySalesForSelectedPeriod() {
        loadCategorySalesForSelectedPeriod(true);
    }

    private void loadCategorySalesForSelectedPeriod(boolean showErrors) {
        String selected = (String) categoryPeriod.getSelectedItem();
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end = today.plusDays(1);
        if ("Yesterday".equals(selected)) {
            start = today.minusDays(1);
            end = today;
        } else if ("Last 7 days".equals(selected)) {
            start = today.minusDays(6);
        } else if ("Last Month".equals(selected)) {
            start = today.withDayOfMonth(1).minusMonths(1);
            end = today.withDayOfMonth(1);
        } else if ("Last Year".equals(selected)) {
            start = today.minusYears(1);
        } else {
            start = today;
        }
        final LocalDate rangeStart = start;
        final LocalDate rangeEnd = end;
        final int generation = ++categoryLoadGeneration;
        categoryLegend.removeAll();
        categoryLegend.add(new JLabel("Loading category sales..."));
        categoryLegend.revalidate();
        categoryLegend.repaint();

        new SwingWorker<OverviewData, Void>() {
            @Override
            protected OverviewData doInBackground() throws SQLException {
                OverviewData data = new OverviewData();
                try (Connection connection = database.getConnection()) {
                    InvoiceService.ensureInvoiceSchema(connection);
                    loadCategorySales(connection, data, rangeStart, rangeEnd);
                }
                return data;
            }

            @Override
            protected void done() {
                if (generation != categoryLoadGeneration) {
                    return;
                }
                try {
                    applyCategoryData(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    loadStatus.setText("Loading category sales was interrupted.");
                    if (showErrors) {
                        showLoadError("Loading category sales was interrupted.");
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    loadStatus.setText("Category sales could not be loaded.");
                    if (showErrors) {
                        showLoadError("Unable to load category sales:\n" + cause.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void loadStaff(Connection connection, OverviewData data) throws SQLException {
        String sql = "SELECT first_name, last_name, role, status FROM employee "
                + "WHERE status IS NULL OR LOWER(status) <> 'archived' "
                + "ORDER BY first_name, last_name";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String first = result.getString("first_name");
                String last = result.getString("last_name");
                String role = result.getString("role");
                String status = result.getString("status");
                data.staff.add(new StaffMember(((first == null) ? "" : first) + " "
                        + ((last == null) ? "" : last), role == null ? "Staff" : role,
                        status == null || status.trim().isEmpty() ? "Active" : status));
            }
        }
    }

    private static String displayCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Other";
        }
        String normalized = category.trim().toLowerCase();
        if ("main dish".equals(normalized)) {
            return "Food";
        }
        if ("beverages".equals(normalized)) {
            return "Beverages";
        }
        if ("side dish".equals(normalized)) {
            return "Side Dishes";
        }
        return category;
    }

    private void applyData(OverviewData data) {
        popularDishes.removeAll();
        if (data.popularDishes.isEmpty()) {
            popularDishes.add(new JLabel("No completed orders yet."));
        } else {
            for (PopularDish dish : data.popularDishes) {
                popularDishes.add(createPopularDishCard(dish));
            }
        }
        popularDishes.revalidate();
        popularDishes.repaint();

        salesTotal.setText(String.format("₱%,.2f", data.todaySales));
        overallIncome.setText(String.format("₱%,.2f", data.overallIncome));
        if (data.yesterdaySales == 0) {
            salesChange.setText("Compared with yesterday: no prior sales");
        } else {
            double change = (data.todaySales - data.yesterdaySales) * 100.0 / data.yesterdaySales;
            salesChange.setText(String.format("%s %.1f%% vs yesterday",
                    change >= 0 ? "↑" : "↓", Math.abs(change)));
        }
        salesChart.setSales(data.salesByHour);

        applyCategoryData(data);

        staffCards.removeAll();
        staffCount.setText(data.staff.size() + (data.staff.size() == 1 ? " active staff member" : " active staff members"));
        if (data.staff.isEmpty()) {
            staffCards.add(new JLabel("No active staff records found."));
        } else {
            int visibleCount = Math.min(3, data.staff.size());
            for (int index = 0; index < visibleCount; index++) {
                staffCards.add(createStaffCard(data.staff.get(index)));
            }
        }
        categoryLegend.revalidate();
        categoryLegend.repaint();
        staffCards.revalidate();
        staffCards.repaint();
    }

    private void applyCategoryData(OverviewData data) {
        categoryLegend.removeAll();
        if (data.categorySales.isEmpty()) {
            categoryLegend.add(new JLabel("No completed receipt sales yet."));
            categorySummary.setText("Completed category income: ₱0.00");
            categoryChart.setValues(new double[0]);
        } else {
            double total = data.categorySales.values().stream().mapToDouble(Double::doubleValue).sum();
            int index = 0;
            double[] values = new double[data.categorySales.size()];
            for (Map.Entry<String, Double> entry : data.categorySales.entrySet()) {
                double amount = entry.getValue();
                values[index] = amount;
                JLabel line = new JLabel(String.format(
                        "<html><span style='color:%s'>●</span>&nbsp; <b>%s</b>&nbsp;&nbsp; ₱%,.2f&nbsp;&nbsp; (%.0f%%)</html>",
                        colorHex(CHART_COLORS[index % CHART_COLORS.length]), escapeHtml(entry.getKey()),
                        amount, total == 0 ? 0 : amount * 100 / total));
                line.setFont(new Font("SansSerif", Font.BOLD, 13));
                line.setForeground(new Color(45, 45, 45));
                categoryLegend.add(line);
                index++;
            }
            categorySummary.setText("Completed category income: " + String.format("₱%,.2f", total));
            categoryChart.setValues(values);
        }

        categoryLegend.revalidate();
        categoryLegend.repaint();
    }

    private void setDishImage(JLabel imageLabel, String path) {
        imageLabel.setIcon(null);
        imageLabel.setText("Popular dish");
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File imageFile = new File(path);
        if (!imageFile.isAbsolute() && !imageFile.isFile()) {
            imageFile = new File("src", path);
        }
        if (!imageFile.isFile()) {
            return;
        }
        ImageIcon original = new ImageIcon(imageFile.getAbsolutePath());
        if (original.getIconWidth() <= 0 || original.getIconHeight() <= 0) {
            return;
        }
        int width = imageLabel.getPreferredSize().width;
        int height = imageLabel.getPreferredSize().height;
        double scale = Math.min((double) width / original.getIconWidth(), (double) height / original.getIconHeight());
        int scaledWidth = Math.max(1, (int) Math.round(original.getIconWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(original.getIconHeight() * scale));
        Image image = original.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(image));
        imageLabel.setText("");
    }

    private JPanel createStaffCard(StaffMember staff) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Frame.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(232, 229, 223)),
                new EmptyBorder(10, 12, 10, 12)));
        card.setPreferredSize(new Dimension(230, 110));
        card.setMinimumSize(new Dimension(150, 85));
        JLabel name = new JLabel(staff.name.trim());
        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel role = new JLabel(staff.role);
        role.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JLabel status = new JLabel("●  " + staff.status);
        status.setFont(new Font("SansSerif", Font.PLAIN, 13));
        status.setForeground(new Color(48, 133, 83));
        card.add(name);
        card.add(Box.createVerticalStrut(4));
        card.add(role);
        card.add(Box.createVerticalStrut(4));
        card.add(status);
        return card;
    }

    private void showLoadError(String message) {
        JOptionPane.showMessageDialog(mainFrame, message, "Overview Data Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String colorHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static final class OverviewData {
        private final List<PopularDish> popularDishes = new ArrayList<>();
        private double todaySales;
        private double yesterdaySales;
        private double overallIncome;
        private final double[] salesByHour = new double[24];
        private final Map<String, Double> categorySales = new java.util.LinkedHashMap<>();
        private final List<StaffMember> staff = new ArrayList<>();
    }

    private static final class PopularDish {
        private final String name;
        private final double price;
        private final String description;
        private final int cookingTime;
        private final String imagePath;
        private final long unitsSold;

        private PopularDish(String name, double price, String description, int cookingTime, String imagePath,
                long unitsSold) {
            this.name = name;
            this.price = price;
            this.description = description;
            this.cookingTime = cookingTime;
            this.imagePath = imagePath;
            this.unitsSold = unitsSold;
        }
    }

    private static final class StaffMember {
        private final String name;
        private final String role;
        private final String status;

        private StaffMember(String name, String role, String status) {
            this.name = name;
            this.role = role;
            this.status = status;
        }
    }

    private static final class TableStatusChart extends JPanel {
        private int[] counts = new int[4];

        private TableStatusChart() {
            setOpaque(false);
        }

        private void setCounts(int[] counts) {
            this.counts = counts.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.max(100, Math.min(getWidth(), getHeight()) - 28);
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int total = 0;
            for (int count : counts) {
                total += count;
            }
            int start = 90;
            for (int i = 0; i < counts.length; i++) {
                int extent = total == 0 ? 0 : (int) Math.round(counts[i] * 360.0 / total);
                g.setColor(CHART_COLORS[i]);
                g.fill(new Arc2D.Double(x, y, size, size, start, -extent, Arc2D.PIE));
                start -= extent;
            }
            g.setColor(Color.WHITE);
            int inset = Math.max(24, size / 3);
            g.fillOval(x + inset, y + inset, size - inset * 2, size - inset * 2);
            g.setColor(Frame.NAVY);
            g.setFont(new Font("SansSerif", Font.BOLD, 23));
            String totalText = String.valueOf(total);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(totalText, getWidth() / 2 - metrics.stringWidth(totalText) / 2,
                    getHeight() / 2 + 2);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            String label = "Tables";
            g.drawString(label, getWidth() / 2 - g.getFontMetrics().stringWidth(label) / 2,
                    getHeight() / 2 + 18);
            g.dispose();
        }
    }

    private static final class CategoryChart extends JPanel {
        private double[] values = new double[0];

        private CategoryChart() {
            setOpaque(false);
        }

        private void setValues(double[] values) {
            this.values = values.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double total = 0;
            for (double value : values) {
                total += value;
            }
            int size = Math.max(80, Math.min(getWidth(), getHeight()) - 30);
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int start = 90;
            for (int i = 0; i < values.length; i++) {
                int extent = total <= 0 ? 0 : (int) Math.round(values[i] * 360 / total);
                g.setColor(CHART_COLORS[i % CHART_COLORS.length]);
                g.fill(new Arc2D.Double(x, y, size, size, start, -extent, Arc2D.PIE));
                start -= extent;
            }
            g.setColor(Color.WHITE);
            int inset = size / 3;
            g.fillOval(x + inset, y + inset, size - 2 * inset, size - 2 * inset);
            g.dispose();
        }
    }

    private static final class SalesChart extends JPanel {
        private double[] sales = new double[24];

        private SalesChart() {
            setOpaque(false);
        }

        private void setSales(double[] sales) {
            this.sales = sales.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 56;
            int right = Math.max(left + 20, getWidth() - 8);
            int top = 10;
            int bottom = Math.max(top + 20, getHeight() - 28);
            double max = 0;
            for (double value : sales) {
                max = Math.max(max, value);
            }
            max = max == 0 ? 1 : max * 1.15;
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i < 4; i++) {
                int y = top + (bottom - top) * i / 3;
                g.setColor(new Color(232, 232, 232));
                g.drawLine(left, y, right, y);
                double tickValue = max * (3 - i) / 3.0;
                g.setColor(new Color(90, 90, 90));
                g.drawString(formatCurrencyTick(tickValue), 1, y + 4);
            }
            Path2D line = new Path2D.Double();
            Path2D area = new Path2D.Double();
            for (int hour = 0; hour < sales.length; hour++) {
                int x = left + (right - left) * hour / (sales.length - 1);
                int y = bottom - (int) Math.round((bottom - top) * sales[hour] / max);
                if (hour == 0) {
                    line.moveTo(x, y);
                    area.moveTo(x, bottom);
                    area.lineTo(x, y);
                } else {
                    line.lineTo(x, y);
                    area.lineTo(x, y);
                }
            }
            area.lineTo(right, bottom);
            area.closePath();
            g.setColor(new Color(Frame.ACCENT.getRed(), Frame.ACCENT.getGreen(), Frame.ACCENT.getBlue(), 35));
            g.fill(area);
            g.setColor(new Color(205, 98, 44));
            g.setStroke(new BasicStroke(2f));
            g.draw(line);
            g.setColor(new Color(105, 105, 105));
            for (int hour = 0; hour <= 24; hour += 6) {
                int x = left + (right - left) * Math.min(hour, 23) / 23;
                String label = hour == 24 ? "12 AM" : (hour == 0 ? "12 AM" : (hour < 12 ? hour + " AM"
                        : (hour == 12 ? "12 PM" : (hour - 12) + " PM")));
                g.drawString(label, Math.max(0, x - 14), getHeight() - 7);
            }
            g.dispose();
        }

        private static String formatCurrencyTick(double value) {
            if (value >= 1_000_000) {
                return String.format("₱%.1fM", value / 1_000_000);
            }
            if (value >= 1_000) {
                return String.format("₱%.1fK", value / 1_000);
            }
            return String.format("₱%.0f", value);
        }
    }
}
