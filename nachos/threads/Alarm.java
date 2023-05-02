package nachos.threads;

import nachos.machine.*;

import java.util.Map;
import java.util.TreeMap;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */

	private TreeMap<Long, KThread> sleptThreadQueue = new TreeMap<Long, KThread>();

	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		// KThread.currentThread().yield();
		long currentTime = Machine.timer().getTime();

		while(!sleptThreadQueue.isEmpty() && currentTime >= sleptThreadQueue.firstKey()) {
			KThread nextAwKThread = sleptThreadQueue.pollFirstEntry().getValue();
			nextAwKThread.ready();
		}
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		Machine.interrupt().disable();
		lock.acquire();
		
		if(x <= 0) return;
		long wakeTime = Machine.timer().getTime() + x;
		sleptThreadQueue.put(wakeTime, KThread.currentThread());
		KThread.currentThread().sleep();

		lock.release();
		Machine.interrupt().enable();	
		// while (wakeTime > Machine.timer().getTime())
		// 	KThread.yield();
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
	public boolean cancel(KThread thread) {
		lock.acquire();

		if(sleptThreadQueue.containsValue(thread)) {
			// System.out.println("Cancel thread " + thread.getName());
			for(Map.Entry<Long, KThread> entry : sleptThreadQueue.entrySet()) {
				if(entry.getValue() == thread) {
					sleptThreadQueue.remove(entry.getKey());
					// System.out.println("found thread " + thread.getName());
					lock.release();
					return true;
				}
			}
			lock.release();
			return true;
		}
		else {
			// System.out.println("thread " + thread.getName()+ " not found");
			lock.release();
			return false;
		}
	}

    public static void alarmTest0() {
		int durations[] = {999, -10*1000, 100*1000, 0};
		long t0, t1;

		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil (d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest0: waited for " + (t1 - t0) + " ticks");
		}
    }

    public static void alarmTest1() {
		KThread child1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Child 1 started");
				ThreadedKernel.alarm.waitUntil(10000);
				System.out.println("Child 1 finished");
			}
		});

		child1.setName("child1");
		long t0 = Machine.timer().getTime();
		child1.fork();
		child1.join();
		long t1 = Machine.timer().getTime();
		System.out.println ("child1: waited for " + (t1 - t0) + " ticks");
    }

	public static void alarmTest2() {
		KThread child1 = new KThread(new Runnable() {
			public void run() {
				long t0 = Machine.timer().getTime();
				System.out.println("Child 1 started");
				System.out.println("Child 1 sleep");
				ThreadedKernel.alarm.waitUntil(100000);
				long t1 = Machine.timer().getTime();
				System.out.println ("child1: waited for " + (t1 - t0) + " ticks");
				System.out.println("Child 1 finished");
			}
		});
		child1.setName("child1");
		child1.fork();
		System.out.println("Main thread sleep");
		KThread.currentThread().yield();
		ThreadedKernel.alarm.cancel(child1);
		System.out.println("Main thread cancel child 1 alarm");
	}
    // Implement more test methods here ...

    // Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
    public static void selfTest() {
		// alarmTest0();
		// alarmTest1();
		alarmTest2();
	// Invoke your other test methods here ...
    }

	final Lock lock = new Lock();
}
