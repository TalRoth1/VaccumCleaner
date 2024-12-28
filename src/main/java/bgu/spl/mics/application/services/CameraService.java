package bgu.spl.mics.application.services;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.STATUS;

import java.util.List;
import bgu.spl.mics.MicroService;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 * 
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
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
            this.time = tick.getTick();
            if (cam.getStat() != STATUS.UP)
            {
                terminate();
                return;
            }
            List<DetectedObject> lst = cam.getObjects(time - cam.getFreq());
            if(lst != null)
            {
                for(DetectedObject obj : lst)
                {
                    sendEvent(new DetectObjectsEvent(obj));
                }
            }
        }); 
        subscribeBroadcast(TerminatedBroadcast.class, term -> {
            this.terminate();
        });
        subscribeBroadcast(CrashedBroadcast.class, crash ->{
            this.terminate();
        });
    }
}
