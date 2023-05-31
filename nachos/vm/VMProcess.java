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

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
