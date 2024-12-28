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

    public double[] getCoordinates() 
    {
        double [] result = new double[2];
        result[0] = coordinates[0].getX();
        result[1] = coordinates[0].getY();
        return result;
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
