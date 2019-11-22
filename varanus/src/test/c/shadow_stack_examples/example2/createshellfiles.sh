#!/bin/bash


riscv64-unknown-linux-gnu-as -o shellcode2.o shellcode2.s
riscv64-unknown-linux-gnu-ld -o shellcode2 shellcode2.o
riscv64-unknown-linux-gnu-objdump -d shellcode2 > shellcode2.dump

