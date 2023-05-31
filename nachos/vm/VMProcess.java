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
		return super.loadSections();
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
			int result2 = handlePageFault(processor.readRegister(Processor.regBadVaddr));
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
			pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
			section.loadPage(vpn, ppn);
			return 0;
		}
		return -1;
	}

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
