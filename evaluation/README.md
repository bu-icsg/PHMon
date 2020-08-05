  # Quick instructions
  In this folder, we provide the binaries required for running each of PHMon use cases on a Zedboard FPGA.
  The baseline folder contains the files for the baseline unmodified Rocket core experiments while the PHMon
  folder contains the files for PHMon-based experiments.
  
  ## <a name="toc"></a> Table of Contents
  - [The experiment environment](#env)
  - [Running the experiments](#exp)
    - [Shadow Stack](#shadowstack)
    - [AFL](#afl)
    - [Accelerated Debugging](#debug)
    - [Detecting Information Leakage](#leakage)
    
## <a name="env"></a> The experiment environment
As mentioned before, we assume you have access to a Zedboard FPGA.
We suggest you to use screen for connecting to your FPGA board, rather than ssh, e.g.:

```
  $ screen -S ACM0 /dev/ttyACM0 115200
```

Please make sure to use 115200 as the baud rate in the screen command.
After connecting to ACM0 FPGA, you have access to the Processing System (PS) side of your zedboard.
We provide the required files to configure the Programmable Logic (PL) side of the zedboard with a
baseline Rocketchip processor and to boot up the Linux kernel.

You can find the necessary files and scripts to configure the FPGA, boot up the Linux kernel, and run the
baseline and PHMon experiments in the baseline and PHMon subfolders of the evaluation folder, respectively.

We suggest you to detatch (ctrl a + d) from the screen session when you do not run the experiments
(specially for the AFL use case). To reattach to the screen session use the following command:

```
  $ screen -rd -S ACM0
```

For each use case, we provide a script to boot up Linux. Because of our FPGA limitations, it takes
about 2 minutes for the Linux to boot up (please be patient).
After you are done with one set of experiments, the easiest way to exit the Linux environment is just
by terminating the process using ctrl+c.

###  <a name="exp"></a> Running the experiments
For running the experiments, you need to make sure that you have the rocketchip_wrapper.bit.bin, fesvr-zynq,
and the bbl as well as the script for the target use case (e.g., bbl_afl and afl.sh) on the PS side of the 
zedboard.


### <a name="shadowstack"></a> Shadow Stack
For the shadow stack use case, we provide a benign benchmark and two programs vulnerable to buffer overflow
attacks.
Our vulnerable programs use the strcpy function and we choose the input to the program in a way to exploit
this vulnerability.

#### Shadow Stack: Baseline
To run our baseline experiment for the shadow stack use case, first run the shadow stack script
(shadow_stack.sh) on ACM0:

```
  $ ./shadow_stack.sh
```

After the Linux kernel boots up, you can run the first vulnerable program:

```
  $ ./run_vuln1.sh
```

You can use ctrl + c to terminate the execution. After you boot up the kernel again, please run
the second vulnerable program:

```
  $ ./run_vuln2.sh
```

To run the benign benchmark with test input, execute the run_mcf.sh script:

```
  $ ./run_mcf.sh
```

The values printed as t0 and t1 are the time-stamps at the beginning and end of the
process. However, these values are not in wall clock time. To get the time in seconds, you need to
multiply the printed time with a constant factor of 4.0146. To get the execution, just calculate
the time difference between t1 and t0.
      
#### Shadow Stack: PHMon
To run the shadow stack use case on PHMon, first run the shadow stack script (shadow_stack.sh) on
ACM0:

```
  $ ./shadow_stack.sh
```
  
After the Linux kernel boots up, execute the run_shadow_stack.sh script:

```
  $ ./run_shadow_stack.sh
```

This script first disables ASLR (we do this to simplify our buffer overflow attacks). Then, it
configures PHMon to act as a shadow stack. Subsequently, it runs the first vulnerable program with
an input that exploits this vulnerability. When PHMon detects a mismatch between call and return,
it triggers an interrupt and the interrupt handler terminates the execution of the program. Then,
the script runs the second vulnerable program. Similar to the first vulnerable program, PHMon
successfully detects a mismatch between a call and a return and triggers an interrupt. After this,
the script runs mcf (one of the SPEC 2000) benchmarks with the test input. Since this program is
not malicious, PHMon does not detect any call/ret violation. You can simply test the shadow stack
for other applications such as ls and cat. To calculate the execution time of each program, just
subtract the time reported in t0 from the time reported in t1.

### <a name="afl"></a> AFL
For the hardware-accelerated fuzzing use case, we use PHMon to implement the instrumentation suite
of the AFL. To demonstrate the AFL acceleration, we use one of the vulnerable programs evaluated
in the paper, i.e., nasm.

#### AFL: Baseline
For the baseline experiment, we integrated AFL into the user mode QEMU version 2.7.5. To run the
baseline QEMU-based AFL, first run the afl script (afl.sh) on ACM0:

```
  $ ./afl.sh
```

Please wait until Linux boots up. Then, execute the run_afl.sh script:

```
  $ ./run_afl.sh
```

Before AFL starts fuzzing, you will see a warning: “The target binary is pretty slow!”. Since we
can run programs on our FPGA with a frequency of 25 MHz, fuzzing on the FPGA is very slow compared
to fuzzing on modern processors. Please ignore this warning. Shortly after that, AFL will start
fuzzing. During fuzzing, “exec speed” represents the number of executions per second. We use “exec
speed” as our performance metric. In the overall results section, you can see the number of found
unique crashes. After a while, AFL will find several unique crashes (this might take up to 30
minutes for our baseline AFL experiment). To examine the unique crashes, you can terminate the
afl-fuzz process. After you find the PID for afl-fuzz process, terminate the process and test the
unique crashes. You can find the found crashes in the home/findings/crashes folder:

```
  $ kill -9 PID_afl-fuzz
  $ cd home/findings/crashes
```

To test the found crashes, you can feed the files to the ndisasm binary as an input:

```
  $ ../../ndisasm id:000000,sig:11,src:000000,op:flip1,pos:2
```

To run the fork server version, execute the run_afl_fork_server.sh script:

```
  $ ./run_afl_fork_server.sh
```

During the fork server execution, you will notice that the performance has improved. This time AFL
will find the unique crashes faster.

#### AFL: PHMon
To run the PHMon-based afl use case, first run the afl script (afl.sh) on ACM0:

```
  $ ./afl.sh
```

After Linux boots up, execute the run_afl.sh script:

```
  $ ./run_afl.sh
```

This script configures PHMon to accelerate the fuzzing for ndisasm process. Then, it runs the
PHMon accelerated AFL. This time, you will notice that AFL is running faster (you can compare the
reported “exec speed” with that of the baseline and the fork server versions). Additionally, AFL
will find the unique crashes much faster compared to the baseline (in about a minute). Similar to
the baseline case, you can terminate the afl-fuzz process and test the found crashes in the
home/findings/crashes folder.

### <a name="debug"></a> Accelerated Debugging
For the accelerated debugging use case, we have a simple program (dump.c) consisting of a for loop
(from 0 to 10000), where the for loop just prints the value of the loop index (i). Our goal is to
have a conditional breakpoint over i (e.g., when i = 1000).

#### Debugging: Baseline
To run the baseline of our debugging use case, first run the debug script on ACM0:

```
  $ ./debug.sh
```

Once the Linux kernel boots up, execute the run_debug.sh script:

```
  $ ./run_debug.sh
```

This script sets a conditional breakpoint over the loop index (if break dump.c:8 if i==1000) and
runs the dump.rv inside GDB. After the execution reaches the breakpoint, you can debug the program
execution. To measure the execution time, subtract t0 from t1. Note that to get the real time in
seconds you need to multiply the calculated time by a factor of 4.0146.
  
#### Accelerated Debugging: PHMon
We use PHMon to accelerate the debugging of our conditional breakpoint. To run the accelerated
debugging with PHMon support, first run the debug script (debug.sh) on ACM0:

```
  $ ./debug.sh
```
After the Linux kernel boots up, execute the run_debug.sh script:

```
  $ ./run_debug.sh
```

This script first configures PHMon for accelerating the debugging of a conditional breakpoint
(using break.rv file). The configuration file (break.rv) receives an input argument that specifies
the value of the conditional breakpoint (1000 in this example). After configuring PHMon,
run_debug.sh script runs the dump.rv binary inside GDB without any software breakpoints. When the
execution reaches i = 1000, PHMon triggers an interrupt and traps into GDB. Now, you can debug the
program execution:

```
  $ bt
  $ display/i $pc
  $ info locals
  $ step
  $ quit
```

You can take a look at time-stamp of catching the interrupt and t0 value printed during the
execution of the dump.rv to measure the execution time of the program with PHMon accelerated
debugging (PHMon_GDB).
To test the core dump version of the accelerated debugging, execute the run_core_debug.sh script:

```
  $ ./run_core_debug.sh
```

This script turns-on the core dump, then it runs the ./dump.rv again. When the execution reaches
the specified value of the loop, PHMon triggers an interrupt and the interrupt handler terminates
the execution and generates a core dump file. Then, the script runs GDB for dump.rv with the core
dump. Now, you can backtrace the execution:

```
  $ bt
  $ info locals
  $ info registers
  $ quit
```

As mentioned before, you can take a look at t1 and t0 values to calculate the execution time of
the program for the core dump version (PHMon_CoreDump). Note that the reported times are not real
seconds. To get the time in seconds, you need to multiply the time reported in the kernel by a
constant value of 4.0146.

To measure the execution time for a conditional breakpoint over different loop index values, you
can configure PHMon with a different threshold:

```
  $ cd home
  $ ./break.rv <Your loop index>
  $ ./gdb dump.rv
  $ run
  $ quit
```

### <a name="leakage"></a> Detecting Information Leakage
Due to the complications for testing the information leakage detection use case, we have not
included this use case in the repository.
If you are interested in this use case, please contact us.
