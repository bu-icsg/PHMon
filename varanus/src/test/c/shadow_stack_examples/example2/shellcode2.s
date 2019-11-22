.section .text
.globl _start
_start:
           # addi a3, a3, 1005
           #lui  a5,0x1101b
           #andi a5,a5,0x220
           #jal  0x102f4  #<printf>
	lui a5,0x12
	addi a0,a5,-0x530  #0x12000+0x530=0x12530-> addr of hacked
        lui a4,0x34
	addi a4,a4,500
	addi a4,a4,500
	addi a4,a4,500
	addi a4,a4,500
	addi a4,a4,500
	addi a4,a4,500
	addi a4,a4,88
	jr a4
