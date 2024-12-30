package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.List;

import bgu.spl.mics.MicroService;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 * 
 * This service interacts with the LiDarWorkerTracker object to retrieve and process
 * cloud point data and updates the system's StatisticalFolder upon sending its
 * observations.
 */
public class LiDarService extends MicroService {
    private final LiDarWorkerTracker liDar;
    private int time;

    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidar) {
        super("LiDarService" + lidar.getId());
        this.liDar = lidar;
    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            this.time = tick.getTick();
            List<TrackedObject> list = liDar.getObjects(time - liDar.getFreq()); 
            if(list.size() != 0)
            {
                for(TrackedObject obj : list)
                {
                    TrackedObjectsEvent.addObject(obj);
                }
                sendEvent(TrackedObjectsEvent.getInstance());
            }
        });
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            this.terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
        subscribeEvent(DetectObjectsEvent.class, obj -> {
            liDar.addObject(obj.getObject(), time);
        });
    }
}
