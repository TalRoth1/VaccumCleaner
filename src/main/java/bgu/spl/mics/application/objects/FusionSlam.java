package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam 
{
        // Singleton instance holder
        private static class FusionSlamHolder
        {
            private static FusionSlam instance = new FusionSlam();
        }
        

    public static FusionSlam getInstance() {
        return FusionSlamHolder.instance;
    }
    private final Map<String, LandMark> landmarks;
    private int currentTick;
    private final List<Pose> poses;
    
    private FusionSlam() {
        this.landmarks = new HashMap<>();
        this.currentTick = 0;
        this.poses = new ArrayList<>();
        


    }
    public void updateLandmark(String id,String description,List<CloudPoint> cord) {
        if (cord == null || id == null) {
            return;
        }
        LandMark land = landmarks.get(id);
        if (land == null) {
            landmarks.put(id, new LandMark(id, description, cord));
        } else {
            land.setCoordinates(updatePoints(land,cord));
        }
    }
    
    public List<CloudPoint> updatePoints(LandMark land, List<CloudPoint> newPoints) {
        List<CloudPoint> existingPoints = land.getCoordinates();
        int minSize = Math.min(existingPoints.size(), newPoints.size());
        for (int i = 0; i < minSize; i++) {
            CloudPoint existingPoint = existingPoints.get(i);
            CloudPoint newPoint = newPoints.get(i);
    
            double averageX = (existingPoint.getX() + newPoint.getX()) / 2;
            double averageY = (existingPoint.getY() + newPoint.getY()) / 2;
            existingPoints.set(i, new CloudPoint(averageX, averageY));
        }
    
        // If there are extra points in newPoints, add them to the existingPoints list
        if (newPoints.size() > existingPoints.size()) {
            existingPoints.addAll(newPoints.subList(existingPoints.size(), newPoints.size()));
        }
        return existingPoints; 
    }
    

    /**
     * Updates the current simulation tick.
     *
     * @param tick The current tick.
     */
    public void updateTick(int tick) {
        this.currentTick = tick;
    }

    /**
     * Transforms the coordinates of the tracked object based on the robot's pose.
     *
     * @param coordinates The original coordinates of the object.
     * @param pose        The robot's current pose.
     * @return Transformed coordinates in the global frame.
     */
    /**
     * @return The list of all landmarks.
     */
    public List<LandMark> getLandmarks()
    {
        return new ArrayList<>(landmarks.values());
    }

    public void addPose(Pose pose) {
        poses.add(pose);
    }

    /**
     * @return The current simulation tick.
     */
    public int getCurrentTick() 
    {
        return currentTick;
    }
}
