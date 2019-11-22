**Note: We have released this code for the artifact evaluation of USENIX Security’20. This is not our final or official release of PHMon. We will have an official release before the conference.**

# Overview
In this repository, we provide the hardware code for PHMon, its software API, the kernel patches, and all other required files to synthesize our PHMon for a Zynq FPGA board, boot up Linux, program PHMon, and run applications.

# Setup
To use this repository, first you need a stable version of Rocketchip repository. Currently, we use the version in FPGA-zynq repository (and yes we know that FPGA-zynq repository has been deprecated):

``` bash
git clone https://github.com/ucb-bar/fpga-zynq/
cd fpga-zynq
git submodule update --init --recursive
```

Updating the submodules might take a long time.
Please make sure that you use the exact same commit as the ones that we used:
```bash
rocket-chip: 5392219d86bd8ff3b241122bd38852e89fa06ac2
riscv-tools: c679e014f6cde1d9470019a03f3d366173aac9fc
riscv-pk: 6b501de8e8512abd969199c57c5531737e52a105
riscv-linux: fe92d7905c6ea0ebeabeb725b8040754ede7c220
```

Then, you can clone PHMon repository.
``` bash
cd rocket-chop
export ROCKETCHIP_DIR = `pwd`
git clone git@github.com:bu-icsg/PHMon.git
cd PHMon
git checkout --track origin/Code
cd varanus
Export PHMon=`pwd`
git submodule update --init --recursive
cd ../..
git apply $PHMon/patches/rocket.patch
cd PHMon/varanus
./install_symlink
pushd ../../..
git apply $PHMon/patches/fpga_config.patch
popd
```

Then, you need to install the RISC-V [toolchain](https://github.com/riscv/riscv-gnu-toolchain). Please make sure to use the exact commits as specified above for both riscv-tools and riscv-pk. Also, make sure to apply the follwoing patch before building the toolchain:

``` bash
cd $ROCKETCHIP_DIR/riscv-tools/riscv-pk
git apply $PHMon/patches/pk.patch
```

In the rest of this document, we assume that you have already installed the RISC-V GNU compiler toolchain. Please make sure that you install both the Newlib and Linux cross compilers and propberly specify the RISCV environment variable. After the installation, update the RV_TARGET in $PHMon/Makefile with the path to your installed toolchain.

# Emulation

In $PHMon/src/test/c, we have included 3 sample c files for programming PHMon, i.e., shadow_stack.c, afl_ndisasm.c, and debug_break.c:

``` bash
cd $PHMon
make
```
You can get familiar with programming PHMon by looking at these c files. Later on, we will include a guideline for Programming PHMon. We have also included a test that can be run in the emulator with pk (without the OS support). To run this test (that programs PHMon to act as a shadow stack for its execution), you can use the emulator:

``` bash
cd $ROCKETCHIP_DIR/emulator
make CONFIG=KomodoCppConfig ROCKETCHIP_ADDONS=varanus
./emulator-freechips.rocketchip.system-KomodoCppConfig pk ../PHMon/varanus/build/komodo_test.rv
```

For debugging purposes you can use the verbose mode:
`` bash
./emulator-freechips.rocketchip.system-KomodoCppConfig +verbose pk ../PHMon/varanus/build/komodo_test.rv 2>&1 | grep "KOMODO\|DEBUG" | tee trace.log
```

# FPGA Evaluation

For further testing, we move onto the FPGA. We use the fpga-zynq repository for synthesizing Rocketchip. The fpga-zynq repository is designed to work with various Zynq FPGA boards using Vivado 2016.2. Please make sure to use the exact version of Vivado. We have only tested our design on Zynq Zedboard FPGA. To synthesize Rocketchip with PHMon for Zedboard, run the following script:

``` bash
./$PHMon/tools/fpga.sh
```

The synthesize step can take up to 30 minutes. At the end, you will get a bit.bin file in $ROCKETCHIP_DIR/../fpga-zynq/zedboard/fpga-images-zedboard/rockerchip_wrapper.bit.bin. You can scp this file to your Zedboard FPGA and reconfigure the FPGA:

``` bash
cat rocketchip_wrapper.bit.bin > /dev/xdevcfg
```

## Booting Linux Kernel 
To boot up Linux on Rocket core on the FPGA, you need to create a Berkeley Boot Loader (BBL). In our experiments, we use the Linux Kernel 4.15 and provide you our kernel patches in $PHMon/patches/kernel.patch. In the patches folder, we have also provided the .config file for our kernel. We follow the instructions from https://github.com/riscv/riscv-tools/tree/8ad8d4839acf2cdac0129b8fed8fe12136e77307#-building-the-linux-kernel-040--%CE%B5-sbu to create the busy box, initramfs, and bbl (we will provide step by step instructions in near future). After creating the bbl, you can scp it to the FPGA and boot up linux:

``` bash
./fesvr-zynq bbl
```
