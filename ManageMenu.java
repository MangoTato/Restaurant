import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ManageMenu extends JPanel {

    private final Frame mainFrame;

    private final List<MenuItem> menuItems = MenuData.menuItems;

    // ============================================================
    // MAIN UI
    // ============================================================

    private JPanel menuGrid;
    private JPanel viewPanel;

    private final CardLayout viewLayout = new CardLayout();

    private String currentCategory = "All";

    // ============================================================
    // MENU DETAILS
    // ============================================================

    private final Map<MenuItem, DishDetails> detailsMap =
            new HashMap<>();

    // ============================================================
    // ORDERS
    // ============================================================

    private final List<OrderItem> orders =
            new ArrayList<>();

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public ManageMenu(Frame mainFrame) {

        this.mainFrame = mainFrame;

        /*
         * Make sure every existing MenuItem has details.
         */
        for (MenuItem item : menuItems) {

            if (!detailsMap.containsKey(item)) {

                detailsMap.put(
                        item,
                        new DishDetails(
                                "Main Dish",
                                "No description available.",
                                null
                        )
                );
            }
        }
    }

    // ============================================================
    // MAIN PANEL
    // ============================================================

    public JPanel ShowMenuPanel() {

        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Color.WHITE);
        card.setBorder( BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Frame.NAVY,
                                2
                        ),
                        new EmptyBorder(25,30,25,30)
                )
        );

        card.setPreferredSize(
                new Dimension(1100, 680)
        );

        // ========================================================
        // VIEW PANEL
        // ========================================================

        viewPanel = new JPanel(viewLayout);
        viewPanel.setBackground(Color.WHITE);
        viewPanel.add( createMenuPanel(),"Menu");
        viewPanel.add( createOrderPanel(),"Order");
        viewPanel.add( createCheckoutPanel(),"Check Out" );
        viewLayout.show( viewPanel,"Menu");

        card.add( viewPanel, BorderLayout.CENTER);

        return card;
    }

    // ============================================================
    // MENU SCREEN
    // ============================================================

    private JPanel createMenuPanel() {

        JPanel menuPanel =
                new JPanel(
                        new BorderLayout(0, 15)
                );

        menuPanel.setBackground(Color.WHITE);

        // ========================================================
        // TITLE
        // ========================================================

        JLabel title =
                mainFrame.createLabel(
                        "Restaurant Menu",
                        25,
                        Frame.NAVY
                );

        JPanel titlePanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );

        titlePanel.setOpaque(false);

        titlePanel.add(title);

        menuPanel.add(
                titlePanel,
                BorderLayout.NORTH
        );

        // ========================================================
        // CENTER
        // ========================================================

        JPanel centerPanel =
                new JPanel(
                        new BorderLayout(10, 10)
                );

        centerPanel.setOpaque(false);

        // ========================================================
        // NAVIGATION BUTTONS
        // ========================================================

        JPanel navigationPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                5
                        )
                );

        navigationPanel.setOpaque(false);

        JButton allButton =
                createCategoryButton("Main Menu");

        JButton mainDishButton =
                createCategoryButton("Main Dish");

        JButton sideDishButton =
                createCategoryButton("Side Dish");

        JButton beverageButton =
                createCategoryButton("Beverages");

        JButton orderButton =
                createCategoryButton("Order Panel");

        JButton checkoutButton =
                createCategoryButton("Check Out");

        navigationPanel.add(allButton);
        navigationPanel.add(mainDishButton);
        navigationPanel.add(sideDishButton);
        navigationPanel.add(beverageButton);
        navigationPanel.add(orderButton);
        navigationPanel.add(checkoutButton);

        centerPanel.add(navigationPanel,BorderLayout.NORTH);

        // ========================================================
        // ADMINISTRATIVE BUTTONS
        // ========================================================

        JPanel adminPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                10,
                                5
                        )
                );

        adminPanel.setOpaque(false);

        JButton addItemButton =
                new JButton("ADD ITEM");

        JButton deleteItemButton =
                new JButton("DELETE ITEM");

        JButton modifyItemButton =
                new JButton("MODIFY ITEM");

        styleAdminButton(addItemButton);
        styleAdminButton(deleteItemButton);
        styleAdminButton(modifyItemButton);

        adminPanel.add(addItemButton);
        adminPanel.add(deleteItemButton);
        adminPanel.add(modifyItemButton);

        centerPanel.add(
                adminPanel,
                BorderLayout.SOUTH
        );

        // ========================================================
        // MENU GRID
        // ========================================================

        menuGrid =
                new JPanel(
                        new GridLayout(
                                0,
                                3,
                                15,
                                15
                        )
                );

        menuGrid.setBackground(Color.WHITE);

        menuGrid.setBorder(
                new EmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        JScrollPane scrollPane =
                new JScrollPane(menuGrid);

        scrollPane.setPreferredSize(
                new Dimension(1000, 470)
        );

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        Color.LIGHT_GRAY,
                        1
                )
        );

        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(15);

        centerPanel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        menuPanel.add(
                centerPanel,
                BorderLayout.CENTER
        );

        // ========================================================
        // MENU ACTIONS
        // ========================================================

        allButton.addActionListener(e -> {

            currentCategory = "All";

            refreshMenuGrid();
        });

        mainDishButton.addActionListener(e -> {

            currentCategory = "Main Dish";

            refreshMenuGrid();
        });

        sideDishButton.addActionListener(e -> {

            currentCategory = "Side Dish";

            refreshMenuGrid();
        });

        beverageButton.addActionListener(e -> {

            currentCategory = "Beverages";

            refreshMenuGrid();
        });

        orderButton.addActionListener(e -> {

            refreshOrderPanel();

            viewLayout.show(
                    viewPanel,
                    "Order"
            );
        });

        checkoutButton.addActionListener(e -> {

            refreshCheckout();

            viewLayout.show(
                    viewPanel,
                    "Check Out"
            );
        });

        // ========================================================
        // ADMIN ACTIONS
        // ========================================================

        addItemButton.addActionListener(
                e -> addMenuItem()
        );

        deleteItemButton.addActionListener(
                e -> deleteMenuItem()
        );

        modifyItemButton.addActionListener(
                e -> modifyMenuItem()
        );

        // ========================================================
        // INITIAL DISPLAY
        // ========================================================

        refreshMenuGrid();

        return menuPanel;
    }

    // ============================================================
    // CATEGORY BUTTON
    // ============================================================

    private JButton createCategoryButton(String text) {

        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif",Font.BOLD,13));
        button.setForeground(Color.WHITE);
        button.setBackground(Frame.NAVY);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(135, 40));

        return button;
    }

    // ============================================================
    // ADMIN BUTTON STYLE
    // ============================================================

    private void styleAdminButton(
            JButton button
    ) {

        button.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        button.setForeground(Color.WHITE);

        button.setBackground(Frame.TEAL);

        button.setFocusPainted(false);

        button.setPreferredSize(
                new Dimension(
                        130,
                        38
                )
        );
    }

    // ============================================================
    // REFRESH MENU GRID
    // ============================================================

    private void refreshMenuGrid() {

        if (menuGrid == null) {
            return;
        }

        menuGrid.removeAll();

        for (MenuItem item : menuItems) {

            DishDetails details =
                    detailsMap.get(item);

            if (details == null) {

                details =
                        new DishDetails(
                                "Main Dish",
                                "No description available.",
                                null
                        );

                detailsMap.put(
                        item,
                        details
                );
            }

            if (
                    currentCategory.equals("All")
                    ||
                    details.category.equals(
                            currentCategory
                    )
            ) {

                menuGrid.add(
                        createMenuCard(item)
                );
            }
        }

        menuGrid.revalidate();
        menuGrid.repaint();
    }

    // ============================================================
    // CREATE MENU CARD
    // ============================================================

    private JPanel createMenuCard(
            MenuItem item
    ) {

        DishDetails details =
                detailsMap.get(item);

        if (details == null) {

            details =
                    new DishDetails(
                            "Main Dish",
                            "No description available.",
                            null
                    );

            detailsMap.put(
                    item,
                    details
            );
        }

        JPanel card =
                new JPanel();

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBackground(Color.WHITE);

        card.setPreferredSize(
                new Dimension(
                        250,
                        270
                )
        );

        card.setMinimumSize(
                new Dimension(
                        250,
                        270
                )
        );

        card.setMaximumSize(
                new Dimension(
                        250,
                        270
                )
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Frame.NAVY,
                                1
                        ),
                        new EmptyBorder(
                                12,
                                12,
                                12,
                                12
                        )
                )
        );

        // ========================================================
        // IMAGE
        // ========================================================

        JLabel imageLabel =
                new JLabel();

        imageLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        imageLabel.setPreferredSize(
                new Dimension(
                        210,
                        90
                )
        );

        imageLabel.setMaximumSize(
                new Dimension(
                        210,
                        90
                )
        );

        imageLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        if (
                details.imagePath != null
                &&
                !details.imagePath.isEmpty()
        ) {

            ImageIcon icon =
                    loadImage(
                            details.imagePath,
                            200,
                            85
                    );

            if (icon != null) {

                imageLabel.setIcon(icon);

            } else {

                imageLabel.setText("IMAGE NOT FOUND");

                imageLabel.setForeground(
                        Color.GRAY
                );
            }

        } else {

            imageLabel.setText("No Image");

            imageLabel.setForeground(
                    Color.GRAY
            );
        }

        card.add(imageLabel);

        card.add(
                Box.createVerticalStrut(8)
        );

        // ========================================================
        // NAME
        // ========================================================

        JLabel nameLabel =
                new JLabel(
                        item.getName()
                );

        nameLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        18
                )
        );

        nameLabel.setForeground(
                Frame.NAVY
        );

        nameLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        card.add(nameLabel);

        card.add(
                Box.createVerticalStrut(5)
        );

        // ========================================================
        // PRICE
        // ========================================================

        JLabel priceLabel =
                new JLabel(
                        String.format(
                                "₱%.2f",
                                item.getPrice()
                        )
                );

        priceLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        16
                )
        );

        priceLabel.setForeground(
                Frame.TEAL
        );

        priceLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        card.add(priceLabel);

        card.add(
                Box.createVerticalStrut(5)
        );

        // ========================================================
        // DESCRIPTION
        // ========================================================

        String description =
                details.description == null
                        ? "Delicious restaurant dish"
                        : details.description;

        JLabel descriptionLabel =
                new JLabel(
                        "<html><center>"
                        + description
                        + "</center></html>"
                );

        descriptionLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        descriptionLabel.setForeground(
                Color.DARK_GRAY
        );

        descriptionLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        card.add(descriptionLabel);

        card.add(
                Box.createVerticalStrut(8)
        );

        // ========================================================
        // BUTTONS
        // ========================================================

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                5,
                                0
                        )
                );

        buttonPanel.setOpaque(false);

        JButton noteButton =
                new JButton("Add Note");

        JButton orderButton =
                new JButton("Add Order");

        buttonPanel.add(noteButton);
        buttonPanel.add(orderButton);

        card.add(buttonPanel);

        // ========================================================
        // NOTE ACTION
        // ========================================================

        noteButton.addActionListener(
                e -> addNote(item)
        );

        // ========================================================
        // ORDER ACTION
        // ========================================================

        orderButton.addActionListener(
                e -> addOrder(item)
        );

        return card;
    }

    // ============================================================
    // ADD NOTE
    // ============================================================

    private void addNote(
            MenuItem item
    ) {

        String currentNote = "";

        for (OrderItem order : orders) {

            if (order.item == item) {

                currentNote =
                        order.note;

                break;
            }
        }

        JTextArea noteArea =
                new JTextArea(
                        currentNote,
                        5,
                        25
                );

        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JScrollPane scrollPane =
                new JScrollPane(noteArea);

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        scrollPane,
                        "Add Note - "
                                + item.getName(),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (result == JOptionPane.OK_OPTION) {

            String note =
                    noteArea.getText().trim();

            boolean found = false;

            for (OrderItem order : orders) {

                if (order.item == item) {

                    order.note = note;

                    found = true;

                    break;
                }
            }

            if (!found && !note.isEmpty()) {

                /*
                 * Store a temporary customer note
                 * by adding it to the note map inside
                 * the order only when the item is ordered.
                 */
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Note saved for "
                            + item.getName()
                            + ".",
                    "Note Saved",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    // ============================================================
    // ADD ORDER
    // ============================================================

    private void addOrder(
            MenuItem item
    ) {

        String note = "";

        for (OrderItem order : orders) {

            if (order.item == item) {

                order.quantity++;

                JOptionPane.showMessageDialog(
                        this,
                        item.getName()
                                + " quantity increased.",
                        "Order Updated",
                        JOptionPane.INFORMATION_MESSAGE
                );

                return;
            }
        }

        orders.add(
                new OrderItem(
                        item,
                        note,
                        1
                )
        );

        JOptionPane.showMessageDialog(
                this,
                item.getName()
                        + " added to your order.",
                "Order Added",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ============================================================
    // ORDER PANEL
    // ============================================================

    private JPanel createOrderPanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        panel.setBackground(Color.WHITE);

        // ========================================================
        // TITLE
        // ========================================================

        JLabel titleLabel =
                mainFrame.createLabel(
                        "Order Panel",
                        25,
                        Frame.NAVY
                );

        JPanel titlePanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );

        titlePanel.setOpaque(false);

        titlePanel.add(titleLabel);

        panel.add(
                titlePanel,
                BorderLayout.NORTH
        );

        // ========================================================
        // ORDER LIST
        // ========================================================

        JPanel orderList =
                new JPanel();

        orderList.setLayout(
                new BoxLayout(
                        orderList,
                        BoxLayout.Y_AXIS
                )
        );

        orderList.setBackground(Color.WHITE);

        if (orders.isEmpty()) {

            JLabel emptyLabel =
                    new JLabel(
                            "No items in your order."
                    );

            emptyLabel.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            18
                    )
            );

            emptyLabel.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            orderList.add(
                    Box.createVerticalGlue()
            );

            orderList.add(emptyLabel);

            orderList.add(
                    Box.createVerticalGlue()
            );

        } else {

            for (OrderItem order : orders) {

                orderList.add(
                        createOrderItemPanel(
                                order
                        )
                );

                orderList.add(
                        Box.createVerticalStrut(8)
                );
            }
        }

        JScrollPane scrollPane =
                new JScrollPane(orderList);

        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(15);

        panel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        // ========================================================
        // TOTAL
        // ========================================================

        double total = calculateTotal();

        JLabel totalLabel =
                new JLabel(
                        String.format(
                                "Total: ₱%.2f",
                                total
                        )
                );

        totalLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        20
                )
        );

        totalLabel.setForeground(
                Frame.NAVY
        );

        // ========================================================
        // BUTTONS
        // ========================================================

        JButton backButton =
                new JButton("Back to Menu");

        JButton clearButton =
                new JButton("Clear Orders");

        JButton checkoutButton =
                new JButton("Check Out");

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                8,
                                0
                        )
                );

        buttonPanel.setOpaque(false);

        buttonPanel.add(clearButton);
        buttonPanel.add(backButton);
        buttonPanel.add(checkoutButton);

        JPanel bottomPanel =
                new JPanel(
                        new BorderLayout()
                );

        bottomPanel.setOpaque(false);

        bottomPanel.add(
                totalLabel,
                BorderLayout.WEST
        );

        bottomPanel.add(
                buttonPanel,
                BorderLayout.EAST
        );

        panel.add(
                bottomPanel,
                BorderLayout.SOUTH
        );

        // ========================================================
        // ACTIONS
        // ========================================================

        backButton.addActionListener(e -> {

            viewLayout.show(
                    viewPanel,
                    "Menu"
            );
        });

        clearButton.addActionListener(e -> {

            if (orders.isEmpty()) {
                return;
            }

            int result =
                    JOptionPane.showConfirmDialog(
                            this,
                            "Clear all orders?",
                            "Clear Orders",
                            JOptionPane.YES_NO_OPTION
                    );

            if (result ==
                    JOptionPane.YES_OPTION) {

                orders.clear();

                refreshOrderPanel();
            }
        });

        checkoutButton.addActionListener(e -> {

            refreshCheckout();

            viewLayout.show(
                    viewPanel,
                    "Check Out"
            );
        });

        return panel;
    }

    // ============================================================
    // ORDER ITEM PANEL
    // ============================================================

    private JPanel createOrderItemPanel(
            OrderItem order
    ) {

        JPanel itemPanel =
                new JPanel(
                        new BorderLayout(
                                10,
                                5
                        )
                );

        itemPanel.setBackground(Color.WHITE);

        itemPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Color.LIGHT_GRAY
                        ),
                        new EmptyBorder(
                                10,
                                10,
                                10,
                                10
                        )
                )
        );

        // ========================================================
        // ITEM INFORMATION
        // ========================================================

        String note =
                order.note == null
                        || order.note.isEmpty()
                        ? "None"
                        : order.note;

        JLabel itemLabel =
                new JLabel(
                        "<html>"
                        + "<b>"
                        + order.item.getName()
                        + "</b><br>"
                        + "₱"
                        + String.format(
                                "%.2f",
                                order.item.getPrice()
                        )
                        + "<br>"
                        + "Quantity: "
                        + order.quantity
                        + "<br>"
                        + "Note: "
                        + note
                        + "</html>"
                );

        itemPanel.add(
                itemLabel,
                BorderLayout.CENTER
        );

        // ========================================================
        // BUTTONS
        // ========================================================

        JPanel buttons =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                5,
                                0
                        )
                );

        buttons.setOpaque(false);

        JButton minusButton =
                new JButton("-");

        JButton plusButton =
                new JButton("+");

        JButton noteButton =
                new JButton("Edit Note");

        JButton removeButton =
                new JButton("Remove");

        buttons.add(minusButton);
        buttons.add(plusButton);
        buttons.add(noteButton);
        buttons.add(removeButton);

        itemPanel.add(
                buttons,
                BorderLayout.EAST
        );

        // ========================================================
        // MINUS
        // ========================================================

        minusButton.addActionListener(e -> {

            if (order.quantity > 1) {

                order.quantity--;

            } else {

                orders.remove(order);
            }

            refreshOrderPanel();
        });

        // ========================================================
        // PLUS
        // ========================================================

        plusButton.addActionListener(e -> {

            order.quantity++;

            refreshOrderPanel();
        });

        // ========================================================
        // REMOVE
        // ========================================================

        removeButton.addActionListener(e -> {

            orders.remove(order);

            refreshOrderPanel();
        });

        // ========================================================
        // EDIT NOTE
        // ========================================================

        noteButton.addActionListener(e -> {

            JTextArea noteArea =
                    new JTextArea(
                            order.note,
                            5,
                            25
                    );

            noteArea.setLineWrap(true);

            noteArea.setWrapStyleWord(true);

            int result =
                    JOptionPane.showConfirmDialog(
                            this,
                            new JScrollPane(
                                    noteArea
                            ),
                            "Edit Note - "
                                    + order.item.getName(),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE
                    );

            if (result ==
                    JOptionPane.OK_OPTION) {

                order.note =
                        noteArea
                                .getText()
                                .trim();

                refreshOrderPanel();
            }
        });

        return itemPanel;
    }

    // ============================================================
    // CHECKOUT / RECEIPT
    // ============================================================

    private JPanel createCheckoutPanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        panel.setBackground(Color.WHITE);

        // ========================================================
        // TITLE
        // ========================================================

        JLabel titleLabel =
                mainFrame.createLabel(
                        "Receipt",
                        25,
                        Frame.NAVY
                );

        JPanel titlePanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );

        titlePanel.setOpaque(false);

        titlePanel.add(titleLabel);

        panel.add(
                titlePanel,
                BorderLayout.NORTH
        );

        // ========================================================
        // RECEIPT LIST
        // ========================================================

        JPanel receiptList =
                new JPanel();

        receiptList.setLayout(
                new BoxLayout(
                        receiptList,
                        BoxLayout.Y_AXIS
                )
        );

        receiptList.setBackground(Color.WHITE);

        if (orders.isEmpty()) {

            JLabel emptyLabel =
                    new JLabel(
                            "No items to check out."
                    );

            emptyLabel.setFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            18
                    )
            );

            emptyLabel.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            receiptList.add(
                    Box.createVerticalGlue()
            );

            receiptList.add(emptyLabel);

            receiptList.add(
                    Box.createVerticalGlue()
            );

        } else {

            for (OrderItem order : orders) {

                double itemTotal =
                        order.item.getPrice()
                        * order.quantity;

                JLabel itemLabel =
                        new JLabel(
                                "<html>"
                                + "<b>"
                                + order.item.getName()
                                + "</b><br>"
                                + "Quantity: "
                                + order.quantity
                                + "<br>"
                                + "Price: ₱"
                                + String.format(
                                        "%.2f",
                                        order.item.getPrice()
                                )
                                + "<br>"
                                + "Subtotal: ₱"
                                + String.format(
                                        "%.2f",
                                        itemTotal
                                )
                                + "<br>"
                                + "Note: "
                                + (
                                        order.note.isEmpty()
                                                ? "None"
                                                : order.note
                                )
                                + "</html>"
                        );

                itemLabel.setBorder(
                        new EmptyBorder(
                                10,
                                10,
                                10,
                                10
                        )
                );

                receiptList.add(
                        itemLabel
                );

                receiptList.add(
                        Box.createVerticalStrut(8)
                );
            }
        }

        JScrollPane scrollPane =
                new JScrollPane(
                        receiptList
                );

        scrollPane.getVerticalScrollBar()
                .setUnitIncrement(15);

        panel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        // ========================================================
        // TOTAL
        // ========================================================

        JLabel totalLabel =
                new JLabel(
                        String.format(
                                "Total: ₱%.2f",
                                calculateTotal()
                        )
                );

        totalLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        20
                )
        );

        totalLabel.setForeground(
                Frame.NAVY
        );

        // ========================================================
        // BACK BUTTON
        // ========================================================

        JButton backButton =
                new JButton("Back to Menu");

        JButton orderButton =
                new JButton("Back to Order");

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                8,
                                0
                        )
                );

        buttonPanel.setOpaque(false);

        buttonPanel.add(orderButton);
        buttonPanel.add(backButton);

        JPanel bottomPanel =
                new JPanel(
                        new BorderLayout()
                );

        bottomPanel.setOpaque(false);

        bottomPanel.add(
                totalLabel,
                BorderLayout.WEST
        );

        bottomPanel.add(
                buttonPanel,
                BorderLayout.EAST
        );

        panel.add(
                bottomPanel,
                BorderLayout.SOUTH
        );

        // ========================================================
        // ACTIONS
        // ========================================================

        backButton.addActionListener(e -> {

            viewLayout.show(
                    viewPanel,
                    "Menu"
            );
        });

        orderButton.addActionListener(e -> {

            refreshOrderPanel();

            viewLayout.show(
                    viewPanel,
                    "Order"
            );
        });

        return panel;
    }

    // ============================================================
    // REFRESH ORDER PANEL
    // ============================================================

    private void refreshOrderPanel() {

        if (viewPanel == null) {
            return;
        }

        viewPanel.removeAll();

        viewPanel.add(
                createMenuPanel(),
                "Menu"
        );

        viewPanel.add(
                createOrderPanel(),
                "Order"
        );

        viewPanel.add(
                createCheckoutPanel(),
                "Check Out"
        );

        viewLayout.show(
                viewPanel,
                "Order"
        );

        viewPanel.revalidate();
        viewPanel.repaint();
    }

    // ============================================================
    // REFRESH CHECKOUT
    // ============================================================

    private void refreshCheckout() {

        if (viewPanel == null) {
            return;
        }

        viewPanel.removeAll();

        viewPanel.add(
                createMenuPanel(),
                "Menu"
        );

        viewPanel.add(
                createOrderPanel(),
                "Order"
        );

        viewPanel.add(
                createCheckoutPanel(),
                "Check Out"
        );

        viewLayout.show(
                viewPanel,
                "Check Out"
        );

        viewPanel.revalidate();
        viewPanel.repaint();
    }

    // ============================================================
    // CALCULATE TOTAL
    // ============================================================

    private double calculateTotal() {

        double total = 0;

        for (OrderItem order : orders) {

            total +=
                    order.item.getPrice()
                    * order.quantity;
        }

        return total;
    }

    // ============================================================
    // ADD MENU ITEM
    // ============================================================

    private void addMenuItem() {

        JTextField nameField =
                new JTextField();

        JTextField priceField =
                new JTextField();

        JComboBox<String> categoryBox =
                new JComboBox<>(
                        new String[]{
                                "Main Dish",
                                "Side Dish",
                                "Beverages"
                        }
                );

        JTextArea descriptionArea =
                new JTextArea(
                        4,
                        20
                );

        descriptionArea.setLineWrap(true);

        descriptionArea.setWrapStyleWord(true);

        JTextField imageField =
                new JTextField();

        JButton browseButton =
                new JButton("Browse");

        JPanel imagePanel =
                new JPanel(
                        new BorderLayout(5, 0)
                );

        imagePanel.add(
                imageField,
                BorderLayout.CENTER
        );

        imagePanel.add(
                browseButton,
                BorderLayout.EAST
        );

        browseButton.addActionListener(e -> {

            JFileChooser chooser =
                    new JFileChooser();

            int result =
                    chooser.showOpenDialog(
                            mainFrame
                    );

            if (
                    result ==
                    JFileChooser.APPROVE_OPTION
            ) {

                File file =
                        chooser.getSelectedFile();

                imageField.setText(
                        file.getAbsolutePath()
                );
            }
        });

        JPanel form =
                new JPanel(
                        new GridBagLayout()
                );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        5,
                        5,
                        5,
                        5
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        addFormRow(
                form,
                gbc,
                0,
                "Dish Name:",
                nameField
        );

        addFormRow(
                form,
                gbc,
                1,
                "Price:",
                priceField
        );

        addFormRow(
                form,
                gbc,
                2,
                "Category:",
                categoryBox
        );

        addFormRow(
                form,
                gbc,
                3,
                "Picture:",
                imagePanel
        );

        gbc.gridx = 0;
        gbc.gridy = 4;

        form.add(
                new JLabel("Description:"),
                gbc
        );

        gbc.gridx = 1;

        form.add(
                new JScrollPane(
                        descriptionArea
                ),
                gbc
        );

        int result =
                JOptionPane.showConfirmDialog(
                        mainFrame,
                        form,
                        "ADD ITEM",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (
                result !=
                JOptionPane.OK_OPTION
        ) {
            return;
        }

        String name =
                nameField
                        .getText()
                        .trim();

        String priceText =
                priceField
                        .getText()
                        .trim();

        String description =
                descriptionArea
                        .getText()
                        .trim();

        String category =
                (String)
                        categoryBox
                                .getSelectedItem();

        String imagePath =
                imageField
                        .getText()
                        .trim();

        // ========================================================
        // VALIDATION
        // ========================================================

        if (
                name.isEmpty()
                ||
                priceText.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please enter the dish name and price.",
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        double price;

        try {

            price =
                    Double.parseDouble(
                            priceText
                    );

        } catch (
                NumberFormatException ex
        ) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please enter a valid price.",
                    "Invalid Price",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (price < 0) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Price cannot be negative.",
                    "Invalid Price",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // ========================================================
        // CREATE ITEM
        // ========================================================

        MenuItem newItem =
                new MenuItem(
                        name,
                        price
                );

        menuItems.add(newItem);

        detailsMap.put(
                newItem,
                new DishDetails(
                        category,
                        description,
                        imagePath
                )
        );

        currentCategory = category;

        refreshMenuGrid();
    }

    // ============================================================
    // MODIFY MENU ITEM
    // ============================================================

    private void modifyMenuItem() {

        if (menuItems.isEmpty()) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "There are no menu items to modify.",
                    "Modify Item",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        MenuItem selectedItem =
                selectMenuItem(
                        "Select a menu item to modify:"
                );

        if (selectedItem == null) {
            return;
        }

        DishDetails details =
                detailsMap.get(
                        selectedItem
                );

        if (details == null) {

            details =
                    new DishDetails(
                            "Main Dish",
                            "No description available.",
                            null
                    );

            detailsMap.put(
                    selectedItem,
                    details
            );
        }

        JTextField nameField =
                new JTextField(
                        selectedItem.getName()
                );

        JTextField priceField =
                new JTextField(
                        String.valueOf(
                                selectedItem.getPrice()
                        )
                );

        JComboBox<String> categoryBox =
                new JComboBox<>(
                        new String[]{
                                "Main Dish",
                                "Side Dish",
                                "Beverages"
                        }
                );

        categoryBox.setSelectedItem(
                details.category
        );

        JTextArea descriptionArea =
                new JTextArea(
                        details.description,
                        4,
                        20
                );

        descriptionArea.setLineWrap(true);

        descriptionArea.setWrapStyleWord(true);

        JTextField imageField =
                new JTextField(
                        details.imagePath == null
                                ? ""
                                : details.imagePath
                );

        JButton browseButton =
                new JButton("Browse");

        browseButton.addActionListener(e -> {

            JFileChooser chooser =
                    new JFileChooser();

            int result =
                    chooser.showOpenDialog(
                            mainFrame
                    );

            if (
                    result ==
                    JFileChooser.APPROVE_OPTION
            ) {

                imageField.setText(
                        chooser
                                .getSelectedFile()
                                .getAbsolutePath()
                );
            }
        });

        JPanel imagePanel =
                new JPanel(
                        new BorderLayout(5, 0)
                );

        imagePanel.add(
                imageField,
                BorderLayout.CENTER
        );

        imagePanel.add(
                browseButton,
                BorderLayout.EAST
        );

        JPanel form =
                new JPanel(
                        new GridBagLayout()
                );

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.insets =
                new Insets(
                        5,
                        5,
                        5,
                        5
                );

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        addFormRow(
                form,
                gbc,
                0,
                "Dish Name:",
                nameField
        );

        addFormRow(
                form,
                gbc,
                1,
                "Price:",
                priceField
        );

        addFormRow(
                form,
                gbc,
                2,
                "Category:",
                categoryBox
        );

        addFormRow(
                form,
                gbc,
                3,
                "Picture:",
                imagePanel
        );

        gbc.gridx = 0;
        gbc.gridy = 4;

        form.add(
                new JLabel("Description:"),
                gbc
        );

        gbc.gridx = 1;

        form.add(
                new JScrollPane(
                        descriptionArea
                ),
                gbc
        );

        int result =
                JOptionPane.showConfirmDialog(
                        mainFrame,
                        form,
                        "MODIFY ITEM",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (
                result !=
                JOptionPane.OK_OPTION
        ) {
            return;
        }

        String newName =
                nameField
                        .getText()
                        .trim();

        String priceText =
                priceField
                        .getText()
                        .trim();

        if (
                newName.isEmpty()
                ||
                priceText.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Name and price are required.",
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        double newPrice;

        try {

            newPrice =
                    Double.parseDouble(
                            priceText
                    );

        } catch (
                NumberFormatException ex
        ) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please enter a valid price.",
                    "Invalid Price",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (newPrice < 0) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Price cannot be negative.",
                    "Invalid Price",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        // ========================================================
        // SAVE MODIFICATIONS
        // ========================================================

        selectedItem.setName(
                newName
        );

        selectedItem.setPrice(
                newPrice
        );

        details.category =
                (String)
                        categoryBox
                                .getSelectedItem();

        details.description =
                descriptionArea
                        .getText()
                        .trim();

        details.imagePath =
                imageField
                        .getText()
                        .trim();

        currentCategory =
                details.category;

        refreshMenuGrid();
    }

    // ============================================================
    // DELETE MENU ITEM
    // ============================================================

    private void deleteMenuItem() {

        if (menuItems.isEmpty()) {

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "There are no menu items to delete.",
                    "Delete Item",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }

        MenuItem selectedItem =
                selectMenuItem(
                        "Select a menu item to delete:"
                );

        if (selectedItem == null) {
            return;
        }

        int confirm =
                JOptionPane.showConfirmDialog(
                        mainFrame,
                        "Are you sure you want to delete \""
                                + selectedItem.getName()
                                + "\"?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

        if (
                confirm !=
                JOptionPane.YES_OPTION
        ) {
            return;
        }

        menuItems.remove(
                selectedItem
        );

        detailsMap.remove(
                selectedItem
        );

        /*
         * Also remove the item from any current order.
         */
        orders.removeIf(
                order ->
                        order.item == selectedItem
        );

        refreshMenuGrid();
    }

    // ============================================================
    // SELECT MENU ITEM
    // ============================================================

    private MenuItem selectMenuItem(
            String message
    ) {

        String[] itemNames =
                new String[
                        menuItems.size()
                ];

        for (
                int i = 0;
                i < menuItems.size();
                i++
        ) {

            itemNames[i] =
                    menuItems
                            .get(i)
                            .getName();
        }

        String selectedName =
                (String)
                        JOptionPane.showInputDialog(
                                mainFrame,
                                message,
                                "Select Item",
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                itemNames,
                                itemNames[0]
                        );

        if (selectedName == null) {
            return null;
        }

        for (MenuItem item : menuItems) {

            if (
                    item.getName()
                            .equals(selectedName)
            ) {

                return item;
            }
        }

        return null;
    }

    // ============================================================
    // FORM ROW
    // ============================================================

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            Component component
    ) {

        gbc.gridx = 0;
        gbc.gridy = row;

        panel.add(
                new JLabel(label),
                gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        panel.add(
                component,
                gbc
        );

        gbc.weightx = 0;
    }

    // ============================================================
    // LOAD IMAGE
    // ============================================================

    private ImageIcon loadImage(
            String path,
            int width,
            int height
    ) {

        try {

            ImageIcon original =
                    new ImageIcon(path);

            if (
                    original.getIconWidth() <= 0
                    ||
                    original.getIconHeight() <= 0
            ) {

                return null;
            }

            Image image =
                    original
                            .getImage()
                            .getScaledInstance(
                                    width,
                                    height,
                                    Image.SCALE_SMOOTH
                            );

            return new ImageIcon(image);

        } catch (Exception e) {

            return null;
        }
    }

    // ============================================================
    // DISH DETAILS
    // ============================================================

    private static class DishDetails {

        String category;

        String description;

        String imagePath;

        DishDetails(
                String category,
                String description,
                String imagePath
        ) {

            this.category =
                    category;

            this.description =
                    description;

            this.imagePath =
                    imagePath;
        }
    }

    // ============================================================
    // ORDER ITEM
    // ============================================================

    private static class OrderItem {

        MenuItem item;

        String note;

        int quantity;

        OrderItem(
                MenuItem item,
                String note,
                int quantity
        ) {

            this.item = item;

            this.note = note;

            this.quantity = quantity;
        }
    }
}
