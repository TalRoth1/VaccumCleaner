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
    private final List<CloudPoint> coordinates;

    public LandMark(String id, String description, CloudPoint [] transformedCoordinates) 
    {
        this.id = id;
        this.description = description;
        this.coordinates = new ArrayList<>();
        for(CloudPoint point : transformedCoordinates)
            this.coordinates.add(point);
    }

    public void updateCoordinates(CloudPoint [] transformedCoordinates)
    {
        for(CloudPoint point : transformedCoordinates)
            this.coordinates.add(point);
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
}
