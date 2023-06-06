package nachos.vm;

import java.util.LinkedList;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
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
		swapFile = fileSystem.open("swapFile", true);

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
		System.out.println("trying to get PPN");
		
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
		System.out.println("nofree memory");
		int freePPNIdx = clockPPN();
		writeEvictedToSwapFile(freePPNIdx);
		initLock.release();
		//Machine.interrupt().enable();
		return freePPNIdx;
	}

	public static int evictPPN(){
		return -1;
	}

	//get idx of first free page
	public static int clockPPN() {
		if (physicalMemoryAvail.size() > 0)
			return -1;
		int eidx = 0;
		int delta = Machine.processor().getNumPhysPages() >> 2;
		int cidx = eidx + delta;
		while (eidx < ipt.length) 
		{
			//set to false
			if (!ipt[eidx].used)
				break;
			ipt[cidx].used = false;
			//increment circular-ly
			eidx++;
			cidx++;
			if (eidx == ipt.length)
				eidx = 0;
			if (cidx == ipt.length)
				cidx = 0;
		}
		return eidx;
	}
	//return 1 if successful
	//return 0 if not
	public static int writeEvictedToSwapFile(int evictedIPTIndex) {
		//get evicted entry
		TranslationEntry evictedEntry = ipt[evictedIPTIndex];
		int evictedPPN = evictedEntry.ppn;
		int physPageAddr = evictedPPN*Processor.pageSize;	
		//read from physPage
		byte[] physPage = new byte[Processor.pageSize];
		System.arraycopy(Machine.processor().getMemory(), physPageAddr*Processor.pageSize,
		 physPage, 0, Processor.pageSize);
		int spn = getSPN();

		if(evictedEntry.dirty) {
			//not clean
			//write to swap file
			//get spn
			if(spn == -1) {
				//swap file full
				// ummmm...
				return 0;
			}
			else {
				//write to swap file
				int writeSize = swapFile.write(spn*Processor.pageSize, physPage, 0, Processor.pageSize);
				// update swapPageTable
				swapPageTable[evictedEntry.ppn] = spn;
				return 1;
			}
		}
		else {
			//clean 
			//do nothing
			//already evicted and nothing is changed on disk
			int writeSize = swapFile.write(spn*Processor.pageSize, physPage, 0, Processor.pageSize);
			swapPageTable[evictedEntry.ppn] = spn;
			return 1;
		}

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
		if(swapFileFreePages.size() > 0){
			return swapFileFreePages.pop();
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
	public static OpenFile swapFile;
	private static LinkedList<Integer> swapFileFreePages = new LinkedList<>();
	public static Integer[] swapPageTable = new Integer[Machine.processor().getNumPhysPages()];


	private static TranslationEntry[] ipt = new TranslationEntry[Machine.processor().getNumPhysPages()];	
}
