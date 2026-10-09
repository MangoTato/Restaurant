/** A customer order that is shared by the customer and kitchen screens. */
public class KitchenOrder {
    public enum Status { PENDING, COOKING, FOR_SERVING, RECEIVED }
    private final MenuItem item;
    private final int tableNumber;
    private int quantity;
    private String note;
    private long readyAtMillis;
    private Status status = Status.PENDING;
    private boolean sentToKitchen;

    public KitchenOrder(MenuItem item, int quantity, String note, int tableNumber) {
        this.item = item;
        this.quantity = quantity;
        this.note = note;
        this.tableNumber = tableNumber;
    }

    public MenuItem getItem() { return item; }
    public int getTableNumber() { return tableNumber; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }
    public boolean isPending() { return status == Status.PENDING; }
    public boolean isCooking() { return status == Status.COOKING; }
    public boolean isComplete() { return status == Status.FOR_SERVING; }
    public boolean isReceived() { return status == Status.RECEIVED; }
    public boolean isSentToKitchen() { return sentToKitchen; }

    public long getSecondsRemaining() {
        return !isCooking() ? 0 : Math.max(0, (readyAtMillis - System.currentTimeMillis() + 999) / 1000);
    }

    void setQuantity(int quantity) { this.quantity = quantity; }
    void setNote(String note) { this.note = note; }
    void sendToKitchen() { sentToKitchen = true; }
    void addCookingTime(int minutes) {
        long base = Math.max(System.currentTimeMillis(), readyAtMillis);
        readyAtMillis = base + minutes * 60_000L;
        status = Status.COOKING;
    }

    void removeCookingTime(int minutes) {
        if (!isCooking()) return;
        readyAtMillis = Math.max(System.currentTimeMillis(), readyAtMillis - minutes * 60_000L);
    }

    void complete() { status = Status.FOR_SERVING; }
    void receive() { status = Status.RECEIVED; }
}
