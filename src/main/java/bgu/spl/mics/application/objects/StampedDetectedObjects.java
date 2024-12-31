package bgu.spl.mics.application.objects;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    private int time;
    private List<DetectedObject> DetectedObjects; 

    public StampedDetectedObjects(int time)
    {
        this.time = time;
        this.DetectedObjects = new LinkedList<DetectedObject>();
    }

    public int getTime()
    {
        return this.time;
    }
    public List<DetectedObject> getObjects()
    {
        return this.DetectedObjects;
    }
    public void addObject(DetectedObject obj)
    {
        this.DetectedObjects.add(obj);
    }
    public void addObjects(List<DetectedObject> objs) {
        this.DetectedObjects.addAll(objs);
    }
}
