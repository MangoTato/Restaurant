import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Registration extends JPanel {

    private void showReservationForm() {

        JTextField customerNameField = new JTextField();
        JTextField contactNumberField = new JTextField();
        JTextField timeField = new JTextField();
        JTextField guestsField = new JTextField();
        JTextField tableField = new JTextField();

        JComboBox<String> statusBox = new JComboBox<>(
                new String[] {
                        "Pending",
                        "Confirmed",
                        "Cancelled"
                }
        );

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        form.add(new JLabel("Customer Name:"));
        form.add(customerNameField);

        form.add(new JLabel("Contact Number:"));
        form.add(contactNumberField);

        form.add(new JLabel("Time:"));
        form.add(timeField);

        form.add(new JLabel("Number of Guests:"));
        form.add(guestsField);

        form.add(new JLabel("Table:"));
        form.add(tableField);

        form.add(new JLabel("Reservation Status:"));
        form.add(statusBox);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "Create Reservation",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {

            String customerName = customerNameField.getText().trim();
            String contactNumber = contactNumberField.getText().trim();
            String time = timeField.getText().trim();
            String guestsText = guestsField.getText().trim();
            String tableText = tableField.getText().trim();
            String status = (String) statusBox.getSelectedItem();

            if (customerName.isEmpty()
                    || contactNumber.isEmpty()
                    || time.isEmpty()
                    || guestsText.isEmpty()
                    || tableText.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please fill in all fields.",
                        "Missing Information",
                        JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            try {

                int guests = Integer.parseInt(guestsText);
                int tableNumber = Integer.parseInt(tableText);

                JOptionPane.showMessageDialog(
                        this,
                        "Reservation created!\n\n"
                                + "Customer: " + customerName + "\n"
                                + "Contact: " + contactNumber + "\n"
                                + "Time: " + time + "\n"
                                + "Guests: " + guests + "\n"
                                + "Table: " + tableNumber + "\n"
                                + "Status: " + status,
                        "Reservation",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Guests and Table must be numbers.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private final Frame mainFrame;

    private final JPanel contentPanel;
    private final CardLayout cardLayout;

    public Registration(Frame mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout());
        setBackground(Frame.LIGHT);

        // =========================
        // TOP NAVIGATION
        // =========================

        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        navigationPanel.setBackground(Frame.NAVY);
        navigationPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JButton reservationButton = new JButton("Reservation");
        JButton waitingListButton = new JButton("Waiting List");
        JButton menuButton = new JButton("Menu");

        navigationPanel.add(reservationButton);
        navigationPanel.add(waitingListButton);
        navigationPanel.add(menuButton);

        add(navigationPanel, BorderLayout.NORTH);

        // =========================
        // CONTENT
        // =========================

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Frame.LIGHT);

        contentPanel.add(createReservationPanel(), "reservation");
        contentPanel.add(createWaitingListPanel(), "waiting");
        contentPanel.add(createMenuPanel(), "menu");

        add(contentPanel, BorderLayout.CENTER);

        // =========================
        // BUTTON ACTIONS
        // =========================

        reservationButton.addActionListener(e ->
                cardLayout.show(contentPanel, "reservation")
        );

        waitingListButton.addActionListener(e ->
                cardLayout.show(contentPanel, "waiting")
        );

        menuButton.addActionListener(e ->
                cardLayout.show(contentPanel, "menu")
        );

        // Start on Reservation
        cardLayout.show(contentPanel, "reservation");
    }

    // =========================================================
    // RESERVATION
    // =========================================================

    private JPanel createReservationPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Frame.LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = mainFrame.createLabel(
                "Reservation",
                28,
                Frame.NAVY
        );

        panel.add(title, BorderLayout.NORTH);

        // =========================
        // CALENDAR
        // =========================

        JPanel calendarPanel = new JPanel(new BorderLayout());
        calendarPanel.setBackground(Color.WHITE);
        calendarPanel.setBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY)
        );

        JLabel calendarTitle = new JLabel(
                "Calendar",
                SwingConstants.CENTER
        );

        calendarTitle.setFont(
                new Font("SansSerif", Font.BOLD, 20)
        );

        calendarPanel.add(calendarTitle, BorderLayout.NORTH);

        // Temporary calendar area
        JPanel calendarArea = new JPanel(new GridLayout(6, 7, 5, 5));
        calendarArea.setBackground(Color.WHITE);
        calendarArea.setBorder(
                new EmptyBorder(15, 15, 15, 15)
        );

        String[] days = {
                "Sun", "Mon", "Tue", "Wed",
                "Thu", "Fri", "Sat"
        };

        for (String day : days) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(
                    new Font("SansSerif", Font.BOLD, 13)
            );
            calendarArea.add(label);
        }

        for (int i = 1; i <= 35; i++) {
            JButton dateButton = new JButton(String.valueOf(i));

            final int selectedDate = i;

            dateButton.addActionListener(e ->
                    JOptionPane.showMessageDialog(
                            this,
                            "Selected date: " + selectedDate
                    )
            );

            calendarArea.add(dateButton);
        }

        calendarPanel.add(calendarArea, BorderLayout.CENTER);

        panel.add(calendarPanel, BorderLayout.CENTER);

        // =========================
        // RESERVE SIDE PANEL
        // =========================

        JPanel reservePanel = new JPanel();
        reservePanel.setLayout(
                new BoxLayout(reservePanel, BoxLayout.Y_AXIS)
        );

        reservePanel.setBackground(Color.WHITE);
        reservePanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                        new EmptyBorder(20, 20, 20, 20)
                )
        );

        JLabel reserveTitle = mainFrame.createLabel(
                "Reserve",
                22,
                Frame.NAVY
        );

        reservePanel.add(reserveTitle);
        reservePanel.add(Box.createVerticalStrut(20));

        JLabel instruction = new JLabel(
                "Select a date from the calendar."
        );

        reservePanel.add(instruction);
        reservePanel.add(Box.createVerticalStrut(20));

        JButton reserveButton = new JButton("Create Reservation");

        reserveButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        reservePanel.add(reserveButton);

        reserveButton.addActionListener(e -> showReservationForm());

        panel.add(reservePanel, BorderLayout.EAST);

        return panel;
    }

    // =========================================================
    // WAITING LIST
    // =========================================================

    private JPanel createWaitingListPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Frame.LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = mainFrame.createLabel(
                "Waiting List",
                28,
                Frame.NAVY
        );

        panel.add(title, BorderLayout.NORTH);

        JLabel message = new JLabel(
                "Waiting list will be placed here."
        );

        message.setFont(
                new Font("SansSerif", Font.PLAIN, 16)
        );

        panel.add(message, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================
    // MENU
    // =========================================================

    private JPanel createMenuPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Frame.LIGHT);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = mainFrame.createLabel(
                "Menu",
                28,
                Frame.NAVY
        );

        panel.add(title, BorderLayout.NORTH);

        JLabel message = new JLabel(
                "Menu will be placed here."
        );

        message.setFont(
                new Font("SansSerif", Font.PLAIN, 16)
        );

        panel.add(message, BorderLayout.CENTER);

        return panel;
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Frame frame = new Frame();

            JFrame testWindow = new JFrame("Registration");
            testWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            testWindow.setSize(1100, 700);
            testWindow.setLocationRelativeTo(null);

            testWindow.add(new Registration(frame));
            testWindow.setVisible(true);
        });
    }
}