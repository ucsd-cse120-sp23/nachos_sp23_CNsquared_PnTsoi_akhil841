package nachos.vm;

import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
 @Override
	public void initialize(String[] args) {
		super.initialize(args);
	}

	/**
	 * Test this kernel.
	 */
 @Override
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
 @Override
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
 @Override
	public void terminate() {
		super.terminate();
	}


 
	public static int getPPN(){
		
		//Machine.interrupt().disable();
		initLock.acquire();
		if(physicalMemoryAvail == null){
			initializeMemory();
		}


		if(physicalMemoryAvail.size() > 0){
			initLock.release();
			//Machine.interrupt().enable();
			return physicalMemoryAvail.pop();
		}
		initLock.release();
		//Machine.interrupt().enable();
		return -1;
	}

	public static int evictPPN(){





		return -1;
	}

	public static int freePPN(int page){
		initLock.acquire();
		//Machine.interrupt().disable();
		if(physicalMemoryAvail.contains(page)){
			initLock.release();
			//Machine.interrupt().enable();
			return -1;
		}
		initLock.release();
		physicalMemoryAvail.add(page);
		//Machine.interrupt().enable();
		return 0;

	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
}
