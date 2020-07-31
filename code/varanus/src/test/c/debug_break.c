#include <stdio.h>
#include <stdlib.h>
#include "varanus.h"

int main(int argc, char *argv[]) {

  int break_pc = 0;
  if (argc > 1) {
    break_pc = atoi(argv[1]);
  }
  mask_t mask_inst;
  act_conf_table_t action_mu1;
  // Setup an instruction mask that'll match all program counters, but
  // only the breakpoint pc
  mask_inst.care.pc_src    = 0x0000000000010550;
  mask_inst.dont_care.pc_src   = 0x0000000000000000;
  mask_inst.care.pc_dst    = 0x0000000000000000; // match all PC_dst
  mask_inst.dont_care.pc_dst   = 0xffffffffffffffff;
  mask_inst.care.inst  = 0x00000000;
  mask_inst.dont_care.inst = 0xffffffff;
  mask_inst.care.rd = 0x0000000000000000;
  mask_inst.dont_care.rd = 0xffffffffffffffff;
  mask_inst.care.data = 0x0000000000000000;
  mask_inst.dont_care.data = 0xffffffffffffffff;

  komodo_reset_val(0);
  komodo_pattern(0, &mask_inst);
  
  action_mu1.op_type = e_OP_INTR; //interrupt
  action_mu1.in1 = e_IN_LOC3; //Local3
  action_mu1.in2 = e_IN_CONST; //Constant
  action_mu1.fn = e_ALU_SLT; //Set Less Than
  action_mu1.out = e_OUT_INTR; //Interrupt reg
  action_mu1.data = 0;
  komodo_action_config(0, &action_mu1);
  
  xlen_t match_count = 0;
  
  // Set match conditions
  komodo_match_count(0, break_pc+1, &match_count);
  
  komodo_set_commit_index(0, 0);
  
  komodo_set_mem_typ(3);

  return 0;
  
}
