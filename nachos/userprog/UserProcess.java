package nachos.userprog;

import java.io.EOFException;
import java.util.ArrayList;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		files = new OpenFile[16];
		processID = freeProcessID;
		freeProcessID++;

		files[0] = UserKernel.console.openForReading();
		files[1] = UserKernel.console.openForWriting();

	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		String name = Machine.getProcessClassName();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader. Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals("nachos.userprog.UserProcess")) {
			return new UserProcess();
		} else if (name.equals("nachos.vm.VMProcess")) {
			return new VMProcess();
		} else {
			return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr     the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 *                  including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {

		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data  the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr  the first byte of virtual memory to read.
	 * @param data   the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 *               array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		rwLock.acquire();
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		int paddr;
		int amountCopied = 0;

		// loop for reading memory page by page

		while (amountCopied < length) {

			// get the physical address from virtual adresss

			paddr = getPaddr(vaddr);
			if (paddr < 0 || paddr >= memory.length) {
				rwLock.release();
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

		rwLock.release();
		return amountCopied;
	}

	private int getPaddr(int vaddr) {
		;

		int paddr = -1;

		int vpn = Processor.pageFromAddress(vaddr);
		int addrOffest = Processor.offsetFromAddress(vaddr);

		if (vpn >= pageTable.length || vpn < 0 || pageTable[vpn] == null) {
			return -1;
		}

		if (!pageTable[vpn].valid)
			return -1;
		// pageTable[vpn].used = true;
		int ppn = pageTable[vpn].ppn;

		paddr = Processor.makeAddress(ppn, addrOffest);
		// paddr = pageSize * ppn + addrOffest;

		return paddr;

	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data  the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr  the first byte of virtual memory to write.
	 * @param data   the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 *               memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		rwLock.acquire();
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
			paddr = getPaddr(vaddr);
			if (paddr < 0 || paddr >= memory.length || !validWrite(vaddr)) {
				rwLock.release();
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
		rwLock.release();
		return amountWritten;
	}

	private boolean validWrite(int vaddr) {

		int vpn = Processor.pageFromAddress(vaddr);

		if (vpn >= pageTable.length || vpn < 0) {
			return false;
		}
		return !pageTable[vpn].readOnly;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		// System.out.println("Attempting load");
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		System.out.println("I need " + " many pages for COff sections code");

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		// System.out.println("loaded");

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
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

				 
				// get availble physical page
				int ppn = UserKernel.getPPN();
				// if not return -1
				if (ppn == -1) {
					return false;
				}
				

				// create translation entry from vpn to ppn
				pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);
				
				
				section.loadPage(i, ppn);
				
				
				lastVpn = vpn;
			}
		}

		for (int i = 0; i < 9; i++) {

			int ppn = UserKernel.getPPN();
			int vpn = lastVpn + i + 1;

			pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
		}

		// System.out.println("loaded sections");

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {

		// go through pagetable and free all of the physical pages
		for (int i = 0; i < pageTable.length; i++) {
			TranslationEntry entry = pageTable[i];
			if (entry != null)
				UserKernel.freePPN(entry.ppn);

			pageTable[i] = null;
		}

	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if (parent != null) {
			Lib.assertNotReached("Machine.halt() non root process attempted to call halt!");
			return -1;
		}
		Machine.halt();
		Lib.assertNotReached("Machine.halt() did not halt machine!");

		return 0;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(Integer status) {
		// Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// System.out.println("reached exit syscall");
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.

		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
		// System.out.println("Exiting " + processID);
		// close all open files
		for (OpenFile i : files) {
			if (i != null)
				i.close();
		}
		// free all pages
		unloadSections();
		// give every child a null parent
		for (UserProcess i : children) {
			i.parent = null;
		}
		this.exitStatus = status;
		
		// if this process has a parent, tell it that this process
		// is finished
		if (parent != null ) {
			// System.out.println("Parent is " + parent.processID);
			// System.out.println("set parent to ready");
			finished = true;
			this.thread.finish();
			return 0;
		} else {
			// otherwise, terminate (we are at the root)
			// System.out.println("huh? no parent");
			Kernel.kernel.terminate();
			this.thread.finish();
			return 0;
		}
	}

	private int handleExec(String programName, int argc, int ptrArray) {
		// Machine.interrupt().disable();
		// System.out.println("attempting exec and now loading");
		if (programName == null) {
			// Machine.interrupt().enable();
			return -1;
		}

		if (!programName.endsWith(".coff")) {
			return -1;
		}
		if (argc < 0) {
			// Machine.interrupt().enable();
			return -1;
		}
		// get arguments in ptrArray
		int offset = 0;
		byte[] argBytes = new byte[4];
		int[] argArray = new int[argc];
		for (int i = 0; i < argc; i++) {
			int cec = readVirtualMemory(ptrArray + offset, argBytes, 0, 4);
			if (cec != 4) {
				// Machine.interrupt().enable();
				return -1;
			}
			int addr_i = Lib.bytesToInt(argBytes, 0);
			if (addr_i < 0) {
				// Machine.interrupt().enable();
				return -1;
			}
			argArray[i] = addr_i;
			offset += 4;
		}

		// get argument string
		String[] args = new String[argc];
		for (int i = 0; i < argc; i++) {
			args[i] = readVirtualMemoryString(argArray[i], 256);
			if (args[i] == null) {
				// Machine.interrupt().enable();
				return -1;
			}
		}

		// create new process

		UserProcess child = UserProcess.newUserProcess();
		child.execute(programName, args);
		children.add(child);
		child.parent = this;
		// System.out.println("child thread created and readied");
		child.exitStatus = 0;
		// Machine.interrupt().enable();
		// System.out.println("executed " + child.processID);
		return child.processID;
	}

	private int handleRead(int fileDescriptor, int vaddr, int count) {
		// check for invalid file descriptor
		if (fileDescriptor < 0 || fileDescriptor >= 16)
			return -1;
		// check for invalid count
		if (count < 0)
			return -1;
		if (files[fileDescriptor] == null)
			return -1;
		// read from file
		// initialize buffer
		int pageSize = Processor.pageSize;
		byte[] buffer = new byte[pageSize];
		int offset = 0;
		int bytesRead = 0;
		int totalBytesRead = 0;
		OpenFile file = files[fileDescriptor];

		while (count > 0) {
			int bytesToRead = Math.min(count, pageSize);
			bytesRead = file.read(buffer, 0, bytesToRead);
			if (bytesRead <= 0)
				return totalBytesRead;
			int bytesWritten = writeVirtualMemory(vaddr, buffer, 0, bytesRead);
			if (bytesWritten == -1)
				return -1;
			if (bytesWritten != bytesRead)
				return totalBytesRead;
			count -= bytesRead;
			offset += bytesRead;
			vaddr += bytesRead;
			totalBytesRead += bytesWritten;
		}

		return totalBytesRead;
	}

	private int handleWrite(int fileDescriptor, int vaddr, int count) {
		// check for invalid file descriptor
		if (fileDescriptor < 0 || fileDescriptor >= 16)
			return -1;
		// check for invalid count
		if (count < 0)
			return -1;
		if (files[fileDescriptor] == null)
			return -1;
		// read from file
		// initialize buffer
		int pageSize = Processor.pageSize;
		byte[] buffer = new byte[pageSize];
		int offset = 0;
		int bytesRead = 0;
		int totalBytesWrite = 0;
		OpenFile file = files[fileDescriptor];

		while (count > 0) {
			int bytesToRead = Math.min(count, pageSize);
			bytesRead = readVirtualMemory(vaddr, buffer, 0, bytesToRead);
			if (bytesRead <= 0)
				return -1;
			int bytesWritten = file.write(buffer, 0, bytesRead);
			if (bytesWritten != bytesRead)
				return -1;
			count -= bytesRead;
			offset += bytesRead;
			vaddr += bytesRead;
			totalBytesWrite += bytesWritten;
		}

		return totalBytesWrite;
	}

	private int handleCreate(int naddr) {
		// check for nullptr
		if (naddr == 0)
			return -1;
		// get file name (must be 256 bytes or less)
		String fname = readVirtualMemoryString(naddr, 256);
		int fileIdx = -1;
		boolean alreadyOpened = false;
		for (int i = 0; i < files.length; i++) {
			if (files[i] == null && fileIdx == -1)
				fileIdx = i;
			if (files[i] != null && files[i].getName().equals(fname))
				alreadyOpened = true;
		}
		if (fileIdx != -1) {
			// remove file if it exists and we don't have it open
			if (!alreadyOpened)
				ThreadedKernel.fileSystem.remove(fname);
			// create new file
			OpenFile newFile = ThreadedKernel.fileSystem.open(fname, true);
			files[fileIdx] = newFile;
			return fileIdx;
		} else {
			Lib.debug(dbgProcess, "Max open files reached; cannot create a new file");
			return -1;
		}
	}

	private int handleUnlink(int naddr) {
		// check for nullptr
		if (naddr == 0)
			return -1;
		// get file name (must be 256 bytes or less)
		String fname = readVirtualMemoryString(naddr, 256);
		boolean successful = ThreadedKernel.fileSystem.remove(fname);
		if (!successful)
			return -1;
		// clear spot in file table if needed
		int idx = fileIndexNameLinearSearch(fname);
		if (idx != -1)
			files[idx] = null;
		return 0;
	}

	private int fileIndexLinearSearch() {
		for (int i = 2; i < 16; i++)
			if (files[i] == null)
				return i;
		return -1;
	}

	private int fileIndexNameLinearSearch(String name) {
		for (int i = 2; i < 16; i++)
			if (files[i] != null && files[i].getName().equals(name))
				return i;
		return -1;
	}

	private int handleOpen(String name) {

		OpenFile returned = ThreadedKernel.fileSystem.open(name, false);

		if (returned == null) {
			Lib.debug(dbgProcess, name + " not able to be opened");

			return -1;
		}

		int filesIndex = findOpenTableIndex();
		// There was no space for it in the table
		if (filesIndex == -1) {
			Lib.debug(dbgProcess, "No space for " + name);
			return -1;
		}

		files[filesIndex] = returned;

		return filesIndex;
	}

	private int findOpenTableIndex() {

		for (int i = 2; i < files.length; i++) {
			if (files[i] == null) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Close a file descriptor, so that it no longer refers to any file or
	 * stream and may be reused. The resources associated with the file
	 * descriptor are released.
	 *
	 * Returns 0 on success, or -1 if an error occurred.
	 */
	private int handleClose(int fileDescriptor) {
		if (fileDescriptor < 0 || fileDescriptor >= 16) {
			Lib.debug(dbgProcess, "Invalid file Descriptor");
			return -1;
		}

		OpenFile file = files[fileDescriptor];

		if (files[fileDescriptor] == null) {
			Lib.debug(dbgProcess, "Invalid file Descriptor");
			return -1;
		}

		file.close();

		files[fileDescriptor] = null;

		return 0;
	}

	private int handleJoin(int processID, int ecAddr) {
		// check for nullptr
		// System.out.println("Join starting: Joining " + this.processID + " to " + processID);
		if (ecAddr == 0)
			return -1;
		// get child process, if it exists
		UserProcess child = null;
		boolean found = false;
		int childIdx = 0;
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) != null && children.get(i).processID == processID) {
				childIdx = i;
				child = children.get(i);
				found = true;
				break;
			}
		}
		if (!found)
			return -1;
		// if child is finished, return immediately.
		if (child.finished) {
			Integer code = child.exitStatus;
			if (code != null) 
			{
				byte[] mem = Lib.bytesFromInt( code);
				writeVirtualMemory(ecAddr, mem, 0, 4);
			}
			//remove child since it's finished. makes this function return -1 if 
			//join is called on it again, unless we exec it under the same child process.
			children.remove(childIdx);
			// return 1 if normal execution, 0 if exception.
			return (code == null) ? 0 : 1;
		}
		// wait for child to finish
		// Machine.interrupt().disable();
		child.thread.join();
		// Machine.interrupt().enable();
		Integer code = child.exitStatus;
		if (code != null)
		{
			byte[] mem = Lib.bytesFromInt( code);
			writeVirtualMemory(ecAddr, mem, 0, 4);
		}
		//remove child since it's finished. makes this function return -1 if 
		//join is called on it again, unless we exec it under the same child process.
		children.remove(childIdx);
		// return 1 if normal execution, 0 if exception.
		return (code == null) ? 0 : 1;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0      the first syscall argument.
	 * @param a1      the second syscall argument.
	 * @param a2      the third syscall argument.
	 * @param a3      the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
			case syscallExec:
				return handleExec(readVirtualMemoryString(a0, 256), a1, a2);
			case syscallWrite:
				return handleWrite(a0, a1, a2);
			case syscallRead:
				return handleRead(a0, a1, a2);
			case syscallCreate:
				return handleCreate(a0);
			case syscallClose:
				return handleClose(a0);
			case syscallOpen:
				return handleOpen(readVirtualMemoryString(a0, 256));
			case syscallUnlink:
				return handleUnlink(a0);
			case syscallHalt:
				return handleHalt();
			case syscallExit:
				// System.out.println("call " + syscall);
				return handleExit(a0);
			case syscallJoin:
				return handleJoin(a0, a1);

			default:
				// System.out.println("Unknown syscall " + syscall);
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
				Lib.assertNotReached("Unknown system call!" + syscall);
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
			case Processor.exceptionSyscall:
				int result = handleSyscall(processor.readRegister(Processor.regV0),
						processor.readRegister(Processor.regA0),
						processor.readRegister(Processor.regA1),
						processor.readRegister(Processor.regA2),
						processor.readRegister(Processor.regA3));
				processor.writeRegister(Processor.regV0, result);
				processor.advancePC();
				break;
			default:
				Lib.debug(dbgProcess, "Unexpected exception: "
						+ Processor.exceptionNames[cause]);
				handleExit(null);
				Lib.assertNotReached("Unexpected exception");
		}
		
	}


	/** Array of file descriptors to OpenFile Objects */
	protected OpenFile[] files;

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
	protected UThread thread;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private UserProcess parent = null;

	private ArrayList<UserProcess> children = new ArrayList<>();

	private boolean finished = false;

	public int processID;

	public static int freeProcessID = 1;

	private Integer exitStatus = 0;

	private Lock rwLock = new Lock();
}
