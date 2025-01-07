package bgu.spl.mics.application.objects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private int totalMicroservices;      
    private int terminatedCount = 0;
    private boolean errorOccurred = false;
    private String faultySensor = null;
    private final ConcurrentMap<Integer, Pose> posesMap;
    private final ConcurrentMap<Integer, List<TrackedObject>> bufferedTrackedObjects;



    private FusionSlam() 
    {
        this.landmarks =  new ConcurrentHashMap<>();
        this.currentTick = 0;
        this.poses =  Collections.synchronizedList(new ArrayList<>());
         this.posesMap = new ConcurrentHashMap<>();
        this.bufferedTrackedObjects = new ConcurrentHashMap<>();
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
            posesMap.put(pose.getTime(), pose); 
    
            List<TrackedObject> pendingObjects = bufferedTrackedObjects.remove(pose.getTime());
            if (pendingObjects != null) {
                processTrackedObjects(pendingObjects, pose);
            }
        }
    }
    /**
     * Transforms the coordinates of the tracked object based on the robot's pose.
     *
     * @param coordinates The original coordinates of the object.
     * @param pose        The robot's current pose.
     * @return Transformed coordinates in the global frame.
     */
    public void handleTrackedObjectEvent(TrackedObject obj) {
        if (obj == null){
            System.out.println("Received null TrackedObjectEvent.");
            return;
        }
         
        int objTime = obj.getTime(); 
    
        Pose correspondingPose = posesMap.get(objTime);
        if (correspondingPose != null) {
            processTrackedObject(obj, correspondingPose);
        } else {
            bufferedTrackedObjects.computeIfAbsent(objTime, k -> new ArrayList<>()).add(obj);
        }
    }

    
    public void processTrackedObject(TrackedObject obj, Pose pose) {
        if (obj != null && pose != null) {
            List<CloudPoint> globalPoints = this.transformCoordinates(obj.getCoordinates(), pose);

            if (globalPoints == null) {
                return;
            }
            String objectId = obj.getId();
            String desc = obj.getDescription();           
            this.updateLandmark(objectId, desc, globalPoints);
            
            if(obj.getTime()==10 & objectId.equals("Wall_1")){
                List<CloudPoint> updateobj= landmarks.get(objectId).getCoordinates(); 
                for(CloudPoint updated:updateobj ){
                    System.out.println("updated x :" + updated.getX()+"updated y :" + updated.getY());
                }
            }

           
        }
    }

    public void processTrackedObjects(List<TrackedObject> trackedObjects, Pose currentPose) {
        if (trackedObjects != null && !trackedObjects.isEmpty()) {
            for (TrackedObject obj : trackedObjects) {
                processTrackedObject(obj, currentPose);
            }
        }
        else {
            System.out.println("No TrackedObjects to process at current pose.");
        }
    }
    
    private List<CloudPoint> transformCoordinates(List<CloudPoint> localCoordinates, Pose robotPose) {
        List<CloudPoint> globalCoordinates = new ArrayList<>();
    
        if (localCoordinates == null || robotPose == null) {
            System.out.println("Error: Missing coordinates or pose for transformation.");
            return globalCoordinates;
        }
    
        double robotX = robotPose.getX();
        double robotY = robotPose.getY();
        double yaw = Math.toRadians((robotPose.getYaw())); 
    
        for (CloudPoint localPoint : localCoordinates) {
            double localX = localPoint.getX();
            double localY = localPoint.getY();
    
            double globalX = localX * Math.cos(yaw) - localY * Math.sin(yaw) + robotX;
            double globalY = localX * Math.sin(yaw) + localY * Math.cos(yaw) + robotY;
    
            globalCoordinates.add(new CloudPoint(globalX, globalY));
        }    
        return globalCoordinates;
    }
    

    public void updateLandmark(String objectId, String desc, List<CloudPoint> newPoints) 
    {
        if (objectId != null && newPoints != null && !newPoints.isEmpty()) {
            synchronized (landmarks) {
                LandMark lm;
                if (!this.landmarks.containsKey(objectId)) {
                    lm = new LandMark(objectId, desc, newPoints);
                    this.landmarks.put(objectId, lm);
                    if (StatisticalFolder.getInstance() != null) {
                        StatisticalFolder.getInstance().incrementLandmarks(1);
                    }
                } 
                else {
                    lm = this.landmarks.get(objectId);
                    if(objectId=="Wall_1"){
                        List<CloudPoint> merged = this.averageCoordinates(lm.getCoordinates(), newPoints);
                    }
                    List<CloudPoint> merged = this.averageCoordinates(lm.getCoordinates(), newPoints);
                    lm.setCoordinates(merged);
                }
            }
        }
        else
        {
            System.out.println("Invalid parameters for updateLandmark. objectId: " + objectId + ", newPoints size: " + (newPoints != null ? newPoints.size() : "null"));
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
    public List<LandMark> getLandmarks() {
        synchronized (landmarks) {
            return new ArrayList<>(this.landmarks.values());
        }
    }

    /**
     * @return The current simulation tick.
     */
    public int getCurrentTick() 
    {
        return currentTick;
    }

    public List<Pose> getPoses()
    {
        return new ArrayList<>(poses);
    }
    public synchronized void serviceTerminated(String serviceName) 
    {
        System.out.println("micro service terminated "+ serviceName);
        terminatedCount++;
        System.out.println("terminated count: "+ terminatedCount);
        checkForFinish();
    }
    public synchronized void onCrash(String sensorName)
    {
        errorOccurred = true;
        faultySensor = sensorName;
    }
    public Boolean checkForFinish() 
    {
        if (terminatedCount == totalMicroservices) 
        {
            System.out.println("All microservices have terminated successfully.");
            return true; 
        }
        return false; 
    }
    public boolean isErrorOccurred() 
    {
        return errorOccurred;
    }
    public String getFaultySensor() 
    {
        return faultySensor;
    }
    public void printOutputFile(String path, List<Camera> cameras, List<LiDarWorkerTracker> lidars)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, Object>info = new LinkedHashMap<>();
        Map<String, Object>stats = new LinkedHashMap<>();
        boolean error = FusionSlam.getInstance().isErrorOccurred();
        String errorObj = FusionSlam.getInstance().getFaultySensor();
        String errorMSG = "";
        String [] classification = new String[0];
        if (errorObj != null)
        {
            classification = errorObj.split(" ");
        }
        stats.put("systemRuntime", StatisticalFolder.getInstance().getRuntime());
        stats.put("numDetectedObjects", StatisticalFolder.getInstance().getNumDetectedObjects());
        stats.put("numTrackedObjects", StatisticalFolder.getInstance().getNumTrackedObjects());
        System.out.println(StatisticalFolder.getInstance().getNumLandmarks() + " num statistical folder"+ landmarks.size()+ " num landmarks file");
        stats.put("numLandmarks", StatisticalFolder.getInstance().getNumLandmarks());
        if (landmarks.isEmpty()) 
            System.out.println("FusionSlam: No landmarks to serialize.");
        else
        {
            Map<String,Map<String, Object>> worldMap = new HashMap<>();
            for(LandMark landMark : landmarks.values()) {
                Map<String, Object> lmMap = new LinkedHashMap<>();
                lmMap.put("id", landMark.getId());
                lmMap.put("description", landMark.getDescription());
                lmMap.put("coordinates", landMark.getCoordinates());
                worldMap.put(landMark.getId(),lmMap);
            }
            stats.put("landMarks", worldMap);
            System.out.println("FusionSlam: Serialized " + worldMap.size() + " landmarks.");
        }
        if (error)
        {
            if(classification[0].equals("Camera"))
            {
                for(Camera cam : cameras)
                {
                    if ((cam.getId() + "").equals(classification[1]))
                    {
                        for(StampedDetectedObjects objects : cam.getAllObjects())
                        {
                            for(DetectedObject obj : objects.getObjects())
                            {
                                if(obj.getId().equals("ERROR"))
                                {
                                    errorMSG = obj.getDesc();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            else if(classification[0].equals("LiDar"))
            {
                for(LiDarWorkerTracker lidar : lidars)
                {
                    if ((lidar.getId() + "").equals(classification[1]))
                    {
                        for(TrackedObject object : lidar.getAllObjects())
                        {
                            if(object.getId().equals("ERROR"))
                                errorMSG = object.getDescription();
                        }
                    }
                }
            }
            System.out.println("Error detected: " + errorObj + " disconnected.");
            info.put("error", errorMSG);
            info.put("faultySensor", errorObj);
            LinkedHashMap<String, Object> lcframes = new LinkedHashMap<>();
            for(Camera camera : cameras)
            {
                List<StampedDetectedObjects> ldf = camera.getLastDetectedFrame();
                lcframes.put("Camera " + camera.getId(), ldf.get(0));
            }
            info.put("lastCameraFrames", lcframes);

            LinkedHashMap<String, Object> llframes = new LinkedHashMap<>();
            for(LiDarWorkerTracker lidar : lidars)
            {
                List<TrackedObject> ldf = lidar.getLastFrame();
                llframes.put("LidarWorkerTracker" + lidar.getId(), ldf);
            }
            info.put("lastLidarFrames", llframes);
            Pose [] poses = new Pose[StatisticalFolder.getInstance().getRuntime()];
            for(int i = 0; i < poses.length; i++)
            {
                poses[i] = GPSIMU.getInstance().getPose(i);
            }
            info.put("Poses", poses);
            info.put("statistics", stats);
        }
        else
        {
            info = stats;
        }
        try (FileWriter writer = new FileWriter(path + "output_file.json")) {
            gson.toJson(info, writer);
            System.out.println("Output file generated at: " + path + "output_file.json");
        } 
        catch (IOException e)
        {
            System.err.println("Failed to write output file: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}