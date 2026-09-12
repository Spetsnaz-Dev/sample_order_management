package sample;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderManagementSystem {
    public enum Status {
        CREATED,
        PAID,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    public static final class Order {
        private final long id;
        private final String customerId;
        private Status status;

        private Order(long id, String customerId) {
            this.id = id;
            this.customerId = customerId;
            this.status = Status.CREATED;
        }

        public long getId() {
            return id;
        }

        public String getCustomerId() {
            return customerId;
        }

        public Status getStatus() {
            return status;
        }
    }

    private final AtomicLong nextId = new AtomicLong(100);
    private final Map<Long, Order> orders = new HashMap<>();

    public synchronized long createOrder(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId is required");
        }

        long id = nextId.incrementAndGet();
        orders.put(id, new Order(id, customerId));
        return id;
    }

    public synchronized Order getOrder(long orderId) {
        Order order = requireOrder(orderId);
        return copyOf(order);
    }

    public synchronized Status getStatus(long orderId) {
        return requireOrder(orderId).status;
    }

    public synchronized void payOrder(long orderId) {
        transition(orderId, Status.CREATED, Status.PAID);
    }

    public synchronized void shipOrder(long orderId) {
        transition(orderId, Status.PAID, Status.SHIPPED);
    }

    public synchronized void deliverOrder(long orderId) {
        transition(orderId, Status.SHIPPED, Status.DELIVERED);
    }

    public synchronized void cancelOrder(long orderId) {
        Order order = requireOrder(orderId);
        if (order.status != Status.CREATED && order.status != Status.PAID) {
            throw new IllegalStateException("Only CREATED or PAID orders can be cancelled");
        }
        order.status = Status.CANCELLED;
    }

    private void transition(long orderId, Status expected, Status next) {
        Order order = requireOrder(orderId);
        if (order.status != expected) {
            throw new IllegalStateException("Expected " + expected + " but was " + order.status);
        }
        order.status = next;
    }

    private Order requireOrder(long orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Unknown order: " + orderId);
        }
        return order;
    }

    private Order copyOf(Order order) {
        Order copy = new Order(order.id, order.customerId);
        copy.status = order.status;
        return copy;
    }
}
