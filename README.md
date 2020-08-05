# PHMon
## Overview
In this document, we provide a guideline for running the use cases of our Programmable Hardware
Monitor (PHMon) [1]. We run our experiments on the Xilinx Zynq Zedboard FPGA and use a modified Linux
kernel 4.15 to provide the support for our hardware.

**Note: Currently, you need to have access to a Xilinx Zynq Zedboard FPGA to be able to run PHMon experiments.**

This README contains 3 sets of instructions:

1. Quick instructions: This is a quick way to leverage our available binaries in the evaluation folder
to test our various use cases on an FPGA.
1. Using a Docker image: This is an easy way to use our pre-installed tools to test new programs with our
use cases or to program PHMon for new use cases.
1. Building everything from scratch: In this guideline, we provide instructions to build all the required
tools, apply our patches, generate the bitstream, and finally run our use cases on an FPGA. You can make
modifications to our hardware design and build everything from scratch following this guideline.

## <a name="toc"></a> Table of Contents
- [1) Quick instructions](#quick)
- [2) Using a Docker image](#docker)
- [3) Building everything from scratch](#build)
  - [Overview](#overview)
  - [Building the tools](#tools)
  - [Running PHMon on Emulator using pk](#emulator)
  - [Running PHMon on an FPGA using Linux kernel](#fpga)
- [Additional Documentation](#additional)
  - [Publications](#publications)
  - [Workshop presentations](#workshops)

## <a name="quick"></a> 1) Quick instructions
In the rest of this document, we assume you have access to a Zedboard FPGA.
If you plan to use a different FPGA board, please file an issue so we can add the support for your
target board.
The evaluation folder contains the files required for running our use cases.
We provide a detailed README in this folder about each of the use cases.
In summary, you need to scp the bitstream (rocketchip_wrapper.bit.bin), fesvr-zynq, and bbl to your
FPGA.
Then, you can reconfigure the FPGA with the bitstream and bootup the Linux kernel.
```
$ cat rocketchip_wrapper.bit.bin > /dev/xdevcfg 
$ ./fesvr-zynq bbl
```

## <a name="docker"></a> 2) Using a Docker image
We have an Ubuntu Docker [image](https://hub.docker.com/r/ldelshad/phmon) with pre-installed RISC-V
tools that you can leverage to easily add new programs for evaluation with our use cases or program
PHMon for new use cases.
You can run our Docker image using the following command:
```
$ docker run -it ldelshad/phmon:latest /bin/bash
```
For more information on how to use the docker, please refer to the README file inside the image.
In short, to generate a bbl inside the docker image, follow these commands:
```
$ cd code
$ ./patch.sh
$ ./run_linux.sh
```
You can find the generated bbl in /home/PHMon/code/riscv-pk/build folder and copy it from the docker
to your local machine and FPGA for evaluation.
```
$ docker ps
$ docker cp conainer-id:/home/PHMon/code/riscv-pk/build/bbl .
$ scp bbl root@192.168.1.5:/home/root
```

## <a name="build"></a> 3) Building everything from scratch
In this part of the guideline, we provide the instructions for building everything from scratch.
In this guideline, we assume that you do not have an available version of the RISC-V gnu toolchain.
In case you have the RISC-V toolchain, you can comment the commands for installing the toolchain in
the install.sh script.

### <a name="overview"></a> Overview
The code folder contains the required files for building everything from scratch.
Inside the code folder, we have the required scripts for building everything and running PHMon on
an emulator as well as Linux kernel on the Zedboard FPGA.
Additionally, you can find our hardware source code in the varanus folder and all of our patches in the 
patches folder.

### <a name="tools"></a> Building the tools
In this project, we use a stable version of the Rocket core.
Currently, we use the version in FPGA-zynq [repository](https://github.com/ucb-bar/fpga-zynq)
(and yes we know that FPGA-zynq repository has been deprecated).

In this project, we rely on the RISC-V GNU toolchain [repository](https://github.com/riscv/riscv-gnu-toolchain).
Installing this toolchain, requires severak standard packages.
First, use the following command to ensure you have all the required packages:
```
sudo apt-get install autoconf automake autotools-dev curl python3 libmpc-dev libmpfr-dev libgmp-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev
```

To install all the reuqired tools and apply the patches, follow these commands:

```
$ cd code
$ ./install.sh
```
Please note that this step is very time consuming, it installs the full RISC-V toolchain and builds the
RISC-V Linux kernel.
After this step, you should have all the requirements for running PHMon.

### <a name="emulator"></a> Running PHMon on Emulator using pk
In this step, we rely on the Rocket Chip Emulator to run a program on the Rocket core interfaced with PHMon
using the proxy kernel (pk).
You should pass the program RISC-V binary as an argument to the run_emulator.sh script:
```
cd code
$ ./run_emulator.sh varanus/build/komodo_test.rv
```

### <a name="fpga"></a> Running PHMon on an FPGA using Linux kernel
We assume that you will use our provided bitstream for the zedboard FPGA containing the Rocket core interfaced
with PHMon.
You can find this bitsream in evaluation/PHMon/rocketchip_wrapper.bit.bin, scp it to the zedboard and reconfigure
the FPGA:
```
$ cat rocketchip_wrapper.bit.bin > /dev/xdevcfg 
```

To run a program on the FPGA, we provide the run_linux.sh script:
```
$ ./run_linux.sh
```

If you are interested in modifying the PHMon code or configuration and building the bitstream from scratch,
You can use the fpga.sh script:
```
$ ./fpga.sh
```
This requires a Xilinx license; please note that we synthesize our desing using Vivado 2016.2.

## <a name="additional"></a> Additional Documentation
If you use this repository for research, please cite our USENIX Security paper:
```
@inproceedings {244034,
author = {Leila Delshadtehrani and Sadullah Canakci and Boyou Zhou and Schuyler Eldridge and Ajay Joshi and Manuel Egele},
title = {PHMon: A Programmable Hardware Monitor and Its Security Use Cases},
booktitle = {29th {USENIX} Security Symposium ({USENIX} Security 20)},
year = {2020},
isbn = {978-1-939133-17-5},
pages = {807--824},
url = {https://www.usenix.org/conference/usenixsecurity20/presentation/delshadtehrani},
publisher = {{USENIX} Association},
month = aug,
}
```

### <a name="publications"></a> Publications
[1] Leila Delshadtehrani, Sadullah Canakci, Boyou Zhou, Schuyler Eldridge, Ajay Joshi, and Manuel
  Egele. "PHMon: A Programmable Hardware Monitor and Its Security Use Cases", *USENIX
  Security*, 2020.
  * [Paper](https://www.usenix.org/system/files/sec20spring_delshadtehrani_prepub.pdf)

[2] Leila Delshadtehrani, Schuyler Eldridge, Sadullah Canakci, Manuel Egele, and Ajay Joshi. 
  "Nile: A Programmable Monitoring Coprocessor", *IEEE Computer Architecture Letters (CAL)*, 17(1), 2017.
  * [Paper](http://people.bu.edu/joshi/files/hw-monitors-cal-2017.pdf)

### <a name="workshops"></a> Workshop presentations
[3] Leila Delshadtehrani, Sadullah Canakci, Boyou Zhou, Schuyler Eldridge, Ajay Joshi, and Manuel
  Egele. "A Programmable Hardware Monitor for Security of RISC-V Processors", *Boston Area Architecture
  Workshop (BARC)*, 2020.
  * [Paper](http://people.bu.edu/joshi/files/PHMon-barc-2020.pdf)

[4] Leila Delshadtehrani, Sadullah Canakci, Boyou Zhou, Schuyler Eldridge, Ajay Joshi, and Manuel
  Egele. "A Chisel-Based Programmable Hardware Monitor", *Chisel Community Conference (CCC)*, 2020.
  * [Presentation](https://youtu.be/IaVtwqW00Uk)

[5]  Leila Delshadtehrani, Jonathan Appavoo, Manuel Egele, Ajay Joshi, and Schuyler Eldridge.
"Varanus: An Infrastructure for Programmable Hardware Monitoring Units", *Boston Area Architecture
  Workshop (BARC)*, 2017.
  * [Paper](https://megele.io/varanus-barc2017.pdf)

