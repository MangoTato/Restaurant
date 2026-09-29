import javax.swing.*;

class AddUser{
    private final Frame mainFrame;

    AddUser(Frame mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    void ShowAddUserDialog() {
        
        JTextField newUsername = new JTextField();
        JPasswordField newPassword = new JPasswordField();
        JCheckBox makeAdmin = new JCheckBox("Administrator privileges");
        JCheckBox makeEmployee = new JCheckBox("Employee");

        Object[] message = {
            "Username:", newUsername,
            "Password:", newPassword,
            "", makeAdmin,
            "", makeEmployee
        };

        int option = JOptionPane.showConfirmDialog(mainFrame, message, "Add New Account", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String user = newUsername.getText().trim();
            String pass = new String(newPassword.getPassword());
            boolean isAdmin = makeAdmin.isSelected();
            boolean isEmployee = makeEmployee.isSelected();

            if (!user.isEmpty() && !pass.isEmpty()) {
                if (mainFrame.addUser(user, pass, isAdmin, isEmployee)) {
                    JOptionPane.showMessageDialog(mainFrame, "Account created for " + user + "!");
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Fields cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}