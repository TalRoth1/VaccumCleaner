package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.CloudPoint;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.StampedCloudPoints;
import bgu.spl.mics.application.objects.TrackedObject;

import java.util.ArrayList;
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
            for (TrackedObject obj : trackedObjects) {
                List<CloudPoint> transformedPoints = new ArrayList<>();
                List<CloudPoint> coordinates = obj.getCoordinates();
                for (CloudPoint point : coordinates) {
                    CloudPoint newPoint = transformCoordinate(point, currentPose);
                    transformedPoints.add(newPoint);
                }
                fusionSlam.updateLandmark(obj.getId(), obj.getDescription(), transformedPoints);
            }

        });
        subscribeEvent(PoseEvent.class, event -> {
            this.currentPose = event.getPose();
            fusionSlam.addPose(this.currentPose);
        });
        subscribeBroadcast(TickBroadcast.class, tick -> {
            fusionSlam.updateTick(tick.getTick());
        });
        

    }     
    private CloudPoint transformCoordinate(CloudPoint TrackedObject,Pose pose)
    {
        CloudPoint result = new CloudPoint(TrackedObject.getX(),TrackedObject.getY());
        double x = result.getX();
        double y = result.getY();
        double yawRadians = Math.toRadians(pose.getYaw());////****** can we use this method??  */
        double transformedX = x * Math.cos(yawRadians) - y * Math.sin(yawRadians) + pose.getX();
        double transformedY = x * Math.sin(yawRadians) + y * Math.cos(yawRadians) + pose.getY();
        result = new CloudPoint(transformedX, transformedY);
        return result; 
    }

    

}
