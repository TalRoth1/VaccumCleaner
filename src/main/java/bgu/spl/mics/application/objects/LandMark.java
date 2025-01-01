package bgu.spl.mics.application.objects;

import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark 
{
    private final String id;
    private final String description;
    private List<CloudPoint> coordinates;

    public LandMark(String id, String description, List<CloudPoint> newcord) 
    {
        this.id = id;
        this.description = description;
        this.coordinates = newcord;
    }
    
    public String getId()
    {
        return id;
    }

    public String getDescription()
    {
        return description;
    }

    public List<CloudPoint> getCoordinates()
    {
        return coordinates;
    }

    public void setCoordinates(List<CloudPoint> newcord)
    {
        coordinates = newcord;
    }

    public String toString()
    {
        return "Landmark " + id + ": " + description + " at " + coordinates;
    }
}