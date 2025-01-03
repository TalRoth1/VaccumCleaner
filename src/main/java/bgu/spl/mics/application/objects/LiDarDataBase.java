package bgu.spl.mics.application.objects;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class LiDarDataBase {
    // Volatile singleton instance to ensure visibility and prevent instruction reordering
    private static volatile LiDarDataBase instance = null;

    // Lock object for synchronization
    private static final Object lock = new Object();

    // List to store stamped cloud points
    private final List<StampedCloudPoints> cloudPoints;

    // Variable to store the initialization file path for consistency checks
    private String initializationPath = null;

    // Private constructor to prevent external instantiation
    private LiDarDataBase() {
        this.cloudPoints = new LinkedList<>();
    }
        /**
         * Returns the singleton instance of LiDarDataBase.
         *
         * @param filePath The path to the LiDAR data file.
         * @return The singleton instance of LiDarDataBase.
         */
        
    public static LiDarDataBase getInstance(String filePath){
        if (instance == null) { // First check (no locking)
            synchronized (lock) {
                if (instance == null) { // Second check (with locking)
                    if (filePath == null || filePath.isEmpty()) {
                        throw new IllegalArgumentException("File path must not be null or empty during initialization.");
                    }
                    instance = new LiDarDataBase();
                    instance.initialize(filePath);
                } else {
                    // Instance was initialized while waiting for the lock
                    if (!instance.getInitializationPath().equals(filePath)) {
                        throw new IllegalStateException("LiDarDataBase has already been initialized with a different file path.");
                    }
                }
            }
        } else {
            // Instance already exists, verify filePath consistency if provided
            if (filePath != null && !filePath.isEmpty() && !instance.getInitializationPath().equals(filePath)) {
                throw new IllegalStateException("LiDarDataBase has already been initialized with a different file path.");
            }
        }
        return instance;
    
    }
    private void initialize(String filePath) {
        // Store the initialization path for consistency checks
        this.initializationPath = filePath;

        Path path = Paths.get(filePath);
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(path.toFile())) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            // Parse each JSON object and populate cloudPoints
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
                this.cloudPoints.add(stampedCloudPoints);
            }

            System.out.println("LiDarDataBase initialized successfully with " + cloudPoints.size() + " entries.");
        } catch (IOException e) {
            System.err.println("Failed to read LiDAR data file: " + e.getMessage());
            e.printStackTrace();
            // Optionally, reset the instance to null if initialization fails
            synchronized (lock) {
                instance = null;
            }
        } catch (Exception e) {
            System.err.println("Error while parsing LiDAR data: " + e.getMessage());
            e.printStackTrace();
            // Optionally, reset the instance to null if initialization fails
            synchronized (lock) {
                instance = null;
            }
        }
    }
    public String getInitializationPath() {
        return this.initializationPath;
    }

            
        
    public static List<CloudPoint> getDistance(String id, int time) {
        synchronized (lock) {
            List<CloudPoint> result = new LinkedList<>();
            for (StampedCloudPoints points : getInstance("").cloudPoints) {
                if (points.getObjectId().equals(id) && points.getTimestamp() == time) {
                    result.addAll(points.getCloudPoints());
                }
            }
            return result;
        }
    }

}