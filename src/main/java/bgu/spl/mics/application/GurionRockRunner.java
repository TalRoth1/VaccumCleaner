package bgu.spl.mics.application;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.Pose;
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
        String configPath = args[0];
        String [] arg = configPath.split("/");
        String path = "";
        for (int i = 0; i<arg.length - 1; i++)
        {
            path += arg[i]+"/";
        }
        String cameraPath = path;
        String liDarPath = path;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Camera> cameras = new ArrayList<>();
        List<LiDarWorkerTracker> lidarWorkers = new ArrayList<>();



        try (FileReader reader = new FileReader(configPath))
        {
            // Parse JSON into Config class
            Config config = gson.fromJson(reader, Config.class);

            // Print parsed data
            for (CameraConfiguration camCongif : config.Cameras.CamerasConfigurations)
            {
                cameras.add(new Camera(camCongif.id, camCongif.frequency));
            }
            cameraPath += config.Cameras.camera_datas_path;
            for (LidarConfiguration lidar : config.Lidars.LidarConfigurations) 
            {
                lidarWorkers.add(new LiDarWorkerTracker(lidar.id, lidar.frequency));
            }
            liDarPath += config.Lidars.lidars_data_path;
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }




        try
        {
            Type type = new TypeToken<Map<String, List<List<StampedDetectedObjects>>>>() {}.getType();
            Map<String, List<List<StampedDetectedObjects>>> dataMap = gson.fromJson(new FileReader(cameraPath), type);

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


        LiDarDataBase.getInstance(liDarPath);





        Map<String, Object>info = new HashMap<>();
        boolean error = false;
        String errorObj = "";
        //Check if an error accured and update errorObj if a sensor did cause an error
        if (error)
        {
            info.put("Error", errorObj + "disconnected");
            info.put("faultySensor", errorObj);

            info.put("lastFrames", ); // Dont understand how to implement
            Pose [] poses = new Pose[StatisticalFolder.getRuntime()];
            for(int i = 0; i < poses.length; i++)
            {
                poses[i] = GPSIMU.getInstance().getPose(i);
            }
            info.put("Poses", poses);
        }
        else
        {
            LandMark [] landMarks = new LandMark[StatisticalFolder.getLandmarks()];
            //Put all landmarks into the array
            info.put("WorldMap", landMarks);
        }
        info.put("systemRuntime", StatisticalFolder.getRuntime()); // add all the nececary information.
        info.put("numDetectedObjects", StatisticalFolder.getDetectedObjects());
        info.put("numTrackedObjects", StatisticalFolder.getTrackedObjects());
        info.put("numLandmarks", StatisticalFolder.getLandmarks());

        
        try (FileWriter writer = new FileWriter(path + "output_file.json"))
        {
            gson.toJson(info, writer);
        } 
        catch (IOException e) 
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