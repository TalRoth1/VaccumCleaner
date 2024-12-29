package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.messages.TrackedObjectsEvent;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.StatisticalFolder;

import java.util.LinkedList;
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
    private final StatisticalFolder folder;
    private int currentTime = 0;
    private final List<DetectObjectsEvent> pendingEvents;



    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidar, StatisticalFolder folder) {
        super("LiDarService" + lidar.getId());
        this.liDar = lidar;
        this.folder = folder;
        this.pendingEvents = new LinkedList<>();

        
    }
    /**
     * Initializes the LiDarService.
     * Registers the service to handle DetectObjectsEvents and TickBroadcasts,
     * and sets up the necessary callbacks for processing data.
     */
    @Override
    protected void initialize() {
        subscribeBroadcast(TickBroadcast.class, tick -> {
            this.currentTime = tick.getTick();
        });
        
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            this.terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
        subscribeEvent(DetectObjectsEvent.class, event -> {
            // Instead of processing immediately, store it for later
            pendingEvents.add(event);
        });
    }
    private void processPendingEvents() {
        List<DetectObjectsEvent> toRemove = new LinkedList<>();
        for (DetectObjectsEvent event : pendingEvents) {
            int detectionTime = event.getDetectionTime(); 
            if (currentTime >= detectionTime + liDar.getFreq()) {
                boolean processedOK = handleDetectObjectsEvent(event);
                complete(event, processedOK);
                // Mark event as processed
                toRemove.add(event);
            }
        }
        // Remove processed events from the pending list
        pendingEvents.removeAll(toRemove);
    }
     private boolean handleDetectObjectsEvent(DetectObjectsEvent event) {
        TrackedObjectsEvent trackedEvent = liDar.processDetectObjectsEvent(event);

        if (trackedEvent == null) {
            return false;
        }

        sendEvent(trackedEvent);

        // Update statistics:
        //   - We increment "tracked objects" count by how many objects or points were found
        //   - Or if your requirement is to increment by 1 per event, do that here
        folder.incrementTrackedObjects(trackedEvent.getTrackedObjectsCount());

        return true;
    }
}
