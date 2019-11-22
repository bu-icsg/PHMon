#!/bin/bash -ex

FPGA=$ROCKETCHIP_DIR/../zedboard
BASH=~/.bashrc

cd $FPGA
make rocket CONFIG=ZynqKomodoConfig ROCKETCHIP_ADDONS=varanus

sed -i -e 's/#source/source/g' $BASH
source "$BASH"
rm -r zedboard_rocketchip_ZynqKomodoConfig

make project CONFIG=ZynqKomodoConfig
make fpga-images-zedboard/boot.bin CONFIG=ZynqKomodoConfig
cd fpga-images-zedboard
bootgen -image boot.bif -split bin -w -o BOOT.BIN

sed -i -e 's/source/#source/g' $BASH
source $BASH
