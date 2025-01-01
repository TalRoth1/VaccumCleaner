package bgu.spl.mics.application.objects;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private static class SingeltonHolder
	{
		private static GPSIMU instance = new GPSIMU(0 , new Pose(0, 0, 0, 0));
	}
    private int currentTick;
    private STATUS status;
    private List<Pose> PoseList;

    private GPSIMU(int tick, Pose initPose)
    {
        this.currentTick = tick;
        this.status = STATUS.UP;
        this.PoseList = new LinkedList<>();
        this.PoseList.add(initPose);
    }
    public static GPSIMU getInstance()
    {
        return SingeltonHolder.instance;
    }
    public STATUS getStatus() {
        return this.status;
    }
    public int getTick() {
        return this.currentTick;
    }
    public Pose getPose(int time) {
        for(Pose pos : this.PoseList)
        {
            if(pos.getTime() == time)
                return pos;
        }
        return null;
    }
}
