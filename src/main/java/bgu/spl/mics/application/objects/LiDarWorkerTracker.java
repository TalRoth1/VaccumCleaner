package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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



    public LiDarWorkerTracker(int id, int freq)
    {
        this.id = id;
        this.frequency = freq;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<>();
        this.lastFrame = new ArrayList<>();

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
    public void addObject(DetectedObject obj, int time)
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
            coords = new ArrayList<>();
        }
        TrackedObject tObj = new TrackedObject(obj.getId(), time, obj.getDesc(), coords);
        this.lastTrackedObjects.add(tObj);
        StatisticalFolder.getInstance().incrementTrackedObjects(1);
        lastFrame.clear();
        lastFrame.add(tObj);
    }

    public List<TrackedObject> getObjects(int time)
    {
        List<TrackedObject> result = new LinkedList<TrackedObject>();
        Iterator<TrackedObject> it = lastTrackedObjects.iterator();

        while (it.hasNext()) {
            TrackedObject obj = it.next();
            if (obj.getTime() + frequency <= time) {
                result.add(obj);
                it.remove();
            }
        }
        if (!result.isEmpty()) {
            lastFrame = new ArrayList<>(result);
        }
        return result;
    
    }
}