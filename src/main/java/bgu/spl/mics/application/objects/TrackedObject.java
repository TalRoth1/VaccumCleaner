package bgu.spl.mics.application.objects;

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
    private CloudPoint [] coordinates;

    public String getId()
    {
        return this.id;
    }

    public CloudPoint [] getCoordinates()
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
