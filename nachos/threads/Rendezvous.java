package nachos.threads;

import nachos.machine.*;
import java.util.ArrayList;
import java.util.HashMap;
/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
        rLock = new Lock();
        // condition = new Condition(rLock);
        map = new HashMap<Integer, ArrayList<Object>>(); 

    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block, waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
        ArrayList<Object> list = new ArrayList<Object>();
        rLock.acquire();
        if(!map.containsKey(tag)){
            list.add(new Condition(rLock)); 
            list.add(value);
            map.put(tag, list);
            ( (Condition) list.get(0)).sleep();
            list = map.get(tag);
            map.remove(tag);
            rLock.release();
            return (Integer)list.get(2);
        }
        else{
            list = map.get(tag);
            list.add(value);
            ( (Condition) list.get(0)).wake();
        }

        rLock.release();
        // map.remove(tag);
        return (Integer) list.get(1);
    }
    // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
	final Rendezvous r = new Rendezvous();

	KThread t1 = new KThread( new Runnable () {
		public void run() {
		    int tag = 0;
		    int send = -1;

		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
		    int recv = r.exchange (tag, send);
		    Lib.assertTrue (recv == 2, "Was expecting " + 2 + " but received " + recv);
		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
		}
	    });
	t1.setName("t1");
	KThread t2 = new KThread( new Runnable () {
		public void run() {
		    int tag = 1;
		    int send = 1;

		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
		    int recv = r.exchange (tag, send);
		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
		}
	    });
	KThread t3 = new KThread( new Runnable () {
		public void run() {
		    int tag = 0;
		    int send = 2;

		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
		    int recv = r.exchange (tag, send);
		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
		}
	    });
	t2.setName("t2");
    t3.setName("t3");

	t1.fork(); t2.fork(); t3.fork();
	// assumes join is implemented correctly
	t1.join();  t3.join();
    }
    public static void rendezTest2() {
	final Rendezvous r = new Rendezvous();

	KThread t1 = new KThread( new Runnable () {
		public void run() {
		    int tag = 0;
		    int send = -1;

		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
		    int recv = r.exchange (tag, send);
		    Lib.assertTrue (recv == 2, "Was expecting " + 2 + " but received " + recv);
		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
		}
	    });
	t1.setName("t1");
	KThread t2 = new KThread( new Runnable () {
		public void run() {
		    int tag = 0;
		    int send = 1;

		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
		    int recv = r.exchange (tag, send);
		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
		}
	    });
	KThread t3 = new KThread( new Runnable () {
		public void run() {
		    int tag = 0;
		    int send = 2;

		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
		    int recv = r.exchange (tag, send);
		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
		}
	    });
	t2.setName("t2");
    t3.setName("t3");

	t1.fork(); t2.fork(); t3.fork();
	// assumes join is implemented correctly
	t1.join();  t3.join();
    }
    // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
	// place calls to your Rendezvous tests that you implement here
	rendezTest1();
    }

    private Lock rLock;
    private HashMap<Integer, ArrayList<Object>> map;
    // private boolean flag;
}
