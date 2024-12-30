package bgu.spl.mics.application.objects;

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

    public LiDarWorkerTracker(int id, int freq)
    {
        this.id = id;
        this.frequency = freq;
        this.status = STATUS.UP;
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
    public void addObject(DetectedObject obj, int time)
    {
        List<CloudPoint> coords = LiDarDataBase.getDistance(obj.getId());
        this.lastTrackedObjects.add(new TrackedObject(obj.getId(), time, obj.getDesc(), coords));
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
}