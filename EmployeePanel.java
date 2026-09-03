import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class EmployeePanel extends JPanel {
    private final Frame mainFrame;

    public EmployeePanel(Frame mainFrame, String username) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(0, 20));
        setBackground(Frame.LIGHT);
        setBorder(new EmptyBorder(28, 38, 28, 38));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(mainFrame.createLabel("Good day, " + username, 25, Frame.NAVY), BorderLayout.WEST);

        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> mainFrame.signOut());
        header.add(logout, BorderLayout.EAST);

        // Actions Section
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        actions.setOpaque(false);

        List<String> actionNames = List.of("Open orders", "My schedule", "Shift notes");
        for (String action : actionNames) {
            JButton button = new JButton(action);
            button.setPreferredSize(new Dimension(150, 62));
            button.addActionListener(e -> JOptionPane.showMessageDialog(this, action + " Soon.", "Employee", JOptionPane.INFORMATION_MESSAGE));
            actions.add(button);
        }

        // Status Text Area
        JTextArea status = new JTextArea("You are signed in as an employee.\nChoose an option to begin managing today's restaurant operations.");
        status.setEditable(false);
        status.setLineWrap(true);
        status.setWrapStyleWord(true);
        status.setFont(new Font("SansSerif", Font.PLAIN, 15));
        status.setBackground(Color.WHITE);
        status.setBorder(new EmptyBorder(20, 20, 20, 20));

        add(header, BorderLayout.NORTH);
        add(actions, BorderLayout.CENTER);
        add(new JScrollPane(status), BorderLayout.SOUTH);
    }
}