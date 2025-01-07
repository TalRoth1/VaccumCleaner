package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.ShutdownBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.DetectedObject;
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
    @SuppressWarnings("unused")
    @Override
    protected void initialize()     
    {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            currentTime = tick.getTick();
            if (liDar.getsStatus() == STATUS.ERROR) 
            {
                String sensorName = "LiDar" + liDar.getId();
                sendBroadcast(new CrashedBroadcast(sensorName));
                terminate();
                return;
            }
            if (liDar.getsStatus() == STATUS.DOWN)
            {
                sendBroadcast(new TerminatedBroadcast(this.getName()));
                sendBroadcast(new ShutdownBroadcast());
                terminate();
                System.out.println("shoutdown suppose to be sent");
                return;
            }
            List<TrackedObject> list = liDar.getObjects(currentTime); 
            if (list == null) {
                return;
            }
            if (!list.isEmpty()) {
                System.out.println(getName() +"send TrackedObjectsEvent with " + list.size() + " objects. at time : " +currentTime);
                TrackedObjectsEvent event = new TrackedObjectsEvent(list);
                sendEvent(event);
            }
        });
        subscribeBroadcast(ShutdownBroadcast.class, shutdown -> {
            sendBroadcast(new TerminatedBroadcast(this.getName()));
            terminate();
            System.out.println("shoutdown recieved");
        });
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            if(term.getServiceName().equals("TimeService")){
                sendBroadcast(new TerminatedBroadcast(this.getName()));
                terminate();
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
        subscribeEvent(DetectObjectsEvent.class, detectEvt -> {
            System.out.println(getName() + " received DetectObjectsEvent.");
            int detectionTime = detectEvt.getStampedDetectedObjects().getTime();

            for (DetectedObject obj : detectEvt.getObjects()) {
                if (obj == null) {
                    System.out.println(getName() + ": Encountered null DetectedObject in DetectObjectsEvent.");
                    continue;
                }
                try {
                    liDar.addObject(obj, detectionTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            complete(detectEvt, true);
            System.out.println(getName() + " successfully processed DetectObjectsEvent. for detection time" + detectionTime);
        });

        System.out.println("LiDarService " + liDar.getId() + " is up and running.");
    }
}