package bgu.spl.mics.application.objects;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LidarWorkerTest
{
    private LiDarWorkerTracker lidar;
    @BeforeEach
    public void setUp() 
    {
        LiDarDataBase.getInstance("D:\\Projects\\SPL\\Vaccum Cleaner\\example input\\lidar_data.json");
        lidar = new LiDarWorkerTracker(1, 2);
    }

    @Test
    public void testAddObject_NoError() 
    {
        DetectedObject obj = new DetectedObject("Wall_1", "Wall");
        lidar.addObject(obj, 2);

        // The camera should store it in a new StampedDetectedObjects
        List<TrackedObject> lastFrame = lidar.getObjects(5);
        assertEquals(1, lastFrame.size(), "Should have 1 frame after adding an object.");
        assertEquals("Wall_1", lastFrame.get(0).getId());
        assertEquals(1, StatisticalFolder.getInstance().getNumTrackedObjects(), "Stats should have 1 detected object so far.");
    }

    @Test
    public void testAddObjects_ErrorInList()
    {
        lidar.addObject(new DetectedObject("Wall_1", "Wall"), 2);
        lidar.addObject(new DetectedObject("ERROR", "LiDar disconnected"), 10);
        assertEquals(STATUS.ERROR, lidar.getsStatus(), "Camera should be in ERROR status after seeing 'ERROR' ID.");
        assertEquals(1, StatisticalFolder.getInstance().getNumTrackedObjects(), "No objects should be counted after an error was found.");
    }
}
