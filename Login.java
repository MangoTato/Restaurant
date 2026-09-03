import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

class Login {
    private final Frame mainFrame;

    Login(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }

    JPanel LoginPanel() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Frame.LIGHT);

        JPanel card = new JPanel(new BorderLayout(28, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(38, 42, 38, 42));

        // Left Branding Panel
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(Frame.NAVY);
        brand.setPreferredSize(new Dimension(275, 350));
        JLabel mark = new JLabel("NAME", SwingConstants.CENTER);
        mark.setForeground(Color.WHITE);
        mark.setFont(new Font("SansSerif", Font.BOLD, 36));
        mark.setBorder(BorderFactory.createLineBorder(Frame.TEAL, 2));
        mark.setPreferredSize(new Dimension(82, 82));
        JLabel title = mainFrame.createLabel("NAME RESTAURANT", 22, Color.WHITE);
        JLabel subtitle = mainFrame.createLabel("Restaurant operations<br>made simple", 14, new Color(207, 225, 229));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel brandText = new JPanel(new BorderLayout(0, 12));
        brandText.setOpaque(false);
        brandText.add(mark, BorderLayout.NORTH);
        brandText.add(title, BorderLayout.CENTER);
        brandText.add(subtitle, BorderLayout.SOUTH);
        brand.add(brandText);

        // Right Form Panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        mainFrame.addField(form, gbc, 0, "Welcome back", null);
        mainFrame.addField(form, gbc, 1, "Sign in to continue to your workspace", null);
        mainFrame.addField(form, gbc, 2, "Username", mainFrame.usernameField);
        mainFrame.addField(form, gbc, 3, "Password", mainFrame.passwordField);

        ButtonGroup roles = new ButtonGroup();
        roles.add(mainFrame.adminRole);
        roles.add(mainFrame.employeeRole);
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rolePanel.setBackground(Color.WHITE);
        rolePanel.add(mainFrame.adminRole);
        rolePanel.add(mainFrame.employeeRole);
        mainFrame.addField(form, gbc, 4, "Sign in as", rolePanel);

        JCheckBox showPassword = new JCheckBox("Show password");
        showPassword.setBackground(Color.WHITE);
        showPassword.addActionListener(e -> mainFrame.passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : '\u2022'));
        mainFrame.addField(form, gbc, 5, null, showPassword);

        JButton loginButton = new JButton("Sign in");
        loginButton.setBackground(Frame.TEAL);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.addActionListener(e -> mainFrame.attemptLogin());
        mainFrame.passwordField.addActionListener(e -> mainFrame.attemptLogin());
        mainFrame.addField(form, gbc, 6, null, loginButton);

        mainFrame.messageLabel.setForeground(new Color(190, 55, 55));
        mainFrame.addField(form, gbc, 7, null, mainFrame.messageLabel);

        card.add(brand, BorderLayout.WEST);
        card.add(form, BorderLayout.CENTER);
        root.add(card);
        return root;
    }
}