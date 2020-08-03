#!/bin/bash -ex

PHMon=`pwd`
ROCKETCHIP_DIR=$PHMon/fpga-zynq/rocket-chip
FPGA=$ROCKETCHIP_DIR/../zedboard
BASH=~/.bashrc

cd $FPGA
make rocket CONFIG=ZynqKomodoConfig ROCKETCHIP_ADDONS=varanus

rm -rf zedboard_rocketchip_ZynqKomodoConfig

make project CONFIG=ZynqKomodoConfig
make fpga-images-zedboard/boot.bin CONFIG=ZynqKomodoConfig
cd fpga-images-zedboard
bootgen -image boot.bif -split bin -w -o BOOT.BIN
