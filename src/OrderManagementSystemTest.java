package sample;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class OrderManagementSystemTest {
    public static void main(String[] args) throws Exception {
        rejectsDuplicatePayment();
        allowsOnlyOneConcurrentPaidTransitionToWin();
        createsUniqueIdsConcurrently();
        System.out.println("All sample checks passed");
    }

    private static void rejectsDuplicatePayment() {
        OrderManagementSystem system = new OrderManagementSystem();
        long id = system.createOrder("customer-1");
        system.payOrder(id);

        try {
            system.payOrder(id);
            throw new AssertionError("Duplicate payment should fail");
        } catch (IllegalStateException expected) {
            // Expected invalid transition.
        }
    }

    private static void allowsOnlyOneConcurrentPaidTransitionToWin() throws Exception {
        OrderManagementSystem system = new OrderManagementSystem();
        long id = system.createOrder("customer-1");
        system.payOrder(id);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Set<String> outcomes = Collections.synchronizedSet(new HashSet<>());

        executor.submit(() -> attempt(start, () -> system.cancelOrder(id), outcomes, "cancelled"));
        executor.submit(() -> attempt(start, () -> system.shipOrder(id), outcomes, "shipped"));
        start.countDown();
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) throw new AssertionError("Workers did not finish");

        if (outcomes.size() != 1 || !outcomes.contains("cancelled") && !outcomes.contains("shipped")) {
            throw new AssertionError("Exactly one transition should succeed: " + outcomes);
        }
    }

    private static void createsUniqueIdsConcurrently() throws Exception {
        OrderManagementSystem system = new OrderManagementSystem();
        Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int worker = 0; worker < 4; worker++) {
            executor.submit(() -> {
                for (int index = 0; index < 100; index++) ids.add(system.createOrder("customer-" + index));
            });
        }
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) throw new AssertionError("Workers did not finish");
        if (ids.size() != 400) throw new AssertionError("Expected 400 unique IDs but got " + ids.size());
    }

    private static void attempt(CountDownLatch start, Runnable operation, Set<String> outcomes, String success) {
        try {
            start.await();
            operation.run();
            outcomes.add(success);
        } catch (IllegalStateException expected) {
            // The competing transition is expected to lose.
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(error);
        }
    }
}
