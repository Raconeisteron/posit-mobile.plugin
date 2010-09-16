package org.hfoss.adhoc;

import java.util.concurrent.ArrayBlockingQueue;

public class Queues {
	
	public static ArrayBlockingQueue<AdhocData> outputQueue = new ArrayBlockingQueue<AdhocData>(50);
	public static ArrayBlockingQueue<AdhocData> inputQueue  = new ArrayBlockingQueue<AdhocData>(50);
}
