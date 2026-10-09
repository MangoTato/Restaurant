import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class Frame extends JFrame {

    public static final Color NAVY = new Color(24, 42, 62);
    public static final Color TEAL = new Color(0, 145, 150);
    public static final Color LIGHT = new Color(245, 248, 250);

    final Map<String, User> userMap = new HashMap<>();
    final CardLayout cardLayout = new CardLayout();
    final JPanel content = new JPanel(cardLayout);
    final JTextField usernameField = new JTextField(18);
    final JPasswordField passwordField = new JPasswordField(18);

    final JRadioButton adminRole = new JRadioButton( "Administrator",true);
    final JRadioButton employeeRole = new JRadioButton("Employee");
    final JRadioButton customer = new JRadioButton( "Customer");
    final JLabel messageLabel = new JLabel(" ");
    final OrderService orderService = new OrderService();
    final TableService tableService = new TableService();

    private String currentuser;

    public Frame() {

        userMap.put( "admin", new User("admin","admin123", true, false));
        userMap.put( "employee", new User("employee","employee123", false, true));
        userMap.put( "customer", new User("customer", "customer123", false, false));

        setTitle("Restaurant Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(850, 520));
        setSize(850, 520);
        setLocationRelativeTo(null);

        content.add(new Login(this).LoginPanel(),"login");
        setContentPane(content);
    }

    public boolean addUser(String username,String password,boolean isAdmin, boolean IsEmployee) {

        if (userMap.containsKey(username)) {
            return false;
        }
        userMap.put(username,new User(username,password,isAdmin,IsEmployee));
        return true;
    }

    public JLabel createLabel(String text,int size,Color color) {

        JLabel label = new JLabel( "<html>" +text.replace("\n", "<br>") +"</html>");
            label.setFont(new Font("SansSerif", Font.PLAIN,size));
            label.setForeground(color);

        return label;
    }

    public void addField(JPanel form,GridBagConstraints gbc,int row, String labelText, Component comp) {
        gbc.gridy = row;
        if (labelText != null) {

            JLabel lbl = new JLabel(labelText);
            lbl.setFont(new Font("SansSerif",Font.PLAIN,12));

            form.add(lbl, gbc);
            gbc.gridy = row + 1;
        }

        if (comp != null) {
            form.add(comp, gbc);
        }
    }

    public void attemptLogin() { new attemptlogin(this).attemptLogin();}

    public void showDashboard(String username, boolean isAdmin, boolean isEmployee) {
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        if (isAdmin) {

            Admin adminPanel = new Admin(this,username);
            content.add(adminPanel,"dashboard");

        } else if (isEmployee){ 

            EmployeePanel employeePanel = new EmployeePanel( this, username );
            content.add(employeePanel,"dashboard");
        } else {
            Customer customerpanel = new Customer(this, username);
            content.add(customerpanel,"dashboard");
            
        }

        cardLayout.show( content, "dashboard");

        content.revalidate();
        content.repaint();
    }

    public void showManageMenu() {

        ManageMenu menu = new ManageMenu(this);
        menu.ShowMenuPanel();
    }

    public void showViewMenu() {

        ViewMenu menu = new ViewMenu(this, true);
        menu.ShowViewMenuPanel();
    }

    public void showAdmin(String username) {

        Admin adminPanel = new Admin(this,username);
        content.add(adminPanel,"dashboard");
        cardLayout.show( content, "dashboard");

        content.revalidate();
        content.repaint();
    }
    
    public void ShowStaff(String username){
        Staff staffpanel = new Staff(this);
        staffpanel.showStaff();
    }

    public void signOut() {

        usernameField.setText("");
        passwordField.setText("");
        messageLabel.setText(" ");

        setExtendedState(JFrame.NORMAL);
        setResizable(false);
        setSize(850, 520);
        setLocationRelativeTo(null);
        cardLayout.show( content,"login");

        content.revalidate();
        content.repaint();
    }
}
