package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */

	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		waitQueue.add(KThread.currentThread());

		conditionLock.release();
		KThread.currentThread().sleep();
		conditionLock.acquire();
		Machine.interrupt().enable();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		boolean intStatus = Machine.interrupt().disable();
        System.out.println("wake");
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		if(!waitQueue.isEmpty()) {
			KThread wokeThread = ((KThread) waitQueue.removeFirst());
            ThreadedKernel.alarm.cancel(wokeThread);
            System.out.println("wake "+wokeThread.getName());
            wokeThread.ready();
        }

		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		while(!waitQueue.isEmpty()) {
            KThread wokeThread = ((KThread) waitQueue.removeFirst());
            ThreadedKernel.alarm.cancel(wokeThread);
            wokeThread.ready();
        }

		Machine.interrupt().restore(intStatus);
	}

        /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
    private static class InterlockTest {
        private static Lock lock;
        private static Condition cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire();
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();   // signal
                    cv.sleep();  // wait
                }
                lock.release();
            }
        }

        public InterlockTest () {
            lock = new Lock();
            cv = new Condition(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            // We need to wait for ping to finish, and the proper way
            // to do so is to join on ping.  (Note that, when ping is
            // done, pong is sleeping on the condition variable; if we
            // were also to join on pong, we would block forever.)
            // For this to work, join must be implemented.  If you
            // have not implemented join yet, then comment out the
            // call to join and instead uncomment the loop with
            // yields; the loop has the same effect, but is a kludgy
            // way to do it.
            ping.join();
            // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
        }
    }
    public static void cvTest5() {
        final Lock lock = new Lock();
        // final Condition empty = new Condition(lock);
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread consumer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    while(list.isEmpty()){
                        empty.sleep();
                    }
                    System.out.println("List has " + list.size() + " values.");
                    Lib.assertTrue(list.size() == 5, "List should have 5 values.");
                    while(!list.isEmpty()) {
                        // context swith for the fun of it
                        KThread.currentThread().yield();
                        System.out.println("Removed " + list.removeFirst());
                    }
                    System.out.println("List has " + list.size() + " values.");
                    lock.release();
                }
            });

        KThread producer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    for (int i = 0; i < 5; i++) {
                        list.add(i);
                        System.out.println("Added " + i);
                        // context swith for the fun of it
                        KThread.currentThread().yield();
                    }
                    empty.wake();
                    lock.release();
                }
            });

        consumer.setName("Consumer");
        producer.setName("Producer");
        consumer.fork();
        producer.fork();

        // We need to wait for the consumer and producer to finish,
        // and the proper way to do so is to join on them.  For this
        // to work, join must be implemented.  If you have not
        // implemented join yet, then comment out the calls to join
        // and instead uncomment the loop with yield; the loop has the
        // same effect, but is a kludgy way to do it.
        consumer.join();
        System.out.println("Consumer joined");
        producer.join();
        System.out.println("Producer joined");
        // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
    }
    // Place sleepFor test code inside of the Condition2 class.

    private static void sleepForTest1 () {
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);

        lock.acquire();
        long t0 = Machine.timer().getTime();
        System.out.println (KThread.currentThread().getName() + " sleeping");
        // no other thread will wake us up, so we should time out
        cv.sleepFor(200000);
        cv.wake();
        long t1 = Machine.timer().getTime();
        System.out.println(KThread.currentThread().getName() +
                           " woke up, slept for " + (t1 - t0) + " ticks");
        lock.release();
    }

    public static void selfTest() {
        sleepForTest1();
    }
    // Invoke Condition2.selfTest() from ThreadedKernel.selfTest()
    // public static void selfTest() {
    //     new InterlockTest();
    // }

	public void sleepFor(long timeout) {

		Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		waitQueue.add(KThread.currentThread());

		conditionLock.release();
        ThreadedKernel.alarm.waitUntil(timeout);
		conditionLock.acquire();
		Machine.interrupt().enable();
	}

        private Lock conditionLock;
		private LinkedList<KThread> waitQueue;
}
