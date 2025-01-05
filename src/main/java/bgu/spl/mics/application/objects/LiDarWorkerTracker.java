package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker 
{
    private int id;
    private int frequency;
    private STATUS status;
    private List<TrackedObject> lastTrackedObjects;
    private List<TrackedObject> lastFrame;
    private Map<Integer,List<StampedDetectedObjects>> pendingObjects;

    public LiDarWorkerTracker(int id, int freq)
    {
        this.id = id;
        this.frequency = freq;
        this.status = STATUS.UP;
        this.lastTrackedObjects = Collections.synchronizedList(new ArrayList<>());
        this.lastFrame =Collections.synchronizedList(new ArrayList<>());
        this.pendingObjects = new ConcurrentHashMap<>(); 


    }
    public int getId()
    {
        return this.id;
    }
    public int getFreq()
    {
        return this.frequency;
    }
    public List<TrackedObject> getLastFrame() {
        return new ArrayList<>(lastFrame);
    }
    public STATUS getsStatus()
    {
        return this.status;
    }
    public void setStatus(STATUS newStatus) {
        this.status = newStatus;
    }
    public synchronized void addObject(DetectedObject obj, int time)// add syncr
    {
        if ("ERROR".equals(obj.getId()))
        {
            // We found an error => set status=ERROR
            this.status = STATUS.ERROR;
            return;
        }
        List<CloudPoint> coords = LiDarDataBase.getDistance(obj.getId(), time);
        if (coords == null) 
        {
            System.out.println("LiDarWorkerTracker: No coordinates found for object ID: " + obj.getId() + " at time: " + time);
            coords = new ArrayList<>();
        }
        System.out.println("LiDarWorkerTracker: Retrieved " + coords.size() + " coordinates for object ID: " + obj.getId() + " at time: " + time);
        TrackedObject tObj = new TrackedObject(obj.getId(), time, obj.getDesc(), coords);
        this.lastTrackedObjects.add(tObj);
        System.out.println("num tracked object" +lastTrackedObjects.size());
        StatisticalFolder.getInstance().incrementTrackedObjects(1);
        lastFrame.clear();
        }

    public ArrayList<TrackedObject> processDetectedObjects(StampedDetectedObjects objects, int currentTick) {
        int processingTime = objects.getTime() + frequency;
        if (processingTime > currentTick) {
            pendingObjects.computeIfAbsent(processingTime, k -> new ArrayList<>()).add(objects);
            System.out.println("Object added to pendingObjects for time: " + processingTime);
            return new ArrayList<>();
        }
       
        return processObjectsAtTime(objects);
    }
    private ArrayList<TrackedObject> processObjectsAtTime(StampedDetectedObjects objects) {
        ArrayList<TrackedObject> trackedObjects = new ArrayList<>();
        if (objects == null) {
            System.out.println("processObjectsAtTime: StampedDetectedObjects is null.");
            return trackedObjects; // Return empty list
        }
        List<DetectedObject> detectedObjects = objects.getObjects();
        if (detectedObjects == null || detectedObjects.isEmpty()) {
        System.out.println("processObjectsAtTime: No detected objects to process for time: " + objects.getTime());
        return trackedObjects; // Return empty list
        }

        for (DetectedObject obj : objects.getObjects()) {
            if ("ERROR".equals(obj.getId())) {
                status = STATUS.ERROR;
                System.out.println("processObjectsAtTime: ERROR object encountered. Setting status to ERROR.");
                return new ArrayList<>(); // Return empty list on error
            }

            StampedCloudPoints cloudPoints =LiDarDataBase.getCloudPoints(obj.getId(), objects.getTime());
            if (cloudPoints != null) {
                trackedObjects.add(new TrackedObject(obj.getId(), objects.getTime(), obj.getDesc(), cloudPoints.getCloudPoints()));
            } else {
                status = STATUS.ERROR;
                System.out.println("No cloud points found for object: " + obj.getId());
                return new ArrayList<>(); // Return empty list on error
            }
        }

        if (!trackedObjects.isEmpty()) {
            StatisticalFolder.getInstance().incrementTrackedObjects(trackedObjects.size());
            if (StatisticalFolder.getInstance().getNumTrackedObjects() == lastTrackedObjects.size()) {// not sure about this logic
                status = STATUS.DOWN;
            }

            System.out.println("Tracked objects incremented by: " + trackedObjects.size());
            lastTrackedObjects = trackedObjects;
            lastFrame.clear();
            lastFrame=lastTrackedObjects;// change to be here unp
        } else {
            System.out.println("No objects to track at this time.");
        }

        return trackedObjects;
    }

    public synchronized List<TrackedObject> getObjects(int time) {
        List<StampedDetectedObjects> pendingForTime = pendingObjects.remove(time);
        if (pendingForTime != null) {
            System.out.println("pending not null");/// not get here
            for (StampedDetectedObjects obj : pendingForTime) {
                processObjectsAtTime(obj);
            }
        }
        List<TrackedObject> result = new LinkedList<>();
        for (TrackedObject obj : lastTrackedObjects) {
            System.out.println("Object time: " + obj.getTime() + " | Requested time: " + time);
            if (obj.getTime() == time) {
                result.add(obj);
                System.out.println(result.size() + " objects retrieved for time " + time);
            }
        }

        if (!result.isEmpty()) {
            lastFrame = new ArrayList<>(result);
            System.out.println("LiDarWorkerTracker: Retrieved " + result.size() + " objects for time " + time);
        }
        else
            System.out.println("get objects list empty");
        return result;
    }
    
    public ArrayList<TrackedObject> checkPendingObjects(int currentTick) {
        ArrayList<TrackedObject> allTrackedObjects = new ArrayList<>();
        ArrayList<Integer> timesToProcess = new ArrayList<>(pendingObjects.keySet());

        for (int time : timesToProcess) {
            if (time <= currentTick) {
                List<StampedDetectedObjects> objectsList = pendingObjects.remove(time);
                if (objectsList != null) {
                    System.out.println("Processing pending objects for time: " + time);
                    for (StampedDetectedObjects obj : objectsList) {
                        ArrayList<TrackedObject> processed = processObjectsAtTime(obj);
                        if (processed == null || processed.isEmpty()) {
                            System.out.println("No tracked objects were created for StampedDetectedObjects at time: " + time);
                        } else {
                            allTrackedObjects.addAll(processed);
                            System.out.println("Added " + processed.size() + " tracked objects for time: " + time);
                        }
                    }
                } else {
                    System.out.println("No pending objects found for time: " + time);
                }
            }
        }
    
        return allTrackedObjects;
    }
    public synchronized void addPendingObject(DetectedObject obj, int time) {
        if (obj == null) {
            System.out.println("Cannot add null DetectedObject to pending list.");
            return;
        }
        ((StampedDetectedObjects) pendingObjects.computeIfAbsent(time, k -> new ArrayList<>())).addObject(obj);
        System.out.println("LiDarWorkerTracker: Added DetectedObject ID: " + obj.getId() + " to pending list for time: " + time);
    }
    
}