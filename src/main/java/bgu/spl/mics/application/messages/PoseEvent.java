package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Pose> //implemented
{
    private final Pose pose;
    private final int tick;

    public PoseEvent(Pose pose, int tick) {
        this.pose = pose;
        this.tick = tick;
    }

    public Pose getPose() {
        return pose;
    }
    public int getTick() {
        return tick;
    }
}