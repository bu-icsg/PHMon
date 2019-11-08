#!/bin/bash
cat rocketchip_wrapper.bit.bin > /dev/xdevcfg
./fesvr-zynq bbl_afl
