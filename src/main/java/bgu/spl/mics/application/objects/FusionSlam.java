package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    // Singleton instance holder
    private static class FusionSlamHolder
    {
        private static FusionSlam instance = new FusionSlam();
    }
    

public static FusionSlam getInstance() {
    return FusionSlamHolder.instance;
}
private final Map<String, LandMark> landmarks;
private int currentTick;

private FusionSlam() {
    this.landmarks = new HashMap<>();
    this.currentTick = 0;
}
public void updateLandmark(TrackedObject trackedObject, Pose pose) {
    if (pose == null || trackedObject == null) {
        return;
    }

    String id = trackedObject.getId();
    double[] transformedCoordinates = transformCoordinates(trackedObject.getCoordinates(), pose);

    if (landmarks.containsKey(id)) {
        // Update existing landmark
        LandMark landmark = landmarks.get(id);
        landmark.updateCoordinates(transformedCoordinates);
    } else {
        // Add new landmark
        landmarks.put(id, new LandMark(id, trackedObject.getDescription(), transformedCoordinates));
    }
}

/**
 * Updates the current simulation tick.
 *
 * @param tick The current tick.
 */
public void updateTick(int tick) {
    this.currentTick = tick;
}

/**
 * Transforms the coordinates of the tracked object based on the robot's pose.
 *
 * @param coordinates The original coordinates of the object.
 * @param pose        The robot's current pose.
 * @return Transformed coordinates in the global frame.
 */
private double[] transformCoordinates(double[] coordinates, Pose pose) {
    double x = coordinates[0];
    double y = coordinates[1];
    double transformedX = x * Math.cos(pose.getYaw()) - y * Math.sin(pose.getYaw()) + pose.getX();
    double transformedY = x * Math.sin(pose.getYaw()) + y * Math.cos(pose.getYaw()) + pose.getY();
    return new double[]{transformedX, transformedY};
    

/**
 * @return The list of all landmarks.
 */
public List<Landmark> getLandmarks() {
    return new ArrayList<>(landmarks.values());
}

/**
 * @return The current simulation tick.
 */
public int getCurrentTick() {
    return currentTick;
}

/**
 * Landmark class representing a point in the map.
 */
public static class Landmark {
    private final String id;
    private final String description;
    private final List<double[]> coordinates;

    public Landmark(String id, String description, double[] initialCoordinates) {
        this.id = id;
        this.description = description;
        this.coordinates = new ArrayList<>();
        this.coordinates.add(initialCoordinates);
    }

    public void updateCoordinates(double[] newCoordinates) {
        this.coordinates.add(newCoordinates);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public List<double[]> getCoordinates() {
        return coordinates;
    }
}
}
