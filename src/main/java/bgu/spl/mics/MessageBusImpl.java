package bgu.spl.mics;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import bgu.spl.mics.application.messages.TerminatedBroadcast;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	private static class SingeltonHolder
	{
		private static MessageBusImpl instance = new MessageBusImpl();
	}
	private Map<Class<? extends Message>, Queue<MicroService>> Esubscribers;
	private Map<Class<? extends Message>, Queue<MicroService>> Bsubscribers;
	private Map<MicroService, BlockingQueue<Message>> queues; 
	private Map<Event<?>, Future<?>> futures;
	private MessageBusImpl()
	{
		Esubscribers = new ConcurrentHashMap<>();
		Bsubscribers = new ConcurrentHashMap<>();
		queues = new ConcurrentHashMap<>();
		futures = new ConcurrentHashMap<>();
	}
	public static MessageBusImpl getInstance()
	{
		return SingeltonHolder.instance;
	}
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) 
	{
		if(!Esubscribers.containsKey(type))
			Esubscribers.put(type, new LinkedList<MicroService>());
		Esubscribers.get(type).add(m);
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m)
	{
		if(!Bsubscribers.containsKey(type))
			Bsubscribers.put(type, new LinkedList<MicroService>());
		Bsubscribers.get(type).add(m);
	}

	@Override
	public <T> void complete(Event<T> e, T result)
	{
		Future<T> future = null;
		if(futures.containsKey(e))
			future = (Future<T>)futures.get(e);
		if(future != null)
			future.resolve(result);
	}

	@Override
	public void sendBroadcast(Broadcast b) 
	{
		Queue<MicroService> ms = Bsubscribers.get(b.getClass());
		if(ms == null)
			return;
		for (MicroService m : ms) 
		{
			if(m != null)
				queues.get(m).offer(b);
		}
	}
	@Override
	public <T> Future<T> sendEvent(Event<T> e) 
	{
		Queue<MicroService> ms = Esubscribers.get(e.getClass());
		if(ms == null || ms.isEmpty())
			return null;
		MicroService head = ms.poll();
		if (head == null)
			return null;
		BlockingQueue<Message> que = queues.get(head);
		if(que == null || que.isEmpty())
			return null;
		ms.offer(head);
		que.offer(e);
		Future<T> future = new Future<>();
		futures.put(e, future);
		return future;
	}

	@Override
	public void register(MicroService m)
	{
		if(m == null)
			return;
		if(!queues.containsKey(m))
			queues.put(m, new LinkedBlockingQueue<Message>());
		Bsubscribers.get(TerminatedBroadcast.class).add(m);
	}

	@Override
	public void unregister(MicroService m) 
	{
		if(m == null)
			return;
		if(queues.containsKey(m))
		{
			queues.remove(m);
			Esubscribers.values().forEach(queue -> queue.remove(m));
			Bsubscribers.values().forEach(queue -> queue.remove(m));
		}
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException
	{
		if(m == null || !queues.containsKey(m))
			throw new IllegalStateException();
		BlockingQueue<Message> queue = queues.get(m);
		return queue.take();
	}
}
