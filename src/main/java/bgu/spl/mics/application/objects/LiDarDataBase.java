package bgu.spl.mics.application.objects;

import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


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
    public static LiDarDataBase getInstance(String filePath)
    {
        if (!filePath.isEmpty()) 
        {
            try 
            {
                Gson gson = new Gson();
                JsonArray jsonArray = gson.fromJson(new FileReader(filePath), JsonArray.class);
                // Parse each object and add to cloudPoints
                for (JsonElement element : jsonArray) {
                    JsonObject jsonObject = element.getAsJsonObject();

                    int time = jsonObject.get("time").getAsInt();
                    String id = jsonObject.get("id").getAsString();

                    List<CloudPoint> points = new LinkedList<>();
                    JsonArray cloudPointsArray = jsonObject.getAsJsonArray("cloudPoints");
                    for (JsonElement pointElement : cloudPointsArray) {
                        JsonArray point = pointElement.getAsJsonArray();
                        double x = point.get(0).getAsDouble();
                        double y = point.get(1).getAsDouble();
                        points.add(new CloudPoint(x, y));
                    }

                    StampedCloudPoints stampedCloudPoints = new StampedCloudPoints(time, id, points);
                    SingeltonHolder.instance.cloudPoints.add(stampedCloudPoints);
                }
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return SingeltonHolder.instance;
    }
    public static List<CloudPoint> getDistance(String id, int time)
    {
        List<CloudPoint> result = new LinkedList<CloudPoint>();
        for(StampedCloudPoints points : getInstance("").cloudPoints)
        {
            if (points.getObjectId() == id && points.getTimestamp() == time)
            {
                result.addAll(points.getCloudPoints());
            }
        }
        return result;
    }
}