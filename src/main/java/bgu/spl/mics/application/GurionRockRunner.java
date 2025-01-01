package bgu.spl.mics.application;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import bgu.spl.mics.application.objects.Camera;
import bgu.spl.mics.application.objects.DetectedObject;
import bgu.spl.mics.application.objects.FusionSlam;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.LandMark;
import bgu.spl.mics.application.objects.LiDarDataBase;
import bgu.spl.mics.application.objects.LiDarWorkerTracker;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.StatisticalFolder;
import bgu.spl.mics.application.services.CameraService;
import bgu.spl.mics.application.services.FusionSlamService;
import bgu.spl.mics.application.services.LiDarService;
import bgu.spl.mics.application.services.PoseService;
import bgu.spl.mics.application.services.TimeService;

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
        String configPath = "D:\\Projects\\SPL\\Vaccum Cleaner\\example_input_2\\configuration_file.json";
        String [] arg = configPath.split("\\\\");
        String path = "";
        for (int i = 0; i<arg.length - 1; i++)
        {
            path += arg[i]+"\\";
        }
        String cameraPath = path;
        String liDarPath = path;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        List<Camera> cameras = new ArrayList<>();
        List<LiDarWorkerTracker> lidarWorkers = new ArrayList<>();
        Config config = null;


        try (FileReader reader = new FileReader(configPath))
        {
            // Parse JSON into Config class
            config = gson.fromJson(reader, Config.class);

            // Print parsed data
            for (CameraConfiguration camCongif : config.Cameras.CamerasConfigurations)
            {
                cameras.add(new Camera(camCongif.id, camCongif.frequency));
            }
            cameraPath += config.Cameras.camera_datas_path.substring(2);
            for (LidarConfiguration lidar : config.LiDarWorkers.LidarConfigurations)
            {
                lidarWorkers.add(new LiDarWorkerTracker(lidar.id, lidar.frequency));
            }
            liDarPath += config.LiDarWorkers.lidars_data_path.substring(2);
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }


        try(FileReader reader = new FileReader(cameraPath))
        {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            for (Camera camera : cameras)
            {
                String cameraKey = "camera" + camera.getId();
                JsonArray cameraData = root.getAsJsonArray(cameraKey); 
                JsonArray objPerTime = cameraData.getAsJsonArray();
                for (JsonElement cameraObject: objPerTime) 
                {
                    List<DetectedObject> objects = new ArrayList<>();
                    JsonObject obj = cameraObject.getAsJsonObject();
                    int time = obj.get("time").getAsInt();
                    JsonArray detectedObjects = obj.getAsJsonArray("detectedObjects");
                    for (JsonElement objectElement : detectedObjects) 
                    {
                        JsonObject obje = objectElement.getAsJsonObject();
                        String id = obje.get("id").getAsString();
                        String description = obje.get("description").getAsString();
                        objects.add(new DetectedObject(id, description));
                    }
                    camera.addObjects(objects, time);
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }


        LiDarDataBase.getInstance(liDarPath);

        // Add Pose data


        // Start the simulation
        FusionSlam.getInstance().setTotalMicroservices(cameras.size() + lidarWorkers.size() + 1);
        for (Camera camera : cameras)
        {
            CameraService cameraService = new CameraService(camera);
            Thread thread = new Thread(cameraService);
            thread.start();
        }
        for (LiDarWorkerTracker liDar : lidarWorkers)
        {
            LiDarService liDarService = new LiDarService(liDar);
            Thread thread = new Thread(liDarService);
            thread.start();
        }
        PoseService poseService = new PoseService(GPSIMU.getInstance());
        Thread thread = new Thread(poseService);
        thread.start();
        FusionSlamService fusionSlamService = new FusionSlamService(FusionSlam.getInstance());
        Thread fusionThread = new Thread(fusionSlamService);
        fusionThread.start();
        TimeService timeService = new TimeService(config.TickTime,config.Duration);
        Thread timThread = new Thread(timeService);
        timThread.start();



        Map<String, Object>info = new HashMap<>();
        boolean error = FusionSlam.getInstance().isErrorOccurred();
        String errorObj = FusionSlam.getInstance().getFaultySensor();
        if (error)
        {
            info.put("Error", errorObj + "disconnected");
            info.put("faultySensor", errorObj);

            info.put("lastFrames", "test"); // Dont understand how to implement
            Pose [] poses = new Pose[StatisticalFolder.getInstance().getRuntime()];
            for(int i = 0; i < poses.length; i++)
            {
                poses[i] = GPSIMU.getInstance().getPose(i);
            }
            info.put("Poses", poses);
        }
        else
        {
            LandMark [] landMarks = new LandMark[StatisticalFolder.getInstance().getNumLandmarks()];
            Iterator<LandMark> it = FusionSlam.getInstance().getLandmarks().iterator();
            for (int i = 0; i < landMarks.length; i++)
            {
                landMarks[i] = it.next();
            }
            for(LandMark landMark : landMarks)
            {
                info.put("WorldMap", landMark.toString());
            }
        }
        info.put("systemRuntime", StatisticalFolder.getInstance().getRuntime()); // add all the nececary information.
        info.put("numDetectedObjects", StatisticalFolder.getInstance().getNumDetectedObjects());
        info.put("numTrackedObjects", StatisticalFolder.getInstance().getNumTrackedObjects());
        info.put("numLandmarks", StatisticalFolder.getInstance().getNumLandmarks());

        
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
        Lidars LiDarWorkers;
        String poseJsonFile;
        int TickTime;
        int Duration;
    }
}