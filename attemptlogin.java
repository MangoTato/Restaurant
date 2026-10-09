class attemptlogin {
    private final Frame mainFrame;

    attemptlogin(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }

    void attemptLogin() {
        String username = mainFrame.usernameField.getText().trim();
        String password = new String(mainFrame.passwordField.getPassword());
        boolean selectedIsAdmin = mainFrame.adminRole.isSelected();
        boolean selectedIsEmployee = mainFrame.employeeRole.isSelected();
        if (username.isEmpty() || password.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(mainFrame, "Fields cannot be empty.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = mainFrame.userMap.get(username);

        if (user == null
            || !user.getPassword().equals(password)
            || user.isAdmin() != selectedIsAdmin
            || user.isEmployee() != selectedIsEmployee) {
             javax.swing.JOptionPane.showMessageDialog(mainFrame, "Invalid username or password.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            mainFrame.passwordField.setText("");
            return;
        }

        mainFrame.showDashboard(username, user.isAdmin(), user.isEmployee());
    }
}
