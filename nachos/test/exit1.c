/* 
 * exit1.c
 *
 * It does not get simpler than this...
 */
   
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int
main (int argc, char *argv[])
{
    printf ("HELLO");
    exit (123);
}