import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** Shares table orders across screens and stores their item snapshots in MySQL receipts. */
public class OrderService {
    private final List<KitchenOrder> orders = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final Map<Integer, ReceiptReference> receiptByTable = new HashMap<>();
    private final TableService tableService;
    private boolean loading;
    private boolean loaded;

    public OrderService(TableService tableService) {
        this.tableService = tableService;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isLoading() {
        return loading;
    }
    public void loadActiveOrders(Consumer<Throwable> onError) {
        if (loading || loaded) {
            return;
        }
        loading = true;
        new SwingWorker<List<ReceiptSnapshot>, Void>() {
            @Override
            protected List<ReceiptSnapshot> doInBackground() throws SQLException {
                List<ReceiptSnapshot> receipts = new ArrayList<>();
                String sql = "SELECT r.receipt_id, r.customer_id, r.table_number, c.status "
                        + "FROM receipt r JOIN customer c ON c.customer_id = r.customer_id "
                        + "WHERE r.chef_received_at IS NULL ORDER BY r.created_at, r.receipt_id";
                try (Connection connection = database.getConnection();
                        PreparedStatement statement = connection.prepareStatement(sql);
                        ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        ReceiptSnapshot receipt = new ReceiptSnapshot(result.getLong("receipt_id"),
                                result.getLong("customer_id"), result.getInt("table_number"),
                                result.getString("status"));
                        if (receipt.tableNumber < 1 || receipt.tableNumber > TableService.TABLE_COUNT) {
                            throw new SQLException("An active receipt uses an unsupported table number: "
                                    + receipt.tableNumber);
                        }
                        loadReceiptItems(connection, receipt);
                        receipts.add(receipt);
                    }
                }
                return receipts;
            }

            @Override
            protected void done() {
                loading = false;
                try {
                    List<ReceiptSnapshot> activeReceipts = get();
                    orders.clear();
                    receiptByTable.clear();
                    for (ReceiptSnapshot receipt : activeReceipts) {
                        receiptByTable.put(receipt.tableNumber,
                                new ReceiptReference(receipt.receiptId, receipt.customerId));
                        for (KitchenOrder order : receipt.orders) {
                            orders.add(order);
                        }
                        tableService.occupy(receipt.tableNumber);
                        tableService.setCustomer(receipt.tableNumber, receipt.customerId, receipt.customerStatus);
                    }
                    loaded = true;
                    notifyListeners();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                } catch (ExecutionException e) {
                    onError.accept(e.getCause());
                }
            }
        }.execute();
    }

