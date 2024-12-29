package bgu.spl.mics.application.objects;

import java.util.LinkedList;
import java.util.List;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker 
{
    private int id;
    private int frequency;
    private STATUS status;
    private List<TrackedObject> lastTrackedObjects;
    private final StatisticalFolder statisticalFolder;
    private final LiDarDataBase liDarDataBase;

    public LiDarWorkerTracker(int id, int freq, List<TrackedObject> trackedObjects, StatisticalFolder statisticalFolder, LiDarDataBase liDarDataBase)
    {
        this.id = id;
        this.frequency = freq;
        this.status = STATUS.UP;
        this.lastTrackedObjects = trackedObjects;
        this.statisticalFolder = statisticalFolder;
        this.liDarDataBase = liDarDataBase;
    }
    protected void initialize() {
        subscribeEvent(DetectObjectsEvent.class, event -> {
            processDetectObjectsEvent(event);
        });
    }
    public int getId()
    {
        return this.id;
    }
    public int getFreq()
    {
        return this.frequency;
    }
    public STATUS getsStatus()
    {
        return this.status;
    }
    public void addObject(DetectedObject obj) // Need to check what they want us to do here
    {
        boolean exists = false;
        for (TrackedObject object : lastTrackedObjects)
        {
            if(object.getId().equals(obj.getId()))
                exists = true;
        }
        if(!exists)
            System.out.print("Adding a nonexistant object");
    }
    public List<TrackedObject> getObjects(int time)
    {
        List<TrackedObject> result = new LinkedList<TrackedObject>();
        for(TrackedObject obj : lastTrackedObjects)
        {
            if(obj.getTime() == time)
                result.add(obj);
        }
        return result;
    }
   

    /**
     * Processes a DetectObjectsEvent by retrieving cloud points from the database
     * and sending a TrackedObjectsEvent to the FusionSLAM service.
     *
     * @param event The DetectObjectsEvent containing detection parameters.
     */
    private void processDetectObjectsEvent(DetectObjectsEvent event) {
        // Retrieve cloud points based on the detection parameters
        List<CloudPoint> cloudPoints = liDarDataBase.getCloudPoints(event.getDetectionParameters());

        if (cloudPoints == null || cloudPoints.isEmpty()) {
            return; // No data to process
        }

        // Create a TrackedObjectsEvent with the retrieved cloud points
        TrackedObjectsEvent trackedObjectsEvent = new TrackedObjectsEvent(cloudPoints);

        // Send the event to the FusionSLAM service
        sendEvent(trackedObjectsEvent);

        // Update the StatisticalFolder
        statisticalFolder.incrementTrackedObjects(cloudPoints.size());
    }
    public int getTime() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTime'");
    }
}
