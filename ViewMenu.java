    import java.awt.*;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import javax.swing.*;
    import javax.swing.border.EmptyBorder;

    public class ViewMenu extends JPanel {

        private final Frame mainFrame;

        private JPanel menuGrid;
        private String currentCategory = "All";
        private final List<OrderItem> orders = new ArrayList<>();
        private final Map<MenuItem, String> customerNotes = new HashMap<>();

        private final CardLayout viewLayout = new CardLayout();
        private final JPanel viewPanel = new JPanel(viewLayout);

        public ViewMenu(Frame mainFrame) {
            this.mainFrame = mainFrame;
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
            viewPanel.add(menuPanel, "Menu");
            viewPanel.add(showOrderPanel(), "Order");
            viewPanel.add(checkoutpanel(), "Check Out");

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
            JButton orderButton = new JButton("Order Panel");
            JButton checkoutButton = new JButton("Check Out");

            categoryPanel.add(allButton);
            categoryPanel.add(mainDishButton);
            categoryPanel.add(sideDishButton);
            categoryPanel.add(beverageButton);
            categoryPanel.add(orderButton);
            categoryPanel.add(checkoutButton);

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

            JButton closeButton = new JButton("Back");
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.setOpaque(false);
            bottomPanel.add(closeButton);

            menuPanel.add(bottomPanel, BorderLayout.SOUTH);

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
                viewLayout.show(viewPanel, "Order");
            });

            checkoutButton.addActionListener(e -> {
                refreshCheckOut();
                viewLayout.show(viewPanel, "Check Out");
            });
            closeButton.addActionListener(e -> {
                Container parent = menuPanel.getParent();

                if (parent instanceof JPanel) {
                    parent.remove(menuPanel);
                    parent.revalidate();
                    parent.repaint();
                }
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
            card.setPreferredSize(new Dimension(250, 230));
            card.setMinimumSize(new Dimension(250, 230));
            card.setMaximumSize(new Dimension(250, 230));
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

            card.add(descriptionLabel);
            card.add(Box.createVerticalStrut(8));

            JPanel buttonPanel = new JPanel(
                    new FlowLayout(FlowLayout.CENTER, 5, 0)
            );
            buttonPanel.setOpaque(false);

            JButton noteButton = new JButton("Add Note");
            JButton orderButton = new JButton("Add Order");

            buttonPanel.add(noteButton);
            buttonPanel.add(orderButton);

            card.add(buttonPanel);

            noteButton.addActionListener(e -> addNote(item));
            orderButton.addActionListener(e -> addOrder(item));

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

        private void addOrder(MenuItem item) {
            String note = customerNotes.getOrDefault(item, "");

            for (OrderItem order : orders) {
                if (order.item == item) {
                    order.quantity++;
                    order.note = note;

                    JOptionPane.showMessageDialog(
                            this,
                            item.getName() + " quantity increased.",
                            "Order Updated",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    return;
                }
            }

            orders.add(new OrderItem(item, 1, note));

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

            if (orders.isEmpty()) {
                JLabel emptyLabel = new JLabel("No items in your order.");
                emptyLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
                emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

                orderList.add(Box.createVerticalGlue());
                orderList.add(emptyLabel);
                orderList.add(Box.createVerticalGlue());
            } else {
                for (OrderItem order : orders) {
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
                            + order.item.getName()
                            + "</b><br>"
                            + "₱"
                            + String.format("%.2f", order.item.getPrice())
                            + "<br>Quantity: "
                            + order.quantity
                            + "<br>Note: "
                            + (order.note.isEmpty() ? "None" : order.note)
                            + "</html>"
                    );

                    JPanel buttons = new JPanel(new FlowLayout());
                    buttons.setOpaque(false);

                    JButton minusButton = new JButton("-");
                    JButton plusButton = new JButton("+");
                    JButton removeButton = new JButton("Remove");
                    JButton noteButton = new JButton("Edit Note");

                    buttons.add(minusButton);
                    buttons.add(plusButton);
                    buttons.add(noteButton);
                    buttons.add(removeButton);

                    itemPanel.add(itemLabel, BorderLayout.CENTER);
                    itemPanel.add(buttons, BorderLayout.EAST);

                    orderList.add(itemPanel);
                    orderList.add(Box.createVerticalStrut(8));

                    minusButton.addActionListener(e -> {
                        if (order.quantity > 1) {
                            order.quantity--;
                        } else {
                            orders.remove(order);
                        }

                    });

                    plusButton.addActionListener(e -> {
                        order.quantity++;
                    });

                    removeButton.addActionListener(e -> {
                        orders.remove(order);
                    });

                    noteButton.addActionListener(e -> {
                        JTextArea noteArea = new JTextArea(
                                order.note,
                                5,
                                25
                        );

                        noteArea.setLineWrap(true);
                        noteArea.setWrapStyleWord(true);

                        int result = JOptionPane.showConfirmDialog(
                                this,
                                new JScrollPane(noteArea),
                                "Edit Note - " + order.item.getName(),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.PLAIN_MESSAGE
                        );

                        if (result == JOptionPane.OK_OPTION) {
                            order.note = noteArea.getText().trim();
                            customerNotes.put(order.item, order.note);
                            refreshOrderPanel();
                        }
                    });
                }
            }

            JScrollPane scrollPane = new JScrollPane(orderList);
            scrollPane.getVerticalScrollBar().setUnitIncrement(15);

            panel.add(scrollPane, BorderLayout.CENTER);

            double total = 0;

            for (OrderItem order : orders) {
                total += order.item.getPrice() * order.quantity;
            }

            JLabel totalLabel = new JLabel(
                    "Total: ₱" + String.format("%.2f", total)
            );
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
                viewLayout.show(viewPanel, "Check Out");
            });

            return panel;
        }

        private JPanel checkoutpanel(){
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);

            JLabel titleLabel = mainFrame.createLabel(
                    "Receipt ",
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

            JScrollPane scrollPane = new JScrollPane(orderList);
            scrollPane.getVerticalScrollBar().setUnitIncrement(15);

            panel.add(scrollPane, BorderLayout.CENTER);

            double total = 0;

            for (OrderItem order : orders) {
                total += order.item.getPrice() * order.quantity;
            }

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

        private void refreshOrderPanel() {
            viewPanel.remove(1);
            viewPanel.add(showOrderPanel(), "Order");
            viewPanel.revalidate();
            viewPanel.repaint();
        }

        private void refreshCheckOut(){
            viewPanel.remove(1);
            viewPanel.add(checkoutpanel(), "Check Out");
            viewPanel.revalidate();
            viewPanel.repaint();
        }

        private String getCategory(MenuItem item) {
            if (item instanceof CategorizedMenuItem) {
                return ((CategorizedMenuItem) item).getCategory();
            }

            return "Main Dish";
        }

        private String getDescription(MenuItem item) {
            if (item instanceof CategorizedMenuItem) {
                return ((CategorizedMenuItem) item).getDescription();
            }

            return "Delicious restaurant dish";
        }

        private String getImagePath(MenuItem item) {
            if (item instanceof CategorizedMenuItem) {
                return ((CategorizedMenuItem) item).getImagePath();
            }

            return "";
        }

        private static class OrderItem {
            MenuItem item;
            int quantity;
            String note;

            OrderItem(MenuItem item, int quantity, String note) {
                this.item = item;
                this.quantity = quantity;
                this.note = note;
            }
        }

        private interface CategorizedMenuItem {
            String getCategory();
            String getDescription();
            String getImagePath();
        }
    }
