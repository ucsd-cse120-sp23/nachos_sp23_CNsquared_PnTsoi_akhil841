package nachos.vm;

import java.util.LinkedList;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
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


 
	public static int getPPN(TranslationEntry te){
		
		//Machine.interrupt().disable();
		initLock.acquire();
		if(physicalMemoryAvail == null){
			initializeMemory();
		}


		if(physicalMemoryAvail.size() > 0){
			initLock.release();
			//Machine.interrupt().enable();
			int out = physicalMemoryAvail.pop();
			te.ppn = out;
			ipt[out] = te;
			//used.put(out, true);
			return out;
		}
		initLock.release();
		//Machine.interrupt().enable();
		return -1;
	}

	public static int evictPPN(){
		return -1;
	}

	//get idx of first free page
	public static int clockPPN() {
		if (physicalMemoryAvail.size() > 0)
			return -1;
		int idx = 0;
		while (idx < ipt.length) 
		{
			//set to false
			if (!ipt[idx].used)
				break;
			ipt[idx].used = false;
			//increment circular-ly
			idx++;
			if (idx == ipt.length)
				idx = 0;
		}
		return idx;
	}

	public static int freePPN(int page){
		initLock.acquire();
		//Machine.interrupt().disable();
		if(physicalMemoryAvail.contains(page)){
			initLock.release();
			ipt[page] = null;
			//Machine.interrupt().enable();
			return -1;
		}
		initLock.release();
		physicalMemoryAvail.add(page);
		//Machine.interrupt().enable();
		return 0;

	}

	//if there is free pages then 
	public static int getSPN(){
		if(physicalMemoryAvail.size() > 0){
			return physicalMemoryAvail.pop();
		}
		return -1;
	}

	//add locks
	public static int freeSPN(int page){
		if(swapFileFreePages.contains(page)){
			return -1;
		}	
		swapFileFreePages.add(page);
		
		return 0;

	}


	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
	private static OpenFile swapFile = fileSystem.open("swapFile", true);
	private static LinkedList<Integer> swapFileFreePages = new LinkedList<>();
	private static Integer[] swapPageTable = new Integer[Machine.processor().getNumPhysPages()];


	private static TranslationEntry[] ipt = new TranslationEntry[Machine.processor().getNumPhysPages()];	
}
