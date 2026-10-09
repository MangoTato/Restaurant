import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/** Front-desk dashboard for reservations, tables, and customer records. */
public class Registration extends JPanel {
    private final Frame mainFrame;
    private final CustomerLog customerLog;
    private final CustomerLog customerDirectory;
    private final JLabel[] tableCounts = new JLabel[4];
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pagePanel = new JPanel(pageLayout);
    private JScrollPane reservationsScroll;
    private JButton tablesNavigationButton;
    private JButton reservationsNavigationButton;
    private JButton customerNavigationButton;
    private JLabel pageTitleLabel;
    private JLabel pageSubtitleLabel;
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel agendaDateLabel = new JLabel();
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 3, 3));
    private final JPanel appointmentList = new JPanel();
    private final Map<LocalDate, List<Reservation>> reservations = new HashMap<>();
    private LocalDate selectedDate = LocalDate.now();
    private YearMonth displayedMonth = YearMonth.now();
    private int loadGeneration;
    private boolean reservationsLoading;
    private boolean reservationsLoadFailed;
    private final Runnable tableListener = this::refreshTableCounts;
    private boolean listening;

    public Registration(Frame mainFrame) {
        this.mainFrame = mainFrame;
        customerLog = new CustomerLog(mainFrame, false);
        customerLog.setSelectedReservationDate(selectedDate);
        customerLog.setDialogParent(this);
        customerLog.setReservationChangedListener(this::reservationSaved);
        customerDirectory = new CustomerLog(mainFrame, false, true);
        customerDirectory.setBackNavigationListener(this::workspaceScrollToReservations);

        setLayout(new BorderLayout());
        setBackground(Frame.BACKGROUND);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel sidebar = createSidebar();
        JPanel workspace = new JPanel(new BorderLayout(0, 12));
        workspace.setBackground(Frame.BACKGROUND);
        workspace.setBorder(new EmptyBorder(8, 18, 8, 8));
        JPanel mainContent = new JPanel(new BorderLayout(0, 10));
        JPanel verticalContent = new JPanel();
        verticalContent.setLayout(new BorderLayout(0, 10));
        verticalContent.setBackground(Frame.BACKGROUND);
        JPanel summaryCards = createSummaryCards();
        verticalContent.add(summaryCards, BorderLayout.NORTH);
        JPanel agenda = createAgendaPanel();
        agenda.setPreferredSize(new Dimension(700, 290));
        agenda.setMinimumSize(new Dimension(0, 230));
        verticalContent.add(agenda, BorderLayout.CENTER);
        mainContent.add(verticalContent, BorderLayout.CENTER);
        reservationsScroll = new JScrollPane(mainContent);
        reservationsScroll.setBorder(BorderFactory.createEmptyBorder());
        reservationsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        reservationsScroll.getViewport().setBackground(Frame.BACKGROUND);
        reservationsScroll.getVerticalScrollBar().setUnitIncrement(18);
        workspace.add(reservationsScroll, BorderLayout.CENTER);

        JPanel reservationsPage = new JPanel(new BorderLayout());
        reservationsPage.setBackground(Frame.BACKGROUND);
        reservationsPage.add(sidebar, BorderLayout.WEST);
        reservationsPage.add(workspace, BorderLayout.CENTER);
        pagePanel.setOpaque(false);
        pagePanel.add(reservationsPage, "Reservations");
        pagePanel.add(new TableServicePanel(mainFrame, () -> showPage("Reservations")), "Tables");
        pagePanel.add(customerDirectory, "Customer");
        add(createHeader(), BorderLayout.NORTH);
        add(pagePanel, BorderLayout.CENTER);
        showPage("Reservations");
        refreshTableCounts();
        refreshCalendar();
        refreshAgenda();
        loadReservations();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 18));
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(16, 12, 16, 12)));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new javax.swing.BoxLayout(brand, javax.swing.BoxLayout.Y_AXIS));
        JLabel brandName = new JLabel("Pâques");
        brandName.setFont(new Font("SansSerif", Font.BOLD, 17));
        brandName.setForeground(Frame.ACCENT_DARK);
        JLabel role = new JLabel("FRONT DESK");
        role.setFont(new Font("SansSerif", Font.BOLD, 10));
        role.setForeground(Frame.MUTED);
        brand.add(brandName);
        brand.add(javax.swing.Box.createVerticalStrut(5));
        brand.add(role);
        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(createCalendarPanel(), BorderLayout.CENTER);

        JLabel footer = new JLabel("<html><b>Reservations</b><br><br>Select a date to review<br>scheduled guests and times.</html>");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setForeground(Frame.MUTED);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createCalendarPanel() {
        JPanel calendar = new JPanel(new BorderLayout(0, 10));
        calendar.setBackground(Color.WHITE);
        calendar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(10, 7, 10, 7)));

        JPanel monthControls = new JPanel(new BorderLayout(2, 0));
        monthControls.setOpaque(false);
        JButton previous = monthButton("<");
        previous.addActionListener(event -> changeMonth(-1));
        JButton next = monthButton(">");
        next.addActionListener(event -> changeMonth(1));
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        monthLabel.setForeground(Frame.NAVY);
        monthControls.add(previous, BorderLayout.WEST);
        monthControls.add(monthLabel, BorderLayout.CENTER);
        monthControls.add(next, BorderLayout.EAST);
        calendar.add(monthControls, BorderLayout.NORTH);

        calendarGrid.setOpaque(false);
        calendar.add(calendarGrid, BorderLayout.CENTER);
        return calendar;
    }

    private JButton monthButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
        Frame.styleButtonState(button, false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel createAgendaPanel() {
        JPanel agenda = new JPanel(new BorderLayout(0, 8));
        agenda.setBackground(Color.WHITE);
        agenda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(12, 14, 12, 14)));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Reservations for");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(Frame.NAVY);
        agendaDateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        agendaDateLabel.setForeground(Frame.MUTED);
        titleBlock.add(title);
        titleBlock.add(javax.swing.Box.createVerticalStrut(3));
        titleBlock.add(agendaDateLabel);
        header.add(titleBlock, BorderLayout.WEST);

        JButton newReservation = new JButton("+ Add reservation");
        newReservation.setFocusPainted(false);
        newReservation.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(newReservation, false);
        newReservation.setFont(new Font("SansSerif", Font.BOLD, 11));
        newReservation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.ACCENT), new EmptyBorder(7, 10, 7, 10)));
        newReservation.addActionListener(event -> {
            customerLog.setSelectedReservationDate(selectedDate);
            customerLog.showAddCustomerDialog();
        });
        header.add(newReservation, BorderLayout.EAST);
        agenda.add(header, BorderLayout.NORTH);

        appointmentList.setLayout(new javax.swing.BoxLayout(appointmentList, javax.swing.BoxLayout.Y_AXIS));
        appointmentList.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(appointmentList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        agenda.add(scroll, BorderLayout.CENTER);
        return agenda;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS));
        pageTitleLabel = new JLabel("Reservations");
        pageTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 25));
        pageTitleLabel.setForeground(Frame.NAVY);
        pageSubtitleLabel = new JLabel("Review availability and manage customer bookings.");
        pageSubtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pageSubtitleLabel.setForeground(Frame.MUTED);
        titleBlock.add(pageTitleLabel);
        titleBlock.add(javax.swing.Box.createVerticalStrut(4));
        titleBlock.add(pageSubtitleLabel);
        header.add(titleBlock, BorderLayout.WEST);
        JPanel navigation = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        navigation.setOpaque(false);
        tablesNavigationButton = new JButton("Tables");
        reservationsNavigationButton = new JButton("Reservations");
        customerNavigationButton = new JButton("Customer");
        styleNavigationButton(tablesNavigationButton, false);
        styleNavigationButton(reservationsNavigationButton, true);
        styleNavigationButton(customerNavigationButton, false);
        tablesNavigationButton.addActionListener(event -> showPage("Tables"));
        reservationsNavigationButton.addActionListener(event -> showPage("Reservations"));
        customerNavigationButton.addActionListener(event -> showPage("Customer"));
        navigation.add(tablesNavigationButton);
        navigation.add(reservationsNavigationButton);
        navigation.add(customerNavigationButton);
        navigation.setBackground(Frame.CARD);
        navigation.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(4, 4, 4, 4)));
        header.add(navigation, BorderLayout.EAST);
        return header;
    }

    private void showPage(String page) {
        pageLayout.show(pagePanel, page);
        pageTitleLabel.setText("Tables".equals(page) ? "Table service"
                : "Customer".equals(page) ? "Customer" : "Reservations");
        pageSubtitleLabel.setText("Tables".equals(page) ? "Live availability and table reservations"
                : "Customer".equals(page) ? "Confirmed, reserved, in-progress, and cancelled customers"
                        : "Review availability and manage customer bookings.");
        styleNavigationButton(tablesNavigationButton, "Tables".equals(page));
        styleNavigationButton(reservationsNavigationButton, "Reservations".equals(page));
        styleNavigationButton(customerNavigationButton, "Customer".equals(page));
        pagePanel.revalidate();
        pagePanel.repaint();
    }

    private void workspaceScrollToReservations() {
        showPage("Reservations");
        reservationsScroll.getVerticalScrollBar().setValue(0);
    }

    private void styleNavigationButton(JButton button, boolean selected) {
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Frame.styleButtonState(button, selected);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? Frame.ACCENT : Frame.BORDER),
                new EmptyBorder(8, 12, 8, 12)));
    }

    private JPanel createSummaryCards() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 0));
        cards.setOpaque(false);
        String[] labels = { "Available tables", "Reserved tables", "In progress", "Cleaning" };
        Color[] accents = { Frame.SUCCESS, Frame.ACCENT, Frame.ACCENT_DARK, Frame.MUTED };
        for (int index = 0; index < labels.length; index++) {
            JPanel card = new JPanel(new BorderLayout(0, 7));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(10, 12, 10, 12)));
            JLabel label = new JLabel(labels[index]);
            label.setFont(new Font("SansSerif", Font.PLAIN, 11));
            label.setForeground(Frame.MUTED);
            tableCounts[index] = new JLabel("0");
            tableCounts[index].setFont(new Font("SansSerif", Font.BOLD, 22));
            tableCounts[index].setForeground(accents[index]);
            card.add(label, BorderLayout.NORTH);
            card.add(tableCounts[index], BorderLayout.CENTER);
            cards.add(card);
        }
        return cards;
    }

    private void changeMonth(int amount) {
        displayedMonth = displayedMonth.plusMonths(amount);
        selectedDate = displayedMonth.atDay(1);
        customerLog.setSelectedReservationDate(selectedDate);
        refreshCalendar();
        refreshAgenda();
        loadReservations();
    }

    private void reservationSaved(LocalDate date) {
        selectedDate = date;
        displayedMonth = YearMonth.from(date);
        refreshCalendar();
        loadReservations();
    }

    private void refreshCalendar() {
        if (calendarGrid == null) {
            return;
        }
        monthLabel.setText(displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calendarGrid.removeAll();
        String[] weekdays = { "M", "T", "W", "T", "F", "S", "S" };
        for (String weekday : weekdays) {
            JLabel label = new JLabel(weekday, SwingConstants.CENTER);
            label.setFont(new Font("SansSerif", Font.BOLD, 10));
            label.setForeground(Frame.MUTED);
            calendarGrid.add(label);
        }
        LocalDate first = displayedMonth.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        for (int blank = 0; blank < offset; blank++) {
            calendarGrid.add(new JLabel(""));
        }
        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            LocalDate date = displayedMonth.atDay(day);
            int count = reservations.getOrDefault(date, java.util.Collections.emptyList()).size();
            JButton dateButton = new JButton("<html><center>" + day
                    + (count > 0 ? "<br><span style='color:#" + toHex(Frame.ACCENT_DARK) + "'>• " + count
                            + "</span>" : "<br>&nbsp;")
                    + "</center></html>");
            dateButton.setMargin(new java.awt.Insets(1, 1, 1, 1));
            dateButton.setFocusPainted(false);
            dateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            boolean selected = date.equals(selectedDate);
            boolean today = date.equals(LocalDate.now());
            Frame.styleButtonState(dateButton, selected);
            dateButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? Frame.ACCENT
                            : today ? Frame.ACCENT_DARK : Frame.BORDER),
                    new EmptyBorder(3, 1, 3, 1)));
            dateButton.setFont(new Font("SansSerif", selected || today ? Font.BOLD : Font.PLAIN, 10));
            dateButton.setToolTipText(count == 0 ? date.toString()
                    : date + " · " + count + (count == 1 ? " reservation" : " reservations"));
            dateButton.addActionListener(event -> {
                selectedDate = date;
                customerLog.setSelectedReservationDate(date);
                refreshCalendar();
                refreshAgenda();
            });
            calendarGrid.add(dateButton);
        }
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private void loadReservations() {
        int generation = ++loadGeneration;
        reservationsLoading = true;
        reservationsLoadFailed = false;
        refreshAgenda();
        LocalDate monthStart = displayedMonth.atDay(1);
        LocalDate nextMonth = displayedMonth.plusMonths(1).atDay(1);
        new SwingWorker<Map<LocalDate, List<Reservation>>, Void>() {
            @Override
            protected Map<LocalDate, List<Reservation>> doInBackground() throws SQLException {
                Map<LocalDate, List<Reservation>> rows = new HashMap<>();
                String sql = "SELECT first_name, last_name, email, phone_number, scheduled, status FROM customer "
                        + "WHERE scheduled >= ? AND scheduled < ? "
                        + "AND (status IS NULL OR status NOT IN ('Cancelled', 'Completed')) "
                        + "ORDER BY scheduled, customer_id";
                try (Connection connection = database.getConnection();
                        PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setTimestamp(1, Timestamp.valueOf(monthStart.atStartOfDay()));
                    statement.setTimestamp(2, Timestamp.valueOf(nextMonth.atStartOfDay()));
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            Timestamp scheduled = result.getTimestamp("scheduled");
                            if (scheduled == null) {
                                continue;
                            }
                            LocalDateTime dateTime = scheduled.toLocalDateTime();
                            Reservation reservation = new Reservation(dateTime.toLocalTime(),
                                    result.getString("first_name") + " " + result.getString("last_name"),
                                    result.getString("email"), result.getString("phone_number"),
                                    result.getString("status"));
                            rows.computeIfAbsent(dateTime.toLocalDate(), ignored -> new ArrayList<>()).add(reservation);
                        }
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                if (generation != loadGeneration) {
                    return;
                }
                reservationsLoading = false;
                try {
                    reservations.clear();
                    reservations.putAll(get());
                    reservationsLoadFailed = false;
                    refreshCalendar();
                    refreshAgenda();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    reservationsLoadFailed = true;
                    refreshAgenda();
                    showReservationError("Loading reservations was interrupted.", error);
                } catch (ExecutionException error) {
                    reservationsLoadFailed = true;
                    refreshAgenda();
                    showReservationError("Unable to load reservations.", error.getCause());
                }
            }
        }.execute();
    }

    private void refreshAgenda() {
        if (appointmentList == null) {
            return;
        }
        agendaDateLabel.setText(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        appointmentList.removeAll();
        if (reservationsLoading) {
            addAgendaMessage("Loading reservation schedule...");
            appointmentList.revalidate();
            appointmentList.repaint();
            return;
        }
        if (reservationsLoadFailed) {
            addAgendaMessage("Reservations could not be loaded. Check the database connection.");
            appointmentList.revalidate();
            appointmentList.repaint();
            return;
        }
        List<Reservation> dayReservations = reservations.getOrDefault(selectedDate, java.util.Collections.emptyList());
        if (dayReservations.isEmpty()) {
            addAgendaMessage("No reservations scheduled. This date is open for bookings.");
        } else {
            for (Reservation reservation : dayReservations) {
                JPanel card = new JPanel(new BorderLayout(12, 0));
                card.setBackground(Color.WHITE);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(10, 12, 10, 12)));
                JPanel timeBlock = new JPanel();
                timeBlock.setLayout(new javax.swing.BoxLayout(timeBlock, javax.swing.BoxLayout.Y_AXIS));
                timeBlock.setOpaque(false);
                JLabel dayCategory = new JLabel(selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                        .toUpperCase());
                dayCategory.setFont(new Font("SansSerif", Font.BOLD, 9));
                dayCategory.setForeground(Frame.ACCENT_DARK);
                JLabel time = new JLabel(reservation.time.format(DateTimeFormatter.ofPattern("h:mm a")));
                time.setFont(new Font("SansSerif", Font.BOLD, 17));
                time.setForeground(Frame.NAVY);
                timeBlock.add(dayCategory);
                timeBlock.add(javax.swing.Box.createVerticalStrut(4));
                timeBlock.add(time);
                JPanel details = new JPanel(new BorderLayout(6, 5));
                details.setOpaque(false);
                JPanel guestDetails = new JPanel();
                guestDetails.setOpaque(false);
                guestDetails.setLayout(new javax.swing.BoxLayout(guestDetails, javax.swing.BoxLayout.Y_AXIS));
                JLabel name = new JLabel(reservation.name);
                name.setFont(new Font("SansSerif", Font.BOLD, 13));
                name.setForeground(Frame.NAVY);
                JLabel contact = new JLabel(reservation.email + "  ·  " + reservation.phone);
                contact.setFont(new Font("SansSerif", Font.PLAIN, 11));
                contact.setForeground(Frame.MUTED);
                guestDetails.add(name);
                guestDetails.add(javax.swing.Box.createVerticalStrut(3));
                guestDetails.add(contact);
                JLabel status = new JLabel(reservation.status == null || reservation.status.isBlank()
                        ? "Reservation" : reservation.status.toUpperCase());
                status.setFont(new Font("SansSerif", Font.PLAIN, 10));
                status.setForeground("Reserved".equalsIgnoreCase(reservation.status)
                        ? Frame.ACCENT_DARK : Frame.SUCCESS);
                status.setHorizontalAlignment(SwingConstants.CENTER);
                status.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Frame.BORDER), new EmptyBorder(5, 8, 5, 8)));
                details.add(guestDetails, BorderLayout.CENTER);
                details.add(status, BorderLayout.EAST);
                card.add(timeBlock, BorderLayout.WEST);
                card.add(details, BorderLayout.CENTER);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
                appointmentList.add(card);
                appointmentList.add(javax.swing.Box.createVerticalStrut(7));
            }
        }
        appointmentList.revalidate();
        appointmentList.repaint();
    }

    private void addAgendaMessage(String message) {
        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(Frame.MUTED);
        label.setBorder(new EmptyBorder(14, 4, 14, 4));
        appointmentList.add(label);
    }

    private void refreshTableCounts() {
        if (tableCounts[0] == null) {
            return;
        }
        int[] counts = new int[4];
        for (int table = 1; table <= TableService.TABLE_COUNT; table++) {
            switch (mainFrame.tableService.getStatus(table)) {
                case AVAILABLE:
                    counts[0]++;
                    break;
                case RESERVED:
                    counts[1]++;
                    break;
                case OCCUPIED:
                    counts[2]++;
                    break;
                case CLEANING:
                    counts[3]++;
                    break;
                default:
                    break;
            }
        }
        for (int index = 0; index < counts.length; index++) {
            tableCounts[index].setText(String.valueOf(counts[index]));
        }
    }

    private void showReservationError(String message, Throwable error) {
        javax.swing.JOptionPane.showMessageDialog(mainFrame, message + "\n" + error.getMessage(),
                "Reservation Calendar Error", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    private String toHex(Color color) {
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!listening) {
            mainFrame.tableService.addListener(tableListener);
            listening = true;
        }
    }

    @Override
    public void removeNotify() {
        if (listening) {
            mainFrame.tableService.removeListener(tableListener);
            listening = false;
        }
        super.removeNotify();
    }

    private static final class Reservation {
        private final LocalTime time;
        private final String name;
        private final String email;
        private final String phone;
        private final String status;

        private Reservation(LocalTime time, String name, String email, String phone, String status) {
            this.time = time;
            this.name = name;
            this.email = email == null ? "" : email;
            this.phone = phone == null ? "" : phone;
            this.status = status;
        }
    }
}
