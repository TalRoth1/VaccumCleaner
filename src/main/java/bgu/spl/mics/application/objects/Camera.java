package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    private int id;
    private int frequancy;
    private STATUS stat; 
    private List<StampedDetectedObjects> stamp;

    public Camera(int id, int frequancy)
    {
        this.id = id;
        this.frequancy = frequancy;
        this.stat = STATUS.UP;
    }
    public int getId()
    {
        return this.id;
    }
    public STATUS getStat()
    {
        return this.stat;
    }
    public int getFreq()
    {
        return this.frequancy;
    }
    public List<DetectedObject> getObjects(int time)
    {
        List<DetectedObject> result = null;
        for (StampedDetectedObjects obj : this.stamp)
        {
            if(obj.getTime() == time)
                result = obj.getObjects();
        }
        return result;
    }
    public void addObject(DetectedObject obj, int time)
    {
        StampedDetectedObjects object = new StampedDetectedObjects(time);
        object.addObject(obj);
        stamp.add(object);
    }
    public void addObjects(List<DetectedObject> obj, int time)
    {
        StampedDetectedObjects object = new StampedDetectedObjects(time);
        for(DetectedObject obje : obj)
        {
            object.addObject(obje);
        }
        stamp.add(object);
    }
}