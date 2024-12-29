package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;

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
    private final StatisticalFolder statisticalFolder;
    private final LiDarDataBase liDarDataBase;

    public LiDarWorkerTracker(int id, int freq, List<TrackedObject> trackedObjects, StatisticalFolder statisticalFolder, LiDarDataBase liDarDataBase)
    {
        this.id = id;
        this.frequency = freq;
        this.status = STATUS.UP;
        this.lastTrackedObjects = trackedObjects;
        this.statisticalFolder = statisticalFolder;
        this.liDarDataBase = liDarDataBase;
    }
    
    public int getId()
    {
        return this.id;
    }
    
    public int getFreq()
    {
        return this.frequency;
    }
    
    public STATUS getsStatus()
    {
        return this.status;
    }
    public TrackedObjectsEvent processDetectObjectsEvent(DetectObjectsEvent event) {/// FINISH HERE
        StampedDetectedObjects detectedObj = event.getStampedDetectedObjects();
        if (detectedObj == null) {
            return null;
        }
        List<TrackedObject> newTrackedObjs = new ArrayList<>();

        for (DetectedObject obj : detectedObj.getObjects()) {
            List<CloudPoint> cloudPoints = liDarDataBase.getCloudPoints(detectedObj.getTime());
            if (cloudPoints == null || cloudPoints.isEmpty()) {
                continue;
            }
            TrackedObject tracked = new TrackedObject(obj.getId(),obj.getDesc(),cloudPoints);
        
            newTrackedObjs.add(tracked);
            lastTrackedObjects.add(tracked);
        }

        if (newTrackedObjs.isEmpty()) {
            return null;
        }

        return new TrackedObjectsEvent(newTrackedObjs);
    }
    
    /*public void addObject(DetectedObject obj) // Need to check what they want us to do here /// why we add this method? 
    {
        boolean exists = false;
        for (TrackedObject object : lastTrackedObjects)
        {
            if(object.getId().equals(obj.getId()))
                exists = true;
        }
        if(!exists)
            System.out.print("Adding a nonexistant object");
    }
    public List<TrackedObject> getObjects(int time)
    {
        List<TrackedObject> result = new LinkedList<TrackedObject>();
        for(TrackedObject obj : lastTrackedObjects)
        {
            if(obj.getTime() == time)
                result.add(obj);
        }
        return result;
    }

   
    */
}
