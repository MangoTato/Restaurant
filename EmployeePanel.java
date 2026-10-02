    import java.awt.*;
    import java.awt.image.BufferedImage;
    import java.io.File;
    import java.io.IOException;
    import java.util.List;
    import javax.imageio.ImageIO;
    import javax.swing.*;
    import javax.swing.border.EmptyBorder;

    public class EmployeePanel extends JPanel {

        private final Frame mainFrame;
        private final BufferedImage backgroundImage;
        
        // Content area beside the employee buttons
        private final CardLayout contentLayout = new CardLayout();
        private final JPanel contentPanel = new JPanel(contentLayout);

        public EmployeePanel(Frame mainFrame, String username) {

            this.mainFrame = mainFrame;
            backgroundImage = loadBackgroundImage();

            setLayout(new BorderLayout(0, 20));
            setBackground(Frame.LIGHT);
            setBorder(new EmptyBorder(18, 38, 28, 38));

            // =====================================================
            // HEADER
            // =====================================================

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(mainFrame.createLabel("" ,25,Frame.NAVY),BorderLayout.WEST);

            JButton logout = new JButton("Sign out");
            logout.addActionListener(e -> mainFrame.signOut());
            header.add(logout, BorderLayout.EAST);
            

            // LEFT BUTTONS

            JPanel actions = new JPanel(new GridBagLayout());
            actions.setOpaque(false);

            JPanel buttonPanel = new JPanel(new GridLayout(0,1,0,14));
            buttonPanel.setOpaque(false);

            List<String> actionNames = List.of(
                "Dashboard",
                "My schedule",
                "Shift notes",
                "View Menu",
                "Kitchen",
                "Cashier"
            );

            for (String action : actionNames) {

                JButton button = new JButton(action);
                button.setPreferredSize( new Dimension(180, 55));
                button.addActionListener(e -> handleEmployeeAction(action));

                buttonPanel.add(button);
            }

            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(120, 0, 0, 0);
            actions.add(buttonPanel,gbc);

            // RIGHT CONTENT PANEL

            contentPanel.setBackground(Color.WHITE);
            contentPanel.setBorder(BorderFactory.createLineBorder( Color.LIGHT_GRAY, 1));

            // Default screen
            contentPanel.add(createWelcomePanel(),"Home");

            contentLayout.show(contentPanel,"Home");

            // =====================================================
            // MAIN AREA
            // =====================================================

            JPanel mainArea = new JPanel( new BorderLayout(20, 0));
            mainArea.setOpaque(false);
            mainArea.add(actions,BorderLayout.WEST);
            mainArea.add(contentPanel,BorderLayout.CENTER);

            add( header, BorderLayout.NORTH);
            add( mainArea, BorderLayout.CENTER);
            add(createSignedInStatus(), BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (backgroundImage != null) {
                graphics.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }

        private BufferedImage loadBackgroundImage() {
            try {
                return ImageIO.read(new File("Images", "5.png"));
            } catch (IOException e) {
                return null;
            }
        }

        private void handleEmployeeAction( String action) {

            if ("View Menu".equals(action)) {
               ViewMenu menu = new ViewMenu(mainFrame, false);

                contentPanel.add(menu.ShowViewMenuPanel(),"View Menu");
                contentLayout.show(contentPanel,"View Menu");
                contentPanel.revalidate();
                contentPanel.repaint();

            } else if ("Kitchen".equals(action)) {
                contentPanel.removeAll();
                contentPanel.add(createKitchenPanel(), "Kitchen");
                contentLayout.show(contentPanel, "Kitchen");
                contentPanel.revalidate();
                contentPanel.repaint();

            } else if ("Cashier".equals(action)) {
                contentPanel.removeAll();
                contentPanel.add(createCashierPanel(), "Cashier");
                contentLayout.show(contentPanel, "Cashier");
                contentPanel.revalidate();
                contentPanel.repaint();

            } else {

                contentPanel.removeAll();
                contentPanel.add(ConstructionPanel(action), action);
                contentLayout.show(contentPanel, action);
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        }

        private JPanel createCashierPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(30, 35, 30, 35));

            JLabel title = new JLabel("Cashier - Checkout Requests");
            title.setFont(new Font("SansSerif", Font.BOLD, 25));
            title.setForeground(Frame.NAVY);
            panel.add(title, BorderLayout.NORTH);

            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setBackground(Color.WHITE);
            JScrollPane scroll = new JScrollPane(list);
            scroll.getVerticalScrollBar().setUnitIncrement(15);
            panel.add(scroll, BorderLayout.CENTER);

            Runnable refresh = () -> {
                list.removeAll();
                boolean hasRequests = false;
                for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
                    if (!mainFrame.tableService.isCheckoutRequested(table)) continue;
                    hasRequests = true;
                    JPanel card = new JPanel(new BorderLayout());
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
                    card.setBackground(Color.WHITE);
                    card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY), new EmptyBorder(14, 14, 14, 14)));
                    JLabel details = new JLabel("<html><b>Table " + table + "</b><br>Total: ₱"
                        + String.format("%.2f", mainFrame.orderService.getTableTotal(table)) + "</html>");
                    details.setFont(new Font("SansSerif", Font.PLAIN, 16));
                    card.add(details, BorderLayout.CENTER);
                    list.add(card);
                    list.add(Box.createVerticalStrut(8));
                }
                if (!hasRequests) {
                    JLabel empty = new JLabel("No customers are waiting for checkout.");
                    empty.setFont(new Font("SansSerif", Font.PLAIN, 16));
                    list.add(empty);
                }
                list.revalidate();
                list.repaint();
            };
            mainFrame.tableService.addListener(refresh);
            mainFrame.orderService.addListener(refresh);
            refresh.run();
            return panel;
        }

        private JPanel createKitchenPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(30, 35, 30, 35));
            JLabel title = new JLabel("Kitchen Orders");
            title.setFont(new Font("SansSerif", Font.BOLD, 25));
            title.setForeground(Frame.NAVY);
            panel.add(title, BorderLayout.NORTH);

            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setBackground(Color.WHITE);
            JScrollPane scroll = new JScrollPane(list);
            panel.add(scroll, BorderLayout.CENTER);

            Runnable refresh = () -> {
                list.removeAll();
                java.util.List<KitchenOrder> orders = mainFrame.orderService.getKitchenOrders();
                if (orders.isEmpty()) {
                    JLabel empty = new JLabel("No customer orders waiting.");
                    empty.setFont(new Font("SansSerif", Font.PLAIN, 16));
                    list.add(empty);
                }
                for (KitchenOrder order : orders) {
                    JPanel card = new JPanel(new BorderLayout(10, 8));
                    card.setBackground(Color.WHITE);
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
                    card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), new EmptyBorder(10, 10, 10, 10)));
                    String status = order.isPending() ? "Waiting for cooking time" : order.isComplete() ? "For Serving" : "Cooking: " + formatRemaining(order.getSecondsRemaining());
                    JLabel details = new JLabel("<html><b>" + order.getItem().getName() + "</b><br>Table: " + order.getTableNumber() + "<br>Quantity: " + order.getQuantity() + "<br>Note: " + (order.getNote().isEmpty() ? "None" : order.getNote()) + "<br>" + status + "</html>");
                    card.add(details, BorderLayout.CENTER);
                    JPanel controls = new JPanel(new GridLayout(5, 1, 4, 4));
                    JButton minusTime = new JButton("− 5 mins");
                    JButton five = new JButton("5 mins");
                    JButton thirty = new JButton("30 mins");
                    JButton hour = new JButton("1 hour");
                    JButton complete = new JButton("Complete Order");
                    minusTime.setEnabled(order.isCooking());
                    five.setEnabled(!order.isComplete());
                    thirty.setEnabled(!order.isComplete());
                    hour.setEnabled(!order.isComplete());
                    complete.setEnabled(!order.isComplete());
                    minusTime.addActionListener(e -> mainFrame.orderService.removeCookingTime(order, 5));
                    five.addActionListener(e -> mainFrame.orderService.startCooking(order, 5));
                    thirty.addActionListener(e -> mainFrame.orderService.startCooking(order, 30));
                    hour.addActionListener(e -> mainFrame.orderService.startCooking(order, 60));
                    complete.addActionListener(e -> mainFrame.orderService.completeOrder(order));
                    controls.add(minusTime); controls.add(five); controls.add(thirty); controls.add(hour); controls.add(complete);
                    card.add(controls, BorderLayout.EAST);
                    list.add(card);
                    list.add(Box.createVerticalStrut(8));
                }
                list.revalidate(); list.repaint();
            };
            mainFrame.orderService.addListener(refresh);
            Timer kitchenClock = new Timer(1000, e -> {
                if (!panel.isDisplayable()) {
                    ((Timer) e.getSource()).stop();
                } else {
                    refresh.run();
                }
            });
            kitchenClock.start();
            refresh.run();
            return panel;
        }

        private String formatRemaining(long seconds) {
            return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
        
        private JPanel createWelcomePanel() {

            JPanel panel = new JPanel( new BorderLayout() );
            panel.setBackground( Color.WHITE);
            panel.setBorder( new EmptyBorder( 30,35,30, 35));

            JLabel title = new JLabel("Employee Dashboard");
            title.setFont( new Font( "SansSerif", Font.BOLD,25));
            title.setForeground( Frame.NAVY );

            panel.add(title, BorderLayout.NORTH);

            JTextArea text = new JTextArea(
                    "\nSelect an option from the left "
                    + "to begin managing today's "
                    + "restaurant operations."
            );

            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont( new Font("SansSerif", Font.PLAIN,16 ) );
            text.setBackground(Color.WHITE);

            panel.add( text,BorderLayout.CENTER);

            return panel;
        }

        private JPanel createSignedInStatus() {

            JPanel status = new JPanel();
            status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));
            status.setBackground(Color.WHITE);
            status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                new EmptyBorder(20, 20, 20, 20)
            ));

            JLabel signedIn = new JLabel(
                "<html>You are signed in as an <b>employee</b>.</html>"
            );
            signedIn.setFont(new Font("SansSerif", Font.PLAIN, 15));

            JLabel message = new JLabel(
                "Choose an option to begin managing today's restaurant operations."
            );
            message.setFont(new Font("SansSerif", Font.PLAIN, 15));

            status.add(signedIn);
            status.add(Box.createVerticalStrut(6));
            status.add(message);

            return status;
        }

        private JPanel ConstructionPanel(String title) {

            JPanel panel =new JPanel(new BorderLayout());

            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder( 30,35,30, 35));

            JLabel label = new JLabel(title);
            label.setFont( new Font("SansSerif", Font.BOLD, 25));
            label.setForeground( Frame.NAVY);

            panel.add( label, BorderLayout.NORTH);

            JLabel message = new JLabel( title + " is Under Construction.");
            message.setFont(new Font("SansSerif", Font.PLAIN,16));

            panel.add( message, BorderLayout.CENTER);

            return panel;
        }

    }
