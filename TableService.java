import java.util.ArrayList;
import java.util.List;

/** Tracks the dining-room state independently from the currently displayed screen. */
public class TableService {
    public static final int TABLE_COUNT = 10;

    public enum Status {
        OCCUPIED, RESERVED, AVAILABLE, CLEANING
    }

    private final boolean[] occupied = new boolean[TABLE_COUNT + 1];
    private final boolean[] cleaning = new boolean[TABLE_COUNT + 1];
    private final boolean[] checkoutRequested = new boolean[TABLE_COUNT + 1];
    private final Long[] customerIds = new Long[TABLE_COUNT + 1];
    private final String[] customerStatuses = new String[TABLE_COUNT + 1];
    private final List<Runnable> listeners = new ArrayList<>();

    public static int capacityFor(int tableNumber) {
        if (tableNumber < 1 || tableNumber > TABLE_COUNT) {
            throw new IllegalArgumentException("Unknown table: " + tableNumber);
        }
        if (tableNumber <= 4) {
            return 8;
        }
        return tableNumber <= 7 ? 6 : 5;
    }

    public boolean isOccupied(int tableNumber) {
        validate(tableNumber);
        return occupied[tableNumber];
    }

    public boolean occupy(int tableNumber) {
        return occupy(tableNumber, null);
    }

    public boolean occupy(int tableNumber, Long customerId) {
        validate(tableNumber);
        if (occupied[tableNumber] || cleaning[tableNumber])
            return false;
        if ("Reserved".equalsIgnoreCase(customerStatuses[tableNumber])
                && (customerIds[tableNumber] == null || !customerIds[tableNumber].equals(customerId))) {
            return false;
        }
        occupied[tableNumber] = true;
        notifyListeners();
        return true;
    }

    public Status getStatus(int tableNumber) {
        validate(tableNumber);
        if (occupied[tableNumber]) {
            return Status.OCCUPIED;
        }
        if (cleaning[tableNumber]) {
            return Status.CLEANING;
        }
        if ("Reserved".equalsIgnoreCase(customerStatuses[tableNumber])) {
            return Status.RESERVED;
        }
        return Status.AVAILABLE;
    }

    public boolean isAvailableTo(int tableNumber, long customerId) {
        validate(tableNumber);
        if (occupied[tableNumber]) {
            return customerIds[tableNumber] != null && customerIds[tableNumber] == customerId;
        }
        if (cleaning[tableNumber]) {
            return false;
        }
        if ("Reserved".equalsIgnoreCase(customerStatuses[tableNumber])) {
            return customerIds[tableNumber] != null && customerIds[tableNumber] == customerId;
        }
        return true;
    }

    public void markCleaning(int tableNumber) {
        validate(tableNumber);
        if (occupied[tableNumber] || cleaning[tableNumber]) {
            return;
        }
        customerIds[tableNumber] = null;
        customerStatuses[tableNumber] = null;
        checkoutRequested[tableNumber] = false;
        cleaning[tableNumber] = true;
        notifyListeners();
    }

    public void markAvailable(int tableNumber) {
        validate(tableNumber);
        if (!cleaning[tableNumber]) {
            return;
        }
        cleaning[tableNumber] = false;
        notifyListeners();
    }

    public void clear(int tableNumber) {
        validate(tableNumber);
        if (!occupied[tableNumber] && !checkoutRequested[tableNumber] && customerIds[tableNumber] == null)
            return;
        occupied[tableNumber] = false;
        cleaning[tableNumber] = false;
        checkoutRequested[tableNumber] = false;
        customerIds[tableNumber] = null;
        customerStatuses[tableNumber] = null;
        notifyListeners();
    }

    public void resetForNewAdminSession() {
        for (int tableNumber = 1; tableNumber <= TABLE_COUNT; tableNumber++) {
            occupied[tableNumber] = false;
            cleaning[tableNumber] = false;
            checkoutRequested[tableNumber] = false;
            customerIds[tableNumber] = null;
            customerStatuses[tableNumber] = null;
        }
        notifyListeners();
    }

    public Long getCustomerId(int tableNumber) {
        validate(tableNumber);
        return customerIds[tableNumber];
    }

    public String getCustomerStatus(int tableNumber) {
        validate(tableNumber);
        return customerStatuses[tableNumber];
    }

    public void setCustomer(int tableNumber, Long customerId, String customerStatus) {
        validate(tableNumber);
        customerIds[tableNumber] = customerId;
        customerStatuses[tableNumber] = customerStatus;
        if (customerId != null && "Reserved".equalsIgnoreCase(customerStatus)) {
            cleaning[tableNumber] = false;
        }
        notifyListeners();
    }

    public boolean isCheckoutRequested(int tableNumber) {
        validate(tableNumber);
        return checkoutRequested[tableNumber];
    }

    public void requestCheckout(int tableNumber) {
        validate(tableNumber);
        if (!occupied[tableNumber] || checkoutRequested[tableNumber])
            return;
        checkoutRequested[tableNumber] = true;
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void validate(int tableNumber) {
        if (tableNumber < 1 || tableNumber > TABLE_COUNT) {
            throw new IllegalArgumentException("Unknown table: " + tableNumber);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners))
            listener.run();
    }
}
