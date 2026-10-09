import java.awt.*;
import javax.swing.*;

public class Login {
    private final Frame mainFrame;

    Login(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }

    JPanel LoginPanel() {
        JPanel root = new JPanel(null);
        root.setPreferredSize(new Dimension(850, 520));
        root.setBackground(Frame.BACKGROUND);

        JLabel brand = new JLabel("Pâques");
        brand.setBounds(48, 128, 300, 48);
        brand.setFont(new Font("SansSerif", Font.BOLD, 36));
        brand.setForeground(Frame.NAVY);
        root.add(brand);

        JLabel description = new JLabel("<html>Restaurant operations<br>made simple.</html>");
        description.setBounds(50, 184, 300, 60);
        description.setFont(new Font("SansSerif", Font.PLAIN, 18));
        description.setForeground(Frame.MUTED);
        root.add(description);

        JPanel loginCard = new JPanel(null);
        loginCard.setBounds(420, 90, 400, 340);
        loginCard.setBackground(Color.WHITE);
        loginCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER),
                BorderFactory.createEmptyBorder(16, 24, 16, 24)));
        root.add(loginCard);

        JLabel welcome = new JLabel("Welcome back");
        welcome.setBounds(24, 18, 345, 32);
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcome.setForeground(Frame.NAVY);
        loginCard.add(welcome);

        JLabel instruction = new JLabel("Sign in to continue to your workspace.");
        instruction.setBounds(24, 52, 345, 24);
        instruction.setFont(new Font("SansSerif", Font.PLAIN, 13));
        instruction.setForeground(Frame.MUTED);
        loginCard.add(instruction);

        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setBounds(27, 91, 310, 20);
        usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        usernameLabel.setForeground(Frame.NAVY);
        loginCard.add(usernameLabel);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(27, 158, 310, 20);
        passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        passwordLabel.setForeground(Frame.NAVY);
        loginCard.add(passwordLabel);

        JButton customerButton = new JButton("Continue as customer");
        customerButton.setBounds(48, 440, 180, 34);
        Frame.styleButtonState(customerButton, false);
        customerButton.setFocusPainted(false);
        customerButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        customerButton.setToolTipText("Continue as a customer");
        customerButton.addActionListener(e -> mainFrame.showOverview("Customer", false, false));
        root.add(customerButton);

        mainFrame.usernameField.setBounds(27, 113, 309, 32);
        mainFrame.passwordField.setBounds(27, 180, 309, 32);
        loginCard.add(mainFrame.usernameField);
        loginCard.add(mainFrame.passwordField);

        ButtonGroup roles = new ButtonGroup();
        roles.add(mainFrame.adminRole);
        roles.add(mainFrame.employeeRole);
        mainFrame.adminRole.setOpaque(false);
        mainFrame.employeeRole.setOpaque(false);
        mainFrame.adminRole.setBounds(27, 224, 130, 25);
        mainFrame.employeeRole.setBounds(164, 224, 130, 25);
        loginCard.add(mainFrame.adminRole);
        loginCard.add(mainFrame.employeeRole);

        JButton loginButton = new JButton("Sign in");
        loginButton.setBounds(27, 264, 309, 36);
        Frame.styleButtonState(loginButton, false);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.addActionListener(e -> mainFrame.attemptLogin());
        loginCard.add(loginButton);

        mainFrame.usernameField.addActionListener(e -> mainFrame.passwordField.requestFocusInWindow());
        mainFrame.passwordField.addActionListener(e -> mainFrame.attemptLogin());
        return root;
    }
}
