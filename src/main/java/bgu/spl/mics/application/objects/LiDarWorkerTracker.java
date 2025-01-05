package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private Map<Integer, List<TrackedObject>>  trackedObjectsByTime;


    public LiDarWorkerTracker(int id, int freq)
    {
        this.id = id;
        this.frequency = freq;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<>();
        this.lastFrame = new ArrayList<>();
        trackedObjectsByTime = new HashMap<>();// added for efficent 


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
        trackedObjectsByTime.computeIfAbsent(time, k -> new ArrayList<>()).add(tObj);
        this.lastTrackedObjects.add(tObj);
        StatisticalFolder.getInstance().incrementTrackedObjects(1);
        lastFrame.add(tObj);// remove all clear
    }

    public synchronized List<TrackedObject> getObjects(int time) // add sync
    {
        List<TrackedObject> result = new LinkedList<TrackedObject>();
        for (TrackedObject obj : lastTrackedObjects)
        {
            if (obj.getTime() == time)
            {
                result.add(obj);
            }
        }
        if (!result.isEmpty()) {
            lastFrame = new ArrayList<>(result);
            System.out.println("LiDarWorkerTracker: Retrieved " + result.size() + " objects for time " + time);
        }
        return result;
    
    }
}