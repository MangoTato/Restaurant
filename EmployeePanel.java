    import java.awt.*;
    import java.util.List;
    import javax.swing.*;
    import javax.swing.border.EmptyBorder;

    public class EmployeePanel extends JPanel {

        private final Frame mainFrame;
        
        // Content area beside the employee buttons
        private final CardLayout contentLayout = new CardLayout();
        private final JPanel contentPanel = new JPanel(contentLayout);

        public EmployeePanel(Frame mainFrame, String username) {

            this.mainFrame = mainFrame;

            setLayout(new BorderLayout(0, 20));
            setBackground(Frame.LIGHT);
            setBorder(new EmptyBorder(28, 38, 28, 38));

            // =====================================================
            // HEADER
            // =====================================================

            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(mainFrame.createLabel("Good day, " + username,25,Frame.NAVY),BorderLayout.WEST);

            JButton logout = new JButton("Sign out");
            logout.addActionListener(e -> mainFrame.signOut());
            header.add(logout,BorderLayout.EAST);

            // LEFT BUTTONS

            JPanel actions = new JPanel(new GridBagLayout());
            actions.setOpaque(false);

            JPanel buttonPanel = new JPanel(new GridLayout(0,1,0,14));
            buttonPanel.setOpaque(false);

            List<String> actionNames = List.of(
                "Dashboard",
                "My schedule",
                "Shift notes",
                "View Menu"
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
        }

        private void handleEmployeeAction( String action) {

            if ("View Menu".equals(action)) {
               ViewMenu menu = new ViewMenu(mainFrame);

                contentPanel.add(menu.ShowViewMenuPanel(),"View Menu");
                contentLayout.show(contentPanel,"View Menu");
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
