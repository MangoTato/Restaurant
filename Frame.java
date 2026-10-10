import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class Frame extends JFrame {

    public static final Color NAVY = new Color(18, 31, 62);
    public static final Color ACCENT = new Color(72, 135, 247);
    public static final Color ACCENT_DARK = new Color(18, 31, 62);
    public static final Color SUCCESS = new Color(59, 143, 94);
    public static final Color BACKGROUND = new Color(212, 237, 243);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(216, 222, 232);
    public static final Color MUTED = new Color(99, 111, 130);
    public static final Color LIGHT = BACKGROUND;
    private static boolean buttonHoverEffectsInstalled;

    final Map<String, User> userMap = new HashMap<>();
    final CardLayout cardLayout = new CardLayout();
    final JPanel content = new JPanel(cardLayout);
    private final JPanel loginPanel;
    final JTextField usernameField = new JTextField(18);
    final JPasswordField passwordField = new JPasswordField(18);

    final JRadioButton adminRole = new JRadioButton("Administrator", true);
    final JRadioButton employeeRole = new JRadioButton("Employee");
    final JRadioButton customer = new JRadioButton("Customer");
    final JLabel messageLabel = new JLabel(" ");
    final TableService tableService = new TableService();
    final OrderService orderService = new OrderService(tableService);

    public Frame() {

        installButtonHoverEffects();
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Button.background", ACCENT);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.select", Color.WHITE);
        userMap.put("admin", new User("admin", "admin123", true, false));
        userMap.put("employee", new User("employee", "employee123", false, true));
        userMap.put("customer", new User("customer", "customer123", false, false));

        setTitle("Pâques • Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(850, 520));
        setSize(850, 520);
        setLocationRelativeTo(null);

        loginPanel = new Login(this).LoginPanel();
        content.add(loginPanel, "login");
        setContentPane(content);
        orderService.loadActiveOrders(error -> JOptionPane.showMessageDialog(this,
                "Unable to restore active orders from the database:\n" + error.getMessage(), "Order Database Error",
                JOptionPane.ERROR_MESSAGE));
    }

    private static void installButtonHoverEffects() {
        if (buttonHoverEffectsInstalled) {
            return;
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof MouseEvent) || !(event.getSource() instanceof JButton)) {
                return;
            }
            MouseEvent mouseEvent = (MouseEvent) event;
            JButton button = (JButton) event.getSource();
            if (Boolean.TRUE.equals(button.getClientProperty("pâques.hover.only"))) {
                if (mouseEvent.getID() == MouseEvent.MOUSE_ENTERED
                        || mouseEvent.getID() == MouseEvent.MOUSE_PRESSED
                        || (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED
                                && button.contains(mouseEvent.getPoint()))) {
                    button.setOpaque(true);
                    button.setContentAreaFilled(true);
                    button.setBackground(new Color(239, 242, 247));
                    button.setForeground(NAVY);
                } else if (mouseEvent.getID() == MouseEvent.MOUSE_EXITED
                        || mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
                    button.setOpaque(false);
                    button.setContentAreaFilled(false);
                    boolean selected = button.getFont().isBold();
                    button.setForeground(selected ? ACCENT : NAVY);
                }
                return;
            }
            if (isHoverSuppressed(button)) {
                return;
            }
            if (!button.isEnabled()) {
                return;
            }
            if (mouseEvent.getID() == MouseEvent.MOUSE_ENTERED) {
                button.setBackground(NAVY);
                button.setForeground(Color.WHITE);
            } else if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED) {
                button.setBackground(Color.WHITE);
                button.setForeground(ACCENT);
            } else if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
                boolean pointerInside = button.contains(mouseEvent.getPoint());
                button.setBackground(pointerInside ? NAVY : buttonBaseBackground(button));
                button.setForeground(pointerInside ? Color.WHITE : buttonBaseForeground(button));
            } else if (mouseEvent.getID() == MouseEvent.MOUSE_EXITED) {
                button.setBackground(buttonBaseBackground(button));
                button.setForeground(buttonBaseForeground(button));
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
        buttonHoverEffectsInstalled = true;
    }

    private static boolean isHoverSuppressed(Component component) {
        for (Component current = component; current != null; current = current.getParent()) {
            if (current instanceof JComponent && Boolean.TRUE.equals(
                    ((JComponent) current).getClientProperty("pâques.disable.button.hover"))) {
                return true;
            }
        }
        return false;
    }

    private static Color buttonBaseBackground(JButton button) {
        return Boolean.TRUE.equals(button.getClientProperty("pâques.selected")) ? Color.WHITE : ACCENT;
    }

    private static Color buttonBaseForeground(JButton button) {
        return Boolean.TRUE.equals(button.getClientProperty("pâques.selected")) ? ACCENT : Color.WHITE;
    }

    public static void styleButtonState(JButton button, boolean selected) {
        button.putClientProperty("pâques.selected", selected);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(selected ? Color.WHITE : ACCENT);
        button.setForeground(selected ? ACCENT : Color.WHITE);
    }

    public static void clearButtonHoverAppearance(JButton button) {
        button.putClientProperty("pâques.selected", false);
    }

    public static JPanel createWorkspaceCanvas(JPanel workspace) {
        return workspace;
    }

    
    public static class BackgroundPanel extends JPanel {
        private boolean decorative;

        public BackgroundPanel() {
            this(false);
        }

        public BackgroundPanel(boolean decorative) {
            this.decorative = decorative;
            setOpaque(true);
        }

        public void setDecorativeBackground(boolean decorative) {
            this.decorative = decorative;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (!decorative) {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(246, 242, 255),
                        width, height, new Color(225, 243, 255)));
                g2.fillRect(0, 0, width, height);

              
                g2.setColor(new Color(112, 72, 208, 42));
                g2.fillOval(-115, height - 230, 360, 360);
                g2.setColor(new Color(22, 155, 180, 32));
                g2.fillOval(width - 235, -145, 350, 350);
                g2.setColor(new Color(112, 72, 208, 18));
                g2.fillOval(-56, -54, 142, 142);
                g2.setColor(new Color(255, 187, 102, 24));
                g2.fillOval(66, 34, 48, 48);
                g2.setColor(new Color(112, 72, 208, 20));
                g2.fillOval(width - 175, 48, 145, 145);
                g2.setColor(new Color(22, 155, 180, 26));
                g2.fillOval(width - 92, 154, 68, 68);
                g2.setColor(new Color(255, 187, 102, 28));
                g2.fillOval(width - 238, 177, 46, 46);
                g2.setColor(new Color(255, 255, 255, 138));
                g2.fillRoundRect(28, 82, 305, 218, 28, 28);
                g2.setColor(new Color(255, 255, 255, 94));
                g2.fillRoundRect(370, 58, 420, 405, 30, 30);
            } finally {
                g2.dispose();
            }
        }
    }

    public boolean addUser(String username, String password, boolean isAdmin, boolean IsEmployee) {

        if (userMap.containsKey(username)) {
            return false;
        }
        userMap.put(username, new User(username, password, isAdmin, IsEmployee));
        return true;
    }

    public JLabel createLabel(String text, int size, Color color) {

        JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        label.setFont(new Font("SansSerif", Font.PLAIN, size));
        label.setForeground(color);

        return label;
    }

    public void addField(JPanel form, GridBagConstraints gbc, int row, String labelText, Component comp) {
        gbc.gridy = row;
        if (labelText != null) {

            JLabel lbl = new JLabel(labelText);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

            form.add(lbl, gbc);
            gbc.gridy = row + 1;
        }

        if (comp != null) {
            form.add(comp, gbc);
        }
    }

    public void attemptLogin() {
        new attemptlogin(this).attemptLogin();
    }

    public void showOverview(String username, boolean isAdmin, boolean isEmployee) {
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        content.removeAll();
        content.add(loginPanel, "login");
        if (isAdmin) {
            Admin adminPanel = new Admin(this, username);
            content.add(adminPanel, "dashboard");
        } else if (isEmployee) {
            EmployeePanel employeePanel = new EmployeePanel(this, username);
            content.add(employeePanel, "dashboard");
        } else {
            Customer customerPanel = new Customer(this, username);
            content.add(customerPanel, "dashboard");
        }

        cardLayout.show(content, "dashboard");

        content.revalidate();
        content.repaint();
    }

    public void showManageMenu() {

        ManageMenu menu = new ManageMenu(this);
        menu.ShowMenuPanel();
    }

    public void showViewMenu() {

        ViewMenu menu = new ViewMenu(this, true);
        menu.ShowViewMenuPanel();
    }

    public void showAdmin(String username) {
        showOverview(username, true, false);
    }

    public void ShowStaff(String username) {
        Staff staffpanel = new Staff(this);
        staffpanel.showStaff();
    }

    public void signOut() {

        usernameField.setText("");
        passwordField.setText("");
        messageLabel.setText(" ");

        setExtendedState(JFrame.NORMAL);
        setResizable(false);
        setSize(850, 520);
        setLocationRelativeTo(null);
        content.removeAll();
        content.add(loginPanel, "login");
        cardLayout.show(content, "login");

        content.revalidate();
        content.repaint();
    }
}
