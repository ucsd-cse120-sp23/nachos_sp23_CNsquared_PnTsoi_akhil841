/* 
 * exit1.c
 *
 * It does not get simpler than this...
 */
   
#include "syscall.h"

int
main (int argc, char *argv[])
{
    printf("exit1.c\n");
    exit (123);
}