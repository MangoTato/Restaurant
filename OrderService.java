import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Keeps the active orders in one place so both dashboards show the same data. */
public class OrderService {
    private final List<KitchenOrder> orders = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public List<KitchenOrder> getOrders() {
        return Collections.unmodifiableList(new ArrayList<>(orders));
    }

    public List<KitchenOrder> getOrders(int tableNumber) {
        List<KitchenOrder> tableOrders = new ArrayList<>();
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber) tableOrders.add(order);
        }
        return Collections.unmodifiableList(tableOrders);
    }

    public List<KitchenOrder> getKitchenOrders() {
        List<KitchenOrder> kitchenOrders = new ArrayList<>();
        for (KitchenOrder order : orders) {
            if (order.isSentToKitchen() && !order.isReceived()) kitchenOrders.add(order);
        }
        return Collections.unmodifiableList(kitchenOrders);
    }

    public KitchenOrder add(MenuItem item, String note, int quantity, int tableNumber) {
        for (KitchenOrder order : orders) {
            if (order.getItem() == item && order.getTableNumber() == tableNumber
                    && order.isPending() && !order.isSentToKitchen()) {
                order.setQuantity(order.getQuantity() + quantity);
                order.setNote(note);
                notifyListeners();
                return order;
            }
        }
        KitchenOrder order = new KitchenOrder(item, quantity, note, tableNumber);
        orders.add(order);
        notifyListeners();
        return order;
    }

    public void changeQuantity(KitchenOrder order, int quantity) {
        if (quantity <= 0) orders.remove(order); else order.setQuantity(quantity);
        notifyListeners();
    }

    public void updateNote(KitchenOrder order, String note) {
        order.setNote(note);
        notifyListeners();
    }

    /** Makes all draft items for a table visible to the kitchen at once. */
    public void sendTableOrdersToKitchen(int tableNumber) {
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber && !order.isSentToKitchen()) {
                order.sendToKitchen();
            }
        }
        notifyListeners();
    }

    public void startCooking(KitchenOrder order, int minutes) {
        order.addCookingTime(minutes);
        notifyListeners();
    }

    public void removeCookingTime(KitchenOrder order, int minutes) {
        order.removeCookingTime(minutes);
        notifyListeners();
    }

    public void completeOrder(KitchenOrder order) {
        order.complete();
        notifyListeners();
    }

    public void receiveOrder(KitchenOrder order) {
        order.receive();
        notifyListeners();
    }

    public void clearTableOrders(int tableNumber) {
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
        notifyListeners();
    }

    public double getTableTotal(int tableNumber) {
        double total = 0;
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber) {
                total += order.getItem().getPrice() * order.getQuantity();
            }
        }
        return total;
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) listener.run();
    }
}
