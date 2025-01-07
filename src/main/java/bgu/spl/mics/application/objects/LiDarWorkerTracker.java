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
        this.lastFrame = Collections.synchronizedList(new ArrayList<>());
        this.lastFrame.add(new TrackedObject("init", 0, "Initialize", new LinkedList<>()));
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
        return lastFrame;
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
        TrackedObject tObj = findTrackedObjectInLast(obj.getId(), time);
        if (tObj == null) {
            tObj = new TrackedObject(obj.getId(), time, obj.getDesc(), LiDarDataBase.getInstance("").getCloudPoints(obj.getId(),time).getCloudPoints());
            this.lastTrackedObjects.add(tObj);
            if (StatisticalFolder.getInstance().getNumTrackedObjects() ==(LiDarDataBase.getInstance("").getSize())-1) {
                System.out.println("change to down");
                status = STATUS.DOWN;
            }
            System.out.println("Added new TrackedObject with ID: " + obj.getId() + " at time: " + time);
        }
        if(lastFrame.get(0).getTime() != time)
            lastFrame.clear();
        lastFrame.add(tObj);
        StatisticalFolder.getInstance().incrementTrackedObjects(1);
    }


    public ArrayList<TrackedObject> processDetectedObjects(StampedDetectedObjects objects, int currentTick) {
        int processingTime = objects.getTime() + frequency;   
        ArrayList<TrackedObject> allProcessedObjects = new ArrayList<>();
    
        // Process all pending objects with times <= processingTime
        List<Integer> timesToProcess = new ArrayList<>(pendingObjects.keySet());
        for (int time : timesToProcess) {
            if (time <= processingTime) {
                List<StampedDetectedObjects> objectsList = pendingObjects.remove(time);
                if (objectsList != null) {
                    for (StampedDetectedObjects pendingObject : objectsList) {
                        allProcessedObjects.addAll(processObjectsAtTime(pendingObject));
                    }
                }
            }
        }    
        if (processingTime > currentTick) {
            pendingObjects.computeIfAbsent(processingTime, k -> new ArrayList<>()).add(objects);
            System.out.println("Object added to pendingObjects for time: " + processingTime);
            return allProcessedObjects;
        }
       
        allProcessedObjects.addAll(processObjectsAtTime(objects));
        return allProcessedObjects;
    }
    @SuppressWarnings("unused")
    private ArrayList<TrackedObject> processObjectsAtTime(StampedDetectedObjects objects)
    {
        ArrayList<TrackedObject> trackedObjects = new ArrayList<>();
        if (objects == null) {
            return trackedObjects; // Return empty list
        }
        List<DetectedObject> detectedObjects = objects.getObjects();
        if (detectedObjects == null || detectedObjects.isEmpty()) {
            return trackedObjects; // Return empty list
        }

        for (DetectedObject obj : detectedObjects) {

            TrackedObject trackedObj = findTrackedObjectInLast(obj.getId(), objects.getTime());
            pendingObjects.remove(trackedObj.getTime());// remove from pending list 
            if (trackedObj != null) {
                trackedObjects.add(trackedObj);
            } 
        }

        if (!trackedObjects.isEmpty()) {
            StatisticalFolder.getInstance().incrementTrackedObjects(trackedObjects.size());
        } 
        return trackedObjects;
    }
    private TrackedObject findTrackedObjectInLast(String id, int time) {
        synchronized (lastTrackedObjects) {
            for (TrackedObject trackedObj : lastTrackedObjects) {
                if (trackedObj.getId().equals(id) && trackedObj.getTime() == time) {
                    return trackedObj;
                }
            }
        }
        return null; // Return null if no matching tracked object is found
    }

    public synchronized List<TrackedObject> getObjects(int time)
    {
        List<TrackedObject> result = new LinkedList<>();
        for (TrackedObject obj : lastTrackedObjects)
        {
            if (obj.getTime() <= time)
            {
                result.add(obj);
            }
        }
        for (TrackedObject obj : result)
        {
            lastTrackedObjects.remove(obj);
        }
        List<StampedDetectedObjects> pendingForTime = pendingObjects.get(time);
        if (pendingForTime != null) {
            for (StampedDetectedObjects obj : pendingForTime) {
                result.addAll(processObjectsAtTime(obj));
            }
        }    

        if (!result.isEmpty()) {
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
                    for (StampedDetectedObjects obj : objectsList) {
                        ArrayList<TrackedObject> processed = processObjectsAtTime(obj);
                        if (processed == null || processed.isEmpty()) {
                        } else {
                            allTrackedObjects.addAll(processed);
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
            return;
        }
        ((StampedDetectedObjects) pendingObjects.computeIfAbsent(time, k -> new ArrayList<>())).addObject(obj);
        System.out.println("LiDarWorkerTracker: Added DetectedObject ID: " + obj.getId() + " to pending list for time: " + time);
    }

    public List<TrackedObject> getAllObjects()
    {
        return this.lastTrackedObjects;
    }
    
}