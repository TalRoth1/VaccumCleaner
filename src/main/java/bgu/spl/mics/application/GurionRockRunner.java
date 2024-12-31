package bgu.spl.mics.application;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.StampedDetectedObjects;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * <p>
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 * </p>
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * This method sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) //gets one arg, the path to config file 
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Camera> cameras = new ArrayList<>();
        @SuppressWarnings("unused")
        List<LiDarWorkerTracker> lidarWorkers = new ArrayList<>();
        
        // add - to the StatisticalFolder - every camera and lidar need fet stats in the constructos
        StatisticalFolder stats = new StatisticalFolder();
        FusionSlam slam = FusionSlam.getInstance();
        int total=0;// need to be update for  number of cameras+ lidars+ gps and imu 
        slam.setStatisticalFolder(stats);
        slam.setTotalMicroservices(total); 

        /*try (FileReader reader = new FileReader(args[0]))
        {
            // Parse JSON into Config class
            Config config = gson.fromJson(reader, Config.class);

            // Print parsed data
            for (CameraConfiguration camCongif : config.Cameras.CamerasConfigurations)
            {
                cameras.add(new Camera(camCongif.id, camCongif.frequency));
            }
            for (LidarConfiguration lidar : config.Lidars.LidarConfigurations) 
            {
                lidarWorkers.add(new LiDarWorkerTracker(lidar.id, lidar.frequency));
            }
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        */

        try
        {
            Type type = new TypeToken<Map<String, List<List<StampedDetectedObjects>>>>() {}.getType();
            Map<String, List<List<StampedDetectedObjects>>> dataMap = gson.fromJson(new FileReader("Path/to/camera/config.json"), type);

            // Map data to cameras
            for (Camera camera : cameras) {
                String cameraKey = "camera" + camera.getId();
                if (dataMap.containsKey(cameraKey)) {
                    // Flatten nested lists
                    List<StampedDetectedObjects> flattenedData = new ArrayList<>();
                    for (List<StampedDetectedObjects> nestedList : dataMap.get(cameraKey)) {
                        flattenedData.addAll(nestedList);
                    }
                    // Add data to the camera
                    for (StampedDetectedObjects object : flattenedData) {
                        camera.addObjects(object.getObjects(), object.getTime());
                    }
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    

    // Classes to represent JSON structure
    static class CameraConfiguration {
        int id;
        int frequency;
        String camera_key;
    }

    static class LidarConfiguration {
        int id;
        int frequency;
    }

    static class Cameras {
        List<CameraConfiguration> CamerasConfigurations;
        String camera_datas_path;
    }

    static class Lidars {
        List<LidarConfiguration> LidarConfigurations;
        String lidars_data_path;
    }

    static class Config {
        Cameras Cameras;
        Lidars Lidars;
        String poseJsonFile;
        int TickTime;
        int Duration;
    }
}