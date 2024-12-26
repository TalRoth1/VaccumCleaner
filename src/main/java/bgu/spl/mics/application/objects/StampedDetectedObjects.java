package bgu.spl.mics.application.objects;

import java.util.LinkedList;

/**
 * Represents objects detected by the camera at a specific timestamp.
 * Includes the time of detection and a list of detected objects.
 */
public class StampedDetectedObjects {
    private int time;
    private LinkedList<DetectedObject> DetectedObjects; //Rotem needs to check if this is ok

    public StampedDetectedObjects(int time)
    {
        this.time = time;
        this.DetectedObjects = new LinkedList<DetectedObject>();
    }
    public int getTime()
    {
        return this.time;
    }
    public LinkedList<DetectedObject> getObjects()
    {
        return this.DetectedObjects;
    }
    public void addObject(DetectedObject obj)
    {
        this.DetectedObjects.add(obj);
    }
}
