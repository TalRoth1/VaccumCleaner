package bgu.spl.mics.application.services;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for FusionSlam – particularly the method that
 * transforms tracked objects into landmarks or updates landmarks.
 */
public class FusionSlamTest {

    private FusionSlam fusionSlam;

    @BeforeEach
    public void setUp() {
        // Retrieve the singleton and reset it if needed
        fusionSlam = FusionSlam.getInstance();
        
        // If your assignment needs a fresh state each time, you might add a reset method:
        // fusionSlam.reset(); (not shown here—depends on your design)
    }

    @Test
    public void testUpdateLandmark_NewLandmark() {
        // Arrange
        String objectId = "Chair_1";
        String desc = "A simple chair";
        List<CloudPoint> newPoints = new ArrayList<>();
        newPoints.add(new CloudPoint(1.0, 2.0));
        newPoints.add(new CloudPoint(3.0, 4.0));

        // Act
        fusionSlam.updateLandmark(objectId, desc, newPoints);

        // Assert
        // We expect a new landmark was added
        List<LandMark> allLandmarks = fusionSlam.getLandmarks();
        assertFalse(allLandmarks.isEmpty(), "Landmarks should not be empty after adding a new one.");
        LandMark lm = findLandmarkById(allLandmarks, objectId);
        assertNotNull(lm, "Expected landmark with id=" + objectId + " to exist.");
        assertEquals(desc, lm.getDescription());
        assertEquals(2, lm.getCoordinates().size());
    }

    @Test
    public void testUpdateLandmark_ExistingLandmark_AveragingPoints() {
        // Arrange
        String objectId = "Table_1";
        List<CloudPoint> firstPoints = new ArrayList<>();
        firstPoints.add(new CloudPoint(0.0, 0.0));
        firstPoints.add(new CloudPoint(2.0, 2.0));
        fusionSlam.updateLandmark(objectId, "A table", firstPoints);

        List<CloudPoint> secondPoints = new ArrayList<>();
        secondPoints.add(new CloudPoint(2.0, 2.0));
        secondPoints.add(new CloudPoint(4.0, 4.0));

        // Act
        fusionSlam.updateLandmark(objectId, "A table", secondPoints);

        // Assert
        LandMark tableLm = findLandmarkById(fusionSlam.getLandmarks(), objectId);
        assertNotNull(tableLm, "Table landmark should exist.");
        List<CloudPoint> coords = tableLm.getCoordinates();
        // After averaging ( (0+2)/2 , (0+2)/2 ) => (1,1 ), ( (2+4)/2 , (2+4)/2 ) => (3,3 )
        assertEquals(2, coords.size());
        assertEquals(1.0, coords.get(0).getX(), 0.0001);
        assertEquals(1.0, coords.get(0).getY(), 0.0001);
        assertEquals(3.0, coords.get(1).getX(), 0.0001);
        assertEquals(3.0, coords.get(1).getY(), 0.0001);
    }

    @Test
    public void testProcessTrackedObjects_WithPose() {
        Pose pose = new Pose(10f, 5f, (float) Math.toRadians(90), 1); // x=10, y=5, yaw=90°, time=1

        List<TrackedObject> trackedList = new ArrayList<>();
        List<CloudPoint> coords = new ArrayList<>();
        coords.add(new CloudPoint(0, 0));
        coords.add(new CloudPoint(1, 1));

        trackedList.add(new TrackedObject("Box_1", 1, "A box", coords));

        fusionSlam.processTrackedObjects(trackedList, pose);

        LandMark boxLm = findLandmarkById(fusionSlam.getLandmarks(), "Box_1");
        assertNotNull(boxLm);
        // The expected transform: yaw=90 => (x,y)->(-y,x). Then add (10,5).
        // (0,0)->(10,5), (1,1)->( ( -1 +10),( 1 +5))= (9,6).
        List<CloudPoint> updated = boxLm.getCoordinates();
        assertEquals(2, updated.size());
        // (10,5) for the first point, (9,6) for the second
        assertEquals(10.0, updated.get(0).getX(), 0.0001);
        assertEquals(5.0, updated.get(0).getY(), 0.0001);
        assertEquals(9.0, updated.get(1).getX(), 0.0001);
        assertEquals(6.0, updated.get(1).getY(), 0.0001);
    }

    private LandMark findLandmarkById(List<LandMark> list, String id) {
        for (LandMark lm : list) {
            if (lm.getId().equals(id)) {
                return lm;
            }
        }
        return null;
    }
}
