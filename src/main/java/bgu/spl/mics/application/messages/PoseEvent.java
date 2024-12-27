package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Pose> //implemented
{
    private final Pose pose;

    public PoseEvent(Pose pose) {
        this.pose = pose;
    }

    public Pose getPose() {
        return pose;
    }
}