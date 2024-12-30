package bgu.spl.mics.application.objects;

import java.util.LinkedList;
import java.util.List;


/**
 * LiDarDataBase is a singleton class responsible for managing LiDAR data.
 * It provides access to cloud point data and other relevant information for tracked objects.
 */
public class LiDarDataBase 
{
    private static class SingeltonHolder
	{
		private static LiDarDataBase instance = new LiDarDataBase();
	}
    private LiDarDataBase()
    {
        this.cloudPoints = new LinkedList<StampedCloudPoints>();
    }
    private List<StampedCloudPoints> cloudPoints;
    /**
     * Returns the singleton instance of LiDarDataBase.
     *
     * @param filePath The path to the LiDAR data file.
     * @return The singleton instance of LiDarDataBase.
     */
    public static LiDarDataBase getInstance(String filePath) // What is the filePath for?
    {
        if (filePath != "")
        {
            
        }
        return SingeltonHolder.instance;
    }
    public static List<CloudPoint> getDistance(String id)
    {
        List<CloudPoint> result = new LinkedList<CloudPoint>();
        for(StampedCloudPoints points : getInstance("").cloudPoints)
        {
            if (points.getObjectId() == id)
            {
                result.addAll(points.getCloudPoints());
            }
        }
        return result;
    }
}
