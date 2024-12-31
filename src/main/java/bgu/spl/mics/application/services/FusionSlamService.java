package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.TrackedObject;
import java.util.List;

import bgu.spl.mics.MicroService;

/**
 * FusionSlamService integrates data from multiple sensors to build and update
 * the robot's global map.
 * 
 * This service receives TrackedObjectsEvents from LiDAR workers and PoseEvents from the PoseService,
 * transforming and updating the map with new landmarks.
 */
public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;
    private Pose currentPose;

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlamService");
        this.fusionSlam = fusionSlam;
        this.currentPose = null;
    }
    /**
     * Initializes the FusionSlamService.
     * Registers the service to handle TrackedObjectsEvents, PoseEvents, and TickBroadcasts,
     * and sets up callbacks for updating the global map.
     */
   
     @Override
    protected void initialize() {
        subscribeEvent(TrackedObjectsEvent.class, event -> {
        List<TrackedObject> trackedObjects = event.getTrackedObjects();
        if (trackedObjects == null || trackedObjects.isEmpty()) {
            return;
        }
        fusionSlam.processTrackedObjects(trackedObjects, currentPose);
        });

        subscribeEvent(PoseEvent.class, poseEvent -> {
            this.currentPose = poseEvent.getPose();
            fusionSlam.addPose(this.currentPose);

        });

        subscribeBroadcast(TickBroadcast.class, tick -> {
            fusionSlam.updateTick(tick.getTick());
        });
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            terminate();
        });

        subscribeBroadcast(CrashedBroadcast.class, crash -> {
            terminate();
        });
        

    }         

}
