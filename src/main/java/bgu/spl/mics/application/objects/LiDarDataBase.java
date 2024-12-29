package bgu.spl.mics.application.objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase { //// update
    private static class SingeltonHolder
	{
		private static LiDarDataBase instance = new LiDarDataBase();
	}
    private final Map<String, List<CloudPoint>> objectCloudPoints; 
    private LiDarDataBase()
    {
        this.objectCloudPoints = new HashMap<>();
    }

    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    public static LiDarDataBase getInstance(String filePath) {
        return  SingeltonHolder.instance;
    }
    
    public synchronized void addCloudPoints(String objectId, List<CloudPoint> cloudPoints) {
        objectCloudPoints.put(objectId, cloudPoints);
    }

    /**
     * Retrieves cloud points for a specific object.
     *
     * @param objectId The unique identifier of the object.
     * @return The list of cloud points associated with the object, or null if no data exists.
     */
    public synchronized List<CloudPoint> getCloudPoints(String objectId) {
        return objectCloudPoints.get(objectId);
    }
}
