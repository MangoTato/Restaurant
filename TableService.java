import java.util.ArrayList;
import java.util.List;

/** Tracks the dining-room state independently from the currently displayed screen. */
public class TableService {
    public static final int TABLE_COUNT = 15;
    private final boolean[] occupied = new boolean[TABLE_COUNT + 1];
    private final boolean[] checkoutRequested = new boolean[TABLE_COUNT + 1];
    private final List<Runnable> listeners = new ArrayList<>();

    public boolean isOccupied(int tableNumber) {
        validate(tableNumber);
        return occupied[tableNumber];
    }

    public boolean occupy(int tableNumber) {
        validate(tableNumber);
        if (occupied[tableNumber]) return false;
        occupied[tableNumber] = true;
        notifyListeners();
        return true;
    }

    public void clear(int tableNumber) {
        validate(tableNumber);
        if (!occupied[tableNumber]) return;
        occupied[tableNumber] = false;
        checkoutRequested[tableNumber] = false;
        notifyListeners();
    }

    public boolean isCheckoutRequested(int tableNumber) {
        validate(tableNumber);
        return checkoutRequested[tableNumber];
    }

    public void requestCheckout(int tableNumber) {
        validate(tableNumber);
        if (!occupied[tableNumber] || checkoutRequested[tableNumber]) return;
        checkoutRequested[tableNumber] = true;
        notifyListeners();
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    private void validate(int tableNumber) {
        if (tableNumber < 1 || tableNumber > TABLE_COUNT) {
            throw new IllegalArgumentException("Unknown table: " + tableNumber);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) listener.run();
    }
}
