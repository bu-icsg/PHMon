#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "varanus.h"

// nop different from RISC-V's normal nop. Has binary format 0x00001013.
#define VARANUS_NOP asm volatile ("slli x0, x1, 0")

int test() {
  int a = 20;
  return a*2;
}

void doorbell_hello(int value) {
  printf("[INFO] Doorbell! Value: %d\n", value);
}

int main (int argc, char * argv[]) {
  mask_t mask_inst/*, mask_pc*/, mask_inst2;
  act_conf_table_t action_mu1, action_mu2;
  xlen_t *komodo_sp = (xlen_t*) calloc(50,sizeof(long unsigned int));

  komodo_set_local_reg(0, komodo_sp);
  komodo_set_local_reg(1, komodo_sp);
  printf("komodo_sp @ 0x%p\n", komodo_sp); 
  
  // Setup an instruction mask that'll match all program counters, but
  // only our special nop
  mask_inst.care.pc_src    = 0x0000000000000000; // match all PC_src
  mask_inst.dont_care.pc_src   = 0xffffffffffffffff;
  mask_inst.care.pc_dst    = 0x0000000000000000; // match all PC_dst
  mask_inst.dont_care.pc_dst   = 0xffffffffffffffff;
  mask_inst.care.inst  = 0x000000e7; // match Call insts
  mask_inst.dont_care.inst = 0xfffff008;
  mask_inst.care.rd = 0x0000000000000000;
  mask_inst.dont_care.rd = 0xffffffffffffffff;
  mask_inst.care.data = 0x0000000000000000;
  mask_inst.dont_care.data = 0xffffffffffffffff;

  mask_inst2.care.pc_src    = 0x0000000000000000; // match all PC_src
  mask_inst2.dont_care.pc_src   = 0xffffffffffffffff;
  mask_inst2.care.pc_dst    = 0x0000000000000000; // match all PC_dst
  mask_inst2.dont_care.pc_dst   = 0xffffffffffffffff;
  mask_inst2.care.inst = 0x00008067; // match Ret insts
  mask_inst2.dont_care.inst = 0x00000000;
  mask_inst2.care.rd = 0x0000000000000000;
  mask_inst2.dont_care.rd = 0xffffffffffffffff;
  mask_inst2.care.data = 0x0000000000000000;
  mask_inst2.dont_care.data = 0xffffffffffffffff;

  // Set the patterns and reset the stored comparator values.
  komodo_reset_val(0);
  komodo_pattern(0, &mask_inst);
  komodo_reset_val(1);
  komodo_pattern(1, &mask_inst2);

  // ************** MU0 **************** //
  //Increment the Shadow stack pointer
  action_mu1.op_type = e_OP_ALU; //ALU operation
  action_mu1.in1 = e_IN_LOC2; //Local2
  action_mu1.in2 = e_IN_CONST; //Constant
  action_mu1.fn = e_ALU_ADD; //Add
  action_mu1.out = e_OUT_LOC2; //Local2
  action_mu1.data = 8; //Cxonstant Data = 8
  komodo_action_config(0, &action_mu1);
    
  //Increment PC_src of Call inst by 4
  action_mu1.op_type = e_OP_ALU; //ALU operation
  action_mu1.in1 = e_IN_DATA_MU; //MU_DATA
  action_mu1.in2 = e_IN_CONST; //Constant
  action_mu1.fn = e_ALU_ADD; //ADD
  action_mu1.out = e_OUT_LOC3; //Local3
  action_mu1.data = 4; //Constant Data = 4
  komodo_action_config(0, &action_mu1);

  //Decrement PC_src of call inst for compressed instruction
  action_mu1.op_type = e_OP_ALU; //ALU operation
  action_mu1.in1 = e_IN_LOC3; //Local3
  action_mu1.in2 = e_IN_COMPRESSED; //INST_TYPE
  action_mu1.fn = e_ALU_SUB; //SUB
  action_mu1.out = e_OUT_LOC3; //Local3
  action_mu1.data = 0; //Constant Data = 0
  komodo_action_config(0, &action_mu1);
  
  //Decrement PC_src of call inst for compressed instruction
  action_mu1.op_type = e_OP_ALU; //ALU operation
  action_mu1.in1 = e_IN_LOC3; //Local3
  action_mu1.in2 = e_IN_COMPRESSED; //INST_TYPE
  action_mu1.fn = e_ALU_SUB; //SUB
  action_mu1.out = e_OUT_LOC3; //Local3
  action_mu1.data = 0; //Constant Data = 0
  komodo_action_config(0, &action_mu1);
  
  //Write the PC_src of Call inst in shared memory space
  action_mu1.op_type = e_OP_MEM_WR; // Memory write
  action_mu1.in1 = e_IN_LOC3; //Local3
  action_mu1.in2 = e_IN_LOC2; //Local2
  action_mu1.fn = e_ALU_NOP;  //NOP
  action_mu1.out = e_OUT_LOC1;
  action_mu1.data = 0;
  komodo_action_config(0, &action_mu1);
  
  // **************** MU1 ***************** //  
  //store PC_dst in local3 
  action_mu2.op_type = e_OP_ALU; //ALU operation
  action_mu2.in1 = e_IN_DATA_MU; //DATA_MU
  action_mu2.in2 = e_IN_CONST; //Constant
  action_mu2.fn = e_ALU_SUB; //SUB
  action_mu2.out = e_OUT_LOC3; //Local3
  action_mu2.data = 0; //Constant Data = 0
  komodo_action_config(1, &action_mu2);
    
  //Read the most recent PC_src of call stored in the share memory space
  action_mu2.op_type = e_OP_MEM_RD; //Memory read
  action_mu2.in1 = e_IN_DATA_MU; //Doesn't matter, it's read
  action_mu2.in2 = e_IN_LOC2; //Local2
  action_mu2.fn = e_ALU_NOP;  //Doesn't matter, no ALU operation
  action_mu2.out = e_OUT_LOC1; //Doesn't matter, it get stored in mu_resp
  action_mu2.data = 0; //Doesn't matter, no ALU operation
  komodo_action_config(1, &action_mu2);

  //Comapre pc_dst and pc_src and trigger interrupt
  action_mu2.op_type = e_OP_ALU; //ALU operation
  action_mu2.in1 = e_IN_DATA_RESP; //MU_resp
  action_mu2.in2 = e_IN_LOC3; //Local3
  action_mu2.fn = e_ALU_SEQ; //Set Equal
  action_mu2.out = e_OUT_INTR; //Interrupt reg
  action_mu2.data = 0;
  komodo_action_config(1, &action_mu2);

  //Decrement shadow stack pointer
  action_mu2.op_type = e_OP_ALU; //ALU operation
  action_mu2.in1 = e_IN_LOC2; //Local2
  action_mu2.in2 = e_IN_CONST; //Constant
  action_mu2.fn = e_ALU_SUB; //Subtract
  action_mu2.out = e_OUT_LOC2; //Local2
  action_mu2.data = 8; //Constant Data = 8
  komodo_action_config(1, &action_mu2);
    
  xlen_t match_count = 0;
  xlen_t match_count2 = 0;

  // Set match conditions
  komodo_match_count(0, 1, &match_count);
  komodo_match_count(1, 1, &match_count2);

  // Set memory type
  komodo_set_mem_typ(3);
  
  
  komodo_set_commit_index(0, 0);
  komodo_set_commit_index(1, 1);
  komodo_set_local_reg(0, komodo_sp);
  
  VARANUS_NOP;
  komodo_enable_all();
  VARANUS_NOP;
  test();
  VARANUS_NOP;
  VARANUS_NOP;
  test();
  printf("test\n");
  VARANUS_NOP;
  komodo_disable_all();
  VARANUS_NOP;

  for (int i=0; i<10; i++) {
    printf("[INFO] sp @%p = 0x%lx\n", (komodo_sp+i), *(komodo_sp+i));
  }
  
  return 0;
}
