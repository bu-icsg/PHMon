# PHMon
## Overview
In this document, we provide a guideline for running the use cases of our Programmable Hardware
Monitor (PHMon) [1]. We run our experiments on the Xilinx Zynq Zedboard FPGA and use a modified Linux
kernel 4.15 to provide the support for our hardware.

## Accessing the experiment environment
To simplify the process of testing our use cases, we provide you with access to our FPGA
boards. We have already created a guest account on one of our machines and provided you the
private ssh key (please refer to the “optional bidding instructions” in the submission
website). You can ssh to our machine after configuring the private ssh key:

```  
  $ ssh phmon@icsg-04.bu.edu
```

You have access to two of our FPGAs available at /dev/ttyACM0 and /dev/ttyACM1. For simplicity, in
the rest of this document we call the FPGA connected to /dev/ttyACM0 as ACM0 and the one connected
to /dev/ttyACM1 as ACM1. We suggest you to use screen for connecting to these two FPGAs (we have
provided you screen access to these two nodes). However, if you want to do ssh or scp to ACM0 and
ACM1, their ip addresses are 192.168.1.30 and 192.168.1.31, respectively. The username and
password of the FPGAs are both root.
We have already configured ACM0 and ACM1 with necessary files to run the baseline and PHMon
experiments, respectively. To access the experiment environment of the baseline, connect to ACM0
using screen:

```
  $ screen -S ACM0 /dev/ttyACM0 115200
```

Please make sure to use 115200 as the baud rate in the screen command. After connecting to ACM0
FPGA, you have access to the Processing System (PS) side of our zedboard. If you run ls here, you
will see all the required files to configure the Programmable Logic (PL) side of the zedboard with
a baseline Rocketchip processor and to boot up the Linux kernel.
Similarly, you can connect to ACM1, the experiment environment for PHMon:

```
  $ screen -S ACM1 /dev/ttyACM1 115200
```

Here, you have all the required files to configure the PL side of the zedboard with a Rocket
processor integrated with PHMon and to run the Linux kernel.
For more information about useful screen commands, please refer to the screen man page
(https://linux.die.net/man/1/screen). We suggest you to detatch (ctrl a + d) from the screen
session when you do not run the experiments. To reattach to the screen session use the following
command:

```
  $ screen -rd -S ACM0
  $ screen -rd -S ACM1
```

As mentioned before, we provide the necessary files and scripts to configure the FPGA, boot up the
Linux kernel, and run the experiments. However, if you accidentally remove these files, you can
get them from this repository and scp them to the FPGA. The PHMon files and baseline files are in
the PHMon and baseline folders of this repository, respectively. Additionally, if the FPGA becomes
unresponsive, you can reboot it using the reboot command. Please note that after a reboot, you
need to remove all the default files on the PS side of the FPGA and replace them with the files
provided in this repository.

For each use case, we provide a script to boot up Linux. Because of our FPGA limitations, it takes
about 2 minutes for the Linux to boot up (please be patient). After you are done with one set of
experiments, the easiest way to exit the Linux environment is just by terminating the process
using ctrl+c.

## Running the experiments

### Shadow Stack
For the shadow stack use case, we provide a benign benchmark (mcf from SPEC2006 benchmark suite)
and two programs vulnerable to buffer overflow attacks. Our vulnerable programs use the strcpy
function and we choose the input to the program in a way to exploit this vulnerability.

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

To run the mcf benign benchmark with test input, execute the run_mcf.sh script:

```
  $ ./run_mcf.sh
```

The values printed as t0 and t1 are the time-stamps at the beginning and end of the
process. However, these values are not in wall clock time. To get the time in seconds, you need to
multiply the printed time with a constant factor of 4.0146. To get the execution, just calculate
the time difference between t1 and t0.
      
#### Shadow Stack: PHMon
To run the shadow stack use case on PHMon, first run the shadow stack script (shadow_stack.sh) on
ACM1:

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

### AFL
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
To run the PHMon-based afl use case, first run the afl script (afl.sh) on ACM1:

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

### Accelerated Debugging
For the accelerated debugging use case, we have a simple program (dump.c) consisting of a for loop
(from 0 to 10000), where the for loop just prints the value of the loop index (i). Our goal is to
have a conditional breakpoint over i (e.g., when i = 1000).

####Debugging: Baseline
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
debugging with PHMon support, first run the debug script (debug.sh) on ACM1:

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

### Detecting Information Leakage
Due to the complications for testing the information leakage detection use case, we have not
included this use case in our artifact evaluation.

## Reference
[1] Leila Delshadtehrani, Sadullah Canakci, Boyou Zhou, Schuyler Eldridge, Ajay Joshi, and Manuel
  Egele. "PHMon: A Programmable Hardware Monitor and Its Security Use Cases", USENIX
  Security, 2020. [online] https://www.usenix.org/system/files/sec20spring_delshadtehrani_prepub.pdf