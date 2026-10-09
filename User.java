public class User {
    private final String username;
    private final String password;
    private final boolean isAdmin;
    private final boolean isEmployee;

    public User(String username, String password, boolean isAdmin, boolean isEmployee) {
        this.username = username;
        this.password = password;
        this.isAdmin = isAdmin;
        this.isEmployee = isEmployee;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
    public boolean isEmployee(){
        return isEmployee;
    }
    
}