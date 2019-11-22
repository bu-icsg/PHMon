.section .text
.globl _start
_start:
           # addi a3, a3, 1005
           #lui  a5,0x1101b
           #andi a5,a5,0x220
           #jal  0x102f4  #<printf>
	lui a5,0x11110
	lui a6,0x11100
	lui a4,0x11110
	sub a4,a4,a6  #a4=0x10000
	sub a5,a5,a6  #a5=0x11000	
	addi a0,a5,0x6f0 #a0=0x106f0-> hacked addr
	jalr a2,a4,0x4c0 #R[a2]=Pc+4; PC=(R[a4]+sext(imm))&0xfffffffe
			 #		=0x104c0(puts address)
