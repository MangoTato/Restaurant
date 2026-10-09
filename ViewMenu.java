    import java.awt.*;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import javax.swing.*;
    import javax.swing.border.EmptyBorder;

    public class ViewMenu extends JPanel {

        private final Frame mainFrame;
        private final boolean customerView;
        private final int tableNumber;

        private JPanel menuGrid;
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
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.NAVY, 2),
                    new EmptyBorder(25, 30, 25, 30)
            ));
            card.setPreferredSize(new Dimension(1100, 680));

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
            JPanel menuPanel = new JPanel(new BorderLayout(0, 15));
            menuPanel.setBackground(Color.WHITE);

            JLabel title = mainFrame.createLabel("Restaurant Menu", 25, Frame.NAVY);
            JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titlePanel.setOpaque(false);
            titlePanel.add(title);
            menuPanel.add(titlePanel, BorderLayout.NORTH);

            JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
            centerPanel.setOpaque(false);

            JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            categoryPanel.setOpaque(false);

            JButton allButton = new JButton("Main Menu");
            JButton mainDishButton = new JButton("Main Dish");
            JButton sideDishButton = new JButton("Side Dish");
            JButton beverageButton = new JButton("Beverages");

            categoryPanel.add(allButton);
            categoryPanel.add(mainDishButton);
            categoryPanel.add(sideDishButton);
            categoryPanel.add(beverageButton);
            if (customerView) {
                JButton orderButton = new JButton("Order Panel");
                JButton checkoutButton = new JButton("Check Out");
                categoryPanel.add(orderButton);
                categoryPanel.add(checkoutButton);
                orderButton.addActionListener(e -> {
                    refreshOrderPanel();
                    viewLayout.show(viewPanel, "Order");
                });
                checkoutButton.addActionListener(e -> {
                    showCheckoutWhenReady();
                });
            }

            centerPanel.add(categoryPanel, BorderLayout.NORTH);

            menuGrid = new JPanel(new GridLayout(0, 3, 15, 15));
            menuGrid.setBackground(Color.WHITE);
            menuGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

            refreshMenuGrid();

            JScrollPane scrollPane = new JScrollPane(menuGrid);
            scrollPane.setPreferredSize(new Dimension(1000, 470));
            scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            scrollPane.getVerticalScrollBar().setUnitIncrement(15);

            centerPanel.add(scrollPane, BorderLayout.CENTER);
            menuPanel.add(centerPanel, BorderLayout.CENTER);

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

            return menuPanel;
        }

        private void refreshMenuGrid() {
            if (menuGrid == null) {
                return;
            }

            menuGrid.removeAll();

            for (MenuItem item : MenuData.menuItems) {
                if (currentCategory.equals("All") || getCategory(item).equals(currentCategory)) {
                    menuGrid.add(createMenuCard(item));
                }
            }

            menuGrid.revalidate();
            menuGrid.repaint();
        }

        private JPanel createMenuCard(MenuItem item) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Color.WHITE);
            card.setPreferredSize(new Dimension(250, 260));
            card.setMinimumSize(new Dimension(250, 260));
            card.setMaximumSize(new Dimension(250, 260));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.NAVY, 1),
                    new EmptyBorder(12, 12, 12, 12)
            ));

            JLabel imageLabel = new JLabel();
            imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            imageLabel.setPreferredSize(new Dimension(210, 90));
            imageLabel.setMaximumSize(new Dimension(210, 90));

            String imagePath = getImagePath(item);

            if (imagePath != null && !imagePath.isEmpty()) {
                ImageIcon icon = new ImageIcon(imagePath);
                Image image = icon.getImage().getScaledInstance(
                        200,
                        85,
                        Image.SCALE_SMOOTH
                );
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

            JLabel priceLabel = new JLabel(
                    String.format("₱%.2f", item.getPrice())
            );
            priceLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            priceLabel.setForeground(Frame.TEAL);
            priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(priceLabel);
            card.add(Box.createVerticalStrut(5));

            JLabel descriptionLabel = new JLabel(
                    "<html><center>" + getDescription(item) + "</center></html>"
            );
            descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            descriptionLabel.setForeground(Color.DARK_GRAY);
            descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            descriptionLabel.setMaximumSize(new Dimension(220, 35));

            card.add(descriptionLabel);
            card.add(Box.createVerticalStrut(8));

            JPanel buttonPanel = new JPanel(
                    new FlowLayout(FlowLayout.CENTER, 5, 0)
            );
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

            int result = JOptionPane.showConfirmDialog(
                    this,
                    scrollPane,
                    "Add Note - " + item.getName(),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String note = noteArea.getText().trim();
                customerNotes.put(item, note);

                JOptionPane.showMessageDialog(
                        this,
                        "Note saved for " + item.getName() + ".",
                        "Note Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }

        private void addOrder(MenuItem item, int quantity) {
            String note = customerNotes.getOrDefault(item, "");
            mainFrame.orderService.add(item, note, quantity, tableNumber);

            JOptionPane.showMessageDialog(
                    this,
                    item.getName() + " added to your order.",
                    "Order Added",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        private JPanel showOrderPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);

            JLabel titleLabel = mainFrame.createLabel(
                    "Order Panel",
                    25,
                    Frame.NAVY
            );

            JPanel titlePanel = new JPanel(
                    new FlowLayout(FlowLayout.LEFT, 0, 0)
            );
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
                    itemPanel.setBackground(Color.WHITE);
                    itemPanel.setBorder(
                            BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                                    new EmptyBorder(10, 10, 10, 10)
                            )
                    );

                    JLabel itemLabel = new JLabel(
                            "<html><b>"
                            + order.getItem().getName()
                            + "</b><br>"
                            + "₱"
                            + String.format("%.2f", order.getItem().getPrice())
                            + "<br>Quantity: "
                            + order.getQuantity()
                            + "<br>Note: "
                            + (order.getNote().isEmpty() ? "None" : order.getNote())
                            + "</html>"
                    );

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
                            removeButton.setEnabled(order.isPending() && !order.isSentToKitchen());
                            buttons.revalidate();
                        }
                    });
                    countdown.start();

                    removeButton.setEnabled(order.isPending() && !order.isSentToKitchen());

                    orderList.add(itemPanel);
                    orderList.add(Box.createVerticalStrut(8));

                    removeButton.addActionListener(e -> {
                        mainFrame.orderService.changeQuantity(order, 0);
                        refreshOrderPanel();
                    });

                    noteButton.addActionListener(e -> {
                        JTextArea noteArea = new JTextArea(
                                order.getNote(),
                                5,
                                25
                        );

                        noteArea.setLineWrap(true);
                        noteArea.setWrapStyleWord(true);

                        int result = JOptionPane.showConfirmDialog(
                                this,
                                new JScrollPane(noteArea),
                                "Edit Note - " + order.getItem().getName(),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.PLAIN_MESSAGE
                        );

                        if (result == JOptionPane.OK_OPTION) {
                            String note = noteArea.getText().trim();
                            mainFrame.orderService.updateNote(order, note);
                            customerNotes.put(order.getItem(), note);
                            refreshOrderPanel();
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

            JLabel totalLabel = new JLabel(
                    "Total: ₱" + String.format("%.2f", total)
            );
            totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            totalLabel.setForeground(Frame.NAVY);

            JButton closeButton = new JButton("Back to Menu");
            JButton sendToKitchenButton = new JButton("Send to Kitchen");
            JButton checkoutbutton = new JButton("Check Out");
            boolean hasDraftOrders = orders.stream().anyMatch(order -> !order.isSentToKitchen());
            sendToKitchenButton.setEnabled(hasDraftOrders);

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setOpaque(false);
            bottomPanel.add(totalLabel, BorderLayout.WEST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actions.setOpaque(false);
            actions.add(sendToKitchenButton);
            actions.add(checkoutbutton);
            actions.add(closeButton);
            bottomPanel.add(actions, BorderLayout.EAST);
            
            panel.add(bottomPanel, BorderLayout.SOUTH);

            closeButton.addActionListener(e -> {
                viewLayout.show(viewPanel, "Menu");
            });
            sendToKitchenButton.addActionListener(e -> {
                mainFrame.orderService.sendTableOrdersToKitchen(tableNumber);
                refreshOrderPanel();
            });
            checkoutbutton.addActionListener(e -> {
                showCheckoutWhenReady();
            });

            return panel;
        }

        private JPanel checkoutpanel(){
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);

            JLabel titleLabel = mainFrame.createLabel(
                    "Receipt - Table " + tableNumber,
                    25,
                    Frame.NAVY
            );

            JPanel titlePanel = new JPanel(
                    new FlowLayout(FlowLayout.LEFT, 0, 0)
            );
            titlePanel.setOpaque(false);
            titlePanel.add(titleLabel);

            panel.add(titlePanel, BorderLayout.NORTH);

            JPanel orderList = new JPanel();
            orderList.setLayout(new BoxLayout(orderList, BoxLayout.Y_AXIS));
            orderList.setBackground(Color.WHITE);

            for (KitchenOrder order : getDisplayedOrders()) {
                JLabel item = new JLabel(order.getItem().getName() + "  x" + order.getQuantity()
                    + "  ₱" + String.format("%.2f", order.getItem().getPrice() * order.getQuantity()));
                item.setBorder(new EmptyBorder(7, 10, 7, 10));
                orderList.add(item);
            }
            if (orderList.getComponentCount() == 0) {
                orderList.add(new JLabel("No items were ordered."));
            }

            JScrollPane scrollPane = new JScrollPane(orderList);
            scrollPane.getVerticalScrollBar().setUnitIncrement(15);

            panel.add(scrollPane, BorderLayout.CENTER);

            double total = 0;

            total = mainFrame.orderService.getTableTotal(tableNumber);

            JLabel totalLabel = new JLabel(
                    "Total: ₱" + String.format("%.2f", total)
            );
            totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
            totalLabel.setForeground(Frame.NAVY);

            JButton closeButton = new JButton("Back to Menu");

            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setOpaque(false);
            bottomPanel.add(totalLabel, BorderLayout.WEST);
            bottomPanel.add(closeButton, BorderLayout.EAST);

            panel.add(bottomPanel, BorderLayout.SOUTH);

            closeButton.addActionListener(e -> {
                viewLayout.show(viewPanel, "Menu");
            });

            return panel;
        }

        private JPanel waitForRepresentativePanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.NAVY, 2), new EmptyBorder(45, 70, 45, 70)));

            JLabel title = mainFrame.createLabel("Checkout Requested", 26, Frame.NAVY);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel message = new JLabel("Please wait for a representative.");
            message.setFont(new Font("SansSerif", Font.PLAIN, 17));
            message.setAlignmentX(Component.CENTER_ALIGNMENT);
            waitTotalLabel = new JLabel("Table " + tableNumber + " • Total: ₱"
                + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)));
            waitTotalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton done = new JButton("Done");
            done.setAlignmentX(Component.CENTER_ALIGNMENT);
            done.addActionListener(e -> {
                JPasswordField passwordField = new JPasswordField(15);
                JPanel passwordPanel = new JPanel(new GridLayout(2, 1, 5, 5));
                passwordPanel.add(new JLabel("Employee password:"));
                passwordPanel.add(passwordField);

                int result = JOptionPane.showConfirmDialog(
                        this,
                        passwordPanel,
                        "Employee Verification",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (result == JOptionPane.OK_OPTION) {
                    String password = new String(passwordField.getPassword());
                    if ("emp1".equals(password)) {
                        refreshCheckOut();
                        viewLayout.show(viewPanel, "Check Out");
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                "Incorrect employee password.",
                                "Access Denied",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
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

        private void refreshCheckOut(){
            viewPanel.remove(receiptPanel);
            receiptPanel = checkoutpanel();
            viewPanel.add(receiptPanel, "Check Out");
            viewPanel.revalidate();
            viewPanel.repaint();
        }

        private void showCheckoutWhenReady() {
            java.util.List<KitchenOrder> orders = getDisplayedOrders();
            if (orders.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "You need an order before checking out.",
                        "No Active Order",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            for (KitchenOrder order : orders) {
                if (!order.isReceived()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Checkout is available after every order is marked Order Received.",
                            "Order Not Received",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
            }

            mainFrame.tableService.requestCheckout(tableNumber);
            waitTotalLabel.setText("Table " + tableNumber + " • Total: ₱"
                + String.format("%.2f", mainFrame.orderService.getTableTotal(tableNumber)));
            viewLayout.show(viewPanel, "Wait");
        }

        private void updateStatusLabel(JLabel label, KitchenOrder order) {
            if (!order.isSentToKitchen()) {
                label.setText("");
            } else if (order.isPending()) {
                label.setText("Order Sent to Kitchen");
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
                label.setForeground(seconds == 0 ? new Color(0, 130, 80) : Frame.TEAL);
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

