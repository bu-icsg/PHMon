#!/bin/bash -ex

export RISCV=`pwd`/fpga-zynq/rocket-chip/tools

git submodule update --init --recursive

export PHMon=`pwd`
git clone https://github.com/ucb-bar/fpga-zynq/

cd fpga-zynq
git submodule update --init --recursive

git clone https://github.com/riscv/riscv-linux
cd riscv-linux
git checkout --track origin/riscv-linux-4.15
git reset fe92d7905c6ea0ebeabeb725b8040754ede7c220
git apply $PHMon/patches/kernel.patch
cp $PHMon/patches/.config .
cp $PHMon/patches/varanus.h include/linux/
cp $PHMon/patches/komodo.c fs/

cd ../rocket-chip
mkdir tools
export ROCKETCHIP_DIR=`pwd`
git apply $PHMon/patches/rocket.patch

cd $PHMon
./install-symlinks

cd $ROCKETCHIP_DIR/..
git apply $PHMon/patches/fpga_config.patch

export PATH=$PATH:$RISCV/bin

cd $ROCKETCHIP_DIR
rm -rf riscv-tools
git clone https://github.com/riscv/riscv-gnu-toolchain.git
cd riscv-gnu-toolchain
git submodule update --init --recursive
# Use --enable-multilab if you want to have support for both 32-bit and 64-bit
#./configure --prefix=$RISCV --enable-multilib
./configure --prefix=$RISCV
make -j8
make -j8 linux

cd $ROCKETCHIP_DIR
git clone https://github.com/riscv/riscv-pk.git
cd riscv-pk
git reset --hard 6b501de8e8512abd969199c57c5531737e52a105
git apply $PHMon/patches/pk.patch
mkdir build && cd build
../configure --prefix=$RISCV --host=riscv64-unknown-elf
make

cd $ROCKETCHIP_DIR
git clone https://github.com/riscv/riscv-isa-sim.git
cd riscv-isa-sim
mkdir build && cd build
../configure --prefix=$RISCV
make
make install

cd $ROCKETCHIP_DIR
git clone https://github.com/riscv/riscv-fesvr.git
cd riscv-fesvr
mkdir build && cd build
../configure --prefix=$RISCV
make install

cd $PHMon/varanus
git clone https://github.com/seldridge/rocket-rocc-examples.git
cd rocket-rocc-examples
git reset cc86e29b4f7adcc277a5fe6f65fd448a606bbeac
git submodule update --init --recursive
cp rocc-software/src/xcustom.h $PHMon/fpga-zynq/riscv-linux/include/linux/

cd $PHMon
