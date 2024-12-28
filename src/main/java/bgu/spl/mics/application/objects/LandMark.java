package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a landmark in the environment map.
 * Landmarks are identified and updated by the FusionSlam service.
 */
public class LandMark {
    private final String id;
    private final String description;
    private final List<double[]> coordinates;

    public LandMark(String id, String description, double[] transformedCoordinates) 
    {
        this.id = id;
        this.description = description;
        this.coordinates = new ArrayList<>();
        this.coordinates.add(transformedCoordinates);
    }

    public void updateCoordinates(double[] transformedCoordinates)
    {
        this.coordinates.add(transformedCoordinates);
    }
    public String getId()
    {
        return id;
    }

    public String getDescription()
    {
        return description;
    }

    public List<double[]> getCoordinates()
    {
        return coordinates;
    }
}
