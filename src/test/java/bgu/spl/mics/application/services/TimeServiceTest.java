package bgu.spl.mics.application.services;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import bgu.spl.mics.Callback;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.example.services.ExampleBroadcastListenerService;

public class TimeServiceTest 
{
    @Test
    public void test()
    {
        System.out.println("Check");
        TimeService timeService = new TimeService(1000, 5); // 5 ticks, 1 second each
        MicroService listenerService = new MicroService("ListenerService") 
        {
            @Override
            protected void initialize() 
            {
                subscribeBroadcast(TickBroadcast.class, tick -> {
                    System.out.println("Received tick: " + ((TickBroadcast) tick).getTick());
                });
                subscribeBroadcast(TerminatedBroadcast.class, broadcast -> 
                {
                    System.out.println("Terminate received!");
                terminate();
                });
            }
        };
    System.out.println("Check");
    Thread timeServiceThread = new Thread(timeService);
    Thread listenerServiceThread = new Thread(listenerService);
    timeServiceThread.start();
    listenerServiceThread.start();
    }
}