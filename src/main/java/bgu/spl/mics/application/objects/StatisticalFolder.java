package bgu.spl.mics.application.objects;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 */
public class StatisticalFolder { // check using atomicinteger
    private final AtomicInteger runtime;
    private final AtomicInteger numDetectedObjects;
    private final AtomicInteger numTrackedObjects;
    private final AtomicInteger numLandmarks;

   
    public StatisticalFolder() {
        this.runtime = new AtomicInteger(0);
        this.numDetectedObjects = new AtomicInteger(0);
        this.numTrackedObjects = new AtomicInteger(0);
        this.numLandmarks = new AtomicInteger(0);
    }

    public void incrementRuntime(int ticks) {
        this.runtime.addAndGet(ticks);
    }

   
    public void incrementDetectedObjects(int count) {
        this.numDetectedObjects.addAndGet(count);
    }

   
    public void incrementTrackedObjects(int count) {
        this.numTrackedObjects.addAndGet(count);
    }


    public void incrementLandmarks(int count) {
        this.numLandmarks.addAndGet(count);
    }


    public int getRuntime() {
        return runtime.get();
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects.get();
    }

    
    public int getNumTrackedObjects() {
        return numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return numLandmarks.get();
    }
}