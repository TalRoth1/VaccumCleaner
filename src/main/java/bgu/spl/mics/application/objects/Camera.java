package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.Iterator;
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
    private List<DetectedObject> lastDetectedFrame;

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
    public List<DetectedObject> getLastDetectedFrame() {
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
        List<DetectedObject> result = new ArrayList<>();;
        Iterator<StampedDetectedObjects> it = stamp.iterator();
        while (it.hasNext()) {
            StampedDetectedObjects sdo = it.next();
            for(DetectedObject obj : sdo.getObjects())
            {
                if ("ERROR".equals(obj.getId()))
                {
                    this.stat = STATUS.ERROR;
                    return result;
                }
            }
            if (sdo.getTime() == time)
            {
                result = sdo.getObjects();
                it.remove();
                break;
            }
        }
        return result;
    }
    public void addObject(DetectedObject obj, int time)
    {
        StampedDetectedObjects sdo = new StampedDetectedObjects(time);
        sdo.addObject(obj);
        stamp.add(sdo);
        lastDetectedFrame.clear();
        lastDetectedFrame.add(obj);
        StatisticalFolder.getInstance().incrementDetectedObjects(1);
    }
    public void addObjects(List<DetectedObject> obj, int time)
    {
        StampedDetectedObjects sdo = new StampedDetectedObjects(time);
        for(DetectedObject obje : obj)
        {
            sdo.addObject(obje);
        }
        stamp.add(sdo);
        lastDetectedFrame = new ArrayList<>(obj);
        StatisticalFolder.getInstance().incrementDetectedObjects(obj.size());

    }
}