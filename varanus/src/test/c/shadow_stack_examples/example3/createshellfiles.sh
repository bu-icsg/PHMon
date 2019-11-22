#!/bin/bash


riscv64-unknown-linux-gnu-as -o shellcode3.o shellcode3.s
riscv64-unknown-linux-gnu-ld -o shellcode3 shellcode3.o
riscv64-unknown-linux-gnu-objdump -d shellcode3 > shellcode3.dump