    private void loadReceiptItems(Connection connection, ReceiptSnapshot receipt) throws SQLException {
        String sql = "SELECT receipt_item_id, item_id, item_name, unit_price, quantity, note "
                + "FROM receipt_item WHERE receipt_id = ? ORDER BY receipt_item_id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, receipt.receiptId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    MenuItem item = new MenuItem(result.getString("item_name"), result.getBigDecimal("unit_price")
                            .doubleValue());
                    item.setDatabaseId((Integer) result.getObject("item_id"));
                    KitchenOrder order = new KitchenOrder(item, result.getInt("quantity"), result.getString("note"),
                            receipt.tableNumber);
                    order.setReceiptItemId(result.getLong("receipt_item_id"));
                    receipt.orders.add(order);
                }
            }
        }
    }

    public List<KitchenOrder> getOrders() {
        return Collections.unmodifiableList(new ArrayList<>(orders));
    }

    public List<KitchenOrder> getOrders(int tableNumber) {
        List<KitchenOrder> tableOrders = new ArrayList<>();
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber) {
                tableOrders.add(order);
            }
        }
        return Collections.unmodifiableList(tableOrders);
    }

    public List<KitchenOrder> getKitchenOrders() {
        List<KitchenOrder> kitchenOrders = new ArrayList<>();
        for (KitchenOrder order : orders) {
            if (!order.isReceived()) {
                kitchenOrders.add(order);
            }
        }
        return Collections.unmodifiableList(kitchenOrders);
    }

    public KitchenOrder add(MenuItem item, String note, int quantity, int tableNumber) {
        if (!loaded) {
            throw new IllegalStateException("Active orders have not finished loading.");
        }
        if (tableService.isCheckoutRequested(tableNumber)) {
            throw new IllegalStateException("This table is waiting for checkout and cannot accept more orders.");
        }
        Long customerId = tableService.getCustomerId(tableNumber);
        if (customerId == null) {
            throw new IllegalStateException("Link this table to a valid customer before placing an order.");
        }
        if (item.getDatabaseId() == null) {
            throw new IllegalStateException("This menu item is not linked to a database record.");
        }

        KitchenOrder existing = null;
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber && order.getItem().getDatabaseId() != null
                    && order.getItem().getDatabaseId().equals(item.getDatabaseId()) && order.isPending()) {
                existing = order;
                break;
            }
        }

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String customerStatusSql = "SELECT status FROM customer WHERE customer_id = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(customerStatusSql)) {
                    statement.setLong(1, customerId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || !"In Progress".equalsIgnoreCase(result.getString("status"))) {
                            throw new SQLException("Only a customer currently in progress at this table can order.");
                        }
                    }
                }
                ReceiptReference receipt = getOrCreateReceipt(connection, tableNumber, customerId);
                if (existing != null) {
                    int newQuantity = existing.getQuantity() + quantity;
                    String sql = "UPDATE receipt_item SET quantity = ?, note = ? WHERE receipt_item_id = ?";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setInt(1, newQuantity);
                        statement.setString(2, note);
                        statement.setLong(3, existing.getReceiptItemId());
                        if (statement.executeUpdate() == 0) {
                            throw new SQLException("The saved order item no longer exists.");
                        }
                    }
                    connection.commit();
                    existing.setQuantity(newQuantity);
                    existing.setNote(note);
                    notifyListeners();
                    return existing;
                }

                KitchenOrder order = new KitchenOrder(item, quantity, note, tableNumber);
                String sql = "INSERT INTO receipt_item "
                        + "(receipt_id, item_id, item_name, unit_price, quantity, note) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, receipt.receiptId);
                    statement.setInt(2, item.getDatabaseId());
                    statement.setString(3, item.getName());
                    statement.setBigDecimal(4, BigDecimal.valueOf(item.getPrice()));
                    statement.setInt(5, quantity);
                    statement.setString(6, note);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return an order item ID.");
                        }
                        order.setReceiptItemId(keys.getLong(1));
                    }
                }
                connection.commit();
                orders.add(order);
                notifyListeners();
                return order;
            } catch (SQLException e) {
                connection.rollback();
                receiptByTable.remove(tableNumber);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save the order to the database.", e);
        }
    }

    private ReceiptReference getOrCreateReceipt(Connection connection, int tableNumber, long customerId)
            throws SQLException {
        ReceiptReference cached = receiptByTable.get(tableNumber);
        if (cached != null && cached.customerId == customerId) {
            return cached;
        }

        String findSql = "SELECT receipt_id FROM receipt WHERE customer_id = ? AND table_number = ? "
                + "AND chef_received_at IS NULL ORDER BY created_at DESC, receipt_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, tableNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    ReceiptReference receipt = new ReceiptReference(result.getLong("receipt_id"), customerId);
                    receiptByTable.put(tableNumber, receipt);
                    return receipt;
                }
            }
        }

        String insertSql = "INSERT INTO receipt (customer_id, table_number) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, customerId);
            statement.setInt(2, tableNumber);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database did not return a receipt ID.");
                }
                ReceiptReference receipt = new ReceiptReference(keys.getLong(1), customerId);
                receiptByTable.put(tableNumber, receipt);
                return receipt;
            }
        }
    }

    public void changeQuantity(KitchenOrder order, int quantity) {
        if (quantity <= 0) {
            persistOrderRemoval(order);
            orders.remove(order);
        } else {
            persistOrderQuantity(order, quantity);
            order.setQuantity(quantity);
        }
        notifyListeners();
    }

    private void persistOrderQuantity(KitchenOrder order, int quantity) {
        updateReceiptItem(order, "UPDATE receipt_item SET quantity = ? WHERE receipt_item_id = ?", quantity, null);
    }

    private void persistOrderRemoval(KitchenOrder order) {
        updateReceiptItem(order, "DELETE FROM receipt_item WHERE receipt_item_id = ?", null, null);
        boolean tableHasItems = orders.stream().anyMatch(candidate -> candidate != order
                && candidate.getTableNumber() == order.getTableNumber());
        if (!tableHasItems) {
            finishReceipt(order.getTableNumber());
        }
    }

    private void updateReceiptItem(KitchenOrder order, String sql, Integer quantity, String note) {
        Long itemId = order.getReceiptItemId();
        if (itemId == null) {
            throw new IllegalStateException("This order item is not linked to a saved receipt.");
        }
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            if (quantity != null) {
                statement.setInt(parameter++, quantity);
            }
            if (note != null) {
                statement.setString(parameter++, note);
            }
            statement.setLong(parameter, itemId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("The saved order item no longer exists.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update the saved order.", e);
        }
    }

    public void updateNote(KitchenOrder order, String note) {
        updateReceiptItem(order, "UPDATE receipt_item SET note = ? WHERE receipt_item_id = ?", null, note);
        order.setNote(note);
        notifyListeners();
    }

    public void startCooking(KitchenOrder order, int minutes) {
        order.addCookingTime(minutes);
        notifyListeners();
    }

    public void startTableCooking(int tableNumber, int minutes) {
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber && !order.isComplete() && !order.isReceived()) {
                order.addCookingTime(minutes);
            }
        }
        notifyListeners();
    }

    public void removeCookingTime(KitchenOrder order, int minutes) {
        order.removeCookingTime(minutes);
        notifyListeners();
    }

    public void removeTableCookingTime(int tableNumber, int minutes) {
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber) {
                order.removeCookingTime(minutes);
            }
        }
        notifyListeners();
    }

    public void completeOrder(KitchenOrder order) {
        order.complete();
        notifyListeners();
    }

    public void completeTableOrder(int tableNumber) {
        for (KitchenOrder order : orders) {
            if (order.getTableNumber() == tableNumber && !order.isReceived()) {
                order.complete();
            }
        }
        notifyListeners();
    }

    public void receiveOrder(KitchenOrder order) {
        order.receive();
        notifyListeners();
    }

    public void clearTableOrders(int tableNumber) {
        finishReceipt(tableNumber);
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
        notifyListeners();
    }

    public void completeTableCheckout(int tableNumber) {
        receiptByTable.remove(tableNumber);
        orders.removeIf(order -> order.getTableNumber() == tableNumber);
        notifyListeners();
    }

    private void finishReceipt(int tableNumber) {
        ReceiptReference receipt = receiptByTable.remove(tableNumber);
        if (receipt == null) {
            return;
        }
        String sql = "UPDATE receipt SET chef_received_at = CURRENT_TIMESTAMP "
                + "WHERE receipt_id = ? AND chef_received_at IS NULL";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, receipt.receiptId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to close the saved table receipt.", e);
        }
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

    public void addListener(Runnable listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    private static final class ReceiptReference {
        private final long receiptId;
        private final long customerId;

        private ReceiptReference(long receiptId, long customerId) {
            this.receiptId = receiptId;
            this.customerId = customerId;
        }
    }

    private static final class ReceiptSnapshot {
        private final long receiptId;
        private final long customerId;
        private final int tableNumber;
        private final String customerStatus;
        private final List<KitchenOrder> orders = new ArrayList<>();

        private ReceiptSnapshot(long receiptId, long customerId, int tableNumber, String customerStatus) {
            this.receiptId = receiptId;
            this.customerId = customerId;
            this.tableNumber = tableNumber;
            this.customerStatus = customerStatus;
        }
    }
}
