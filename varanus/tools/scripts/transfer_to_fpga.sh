#!/bin/bash -ex

VARANUS=$TOP/varanus
cd $VARANUS
make

cp $VARANUS/build/shadow_stack.rv $TOP/linux_root/root/home/configure.rv

cd $TOP/../../varanus/riscv-yocto/afl-patch/afl-2.52b
AFL_NO_X86=1 make
cp afl-fuzz $TOP/linux_root/root/home/

cd $TOP/varanus/src/test/c
riscv64-unknown-linux-gnu-gcc -Wall -fPIC -shared -o load_shmat.so load_shat.c -ldl
cp load_shmat.so $TOP/linux_root/root/home/

cd $TOP/linux_root/root
find . | cpio  -o -H newc > ../rootfs.cpio

cd $TOP/riscv-linux

make -j16 ARCH=riscv CROSS_COMPILE=riscv64-unknown-linux-gnu-

cd $TOP/../../riscv-pk/build/

make clean
../configure --prefix=$RISCV --host=riscv64-unknown-linux-gnu --with-payload=/home/leila/Documents/Research/fpga-zynq/rocket-chip/riscv-linux/vmlinux
make

#ACM0
#scp bbl root@192.168.1.30:/home/root

#ACM1
#scp bbl root@192.168.1.31:/home/root

#ACM2
#scp bbl root@192.168.1.32:/home/root

#ACM3
#scp bbl root@192.168.1.33:/home/root

#ACM5
scp bbl root@192.168.1.35:/home/root
