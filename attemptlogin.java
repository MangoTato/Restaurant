class attemptlogin {
    private final Frame mainFrame;

    attemptlogin(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }

    void attemptLogin() {
        String username = mainFrame.usernameField.getText().trim();
        String password = new String(mainFrame.passwordField.getPassword());
        boolean selectedIsAdmin = mainFrame.adminRole.isSelected();

        if (username.isEmpty() || password.isEmpty()) {
            mainFrame.messageLabel.setText("Please enter both username and password.");
            return;
        }

        User user = mainFrame.userMap.get(username);

        if (user == null || !user.getPassword().equals(password) || user.isAdmin() != selectedIsAdmin) {
            mainFrame.messageLabel.setText("Invalid credentials for the selected role.");
            mainFrame.passwordField.setText("");
            return;
        }

        mainFrame.showDashboard(username, user.isAdmin());
    }
}