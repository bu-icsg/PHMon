#!/bin/bash -ex

export PHMon=`pwd`
export VARANUS=$PHMon/varanus
export RISCV=`pwd`/fpga-zynq/rocket-chip/tools
export ROCKETCHIP_DIR=$PHMon/fpga-zynq/rocket-chip

cd $VARANUS
make -B

cp $VARANUS/build/shadow_stack.rv $VARANUS/linux_root/root/home
cp $VARANUS/build/hello.rv $VARANUS/linux_root/root/home

#cd $VARANUS/src/test/c
#riscv64-unknown-linux-gnu-gcc -Wall -fPIC -shared -o load_shmat.so load_shat.c -ldl
#cp load_shmat.so $TOP/linux_root/root/home/

rm -rf $VARANUS/linux_root/root/dev/*
cd $VARANUS/linux_root/root

sudo mknod dev/console c 5 1
sudo mknod dev/null c 1 3
sudo mknod dev/urandom c 1 9

find . | cpio  -o -H newc > ../rootfs.cpio

cd $ROCKETCHIP_DIR/../riscv-linux

make -j16 ARCH=riscv CROSS_COMPILE=riscv64-unknown-linux-gnu-

cd $ROCKETCHIP_DIR/riscv-pk/build/

make clean
../configure --prefix=$RISCV --host=riscv64-unknown-elf --with-payload=$ROCKETCHIP_DIR/../riscv-linux/vmlinux
make  

#ACM0
scp bbl root@192.168.1.30:/home/root
