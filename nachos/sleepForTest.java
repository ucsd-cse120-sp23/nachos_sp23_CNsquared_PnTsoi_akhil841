    // Place sleepFor test code inside of the Condition2 class.

    private static void sleepForTest1 () {
	Lock lock = new Lock();
	Condition2 cv = new Condition2(lock);

	lock.acquire();
	long t0 = Machine.timer().getTime();
	System.out.println (KThread.currentThread().getName() + " sleeping");
	// no other thread will wake us up, so we should time out
	cv.sleepFor(2000);
	long t1 = Machine.timer().getTime();
	System.out.println (KThread.currentThread().getName() +
			    " woke up, slept for " + (t1 - t0) + " ticks");
	lock.release();
    }

    public static void selfTest() {
	sleepForTest1();
    }
