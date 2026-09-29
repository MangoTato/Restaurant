import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

public class Staff extends JPanel {

    private final Frame mainFrame;

    private JPanel contentPanel;

    private JTable staffTable;

    private DefaultTableModel tableModel;

    private JTable archiveTable;

    private DefaultTableModel archiveTableModel;

    private JTextField searchField;

    private JComboBox<String> typeFilter;

    private JComboBox<String> rowsCombo;

    private int nextStaffId = 1;

    public Staff(Frame mainFrame) {

        this.mainFrame = mainFrame;

    }

    public JPanel showStaff() {

        JPanel card = new JPanel(
            new BorderLayout()
        );

        card.setBackground(
            Color.WHITE
        );

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    Frame.NAVY,
                    2
                ),
                new EmptyBorder(
                    20,
                    25,
                    20,
                    25
                )
            )
        );

        card.setPreferredSize(
            new Dimension(
                1100,
                680
            )
        );

        JLabel title = mainFrame.createLabel(
            "Staff",
            25,
            Frame.NAVY
        );

        JPanel titlePanel = new JPanel(
            new FlowLayout(
                FlowLayout.LEFT,
                0,
                0
            )
        );

        titlePanel.setOpaque(false);

        titlePanel.add(title);

        card.add(
            titlePanel,
            BorderLayout.NORTH
        );

        contentPanel = new JPanel(
            new BorderLayout(
                0,
                10
            )
        );

        contentPanel.setBackground(
            Color.WHITE
        );

        card.add(
            contentPanel,
            BorderLayout.CENTER
        );

        showStaffList();

        return card;

    }

    private void showStaffList() {

        contentPanel.removeAll();

        JPanel topArea = new JPanel(
            new FlowLayout(
                FlowLayout.LEFT,
                8,
                0
            )
        );

        topArea.setOpaque(false);

        JButton staffListButton =
            createTopButton(
                "Staff List"
            );

        JButton archiveButton =
            createTopButton(
                "Archive"
            );

        staffListButton.setBackground(
            Frame.TEAL
        );

        topArea.add(
            staffListButton
        );

        topArea.add(
            archiveButton
        );

        contentPanel.add(
            topArea,
            BorderLayout.NORTH
        );

        JPanel tableArea = new JPanel(
            new BorderLayout(
                0,
                10
            )
        );

        tableArea.setBackground(
            Color.WHITE
        );

        JPanel toolbar = createToolbar();

        tableArea.add(
            toolbar,
            BorderLayout.NORTH
        );

        createStaffTable();

        JScrollPane scrollPane =
            new JScrollPane(
                staffTable
            );

        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                new Color(
                    220,
                    220,
                    220
                )
            )
        );

        tableArea.add(
            scrollPane,
            BorderLayout.CENTER
        );

        contentPanel.add(
            tableArea,
            BorderLayout.CENTER
        );

        staffListButton.addActionListener(
            e -> showStaffList()
        );

        archiveButton.addActionListener(
            e -> showArchive()
        );

        contentPanel.revalidate();

        contentPanel.repaint();

    }

    private JPanel createToolbar() {

        JPanel toolbar = new JPanel(
            new BorderLayout()
        );

        toolbar.setOpaque(false);

        JButton addButton =
            new JButton("+");

        addButton.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                15
            )
        );

        addButton.setForeground(
            Color.WHITE
        );

        addButton.setBackground(
            Frame.TEAL
        );

        addButton.setFocusPainted(false);

        addButton.setBorderPainted(false);

        addButton.setPreferredSize(
            new Dimension(
                42,
                42
            )
        );

        addButton.addActionListener(
            e -> addStaff()
        );

        JPanel addPanel = new JPanel(
            new FlowLayout(
                FlowLayout.LEFT,
                5,
                0
            )
        );

        addPanel.setOpaque(false);

        addPanel.add(
            addButton
        );

        toolbar.add(
            addPanel,
            BorderLayout.WEST
        );

        JPanel searchPanel = new JPanel(
            new FlowLayout(
                FlowLayout.RIGHT,
                5,
                0
            )
        );

        searchPanel.setOpaque(false);

        rowsCombo =
            new JComboBox<>(
                new String[] {
                    "10",
                    "25",
                    "50",
                    "100"
                }
            );

        rowsCombo.setPreferredSize(
            new Dimension(
                55,
                30
            )
        );

        typeFilter =
            new JComboBox<>(
                new String[] {
                    "All",
                    "Cashier",
                    "Waiter",
                    "Manager",
                    "Chef"
                }
            );

        typeFilter.setPreferredSize(
            new Dimension(
                100,
                30
            )
        );

        searchField =
            new JTextField();

        searchField.setPreferredSize(
            new Dimension(
                160,
                30
            )
        );

        searchField.setToolTipText(
            "Search staff"
        );

        JButton searchButton =
            new JButton(
                "Search"
            );

        searchButton.setPreferredSize(
            new Dimension(
                75,
                30
            )
        );

        searchButton.setFocusPainted(false);

        searchPanel.add(
            rowsCombo
        );

        searchPanel.add(
            typeFilter
        );

        searchPanel.add(
            searchField
        );

        searchPanel.add(
            searchButton
        );

        toolbar.add(
            searchPanel,
            BorderLayout.EAST
        );

        searchButton.addActionListener(
            e -> searchStaff()
        );

        searchField.addActionListener(
            e -> searchStaff()
        );

        typeFilter.addActionListener(
            e -> filterStaff()
        );

        rowsCombo.addActionListener(
            e -> updateRowLimit()
        );

        return toolbar;

    }

    private void createStaffTable() {

        String[] columns = {
            "ID#",
            "Staff Name",
            "Type",
            "Phone",
            "Status",
            "Actions"
        };

        tableModel =
            new DefaultTableModel(
                columns,
                0
            ) {

                @Override
                public boolean isCellEditable(
                    int row,
                    int column
                ) {

                    return column == 5;

                }

            };

        /*
         * Temporary in-memory staff data.
         * These records exist only while the
         * application is running.
         */

        addMemoryStaff(
            "ABC",
            "Waiter",
            "",
            "Active"
        );

        addMemoryStaff(
            "Raza",
            "Waiter",
            "",
            "Active"
        );

        addMemoryStaff(
            "Bilal",
            "Waiter",
            "",
            "Active"
        );

        addMemoryStaff(
            "Waqar",
            "Cashier",
            "0345-4180138",
            "Active"
        );

        staffTable =
            new JTable(
                tableModel
            );

        staffTable.setRowHeight(
            38
        );

        staffTable.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                12
            )
        );

        staffTable.getTableHeader().setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                12
            )
        );

        staffTable.getTableHeader().setBackground(
            new Color(
                235,
                237,
                242
            )
        );

        staffTable.getTableHeader().setForeground(
            Color.DARK_GRAY
        );

        staffTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        staffTable.setGridColor(
            new Color(
                225,
                225,
                225
            )
        );

        staffTable.setShowVerticalLines(
            false
        );

        staffTable.setBackground(
            Color.WHITE
        );

        TableColumnModel columnModel =
            staffTable.getColumnModel();

        columnModel.getColumn(0).setPreferredWidth(
            50
        );

        columnModel.getColumn(1).setPreferredWidth(
            250
        );

        columnModel.getColumn(2).setPreferredWidth(
            150
        );

        columnModel.getColumn(3).setPreferredWidth(
            180
        );

        columnModel.getColumn(4).setPreferredWidth(
            150
        );

        columnModel.getColumn(5).setPreferredWidth(
            160
        );

        DefaultTableCellRenderer centerRenderer =
            new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(
            SwingConstants.CENTER
        );

        columnModel.getColumn(0).setCellRenderer(
            centerRenderer
        );

        columnModel.getColumn(2).setCellRenderer(
            centerRenderer
        );

        columnModel.getColumn(3).setCellRenderer(
            centerRenderer
        );

        columnModel.getColumn(4).setCellRenderer(
            new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(

                    JTable table,

                    Object value,

                    boolean isSelected,

                    boolean hasFocus,

                    int row,

                    int column

                ) {

                    JLabel label =
                        (JLabel)
                        super.getTableCellRendererComponent(

                            table,

                            value,

                            isSelected,

                            hasFocus,

                            row,

                            column

                        );

                    label.setHorizontalAlignment(
                        SwingConstants.CENTER
                    );

                    if (
                        "Active".equals(
                            value
                        )
                    ) {

                        label.setForeground(
                            new Color(
                                30,
                                150,
                                80
                            )
                        );

                    } else {

                        label.setForeground(
                            Color.RED
                        );

                    }

                    return label;

                }

            }
        );

        columnModel.getColumn(5).setCellRenderer(
            new ActionRenderer()
        );

        columnModel.getColumn(5).setCellEditor(
            new ActionEditor()
        );

    }

    private void addMemoryStaff(

        String name,

        String type,

        String phone,

        String status

    ) {

        tableModel.addRow(
            new Object[] {

                nextStaffId++,

                name,

                type,

                phone,

                status,

                ""

            }
        );

    }

    private class ActionRenderer

        extends JPanel

        implements TableCellRenderer {

        private final JButton editButton;

        private final JButton archiveButton;

        public ActionRenderer() {

            setLayout(
                new FlowLayout(
                    FlowLayout.CENTER,
                    5,
                    4
                )
            );

            setBackground(
                Color.WHITE
            );

            editButton =
                new JButton(
                    "Edit"
                );

            archiveButton =
                new JButton(
                    "Archive"
                );

            styleActionButton(
                editButton,
                new Color(
                    255,
                    193,
                    7
                )
            );

            styleActionButton(
                archiveButton,
                new Color(
                    70,
                    145,
                    210
                )
            );

            add(
                editButton
            );

            add(
                archiveButton
            );

        }

        @Override
        public Component getTableCellRendererComponent(

            JTable table,

            Object value,

            boolean isSelected,

            boolean hasFocus,

            int row,

            int column

        ) {

            setBackground(

                isSelected

                    ? table.getSelectionBackground()

                    : Color.WHITE

            );

            return this;

        }

    }

    private class ActionEditor

        extends DefaultCellEditor

        implements TableCellEditor {

        private final JPanel panel;

        private final JButton editButton;

        private final JButton archiveButton;

        private int currentRow;

        public ActionEditor() {

            super(
                new JTextField()
            );

            setClickCountToStart(
                1
            );

            panel = new JPanel(
                new FlowLayout(
                    FlowLayout.CENTER,
                    5,
                    4
                )
            );

            panel.setBackground(
                Color.WHITE
            );

            editButton =
                new JButton(
                    "Edit"
                );

            archiveButton =
                new JButton(
                    "Archive"
                );

            styleActionButton(
                editButton,
                new Color(
                    255,
                    193,
                    7
                )
            );

            styleActionButton(
                archiveButton,
                new Color(
                    70,
                    145,
                    210
                )
            );

            panel.add(
                editButton
            );

            panel.add(
                archiveButton
            );

            editButton.addActionListener(
                e -> {

                    fireEditingStopped();

                    editStaff(
                        currentRow
                    );

                }
            );

            archiveButton.addActionListener(
                e -> {

                    fireEditingStopped();

                    archiveStaff(
                        currentRow
                    );

                }
            );

        }

        @Override
        public Component getTableCellEditorComponent(

            JTable table,

            Object value,

            boolean isSelected,

            int row,

            int column

        ) {

            currentRow = row;

            return panel;

        }

        @Override
        public Object getCellEditorValue() {

            return "";

        }

    }

    private void styleActionButton(

        JButton button,

        Color color

    ) {

        button.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                11
            )
        );

        button.setForeground(
            Color.WHITE
        );

        button.setBackground(
            color
        );

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setMargin(
            new Insets(
                2,
                7,
                2,
                7
            )
        );

    }

    private JButton createTopButton(

        String text

    ) {

        JButton button =
            new JButton(
                text
            );

        button.setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                13
            )
        );

        button.setForeground(
            Color.WHITE
        );

        button.setBackground(
            Frame.NAVY
        );

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setPreferredSize(
            new Dimension(
                125,
                38
            )
        );

        return button;

    }

    private void addStaff() {

        JTextField nameField =
            new JTextField();

        JComboBox<String> typeBox =
            new JComboBox<>(
                new String[] {
                    "Waiter",
                    "Cashier",
                    "Manager",
                    "Chef"
                }
            );

        JTextField phoneField =
            new JTextField();

        JComboBox<String> statusBox =
            new JComboBox<>(
                new String[] {
                    "Active",
                    "Inactive"
                }
            );

        JPanel form = new JPanel(
            new GridLayout(
                4,
                2,
                8,
                8
            )
        );

        form.add(
            new JLabel(
                "Staff Name:"
            )
        );

        form.add(
            nameField
        );

        form.add(
            new JLabel(
                "Type:"
            )
        );

        form.add(
            typeBox
        );

        form.add(
            new JLabel(
                "Phone:"
            )
        );

        form.add(
            phoneField
        );

        form.add(
            new JLabel(
                "Status:"
            )
        );

        form.add(
            statusBox
        );

        int result =
            JOptionPane.showConfirmDialog(

                mainFrame,

                form,

                "Add Staff",

                JOptionPane.OK_CANCEL_OPTION,

                JOptionPane.PLAIN_MESSAGE

            );

        if (
            result !=
            JOptionPane.OK_OPTION
        ) {

            return;

        }

        String name =
            nameField
                .getText()
                .trim();

        String phone =
            phoneField
                .getText()
                .trim();

        if (name.isEmpty()) {

            JOptionPane.showMessageDialog(

                mainFrame,

                "Please enter the staff name.",

                "Invalid Input",

                JOptionPane.WARNING_MESSAGE

            );

            return;

        }

        tableModel.addRow(
            new Object[] {

                nextStaffId++,

                name,

                typeBox.getSelectedItem(),

                phone,

                statusBox.getSelectedItem(),

                ""

            }
        );

    }

    private void editStaff(

        int row

    ) {

        if (

            row < 0 ||

            row >= tableModel.getRowCount()

        ) {

            return;

        }

        JTextField nameField =
            new JTextField(

                String.valueOf(

                    tableModel.getValueAt(
                        row,
                        1
                    )

                )

            );

        JComboBox<String> typeBox =
            new JComboBox<>(
                new String[] {
                    "Waiter",
                    "Cashier",
                    "Manager",
                    "Chef"
                }
            );

        typeBox.setSelectedItem(
            tableModel.getValueAt(
                row,
                2
            )
        );

        JTextField phoneField =
            new JTextField(

                String.valueOf(

                    tableModel.getValueAt(
                        row,
                        3
                    )

                )

            );

        JComboBox<String> statusBox =
            new JComboBox<>(
                new String[] {
                    "Active",
                    "Inactive"
                }
            );

        statusBox.setSelectedItem(
            tableModel.getValueAt(
                row,
                4
            )
        );

        JPanel form = new JPanel(
            new GridLayout(
                4,
                2,
                8,
                8
            )
        );

        form.add(
            new JLabel(
                "Staff Name:"
            )
        );

        form.add(
            nameField
        );

        form.add(
            new JLabel(
                "Type:"
            )
        );

        form.add(
            typeBox
        );

        form.add(
            new JLabel(
                "Phone:"
            )
        );

        form.add(
            phoneField
        );

        form.add(
            new JLabel(
                "Status:"
            )
        );

        form.add(
            statusBox
        );

        int result =
            JOptionPane.showConfirmDialog(

                mainFrame,

                form,

                "Modify Staff",

                JOptionPane.OK_CANCEL_OPTION,

                JOptionPane.PLAIN_MESSAGE

            );

        if (
            result !=
            JOptionPane.OK_OPTION
        ) {

            return;

        }

        String name =
            nameField
                .getText()
                .trim();

        if (name.isEmpty()) {

            JOptionPane.showMessageDialog(

                mainFrame,

                "Staff name cannot be empty.",

                "Invalid Input",

                JOptionPane.WARNING_MESSAGE

            );

            return;

        }

        tableModel.setValueAt(
            name,
            row,
            1
        );

        tableModel.setValueAt(
            typeBox.getSelectedItem(),
            row,
            2
        );

        tableModel.setValueAt(
            phoneField.getText().trim(),
            row,
            3
        );

        tableModel.setValueAt(
            statusBox.getSelectedItem(),
            row,
            4
        );

    }

    private void archiveStaff(

        int row

    ) {

        if (

            row < 0 ||

            row >= tableModel.getRowCount()

        ) {

            return;

        }

        String name =
            String.valueOf(

                tableModel.getValueAt(
                    row,
                    1
                )

            );

        int result =
            JOptionPane.showConfirmDialog(

                mainFrame,

                "Are you sure you want to Archive "
                    + name
                    + "?",

                "Archive Staff",

                JOptionPane.YES_NO_OPTION,

                JOptionPane.WARNING_MESSAGE

            );

        if (
            result !=
            JOptionPane.YES_OPTION
        ) {

            return;

        }

        Object id =
            tableModel.getValueAt(
                row,
                0
            );

        Object staffName =
            tableModel.getValueAt(
                row,
                1
            );

        Object type =
            tableModel.getValueAt(
                row,
                2
            );

        Object phone =
            tableModel.getValueAt(
                row,
                3
            );

        /*
         * The important part:
         * the archived staff gets stored
         * in a separate memory table.
         */

        if (
            archiveTableModel == null
        ) {

            createArchiveModel();

        }

        archiveTableModel.addRow(
            new Object[] {

                id,

                staffName,

                type,

                phone,

                "Archived"

            }
        );

        /*
         * Remove the staff immediately
         * from the active staff table.
         */

        tableModel.removeRow(
            row
        );

        renumberStaff();

        JOptionPane.showMessageDialog(

            mainFrame,

            name
                + " has been archived.",

            "Archive Staff",

            JOptionPane.INFORMATION_MESSAGE

        );

    }

    private void renumberStaff() {

        /*
         * This only changes the displayed
         * table numbering. The actual ID
         * remains unchanged.
         */

        for (

            int i = 0;

            i < tableModel.getRowCount();

            i++

        ) {

            /*
             * Do not change the real ID.
             * The first column is the real ID.
             */

        }

    }

    private void showArchive() {

        contentPanel.removeAll();

        JPanel panel = new JPanel(
            new BorderLayout(
                0,
                10
            )
        );

        panel.setBackground(
            Color.WHITE
        );

        JPanel top = new JPanel(
            new FlowLayout(
                FlowLayout.LEFT,
                8,
                0
            )
        );

        top.setOpaque(false);

        JButton staffListButton =
            createTopButton(
                "Staff List"
            );

        JButton archiveButton =
            createTopButton(
                "Archive"
            );

        archiveButton.setBackground(
            Frame.TEAL
        );

        top.add(
            staffListButton
        );

        top.add(
            archiveButton
        );

        panel.add(
            top,
            BorderLayout.NORTH
        );

        createArchiveTable();

        JScrollPane scrollPane =
            new JScrollPane(
                archiveTable
            );

        scrollPane.setBorder(
            BorderFactory.createLineBorder(
                new Color(
                    220,
                    220,
                    220
                )
            )
        );

        panel.add(
            scrollPane,
            BorderLayout.CENTER
        );

        staffListButton.addActionListener(
            e -> showStaffList()
        );

        contentPanel.add(
            panel,
            BorderLayout.CENTER
        );

        contentPanel.revalidate();

        contentPanel.repaint();

    }

    private void createArchiveModel() {

        String[] columns = {

            "ID#",

            "Staff Name",

            "Type",

            "Phone",

            "Status"

        };

        archiveTableModel =
            new DefaultTableModel(
                columns,
                0
            );

    }

    private void createArchiveTable() {

        if (
            archiveTableModel == null
        ) {

            createArchiveModel();

        }

        archiveTable =
            new JTable(
                archiveTableModel
            );

        archiveTable.setRowHeight(
            38
        );

        archiveTable.setFont(
            new Font(
                "SansSerif",
                Font.PLAIN,
                12
            )
        );

        archiveTable.getTableHeader().setFont(
            new Font(
                "SansSerif",
                Font.BOLD,
                12
            )
        );

        archiveTable.getTableHeader().setBackground(
            new Color(
                235,
                237,
                242
            )
        );

        archiveTable.getTableHeader().setForeground(
            Color.DARK_GRAY
        );

        archiveTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        archiveTable.setGridColor(
            new Color(
                225,
                225,
                225
            )
        );

        archiveTable.setShowVerticalLines(
            false
        );

        archiveTable.setBackground(
            Color.WHITE
        );

        TableColumnModel columnModel =
            archiveTable.getColumnModel();

        columnModel.getColumn(0).setPreferredWidth(
            50
        );

        columnModel.getColumn(1).setPreferredWidth(
            250
        );

        columnModel.getColumn(2).setPreferredWidth(
            150
        );

        columnModel.getColumn(3).setPreferredWidth(
            180
        );

        columnModel.getColumn(4).setPreferredWidth(
            150
        );

        DefaultTableCellRenderer centerRenderer =
            new DefaultTableCellRenderer();

        centerRenderer.setHorizontalAlignment(
            SwingConstants.CENTER
        );

        columnModel.getColumn(0).setCellRenderer(
            centerRenderer
        );

        columnModel.getColumn(2).setCellRenderer(
            centerRenderer
        );

        columnModel.getColumn(3).setCellRenderer(
            centerRenderer
        );

        columnModel.getColumn(4).setCellRenderer(
            new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(

                    JTable table,

                    Object value,

                    boolean isSelected,

                    boolean hasFocus,

                    int row,

                    int column

                ) {

                    JLabel label =
                        (JLabel)
                        super.getTableCellRendererComponent(

                            table,

                            value,

                            isSelected,

                            hasFocus,

                            row,

                            column

                        );

                    label.setHorizontalAlignment(
                        SwingConstants.CENTER
                    );

                    label.setForeground(
                        Color.RED
                    );

                    return label;

                }

            }
        );

    }

    private void searchStaff() {

        String search =
            searchField
                .getText()
                .trim()
                .toLowerCase();

        String selectedType =
            String.valueOf(
                typeFilter.getSelectedItem()
            );

        if (
            search.isEmpty() &&
            "All".equals(selectedType)
        ) {

            staffTable.clearSelection();

            return;

        }

        for (

            int i = 0;

            i < tableModel.getRowCount();

            i++

        ) {

            String name =
                String.valueOf(
                    tableModel.getValueAt(
                        i,
                        1
                    )
                ).toLowerCase();

            String type =
                String.valueOf(
                    tableModel.getValueAt(
                        i,
                        2
                    )
                );

            String phone =
                String.valueOf(
                    tableModel.getValueAt(
                        i,
                        3
                    )
                ).toLowerCase();

            boolean typeMatch =
                "All".equals(
                    selectedType
                )
                ||
                type.equals(
                    selectedType
                );

            boolean searchMatch =
                search.isEmpty()
                ||
                name.contains(search)
                ||
                type.toLowerCase().contains(search)
                ||
                phone.contains(search);

            if (
                typeMatch &&
                searchMatch
            ) {

                staffTable.setRowSelectionInterval(
                    i,
                    i
                );

                staffTable.scrollRectToVisible(

                    staffTable.getCellRect(
                        i,
                        0,
                        true
                    )

                );

                return;

            }

        }

        staffTable.clearSelection();

        JOptionPane.showMessageDialog(

            mainFrame,

            "No staff member found.",

            "Search",

            JOptionPane.INFORMATION_MESSAGE

        );

    }

    private void filterStaff() {

        String selectedType =
            String.valueOf(
                typeFilter.getSelectedItem()
            );

        if (
            "All".equals(
                selectedType
            )
        ) {

            staffTable.clearSelection();

            return;

        }

        for (

            int i = 0;

            i < tableModel.getRowCount();

            i++

        ) {

            String type =
                String.valueOf(
                    tableModel.getValueAt(
                        i,
                        2
                    )
                );

            if (
                type.equals(
                    selectedType
                )
            ) {

                staffTable.setRowSelectionInterval(
                    i,
                    i
                );

                staffTable.scrollRectToVisible(

                    staffTable.getCellRect(
                        i,
                        0,
                        true
                    )

                );

                return;

            }

        }

        staffTable.clearSelection();

        JOptionPane.showMessageDialog(

            mainFrame,

            "No "
                + selectedType
                + " staff found.",

            "Staff Type",

            JOptionPane.INFORMATION_MESSAGE

        );

    }

    private void updateRowLimit() {

        /*
         * This is prepared for pagination.
         * The selected value represents how
         * many rows should eventually be shown.
         *
         * Since the current data is stored
         * directly in DefaultTableModel,
         * we don't remove data here.
         */

        if (
            rowsCombo == null
        ) {

            return;

        }

        String selectedRows =
            String.valueOf(
                rowsCombo.getSelectedItem()
            );

        System.out.println(
            "Rows per page: "
                + selectedRows
        );

    }

}