package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.DetectedObject;

public class DetectObjectsEvent implements Event<DetectedObject>
{
    private DetectedObject object;
    public DetectObjectsEvent(DetectedObject object)
    {
        this.object = object;
    }
    public DetectedObject getObject()
    {
        return this.object;
    }
}
