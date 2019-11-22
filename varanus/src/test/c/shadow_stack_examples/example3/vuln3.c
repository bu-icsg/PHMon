
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


char* hacked = "Hacked!!!";


void vulnerable_function(char* string) {
    char buffer[100];
    printf ("buffer: %p \n", &buffer);
    strcpy(buffer, string);
}

int main(int argc, char** argv) {
    printf("Main function!\n");

//Print addresses not to use gdb
    printf ("hacked: %p \n", hacked);
    size_t i;
    void (*ptr_to_puts)() = puts;
    for (i=0; i<sizeof ptr_to_puts; i++)
    printf("%.2x", ((unsigned char *)&ptr_to_puts)[i]);
    putchar('\n');

    char name[100];
    gets(name);
    vulnerable_function(name);

    return 0;
}

