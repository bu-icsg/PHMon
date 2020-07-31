#!/bin/bash -ex

export PHMon=`pwd`
export RISCV=`pwd`/fpga-zynq/rocket-chip/tools
export ROCKETCHIP_DIR=$PHMon/fpga-zynq/rocket-chip

TEST=$1

echo $TEST
cd $PHMon/varanus
make RV_TARGET=/newSSD/leila/Final/PHMon/code/fpga-zynq/rocket-chip/tools/bin/riscv64-unknown-elf

cd $ROCKETCHIP_DIR/emulator
make CONFIG=KomodoCppConfig ROCKETCHIP_ADDONS=varanus
./emulator-freechips.rocketchip.system-KomodoCppConfig $ROCKETCHIP_DIR/riscv-pk/build/pk $TEST
