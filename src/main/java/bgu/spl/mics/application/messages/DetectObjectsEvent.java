package bgu.spl.mics.application.messages;

import java.util.List;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<DetectedObject> /// update
{
    private final StampedDetectedObjects stampedDetectedObjects;
    private final int detectionTime;

    
    public DetectObjectsEvent(StampedDetectedObjects stampedDetectedObjects, int detectionTime)
     {
        this.stampedDetectedObjects = stampedDetectedObjects;
        this.detectionTime = detectionTime;
    }
    public StampedDetectedObjects getStampedDetectedObjects() {
        return stampedDetectedObjects;
    }
    public List<DetectedObject> getObjects()
    {
        return this.stampedDetectedObjects.getObjects();
    }
    /**
     * @return The detection time of the event.
     */
    public int getDetectionTime() {
        return detectionTime;
    }
}