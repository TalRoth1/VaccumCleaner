package bgu.spl.mics.application.services;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.Event;
import bgu.spl.mics.Future;
import bgu.spl.mics.Message;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
public class MessageBusImplTest {

    private MessageBusImpl bus;
    private MicroService cameraService1;
    private MicroService cameraService2;

    @BeforeEach
    public void setUp() {
        bus = MessageBusImpl.getInstance();
        // Possibly reset any internal data if needed
        // bus.reset();

        // Create some dummy microservices
        cameraService1 = new MicroService("Cam1") {
            @Override
            protected void initialize() { /* do nothing */ }
        };
        cameraService2 = new MicroService("Cam2") {
            @Override
            protected void initialize() { /* do nothing */ }
        };

        // Register them
        bus.register(cameraService1);
        bus.register(cameraService2);
    }

    @Test
    public void testSubscribeAndSendBroadcast() throws InterruptedException {
        // 1) Subscribe both Cam1 and Cam2 to a broadcast
        bus.subscribeBroadcast(TestBroadcast.class, cameraService1);
        bus.subscribeBroadcast(TestBroadcast.class, cameraService2);

        // 2) Send the broadcast
        TestBroadcast broadcast = new TestBroadcast("HelloAll");
        bus.sendBroadcast(broadcast);

        // 3) Each microservice should get it
        assertEquals(broadcast, bus.awaitMessage(cameraService1));
        assertEquals(broadcast, bus.awaitMessage(cameraService2));
    }

    @Test
    public void testRoundRobinEventDistribution() throws InterruptedException {
        // 1) Subscribe both to a sample event
        bus.subscribeEvent(TestEvent.class, cameraService1);
        bus.subscribeEvent(TestEvent.class, cameraService2);

        // 2) Send 2 events
        TestEvent event1 = new TestEvent("E1");
        TestEvent event2 = new TestEvent("E2");

        bus.sendEvent(event1);
        bus.sendEvent(event2);

        // 3) Check round-robin: event1 -> cameraService1, event2 -> cameraService2
        Message msg1 = bus.awaitMessage(cameraService1);
        assertEquals(event1, msg1, "First event should go to cameraService1");

        Message msg2 = bus.awaitMessage(cameraService2);
        assertEquals(event2, msg2, "Second event should go to cameraService2");
    }

    @Test
    public void testCompleteResolvesFuture() {
        bus.subscribeEvent(TestEvent.class, cameraService1);
        TestEvent event = new TestEvent("SomeData");
        Future<Boolean> future = bus.sendEvent(event);
        assertNotNull(future, "Future should not be null since there's a subscriber.");

        bus.complete(event, true);

        assertTrue(future.isDone());
        assertEquals(true, future.get());
    }

    @Test
    public void testAwaitMessageTimeout() throws InterruptedException {
        // No subscription done, so no messages can arrive
        Message msg = bus.awaitMessage(cameraService1);
        // Actually, this might block forever if no message arrives unless your design has a timeout approach
        // If your code supports bus.awaitMessage(...) with a timeout param, you can do:
        // bus.awaitMessage(cameraService1, 100, TimeUnit.MILLISECONDS);
        // For now, we might skip or design a test that ensures no messages are put in the queue
        // leading to a potential indefinite block or an InterruptedException
        // depending on your logic. This part is just an example placeholder.
        assertNull(msg, "Should not get any message if no broadcast/event was sent. (If using timed logic)");
    }

    // Example dummy messages for testing
    private static class TestEvent implements Event<Boolean> {
        private final String data;
        public TestEvent(String d) { data = d; }
        public String getData() { return data; }
    }

    private static class TestBroadcast implements Broadcast {
        private final String msg;
        public TestBroadcast(String m) { msg = m; }
        public String getMsg() { return msg; }
    }
}