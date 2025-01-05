package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.ShutdownBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.STATUS;
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
    private int currentTime;
    private boolean shutdownReceived = false; // Flag to indicate shutdown

    

    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidar) {
        super("LiDarService" + lidar.getId());
        this.liDar = lidar;
        currentTime = 0;

    }

    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() 
    {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            currentTime = tick.getTick();
            if (liDar.getsStatus() == STATUS.ERROR) {
                String sensorName = "LiDar" + liDar.getId();
                sendBroadcast(new CrashedBroadcast(sensorName));
                terminate();
                return;
            }
            if (shutdownReceived) {
                FusionSlam.getInstance().serviceTerminated(getName());
                terminate();
                return;
            }
            if (liDar.getsStatus() == STATUS.DOWN) {
                sendBroadcast(new ShutdownBroadcast());
                FusionSlam.getInstance().serviceTerminated(getName());
                terminate();
                return;
            }
            int stampedTime = currentTime - liDar.getFreq();
            if (stampedTime < 0) {
                return; 
            }
            System.out.println(getName() + " currentTime: " + currentTime + ", stampedTime: " + stampedTime);
            List<TrackedObject> list = liDar.getObjects(stampedTime); 
            System.out.println(list.size()+"list tracked object size"); 
            if (!list.isEmpty()) {
                System.out.println(getName() + " preparing to send TrackedObjectsEvent with " + list.size() + " objects.");
                TrackedObjectsEvent event = new TrackedObjectsEvent(list);
                sendEvent(event);
                System.out.println(getName() + " sent TrackedObjectsEvent for object ID: ");
            }
        });
        subscribeBroadcast(ShutdownBroadcast.class, shutdown -> {
            shutdownReceived = true;
        });

        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            this.terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
        subscribeEvent(DetectObjectsEvent.class, detectEvt -> {
            System.out.println("LiDarService " + liDar.getId() + " received DetectObjectsEvent");
            int detectionTime = detectEvt.getStampedDetectedObjects().getTime();
            for(DetectedObject obje : detectEvt.getObjects())
            {
                liDar.addObject(obje, detectionTime);
            }
            complete(detectEvt,true);
        });
        System.out.println("LiDarService " + liDar.getId() + " is up");
    }
}