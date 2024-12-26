package bgu.spl.mics.application.messages;

import java.util.LinkedList;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.TrackedObject;

public class TrackedObjectsEvent implements Event
{
    private static TrackedObjectsEvent instance = new TrackedObjectsEvent(); 
    private static LinkedList<TrackedObject> objects = new LinkedList<>();

    public static TrackedObjectsEvent getInstance()
    {
        return instance;
    }
    public static void addObject(TrackedObject object)
    {
        objects.add(object);
    }
}
