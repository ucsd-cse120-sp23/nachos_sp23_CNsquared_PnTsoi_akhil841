/*
 *
 * Program attempt to execute 3 instances of swap4, 1 instance of swap5 and 1 instance of write10
 * all in parallel.
 */

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int
main (int argc, char *argv[])
{
    char *prog[7] = {"swap4.coff", "swap4.coff", "swap4.coff", "swap5.coff", "swap5.coff", "swap5.coff", "write10.coff"};
    int pid;
    
    int i;
    for (i = 0; i < 7; i++) {
        printf("Running %s\n", prog[i]);
        pid = exec (prog[i], 0, 0);
        printf("%d\n", pid);
        if (pid < 0) {
            printf("bad bad bad\n");
            exit (-1);
        }
    }
    exit (0);
}