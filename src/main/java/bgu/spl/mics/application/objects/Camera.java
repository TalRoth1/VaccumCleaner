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
    private final StatisticalFolder stats;
    private List<DetectedObject> lastDetectedFrame;

    public Camera(int id, int frequancy, StatisticalFolder stats)
    {
        this.id = id;
        this.frequancy = frequancy;
        this.stat = STATUS.UP;
        this.stats = stats;
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
    // need to check whem update to down
    public List<DetectedObject> getObjects(int time)
    {
        List<DetectedObject> result = new ArrayList<>();;
        Iterator<StampedDetectedObjects> it = stamp.iterator();
        while (it.hasNext()) {
            StampedDetectedObjects sdo = it.next();
            if (sdo.getTime() == time) {
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
        stats.incrementDetectedObjects(1);
    }
    public void addObjects(List<DetectedObject> obj, int time)
    {
        StampedDetectedObjects sdo = new StampedDetectedObjects(time);
        for(DetectedObject obje : obj)
        {
            if ("ERROR".equals(obje.getId())) {
                this.stat= STATUS.ERROR;
                return;
            }
            sdo.addObject(obje);
        }
        stamp.add(sdo);
        lastDetectedFrame = new ArrayList<>(obj);
        stats.incrementDetectedObjects(obj.size());

    }
}