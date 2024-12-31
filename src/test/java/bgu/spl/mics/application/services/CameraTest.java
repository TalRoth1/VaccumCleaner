package bgu.spl.mics.application.services;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StatisticalFolder;

import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests for Camera – specifically the method(s)
 * that prepare data before sending (e.g., addObject, addObjects, checking "ERROR").
 */
public class CameraTest {

    private Camera camera;
    private StatisticalFolder stats;

    @BeforeEach
    public void setUp() {
        stats = new StatisticalFolder();
        camera = new Camera(1, 2, stats); // id=1, freq=2, some stats folder
    }

    @Test
    public void testAddObject_NoError() {
        DetectedObject obj = new DetectedObject("Chair_1", "A chair");
        camera.addObject(obj, 5);  // time=5

        // The camera should store it in a new StampedDetectedObjects
        List<DetectedObject> lastFrame = camera.getLastDetectedFrame();
        assertFalse(lastFrame.isEmpty(), "Last frame should contain the newly added object");
        assertEquals("Chair_1", lastFrame.get(0).getId());

        assertEquals(1, stats.getNumDetectedObjects(), "Stats should have 1 detected object so far.");
    }

    @Test
    public void testAddObjects_ErrorInList() {
        List<DetectedObject> objs = new ArrayList<>();
        objs.add(new DetectedObject("Wall_1", "Wall"));
        objs.add(new DetectedObject("ERROR", "Camera disconnected"));

        camera.addObjects(objs, 10);
        // If camera logic sets status=ERROR upon detecting "ERROR" ID:
        assertEquals(STATUS.ERROR, camera.getStat(), "Camera should be in ERROR status after seeing 'ERROR' ID.");
        assertEquals(0, stats.getNumDetectedObjects(), "No objects should be counted after an error was found.");
    }

    @Test
    public void testGetObjects_AfterAddObjects() {
        List<DetectedObject> batch = new ArrayList<>();
        batch.add(new DetectedObject("Wall_2", "Wall desc"));
        batch.add(new DetectedObject("Lamp_1", "Lamp desc"));
        camera.addObjects(batch, 8);

        List<DetectedObject> result = camera.getObjects(8);
        assertEquals(2, result.size());
        assertEquals("Wall_2", result.get(0).getId());
        assertEquals("Lamp_1", result.get(1).getId());
        
    }
}
