package bgu.spl.mics.application.objects;

import java.util.ArrayList;
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
    private List<StampedDetectedObjects> lastDetectedFrame;

    public Camera(int id, int frequancy)
    {
        this.id = id;
        this.frequancy = frequancy;
        this.stat = STATUS.UP;
        this.lastDetectedFrame = new ArrayList<>();
        this.stamp = new ArrayList<>();

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
    public List<StampedDetectedObjects> getLastDetectedFrame() 
    {
        return new ArrayList<>(lastDetectedFrame);
    }
    public void setStatus(STATUS newStatus) {
        this.stat = newStatus;
    }
    public List<DetectedObject> getObjects(int time)
    {
        if (time == stamp.get(stamp.size() - 1).getTime())
        {
            this.setStatus(STATUS.DOWN);
        }
        List<DetectedObject> result = new ArrayList<>();
        StampedDetectedObjects sdo = null;
        for(StampedDetectedObjects stampT : stamp) 
        {
            if(stampT.getTime() == time)
            {
                sdo = stampT;
                break;
            }
        }
        if (sdo == null)
        {
            return result;
        }
        for(DetectedObject obj : sdo.getObjects())
        {
            if ("ERROR".equals(obj.getId()))
            {
                this.stat = STATUS.ERROR;
                return result;
            }
            result.add(obj);
        }
        StatisticalFolder.getInstance().incrementDetectedObjects(sdo.getObjects().size());
        lastDetectedFrame.clear();
        lastDetectedFrame.add(sdo);
        return result;
    }
    public void addObject(DetectedObject obj, int time)
    {
        StampedDetectedObjects sdo = new StampedDetectedObjects(time);
        sdo.addObject(obj);
        stamp.add(sdo);
    }
    public void addObjects(List<DetectedObject> obj, int time)
    {
        StampedDetectedObjects sdo = new StampedDetectedObjects(time);
        for(DetectedObject obje : obj)
        {
            sdo.addObject(obje);
        }
        stamp.add(sdo);
        lastDetectedFrame = new ArrayList<>();
        lastDetectedFrame.add(sdo);
    }
    public List<StampedDetectedObjects> getAllObjects()
    {
        return this.stamp;
    }
}