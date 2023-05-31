package nachos.vm;

import nachos.machine.*;
import nachos.userprog.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
 @Override
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
 @Override
	public void restoreState() {
		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
 @Override
	protected boolean loadSections() {
		// System.out.println("attempting loading sections");
		int lastVpn = 0;

		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				/* 
					// get availble physical page
					int ppn = UserKernel.getPPN();
					// if not return -1
					if (ppn == -1) {
						return false;
					}
				*/

				// create translation entry from vpn to ppn
				pageTable[vpn] = new TranslationEntry(vpn, -1, false, section.isReadOnly(), false, false);
				
				
				//section.loadPage(i, ppn);
				
				
				lastVpn = vpn;
			}
		}

		for (int i = 0; i < 9; i++) {

			//int ppn = UserKernel.getPPN();
			int vpn = lastVpn + i + 1;

			pageTable[vpn] = new TranslationEntry(vpn, -1, false, false, false, false);
		}

		// System.out.println("loaded sections");

		return true;


	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
 @Override
	protected void unloadSections() {
		super.unloadSections();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
 @Override
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionPageFault:
			int result2 = handlePageFault(processor.readRegister(Processor.regBadVAddr));
			processor.writeRegister(Processor.regV0, result2);
			//do not advance PC so program attempts to read address again
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	int handlePageFault(int vaddr) {
		int numSections = coff.getNumSections();
		int processVPN = Processor.pageFromAddress(vaddr);
		int lastVPN = 0;	
		for(int i = 0; i < numSections; i++) {
			CoffSection section = coff.getSection(i);
			int vpn = section.getFirstVPN();

			if(vpn + section.getLength() >= processVPN) {
				continue;
			}


			int ppn = UserKernel.getPPN();
			if (ppn == -1) {
				return -1;
			}
			pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), true, false);
			section.loadPage(processVPN - vpn, ppn);
			

			return 0;
		}
		int ppn = UserKernel.getPPN();
		pageTable[processVPN] = new TranslationEntry(processVPN, ppn, true, false, true, false);
		byte[] zeroArray = new byte[Processor.pageSize];
		writeVirtualMemory(vaddr, zeroArray, 0, Processor.pageSize);
		return -1;
	}

 @Override
	public String readVirtualMemoryString(int vaddr, int maxLength) {

		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = this.readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

 @Override
	public int readVirtualMemory(int vaddr, byte[] data) {
		return this.readVirtualMemory(vaddr, data, 0, data.length);
	}

 @Override
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int paddr;
		int amountCopied = 0;

		// loop for reading memory page by page

		while (amountCopied < length) {

			// get the physical address from virtual adresss

			paddr = this.getPaddr(vaddr);
			if (paddr < 0 || paddr >= memory.length) {
				return -1;
			}

			// the amount that we read from this page is either the entire page( starting at
			// the paddr) or the remainder of what we are supposed to copy
			int amount = Math.min(length - amountCopied, pageSize - Processor.offsetFromAddress(vaddr));
			// writes it to the data
			System.arraycopy(memory, paddr, data, offset + amountCopied, amount);
			amountCopied += amount;
			// offsets the virtual address
			vaddr += amount;

		}

		return amountCopied;
	}

	private int getPaddr(int vaddr) {
		;

		int paddr = -1;

		int vpn = Processor.pageFromAddress(vaddr);
		int addrOffest = Processor.offsetFromAddress(vaddr);

		if (vpn >= pageTable.length || vpn < 0) {
			return -1;
		}

		//page fault
		if (pageTable[vpn] == null || !pageTable[vpn].valid)
			handlePageFault(vaddr);
		// pageTable[vpn].used = true;
		int ppn = pageTable[vpn].ppn;

		paddr = Processor.makeAddress(ppn, addrOffest);
		// paddr = pageSize * ppn + addrOffest;

		return paddr;

	}

 @Override
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return this.writeVirtualMemory(vaddr, data, 0, data.length);
	}

 @Override
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int amountWritten = 0;
		int paddr;

		// System.out.println("writeVirtualMemory starts");

		// loop for reading memory page by page

		while (amountWritten < length) {
			// System.out.println("length: " + length);

			// get the physical address from virtual adresss
			paddr = this.getPaddr(vaddr);
			if (paddr < 0 || paddr >= memory.length || !validWrite(vaddr)) {
				return amountWritten;
			}

			// the amount that we write to this page is either the entire page( starting at
			// the paddr) or the remainder of what we are supposed to write
			int amount = Math.min(length - amountWritten, pageSize - Processor.offsetFromAddress(vaddr));
			// System.out.println("amount: " + amount);

			// writes it to the data
			System.arraycopy(data, offset + amountWritten, memory, paddr, amount);
			amountWritten += amount;
			// offsets the virtual address
			vaddr += amount;
			// System.out.println("vaddr: " + vaddr);

		}
		return amountWritten;
	}

	private boolean validWrite(int vaddr) {

		int vpn = Processor.pageFromAddress(vaddr);

		if (vpn >= pageTable.length || vpn < 0) {
			return false;
		}

		//page fault
		if (pageTable[vpn] == null || !pageTable[vpn].valid)
			handlePageFault(vaddr);
		
		return !pageTable[vpn].readOnly;
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
