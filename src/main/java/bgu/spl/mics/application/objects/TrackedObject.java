package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description, 
 * time of tracking, and coordinates in the environment.
 */
public class TrackedObject
{
    private String id;
    private int time;
    private String description;
    private List<CloudPoint> coordinates;

    public TrackedObject(String id, String description, List<CloudPoint> cloudPoints) {
        this.id = id;
        this.description = description;
        this.coordinates = cloudPoints;
    }
    
    public String getId()
    {
        return this.id;
    }

    public List<CloudPoint> getCoordinates()
    {
        return coordinates;
    }
    public int getTime()
    {
        return this.time;
    }
    public String getDescription()
    {
        return this.description;
    }
}
