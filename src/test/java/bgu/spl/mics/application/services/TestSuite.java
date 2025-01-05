package bgu.spl.mics.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import bgu.spl.mics.Future;
import bgu.spl.mics.MessageBus;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.Pose;
public class TestSuite {

    private MessageBus messageBus;
    private CameraService cameraService;
    private LiDarWorkerTracker lidarWorkerTracker;
    private LiDarDataBase lidarDataBase;

    @Before
    public void setUp() {
        messageBus = MessageBusImpl.getInstance();
        lidarDataBase = new LiDarDataBase();
        cameraService = new CameraService("Camera1", 5);
        lidarWorkerTracker = new LiDarWorkerTracker("LiDarWorker1", null, lidarDataBase);
    }

    /**
     * Test 1: Verify MessageBus registration.
     */
    @Test
    public void testMessageBusRegistration() {
        messageBus.register(cameraService);
        assertTrue(messageBus.isRegistered(cameraService));
    }

    /**
     * Test 2: Verify sending and receiving of an event in MessageBus.
     */
    @Test
    public void testMessageBusSendEvent() {
        messageBus.register(cameraService);
        messageBus.register(lidarWorkerTracker);

        // Simulate sending an event
        DetectObjectsEvent event = new DetectObjectsEvent(null, 1);
        messageBus.sendEvent(event);

        // Check that the event is received by the LiDarWorker
        assertEquals(event, messageBus.awaitEvent(lidarWorkerTracker));
    }

    /**
     * Test 3: Verify completion of an event in MessageBus.
     */
    @Test
    public void testMessageBusCompleteEvent() {
        messageBus.register(cameraService);
        DetectObjectsEvent event = new DetectObjectsEvent(null, 1);
        Future<Boolean> future = messageBus.sendEvent(event);

        // Simulate completing the event
        messageBus.complete(event, true);
        assertTrue(future.get());
    }

    /**
     * Test 4: Verify CameraService sends events at correct intervals.
     */
    @Test
    public void testCameraServiceFrequency() {
        cameraService.onTick(5);
        assertTrue(cameraService.shouldSendDetection(5));
        assertFalse(cameraService.shouldSendDetection(4));
    }

    /**
     * Test 5: Verify LiDarWorkerTracker handles DetectObjectsEvent correctly.
     */
    @Test
    public void testLiDarWorkerTrackerProcessing() {
        CloudPoint point = new CloudPoint(1.0, 2.0);
        lidarDataBase.addCloudPoints("object1", List.of(point));

        DetectObjectsEvent event = new DetectObjectsEvent(null, 1);
        lidarWorkerTracker.executeTask(event);

        // Ensure processed data is added
        assertEquals(List.of(point), lidarDataBase.getCloudPoints("object1"));
    }

    /**
     * Test 6: Verify transformCoordinate produces correct results.
     */
    @Test
    public void testTransformCoordinate() {
        Pose pose = new Pose(1.0, 1.0, 90.0);
        CloudPoint point = new CloudPoint(1.0, 0.0);

        CloudPoint transformed = LiDarDataBase.transformCoordinate(point, pose);

        // Validate transformation result
        assertEquals(1.0, transformed.getX(), 0.01);
        assertEquals(2.0, transformed.getY(), 0.01);
    }

    /**
     * Test 7: Verify transformCoordinate with multiple points.
     */
    @Test
    public void testTransformMultipleCoordinates() {
        Pose pose = new Pose(1.0, 1.0, 45.0);
        CloudPoint point1 = new CloudPoint(1.0, 0.0);
        CloudPoint point2 = new CloudPoint(0.0, 1.0);

        CloudPoint transformed1 = LiDarDataBase.transformCoordinate(point1, pose);
        CloudPoint transformed2 = LiDarDataBase.transformCoordinate(point2, pose);

        // Validate transformation results
        assertNotNull(transformed1);
        assertNotNull(transformed2);
    }
}
