package bgu.spl.mics.application.objects;


/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder {
    private int runtime;
    private int numDetectedObjects;
    private int numTrackedObjects;
    private int numLandmarks;

    /**
     * Constructor for StatisticalFolder.
     * Initializes all metrics to zero.
     */
    public StatisticalFolder() {
        this.runtime = 0;
        this.numDetectedObjects = 0;
        this.numTrackedObjects = 0;
        this.numLandmarks = 0;
    }

    /**
     * Increments the runtime by a specified number of ticks.
     * @param ticks Number of ticks to add.
     */
    public synchronized void incrementRuntime(int ticks) {
        this.runtime += ticks;
    }

    /**
     * Increments the number of detected objects.
     * @param count Number of objects to add.
     */
    public synchronized void incrementDetectedObjects(int count) {
        this.numDetectedObjects += count;
    }

    /**
     * Increments the number of tracked objects.
     * @param count Number of objects to add.
     */
    public synchronized void incrementTrackedObjects(int count) {
        this.numTrackedObjects += count;
    }

    /**
     * Increments the number of landmarks.
     * @param count Number of landmarks to add.
     */
    public synchronized void incrementLandmarks(int count) {
        this.numLandmarks += count;
    }

    /**
     * @return The total runtime of the system in ticks.
     */
    public synchronized int getRuntime() {
        return runtime;
    }

    /**
     * @return The total number of detected objects.
     */
    public int getNumDetectedObjects() {
        return numDetectedObjects;
    }

    /**
     * @return The total number of tracked objects.
     */
    public int getNumTrackedObjects() {
        return numTrackedObjects;
    }

    /**
     * @return The total number of landmarks.
     */
    public int getNumLandmarks() {
        return numLandmarks;
    }
}