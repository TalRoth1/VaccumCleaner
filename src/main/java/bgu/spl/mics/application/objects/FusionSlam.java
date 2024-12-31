package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private StatisticalFolder stats;
    private int totalMicroservices;      
    private int terminatedCount = 0;
    private boolean errorOccurred = false;
    private String faultySensor = null;
    private String errorDescription = null;
    private String outputFilePath = "output_file.json";

    private FusionSlam() 
    {
        this.landmarks = new HashMap<>();
        this.currentTick = 0;
        this.poses = new ArrayList<>();
    }
    public void setStatisticalFolder(StatisticalFolder stats) {
        this.stats = stats;
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
        for (TrackedObject obj : trackedObjects) {
            List<CloudPoint> globalPoints = transformCoordinates(obj.getCoordinates(), currentPose);
            String objectId = obj.getId();
            String desc = obj.getDescription();
            updateLandmark(objectId, desc, globalPoints);
        }
    }
    private List<CloudPoint> transformCoordinates(List<CloudPoint> coordinates, Pose pose) 
    {
        if (coordinates == null) {
            return new ArrayList<>();
        }
        if (pose == null) {
            return coordinates;
        }
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

    public void updateLandmark(String objectId, String desc, List<CloudPoint> newPoints) {
        if (objectId == null || newPoints == null || newPoints.isEmpty()) {
            return;
        }

        if (!landmarks.containsKey(objectId)) {
            LandMark lm = new LandMark(objectId, desc, newPoints);
            landmarks.put(objectId, lm);
            if (stats != null) {
                stats.incrementLandmarks(1);
            }
        } else {
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
    
    public synchronized void onCrash(String sensorName, String errorDesc) {
        errorOccurred = true;
        faultySensor = sensorName;
        errorDescription = errorDesc;
        // We'll wait for all microservices to eventually terminate
    }
    private void checkForFinish() {
        if (terminatedCount == totalMicroservices) {
            produceFinalOutput();
        }
    }
   
    private void produceFinalOutput() {
        if (errorOccurred) {
            produceErrorJSON();
        } else {
            produceSuccessJSON();
        }
    }

    private void produceErrorJSON() {
        Map<String, Object> errorOutput = new LinkedHashMap<>();
        errorOutput.put("Error", errorDescription);
        errorOutput.put("faultySensor", faultySensor);

        // Last frames from cameras
        Map<String, Object> cameraFrames = new LinkedHashMap<>();
        /*if (cameras != null) {
            for (Camera cam : cameras) {
                Map<String, Object> oneCamFrame = new LinkedHashMap<>();
                oneCamFrame.put("time" );
                oneCamFrame.put("detectedObjects", cam.getLastDetectedFrame());
                cameraFrames.put("Camera" + cam.getId(), oneCamFrame);
            }
        }*/
        // Last frames from LiDars
        Map<String, Object> lidarFrames = new LinkedHashMap<>();
        /*if (lidars != null) {
            for (LiDarWorkerTracker lid : lidars) {
                Map<String, Object> oneLidarFrame = new LinkedHashMap<>();
                oneLidarFrame.put("lastTrackedObjects", lid.getLastFrame());
                lidarFrames.put("LiDarWorkerTracker" + lid.getId(), oneLidarFrame);
            }
        }*/

        // Combine them
        Map<String, Object> lastFrames = new LinkedHashMap<>();
        lastFrames.put("cameras", cameraFrames);
        lastFrames.put("lidar", lidarFrames);

        errorOutput.put("lastFrames", lastFrames);

        // Poses: from getPoses()
        errorOutput.put("poses", getPoses());

        // Statistics
        Map<String, Object> statsMap = buildStatisticsMap(); 
        errorOutput.put("statistics", statsMap);

        // Serialize to JSON
        writeToJsonFile(errorOutput, outputFilePath);
    }

    private void produceSuccessJSON() {
        // Build normal success JSON
        Map<String, Object> successOutput = new LinkedHashMap<>();

        // 1) statistics
        Map<String, Object> statsMap = buildStatisticsMap();
        successOutput.put("statistics", statsMap);

        // 2) landMarks
        List<Map<String, Object>> landMarkList = new ArrayList<>();
        for (LandMark lm : getLandmarks()) {
            Map<String, Object> lmMap = new LinkedHashMap<>();
            lmMap.put("id", lm.getId());
            lmMap.put("description", lm.getDescription());
            List<Map<String, Double>> coordsArr = new ArrayList<>();
            for (CloudPoint cp : lm.getCoordinates()) {
                Map<String, Double> pointMap = new LinkedHashMap<>();
                pointMap.put("x", cp.getX());
                pointMap.put("y", cp.getY());
                coordsArr.add(pointMap);
            }
            lmMap.put("coordinates", coordsArr);
            landMarkList.add(lmMap);
        }
        successOutput.put("landMarks", landMarkList);
        writeToJsonFile(successOutput, outputFilePath);
    }

    /**
     * Helper method that builds a small map of the statistics from the StatisticalFolder.
     */
    private Map<String, Object> buildStatisticsMap() {
        Map<String, Object> statsMap = new LinkedHashMap<>();
        statsMap.put("systemRuntime", stats.getRuntime());
        statsMap.put("numDetectedObjects", stats.getNumDetectedObjects());
        statsMap.put("numTrackedObjects", stats.getNumTrackedObjects());
        statsMap.put("numLandmarks", stats.getNumLandmarks());
        return statsMap;
    }

    /**
     * Writes a generic Map<String,Object> to JSON using GSON, into the outputFilePath.
     */
    private void writeToJsonFile(Object data, String path) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonStr = gson.toJson(data);
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}