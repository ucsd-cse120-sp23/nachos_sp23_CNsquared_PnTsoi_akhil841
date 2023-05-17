#include "syscall.h"
#include "stdio.h"
int main() {
	char file[] = "someth";
	char out[7];
	int file_descriptor = creat(file); /* create a file*/

	int write_down = write(file_descriptor, file, 7);
	int read_out = read(file_descriptor, out, 7);
	printf("%s\n", out);
	printf("here\n");
	int closed = close(file_descriptor);

	return 0;
}
