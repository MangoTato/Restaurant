import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Login {
    private final Frame mainFrame;

    Login(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }

    JPanel LoginPanel() {
        BufferedImage background = loadBackgroundImage();
        JPanel root = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (background != null) {
                    graphics.drawImage(background, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        root.setPreferredSize(new Dimension(850, 520));
        root.setBackground(Frame.LIGHT);

        JButton customerButton = new JButton("Customer");
        customerButton.setBounds(48, 440, 76, 24);
        customerButton.setBackground(new Color(190, 45, 45));
        customerButton.setForeground(Color.WHITE);
        customerButton.setFocusPainted(false);
        customerButton.setFont(new Font("SansSerif", Font.BOLD, 10));
        customerButton.setToolTipText("Continue as a customer");
        customerButton.addActionListener(
            e -> mainFrame.showDashboard("Customer", false, false)
        );
        root.add(customerButton);

        mainFrame.usernameField.setBounds(460, 184, 309, 31);
        mainFrame.passwordField.setBounds(460, 247, 309, 31);
        root.add(mainFrame.usernameField);
        root.add(mainFrame.passwordField);

        ButtonGroup roles = new ButtonGroup();
        roles.add(mainFrame.adminRole);
        roles.add(mainFrame.employeeRole);
        mainFrame.adminRole.setOpaque(false);
        mainFrame.employeeRole.setOpaque(false);
        mainFrame.adminRole.setBounds(460, 294, 130, 25);
        mainFrame.employeeRole.setBounds(597, 294, 110, 25);
        root.add(mainFrame.adminRole);
        root.add(mainFrame.employeeRole);

        JButton loginButton = new JButton("Sign in");
        loginButton.setBounds(459, 345, 309, 32);
        loginButton.setBackground(Color.BLACK);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.addActionListener(e -> mainFrame.attemptLogin());
        root.add(loginButton);

        mainFrame.passwordField.addActionListener(e -> mainFrame.attemptLogin());
        return root;
    }

    private BufferedImage loadBackgroundImage() {
        try {
            return ImageIO.read(new File("Images", "Customer UI (850 x 520 px).png"));
        } catch (IOException e) {
            return null;
        }
    }
}
