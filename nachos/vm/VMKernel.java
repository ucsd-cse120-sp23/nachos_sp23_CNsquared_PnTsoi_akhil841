package nachos.vm;

import java.util.LinkedList;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;
import nachos.threads.Lock;
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
		swapLock = new Lock();
		evictLock = new Lock();
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
		
		initLock.acquire();
		if(physicalMemoryAvail == null){
			initializeMemory();
		}


		if(physicalMemoryAvail.size() > 0){
			initLock.release();
			int out = physicalMemoryAvail.pop();
			te.ppn = out;
			ipt[out] = te;
			return out;
		}
		
		//System.out.println("Using clock algo");
		int evictIdx = clockPPN();
		//System.out.println("Used clock algo and want to evict ipt: " + evictIdx);
		int freePPN = writeEvictedToSwapFile(evictIdx);
		//this is why we SRP but oopsie
		te.ppn = freePPN;
		ipt[freePPN] = te;

		//System.out.println("Freed ppn: " + freePPN);
		initLock.release();
		
		return freePPN;
	}

	public static void pinPage(int paddr, boolean status) {
		evictLock.acquire();
		int ppn = paddr / Processor.pageSize;
		TranslationEntry curPage = ipt[ppn];
		if (curPage != null && curPage.valid)
			pinArray[ppn] = status;
		evictLock.release();
	}

	public static boolean isPinned(int paddr) {
		evictLock.acquire();
		int ppn = paddr / Processor.pageSize;
		evictLock.release();
		return pinArray[ppn];
	}

	public static boolean allPinned() {
		evictLock.acquire();
		if (physicalMemoryAvail.size() > 0)
		{
			evictLock.release();
			return false;
		}
		for (int i = 0; i < pinArray.length; i++)
		{
			if (!pinArray[i])
			{
				evictLock.release();
				return false;
			}
		}
		evictLock.release();
		return true;
	}

	//get idx of first free page
	public static int clockPPN() {
		evictLock.acquire();
		if (physicalMemoryAvail.size() > 0)
		{
			evictLock.release();
			return -1;
		}
		int eidx = curEIDX;
		int delta = Machine.processor().getNumPhysPages() >> 2;
		int cidx = (curCIDX == -1) ? eidx + delta : curCIDX;
		while (eidx < ipt.length) 
		{
			//set to false
			if (!ipt[eidx].used && !pinArray[eidx])
				break;
			if (!pinArray[cidx])
				ipt[cidx].used = false;
			//increment circular-ly
			eidx++;
			cidx++;
			if (eidx == ipt.length)
				eidx = 0;
			if (cidx == ipt.length)
				cidx = 0;
		}
		curEIDX = eidx;
		curCIDX = cidx;
		evictLock.release();
		return eidx;
	}
	//return 1 if successful
	//return 0 if not
	public static int writeEvictedToSwapFile(int evictedIPTIndex) {
		evictLock.acquire();
		//get evicted entry
		TranslationEntry evictedEntry = ipt[evictedIPTIndex];
		evictedEntry.valid = false;
		
		int evictedPPN = evictedEntry.ppn;
		evictedEntry.ppn = -1;

		int physPageAddr = evictedPPN*Processor.pageSize;	

		//read from physPage
		byte[] physPage = new byte[Processor.pageSize];
		System.arraycopy(Machine.processor().getMemory(), physPageAddr,physPage, 0, Processor.pageSize);

		int spn = getSPN();
		if(evictedEntry.dirty) {
			//not clean
			//write to swap file
			//get spn
			if(spn == -1) {
				int writeSize = swapFile.write( swapFile.length(), physPage, 0, Processor.pageSize);
				evictedEntry.vpn = (swapFile.length() / Processor.pageSize )-1;

				//cleans it
				evictedEntry.dirty = false;
				evictLock.release();
				return evictedPPN;
			}
			else {
				//write to swap file
				int writeSize = swapFile.write(spn*Processor.pageSize, physPage, 0, Processor.pageSize);
				evictedEntry.vpn = spn;
				//cleans it
				evictedEntry.dirty = false;
				evictLock.release();
				return evictedPPN;
			}
		}
		else {
			//clean 
			//do nothing
			//not dirty i.e. never written to
			evictLock.release();
			return evictedPPN;
		}

	}

	public static int freePPN(int page){
		initLock.acquire();
		
		if(physicalMemoryAvail.contains(page)){
			initLock.release();
			ipt[page] = null;
			
			return -1;
		}
		initLock.release();
		physicalMemoryAvail.add(page);
		
		return 0;

	}

	//if there is free pages then 
	public static int getSPN(){
		swapLock.acquire();
		if(swapFileFreePages.size() > 0){
			swapLock.release();
			return swapFileFreePages.pop();
		}
		swapLock.release();
		return -1;
	}

	//add locks
	public static int freeSPN(int page){
		swapLock.acquire();
		if(swapFileFreePages.contains(page)){
			swapLock.release();
			return -1;
		}	
		swapLock.release();
		swapFileFreePages.add(page);
		
		return 0;

	}

	


	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
	public static OpenFile swapFile;
	private static LinkedList<Integer> swapFileFreePages = new LinkedList<>();

	private static TranslationEntry[] ipt = new TranslationEntry[Machine.processor().getNumPhysPages()];
	private static boolean[] pinArray = new boolean[Machine.processor().getNumPhysPages()];	
	private static int curEIDX = 0;
	private static int curCIDX = -1;
	private static Lock swapLock;
	private static Lock evictLock;
}