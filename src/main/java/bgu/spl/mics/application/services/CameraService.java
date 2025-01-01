package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.STATUS;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

import java.util.List;
import bgu.spl.mics.MicroService;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService { //// updates
    private final Camera cam;
    private int time;
    /**
     * Constructor for CameraService.
     *
     * @param camera The Camera object that this service will use to detect objects.
     */
    public CameraService(Camera camera) 
    {
        super("Camera" + camera.getId());
        this.cam = camera;
    }

    /**
     * Initializes the CameraService.
     * Registers the service to handle TickBroadcasts and sets up callbacks for sending
     * DetectObjectsEvents.
     */
    @Override
    protected void initialize()
    {
 
        subscribeBroadcast(TickBroadcast.class, tick -> {
            System.out.println("CameraService " + cam.getId() + " got tick " + tick.getTick());
            this.time = tick.getTick();
            if (cam.getStat() == STATUS.ERROR)
            {
                // Broadcast a crash
                String sensorName = "Camera" + cam.getId();
                sendBroadcast(new CrashedBroadcast(sensorName));
                terminate();
                return;
            }
            if (cam.getStat() == STATUS.DOWN)
            {
                // No more data => normal termination
                FusionSlam.getInstance().serviceTerminated(getName());
                terminate();
                return;
            }

            int stampedTime = time - cam.getFreq();
            if(stampedTime < 0)
            {
                return;
            }
            List<DetectedObject> lst = cam.getObjects(stampedTime);
            if(lst != null  && !lst.isEmpty())
            {
                StampedDetectedObjects detectedObjects = new StampedDetectedObjects(stampedTime);
                detectedObjects.addObjects(lst);
                DetectObjectsEvent event = new DetectObjectsEvent(detectedObjects, time);
                sendEvent(event);    
            }
        }); 
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            FusionSlam.getInstance().serviceTerminated(getName());
            terminate();
            this.terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
        System.out.println("CameraService " + cam.getId() + " is up");
    }
}