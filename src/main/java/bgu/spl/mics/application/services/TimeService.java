package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;

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
                for (int tick = 1; tick <= tickTime; tick++) {
                    sendBroadcast(new TickBroadcast(tick));
                    Thread.sleep(duration);
                }
                sendBroadcast(new TerminatedBroadcast());
                terminate();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        timerThread.start();
    }
}
