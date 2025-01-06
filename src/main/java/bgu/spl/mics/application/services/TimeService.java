package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.StatisticalFolder;

/**
 * TimeService acts as the global timer for the system, broadcasting TickBroadcast messages
 * at regular intervals and controlling the simulation's duration.
 */
public class TimeService extends MicroService
{
    private int tickTime;
    private int duration;

    /**
     * Constructor for TimeService.
     *
     * @param TickTime  The duration of each tick in milliseconds.
     * @param Duration  The total number of ticks before the service terminates.
     */
    public TimeService(int TickTime, int Duration)
    {
        super("TimeService");
        this.tickTime = TickTime;
        this.duration = Duration;
    }

    /**
     * Initializes the TimeService.
     * Starts broadcasting TickBroadcast messages and terminates after the specified duration.
     */
    @Override
    protected void initialize() // updated
    {
        Thread timerThread = new Thread(() -> {
            try {
                for (int tick = 1; tick <= duration; tick++) 
                {
                    StatisticalFolder.getInstance().incrementRuntime(1);
                    sendBroadcast(new TickBroadcast(tick));
                    Thread.sleep(tickTime);
                }
                sendBroadcast(new TerminatedBroadcast(this.getName()));
                terminate();
            } 
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        subscribeBroadcast(TerminatedBroadcast.class, terminate -> {
            if ("FusionSlamService".equals(terminate.getServiceName())) {
                System.out.println("time service stop becuse fusionslam");
                timerThread.interrupt();
                this.terminate();
            }
        });
        subscribeBroadcast(CrashedBroadcast.class, crash -> {
            timerThread.interrupt();
            this.terminate();
        });
        timerThread.start();
        System.out.println("TimeService is up");
    }
}
