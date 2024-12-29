package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.StatisticalFolder;
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
    private final StatisticalFolder folder;


    /**
     * Constructor for LiDarService.
     *
     * @param LiDarWorkerTracker A LiDAR Tracker worker object that this service will use to process data.
     */
    public LiDarService(LiDarWorkerTracker lidar, StatisticalFolder folder) {
        super("LiDarService" + lidar.getId());
        this.liDar = lidar;
        this.folder = folder;
        
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
        });
        
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            this.terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
        subscribeEvent(DetectObjectsEvent.class, event -> {
            handleDetectObjectsEvent(event);
        });
        
        /*subscribeEvent(DetectObjectsEvent.class, event -> {
            // Process the detected object and add it to the tracked list
            TrackedObject trackedObject = liDar.processDetectedObject(event.getObject());
            if (trackedObject != null) {
                trackedObjects.add(trackedObject);
            }
        });*/
    }
    private void handleDetectObjectsEvent(DetectObjectsEvent event) {
        int detectionTime = event.getDetectionTime();
        if (liDar.getTime() < detectionTime + liDar.getFreq()) {
            return; // Wait until the correct tick///// suppose do wait here? 
        }

      
    }
}
