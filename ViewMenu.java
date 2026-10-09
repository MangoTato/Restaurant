import java.awt.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ViewMenu extends JPanel {

    private final Frame mainFrame;
    private final boolean customerView;
    private final int tableNumber;

    private JPanel menuGrid;
    private JPanel cartSummaryPanel;
    private JTextField menuSearchField;
    private final java.util.List<JButton> categoryButtons = new java.util.ArrayList<>();
    private String currentCategory = "All";
    private final Map<MenuItem, String> customerNotes = new HashMap<>();

    private final CardLayout viewLayout = new CardLayout();
    private final JPanel viewPanel = new JPanel(viewLayout);
    private JLabel waitTotalLabel;
    private JPanel orderPanel;
    private JPanel receiptPanel;

    public ViewMenu(Frame mainFrame, boolean customerView) {
        this(mainFrame, customerView, 0);
    }

    public ViewMenu(Frame mainFrame, boolean customerView, int tableNumber) {
        this.mainFrame = mainFrame;
        this.customerView = customerView;
        this.tableNumber = tableNumber;
    }

    public JPanel ShowViewMenuPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Frame.BACKGROUND);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setPreferredSize(new Dimension(1100, 680));

        try {
            MenuData.loadFromDatabase();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(mainFrame, "Unable to load the menu from the database:\n" + e.getMessage(),
                    "Menu Database Error", JOptionPane.ERROR_MESSAGE);
            card.add(new JLabel("Menu could not be loaded from the database.", SwingConstants.CENTER),
                    BorderLayout.CENTER);
            return card;
        }

        JPanel menuPanel = createMenuPanel();

        viewPanel.removeAll();
        orderPanel = showOrderPanel();
        receiptPanel = checkoutpanel();
        viewPanel.add(menuPanel, "Menu");
        viewPanel.add(orderPanel, "Order");
        viewPanel.add(waitForRepresentativePanel(), "Wait");
        viewPanel.add(receiptPanel, "Check Out");

        viewLayout.show(viewPanel, "Menu");

        card.add(viewPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createMenuPanel() {
        JPanel menuPanel = new JPanel(new BorderLayout(16, 12));
        menuPanel.setBackground(Frame.BACKGROUND);

        JPanel heading = new JPanel(new BorderLayout(12, 8));
        heading.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Pâques Menu");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(Frame.NAVY);
        JLabel subtitle = new JLabel("Choose from today's freshly prepared dishes");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Frame.MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);
        heading.add(titleBlock, BorderLayout.WEST);

        JPanel searchWrap = new JPanel(new BorderLayout(6, 0));
        searchWrap.setBackground(Color.WHITE);
        searchWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(6, 10, 6, 10)));
        searchWrap.setPreferredSize(new Dimension(245, 36));
        JLabel searchIcon = new JLabel("Search");
        searchIcon.setForeground(Frame.MUTED);
        menuSearchField = new JTextField();
        menuSearchField.setBorder(BorderFactory.createEmptyBorder());
        menuSearchField.setToolTipText("Search menu items");
        searchWrap.add(searchIcon, BorderLayout.WEST);
        searchWrap.add(menuSearchField, BorderLayout.CENTER);
        heading.add(searchWrap, BorderLayout.EAST);
        menuPanel.add(heading, BorderLayout.NORTH);

        JPanel browsePanel = new JPanel(new BorderLayout(0, 10));
        browsePanel.setOpaque(false);
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        categoryPanel.setOpaque(false);
        categoryButtons.clear();
        addCategoryButton(categoryPanel, "All", "All");
        addCategoryButton(categoryPanel, "Main Dish", "Main Dish");
        addCategoryButton(categoryPanel, "Side Dish", "Side Dish");
        addCategoryButton(categoryPanel, "Beverages", "Beverages");
        browsePanel.add(categoryPanel, BorderLayout.NORTH);

        menuGrid = new JPanel(new GridLayout(0, 3, 12, 12));
        menuGrid.setBackground(Frame.BACKGROUND);
        menuGrid.setBorder(new EmptyBorder(2, 2, 8, 2));
        JScrollPane scrollPane = new JScrollPane(menuGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(Frame.BACKGROUND);
        scrollPane.getViewport().setBackground(Frame.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        browsePanel.add(scrollPane, BorderLayout.CENTER);
        menuPanel.add(browsePanel, BorderLayout.CENTER);

        cartSummaryPanel = customerView ? createCartSummaryPanel() : null;
        if (cartSummaryPanel != null) {
            menuPanel.add(cartSummaryPanel, BorderLayout.EAST);
        }
        updateCategoryButtons();
        refreshMenuGrid();
        menuSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshMenuGrid();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshMenuGrid();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshMenuGrid();
            }
        });
        return menuPanel;
    }

    private void addCategoryButton(JPanel panel, String label, String category) {
        JButton button = new JButton(label);
        button.addActionListener(event -> {
            currentCategory = category;
            updateCategoryButtons();
            refreshMenuGrid();
        });
        categoryButtons.add(button);
        panel.add(button);
    }

    private void updateCategoryButtons() {
        for (int index = 0; index < categoryButtons.size(); index++) {
            JButton button = categoryButtons.get(index);
            String category = index == 0 ? "All" : button.getText();
            boolean selected = currentCategory.equals(category);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            Frame.styleButtonState(button, selected);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? Frame.ACCENT : Frame.BORDER),
                    new EmptyBorder(7, 12, 7, 12)));
        }
    }

    private JPanel createCartSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(270, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(14, 14, 14, 14)));
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Your order");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(Frame.NAVY);
        JLabel tableLabel = new JLabel(tableNumber > 0 ? "Table " + tableNumber : "Order summary");
        tableLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tableLabel.setForeground(Frame.MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(tableLabel);
        panel.add(titleBlock, BorderLayout.NORTH);

        java.util.List<KitchenOrder> orders = getDisplayedOrders();
        JPanel items = new JPanel();
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setOpaque(false);
        if (orders.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='width:180px'>Your order is empty. Add a dish to get started.</div></html>");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 12));
            empty.setForeground(Frame.MUTED);
            items.add(empty);
        } else {
            for (KitchenOrder order : orders) {
                JPanel row = new JPanel(new BorderLayout(6, 4));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(6, 0, 6, 0));
                JLabel itemName = new JLabel("<html><b>" + order.getItem().getName() + "</b><br>"
                        + order.getQuantity() + " × ₱" + String.format("%.2f", order.getItem().getPrice())
                        + "</html>");
                itemName.setFont(new Font("SansSerif", Font.PLAIN, 12));
                itemName.setForeground(Frame.NAVY);
                JLabel lineTotal = new JLabel(String.format("₱%.2f",
                        order.getQuantity() * order.getItem().getPrice()));
                lineTotal.setFont(new Font("SansSerif", Font.BOLD, 12));
                lineTotal.setForeground(Frame.NAVY);
                row.add(itemName, BorderLayout.CENTER);
                row.add(lineTotal, BorderLayout.EAST);
                items.add(row);
                items.add(new JSeparator());
            }
        }
        JScrollPane itemsScroll = new JScrollPane(items);
        itemsScroll.setBorder(BorderFactory.createEmptyBorder());
        itemsScroll.getViewport().setBackground(Color.WHITE);
        panel.add(itemsScroll, BorderLayout.CENTER);

        double total = 0;
        for (KitchenOrder order : orders) {
            total += order.getItem().getPrice() * order.getQuantity();
        }
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JLabel totalLabel = new JLabel("Total     " + String.format("₱%.2f", total));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setForeground(Frame.NAVY);
        JButton orderButton = new JButton("View order");
        styleMenuActionButton(orderButton, false);
        orderButton.addActionListener(event -> {
            refreshOrderPanel();
            viewLayout.show(viewPanel, "Order");
        });
        JButton checkoutButton = new JButton("Continue to checkout");
        styleMenuActionButton(checkoutButton, true);
        checkoutButton.setEnabled(!orders.isEmpty());
        checkoutButton.addActionListener(event -> showCheckoutWhenReady());
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 7));
        actions.setOpaque(false);
        actions.add(orderButton);
        actions.add(checkoutButton);
        footer.add(totalLabel, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshCartSummary() {
        if (cartSummaryPanel == null || menuPanelNotReady()) {
            return;
        }
        Component parent = cartSummaryPanel.getParent();
        if (parent instanceof JPanel) {
            JPanel container = (JPanel) parent;
            container.remove(cartSummaryPanel);
            cartSummaryPanel = createCartSummaryPanel();
            container.add(cartSummaryPanel, BorderLayout.EAST);
            container.revalidate();
            container.repaint();
        }
    }

    private boolean menuPanelNotReady() {
        return menuGrid == null || cartSummaryPanel.getParent() == null;
    }

    private void styleMenuActionButton(JButton button, boolean primary) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT),
                new EmptyBorder(8, 10, 8, 10)));
    }

    private void refreshMenuGrid() {
        if (menuGrid == null) {
            return;
        }

        menuGrid.removeAll();

        for (MenuItem item : MenuData.menuItems) {
            boolean matchesCategory = currentCategory.equals("All") || getCategory(item).equals(currentCategory);
            String query = menuSearchField == null ? "" : menuSearchField.getText().trim().toLowerCase();
            String description = getDescription(item);
            boolean matchesSearch = query.isEmpty() || item.getName().toLowerCase().contains(query)
                    || (description != null && description.toLowerCase().contains(query));
            if (matchesCategory && matchesSearch) {
                menuGrid.add(createMenuCard(item));
            }
        }

        if (menuGrid.getComponentCount() == 0) {
            JLabel empty = new JLabel("No menu items match your search.", SwingConstants.CENTER);
            empty.setForeground(Frame.MUTED);
            menuGrid.add(empty);
        }
        menuGrid.revalidate();
        menuGrid.repaint();
    }

    private JPanel createMenuCard(MenuItem item) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Frame.CARD);
        card.setPreferredSize(new Dimension(230, customerView ? 290 : 250));
        card.setMinimumSize(new Dimension(190, customerView ? 280 : 240));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Frame.BORDER),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel imageLabel = new JLabel();
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imageLabel.setPreferredSize(new Dimension(210, 125));
        imageLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(241, 235, 225));

        String imagePath = getImagePath(item);

        if (imagePath != null && !imagePath.isEmpty()) {
            ImageIcon icon = new ImageIcon(imagePath);
            Image image = icon.getImage().getScaledInstance(210, 125, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(image));
        } else {
            imageLabel.setText("No Image");
            imageLabel.setForeground(Color.GRAY);
        }

        card.add(imageLabel);
        card.add(Box.createVerticalStrut(8));

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        nameLabel.setForeground(Frame.NAVY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(nameLabel);
        card.add(Box.createVerticalStrut(5));

        JLabel priceLabel = new JLabel(String.format("₱%.2f", item.getPrice()));
        priceLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        priceLabel.setForeground(Frame.ACCENT_DARK);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(priceLabel);
        card.add(Box.createVerticalStrut(5));

        JLabel descriptionLabel = new JLabel("<html><center>" + getDescription(item) + "</center></html>");
        descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descriptionLabel.setForeground(Frame.MUTED);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descriptionLabel.setMaximumSize(new Dimension(220, 35));

        card.add(descriptionLabel);
        card.add(Box.createVerticalStrut(8));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.setOpaque(false);

        if (customerView) {
            SpinnerNumberModel quantityModel = new SpinnerNumberModel(1, 1, 99, 1);
            JSpinner quantitySpinner = new JSpinner(quantityModel);
            JButton noteButton = new JButton("Add Note");
            JButton orderButton = new JButton("Add Order");
            buttonPanel.add(new JLabel("Qty:"));
            buttonPanel.add(quantitySpinner);
            buttonPanel.add(noteButton);
            buttonPanel.add(orderButton);
            card.add(buttonPanel);
            noteButton.addActionListener(e -> addNote(item));
            styleMenuActionButton(orderButton, true);
            styleMenuActionButton(noteButton, false);
            orderButton.addActionListener(e -> addOrder(item, (Integer) quantitySpinner.getValue()));
        }

        return card;
    }

    private void addNote(MenuItem item) {
        String currentNote = customerNotes.getOrDefault(item, "");

        JTextArea noteArea = new JTextArea(currentNote, 5, 25);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(noteArea);

        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Add Note - " + item.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String note = noteArea.getText().trim();
            customerNotes.put(item, note);

            JOptionPane.showMessageDialog(this, "Note saved for " + item.getName() + ".", "Note Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addOrder(MenuItem item, int quantity) {
        String note = customerNotes.getOrDefault(item, "");
        try {
            mainFrame.orderService.add(item, note, quantity, tableNumber);
            refreshCartSummary();
            JOptionPane.showMessageDialog(this, item.getName() + " added to your order.", "Order Added",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Unable to Add Order", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel showOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = mainFrame.createLabel("Order Panel", 25, Frame.NAVY);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);

        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel orderList = new JPanel();
        orderList.setLayout(new BoxLayout(orderList, BoxLayout.Y_AXIS));
        orderList.setBackground(Color.WHITE);

        java.util.List<KitchenOrder> orders = getDisplayedOrders();
        if (orders.isEmpty()) {
            JLabel emptyLabel = new JLabel("No items in your order.");
            emptyLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            orderList.add(Box.createVerticalGlue());
            orderList.add(emptyLabel);
            orderList.add(Box.createVerticalGlue());
        } else {
            for (KitchenOrder order : orders) {
                JPanel itemPanel = new JPanel(new BorderLayout(10, 5));
                itemPanel.setBackground(Frame.CARD);
                itemPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Frame.BORDER),
                        new EmptyBorder(10, 10, 10, 10)));

                JLabel itemLabel = new JLabel("<html><b>" + order.getItem().getName() + "</b><br>" + "₱"
                        + String.format("%.2f", order.getItem().getPrice()) + "<br>Quantity: " + order.getQuantity()
                        + "<br>Note: " + (order.getNote().isEmpty() ? "None" : order.getNote()) + "</html>");

                JLabel statusLabel = new JLabel();
                statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
                updateStatusLabel(statusLabel, order);

                JPanel buttons = new JPanel(new FlowLayout());
                buttons.setOpaque(false);

                JButton removeButton = new JButton("Remove");
                JButton noteButton = new JButton("Edit Note");
                JButton receivedButton = new JButton("Order Received");

                buttons.add(noteButton);
                buttons.add(removeButton);
                receivedButton.setVisible(order.isComplete());
                buttons.add(receivedButton);

                JPanel details = new JPanel();
                details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
                details.setOpaque(false);
                details.add(itemLabel);
                details.add(Box.createVerticalStrut(5));
                details.add(statusLabel);
                itemPanel.add(details, BorderLayout.CENTER);
                itemPanel.add(buttons, BorderLayout.EAST);

                Timer countdown = new Timer(1000, e -> {
                    if (!statusLabel.isDisplayable()) {
                        ((Timer) e.getSource()).stop();
                    } else {
                        updateStatusLabel(statusLabel, order);
                        receivedButton.setVisible(order.isComplete());
                        removeButton.setEnabled(order.isPending());
                        buttons.revalidate();
                    }
                });
                countdown.start();

                removeButton.setEnabled(order.isPending());

                orderList.add(itemPanel);
                orderList.add(Box.createVerticalStrut(8));

                removeButton.addActionListener(e -> {
                    try {
                        mainFrame.orderService.changeQuantity(order, 0);
                        refreshOrderPanel();
                    } catch (IllegalStateException exception) {
                        JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to Update Order",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

                noteButton.addActionListener(e -> {
                    JTextArea noteArea = new JTextArea(order.getNote(), 5, 25);

                    noteArea.setLineWrap(true);
                    noteArea.setWrapStyleWord(true);

                    int result = JOptionPane.showConfirmDialog(this, new JScrollPane(noteArea),
                            "Edit Note - " + order.getItem().getName(), JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE);

                    if (result == JOptionPane.OK_OPTION) {
                        String note = noteArea.getText().trim();
                        try {
                            mainFrame.orderService.updateNote(order, note);
                            customerNotes.put(order.getItem(), note);
                            refreshOrderPanel();
                        } catch (IllegalStateException exception) {
                            JOptionPane.showMessageDialog(this, exception.getMessage(), "Unable to Update Order",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                receivedButton.addActionListener(e -> {
                    mainFrame.orderService.receiveOrder(order);
                    refreshOrderPanel();
                });
            }
        }

        JScrollPane scrollPane = new JScrollPane(orderList);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        panel.add(scrollPane, BorderLayout.CENTER);

        double total = 0;

        for (KitchenOrder order : orders) {
            total += order.getItem().getPrice() * order.getQuantity();
        }

        JLabel totalLabel = new JLabel("Total: ₱" + String.format("%.2f", total));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        totalLabel.setForeground(Frame.NAVY);

        JButton closeButton = new JButton("Back to Menu");
        JButton checkoutbutton = new JButton("Check Out");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(totalLabel, BorderLayout.WEST);
        bottomPanel.add(closeButton, BorderLayout.EAST);
        bottomPanel.add(checkoutbutton, BorderLayout.AFTER_LAST_LINE);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        closeButton.addActionListener(e -> {
            viewLayout.show(viewPanel, "Menu");
        });
        checkoutbutton.addActionListener(e -> {
            showCheckoutWhenReady();
        });

        return panel;
    }

    private JPanel checkoutpanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Frame.BACKGROUND);
        JPanel paper = new JPanel(new BorderLayout(0, 14));
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER),
                new EmptyBorder(22, 26, 20, 26)));
        paper.setPreferredSize(new Dimension(520, 500));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel restaurantName = new JLabel("Pâques");
        restaurantName.setFont(new Font("SansSerif", Font.BOLD, 27));
        restaurantName.setForeground(Frame.NAVY);
        restaurantName.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("ORDER RECEIPT");
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(Frame.ACCENT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel table = new JLabel("Table " + tableNumber);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(Frame.MUTED);
        table.setAlignmentX(Component.CENTER_ALIGNMENT);
        heading.add(restaurantName);
        heading.add(Box.createVerticalStrut(4));
        heading.add(title);
        heading.add(Box.createVerticalStrut(5));
        heading.add(table);
        paper.add(heading, BorderLayout.NORTH);

        JPanel orderList = new JPanel();
        orderList.setLayout(new BoxLayout(orderList, BoxLayout.Y_AXIS));
        orderList.setBackground(Color.WHITE);
        orderList.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Frame.BORDER));
        for (KitchenOrder order : getDisplayedOrders()) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(9, 4, 9, 4));
            JLabel item = new JLabel(order.getQuantity() + " × " + order.getItem().getName());
            item.setFont(new Font("SansSerif", Font.PLAIN, 13));
            item.setForeground(Frame.NAVY);
            JLabel amount = new JLabel("₱" + String.format("%.2f",
                    order.getItem().getPrice() * order.getQuantity()));
            amount.setFont(new Font("SansSerif", Font.BOLD, 13));
            amount.setForeground(Frame.NAVY);
            row.add(item, BorderLayout.CENTER);
            row.add(amount, BorderLayout.EAST);
            orderList.add(row);
            orderList.add(new JSeparator());
        }
        if (orderList.getComponentCount() == 0) {
            JLabel empty = new JLabel("No items were ordered.");
            empty.setForeground(Frame.MUTED);
            empty.setBorder(new EmptyBorder(12, 4, 12, 4));
            orderList.add(empty);
        }
        JScrollPane scrollPane = new JScrollPane(orderList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        paper.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        JLabel totalLabel = new JLabel("TOTAL  ₱"
                + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)), SwingConstants.RIGHT);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(Frame.ACCENT);
        totalLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel thanks = new JLabel("Thank you for dining with Pâques.", SwingConstants.CENTER);
        thanks.setFont(new Font("SansSerif", Font.ITALIC, 13));
        thanks.setForeground(Frame.MUTED);
        footer.add(totalLabel, BorderLayout.NORTH);
        footer.add(thanks, BorderLayout.SOUTH);
        paper.add(footer, BorderLayout.SOUTH);
        JButton closeButton = new JButton("Back to Menu");
        closeButton.setFocusPainted(false);
        Frame.styleButtonState(closeButton, false);
        closeButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT), new EmptyBorder(8, 14, 8, 14)));
        closeButton.addActionListener(event -> viewLayout.show(viewPanel, "Menu"));
        JPanel paperWrap = new JPanel(new GridBagLayout());
        paperWrap.setOpaque(false);
        paperWrap.add(paper);
        panel.add(closeButton, BorderLayout.NORTH);
        panel.add(paperWrap, BorderLayout.CENTER);
        return panel;
    }

    private JPanel waitForRepresentativePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Frame.NAVY, 2),
                new EmptyBorder(45, 70, 45, 70)));

        JLabel title = mainFrame.createLabel("Checkout Requested", 26, Frame.NAVY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel message = new JLabel("Please wait for a representative.");
        message.setFont(new Font("SansSerif", Font.PLAIN, 17));
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        waitTotalLabel = new JLabel("Table " + tableNumber + " • Total: ₱"
                + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)));
        waitTotalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton done = new JButton("Done");
        Frame.styleButtonState(done, false);
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(e -> {
            refreshCheckOut();
            viewLayout.show(viewPanel, "Check Out");
        });
        card.add(title);
        card.add(Box.createVerticalStrut(15));
        card.add(message);
        card.add(Box.createVerticalStrut(10));
        card.add(waitTotalLabel);
        card.add(Box.createVerticalStrut(25));
        card.add(done);
        panel.add(card);
        return panel;
    }

    private void refreshOrderPanel() {
        viewPanel.remove(orderPanel);
        orderPanel = showOrderPanel();
        viewPanel.add(orderPanel, "Order");
        viewLayout.show(viewPanel, "Order");
        viewPanel.revalidate();
        viewPanel.repaint();
    }

    private void refreshCheckOut() {
        viewPanel.remove(receiptPanel);
        receiptPanel = checkoutpanel();
        viewPanel.add(receiptPanel, "Check Out");
        viewPanel.revalidate();
        viewPanel.repaint();
    }

    private void showCheckoutWhenReady() {
        java.util.List<KitchenOrder> orders = getDisplayedOrders();
        if (orders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You need an order before checking out.", "No Active Order",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (KitchenOrder order : orders) {
            if (!order.isReceived()) {
                JOptionPane.showMessageDialog(this, "Checkout is available after every order is marked Order Received.",
                        "Order Not Received", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            mainFrame.tableService.requestCheckout(tableNumber);
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Unable to Request Checkout",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        waitTotalLabel.setText("Table " + tableNumber + " • Total: ₱"
                + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)));
        viewLayout.show(viewPanel, "Wait");
    }

    private void updateStatusLabel(JLabel label, KitchenOrder order) {
        if (order.isPending()) {
            label.setText("PENDING");
            label.setForeground(new Color(210, 150, 0));
        } else if (order.isComplete()) {
            label.setText("For Serving");
            label.setForeground(new Color(210, 150, 0));
        } else if (order.isReceived()) {
            label.setText("Order Received");
            label.setForeground(new Color(0, 130, 80));
        } else {
            long seconds = order.getSecondsRemaining();
            label.setText(seconds == 0 ? "Ready to serve" : "Time remaining: " + formatRemaining(seconds));
            label.setForeground(seconds == 0 ? Frame.SUCCESS : Frame.ACCENT);
        }
    }

    private java.util.List<KitchenOrder> getDisplayedOrders() {
        return customerView ? mainFrame.orderService.getOrders(tableNumber) : mainFrame.orderService.getOrders();
    }

    private String formatRemaining(long seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private String getCategory(MenuItem item) {
        return item.getCategory();
    }

    private String getDescription(MenuItem item) {
        return item.getDescription();
    }

    private String getImagePath(MenuItem item) {
        return item.getImagePath();
    }
}
