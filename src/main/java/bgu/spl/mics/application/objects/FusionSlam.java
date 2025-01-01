package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
    public static FusionSlam getInstance()
    {
        return FusionSlamHolder.instance;
    }
    private final Map<String, LandMark> landmarks;
    private int currentTick;
    private final List<Pose> poses; 
    private int totalMicroservices;      
    private int terminatedCount = 0;
    private boolean errorOccurred = false;
    private String faultySensor = null;

    private FusionSlam() 
    {
        this.landmarks = new HashMap<>();
        this.currentTick = 0;
        this.poses = new ArrayList<>();
    }
    public void setTotalMicroservices(int total) {
        this.totalMicroservices = total;
    }

    /**
     * Updates the current simulation tick.
     *
     * @param tick The current tick.
     */
    public void updateTick(int tick) {
        this.currentTick = tick;
    }
    public void addPose(Pose pose) {
        if (pose != null) {
            poses.add(pose);
        }
    }
    /**
     * Transforms the coordinates of the tracked object based on the robot's pose.
     *
     * @param coordinates The original coordinates of the object.
     * @param pose        The robot's current pose.
     * @return Transformed coordinates in the global frame.
     */
    
    public void processTrackedObjects(List<TrackedObject> trackedObjects, Pose currentPose) {
        if (trackedObjects == null || trackedObjects.isEmpty()) return;
        for (TrackedObject obj : trackedObjects)
        {
            List<CloudPoint> globalPoints = transformCoordinates(obj.getCoordinates(), currentPose);
            String objectId = obj.getId();
            String desc = obj.getDescription();
            updateLandmark(objectId, desc, globalPoints);
        }
    }
    private List<CloudPoint> transformCoordinates(List<CloudPoint> coordinates, Pose pose) 
    {
        if (coordinates == null) 
            return new ArrayList<>();
        if (pose == null)
            return null;
        List<CloudPoint> result = new LinkedList<CloudPoint>();
        for(CloudPoint coords : coordinates)
        {
            double x = coords.getX();
            double y = coords.getY();
            double transformedX = x * Math.cos(pose.getYaw()) - y * Math.sin(pose.getYaw()) + pose.getX();
            double transformedY = x * Math.sin(pose.getYaw()) + y * Math.cos(pose.getYaw()) + pose.getY();
            result.add(new CloudPoint(transformedX, transformedY));
        }
        return result;
    }

    public void updateLandmark(String objectId, String desc, List<CloudPoint> newPoints) 
    {
        if (objectId == null || newPoints == null || newPoints.isEmpty())
            return;
        if (!landmarks.containsKey(objectId))
        {
            LandMark lm = new LandMark(objectId, desc, newPoints);
            landmarks.put(objectId, lm);
            if (StatisticalFolder.getInstance() != null) 
                StatisticalFolder.getInstance().incrementLandmarks(1);
        } 
        else 
        {
            LandMark existing = landmarks.get(objectId);
            List<CloudPoint> merged = averageCoordinates(existing.getCoordinates(), newPoints);
            existing.setCoordinates(merged);
        }
    }

    private List<CloudPoint> averageCoordinates(List<CloudPoint> oldPoints, List<CloudPoint> newPoints) {
        int size = Math.min(oldPoints.size(), newPoints.size());
        List<CloudPoint> merged = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CloudPoint oldPt = oldPoints.get(i);
            CloudPoint newPt = newPoints.get(i);
            double avgX = (oldPt.getX() + newPt.getX()) / 2.0;
            double avgY = (oldPt.getY() + newPt.getY()) / 2.0;
            merged.add(new CloudPoint(avgX, avgY));
        }
        if (newPoints.size() > size) {
            merged.addAll(newPoints.subList(size, newPoints.size()));
        }
        else if (oldPoints.size() > size) {
            merged.addAll(oldPoints.subList(size, oldPoints.size()));
        }

        return merged;
    }
    
    /**
     * @return The list of all landmarks.
     */
    public List<LandMark> getLandmarks()
    {
        return new ArrayList<>(landmarks.values());
    }

    /**
     * @return The current simulation tick.
     */
    public int getCurrentTick() 
    {
        return currentTick;
    }

    public List<Pose> getPoses() {
        return new ArrayList<>(poses);
    }
    public synchronized void serviceTerminated(String microServiceName) {
        terminatedCount++;
        checkForFinish();
    }
    
    public synchronized void onCrash(String sensorName) {
        errorOccurred = true;
        faultySensor = sensorName;
    }
    private void checkForFinish() 
    {
        if (terminatedCount == totalMicroservices) 
        {
        }
    }
    public boolean isErrorOccurred() 
    {
        return errorOccurred;
    }
    public String getFaultySensor() 
    {
        return faultySensor;
    }
}