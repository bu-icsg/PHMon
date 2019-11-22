#ifndef __VARANUS_H__
#define __VARANUS_H__

#include <stdint.h>
#include <inttypes.h>

#include "../../../rocket-rocc-examples/rocc-software/src/xcustom.h"

#define XCUSTOM_KOMODO 1
// Specific types most closely aligned with their rocket counterparts.
typedef uint64_t xlen_t; // RISC-V word length: 64 bits
typedef xlen_t pc_t;     // program counter:    64 bits
typedef uint32_t inst_t; // instruction:        32 bits
typedef xlen_t  rd_t;   // rd index:            5 bits
typedef xlen_t data_t;   // data:               64 bits
typedef unsigned char op_t; // config table operands  8 bits

// Data structure encapsulating everything communicated by one entry
// in the commit log. This should match the `CommitLog` class from
// src/main/scala/Varanus.scala.
typedef struct {
  pc_t pc_src;
  pc_t pc_dst;
  inst_t inst;
  inst_t priv;
  rd_t rd;
  data_t data;
} commit_log_t;

// Definition of a match on mask that can be used to match a specific
// commit log. To enable matching of don't cares this requires two
// masks, "care" and "don't care".
typedef struct {
  commit_log_t care;
  commit_log_t dont_care;
} mask_t;

//-------------------------------------- Config Unit
// Data structure encapsulating everything communicated by one entry
// in the config table. This should match the `ActionConfigTable` class
// from src/main/scala/ConfigUnit.scala.
typedef struct {
  op_t op_type;
  op_t in1;
  op_t in2;
  op_t fn;
  op_t out;
  xlen_t data;
} act_conf_table_t;

//-------------------------------------- Komodo Monitor

// Data structure describing a doorbell
typedef struct {
  xlen_t value;
  xlen_t * addr;
  void (* action)();
} doorbell_t;

// For a specific comparator number, change its action to the
// specified config action.
void komodo_action_config(uint32_t comparator, act_conf_table_t * action);

// For a specific comparator number, change its trigger to the
// specified mask.
void komodo_pattern(uint32_t comparator_number, mask_t * mask);

// Enable matching for all comparing units
#define komodo_reset_all()  \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, 0, 0, 2, 11, 12);

// Enable matching for all comparing units
#define komodo_enable_all()  \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, 0, 0, 3, 11, 12);
  //asm volatile ("custom1 0, 0, 0, 3")

// Disable matching for all comparing units
#define komodo_disable_all()   \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, 0, 0, 4, 11, 12);

// Dequeue one element from activation queue
#define komodo_dequeu()   \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, 0, 0, 10, 11, 12);

// Enable matching for a specific comparator unit (specified with a
// comparator number).
void komodo_enable(uint32_t comparator_number);

// Disable matching for a specific comparator unit.
void komodo_disable(uint32_t comparator_number);

// Reset the match count for a specific comparator unit.
void komodo_reset_val(uint32_t comparator_number);

// Write the match count
void komodo_match_count(uint32_t comparator_number, data_t count, xlen_t * addr);

// Write the instruction match count
void komodo_match_count_instruction(uint32_t comparator_number, data_t count,
                                    xlen_t * addr);

// Read the value of a specific comparator
int64_t komodo_read_count(uint32_t comparator_number);

// Read the number of elapsed instructions for a specific comparator
int64_t komodo_read_instruction_count(uint32_t comparator_number);

void komodo_set_mem_typ(xlen_t type);
void komodo_set_sp_base(xlen_t *addr);
void komodo_set_sp_offset(xlen_t *addr);
void komodo_set_local_reg(uint32_t index, xlen_t *addr);

void komodo_set_commit_index(uint32_t comparator, data_t index);

// Get information about the Komodo monitor
uint64_t komodo_info();

uint64_t komodo_info_sp_base();
uint64_t komodo_info_sp_offset();
uint64_t komodo_done_info();
uint64_t komodo_wait_req();
uint64_t komodo_wait_resp_info(uint64_t action);
void komodo_mem_cmd(uint64_t  addr, uint64_t data);
uint64_t komodo_read_mask(uint32_t comparator, uint64_t action);
uint64_t komodo_read_conf(uint32_t comparator, uint64_t action);
uint64_t komodo_read_commit_index(uint32_t comparator);

// Enumerated types for setting a Komodo mask (SM). These should match
// KomodoParameters in Komodo.scala
typedef enum {
  e_SM_PC_SRC_CARE,
  e_SM_PC_DST_CARE,
  e_SM_INST_CARE,
  e_SM_RD_CARE,
  e_SM_DATA_CARE,
  e_SM_PC_SRC_DCARE,
  e_SM_PC_DST_DCARE,
  e_SM_INST_DCARE,
  e_SM_RD_DCARE,
  e_SM_DATA_DCARE
} komodo_change_mask_e;

// Enumerated types for Komodo control requests (C). These should
// match KomodoParameters in Komodo.scala
typedef enum {
  e_C_VALID,
  e_C_INVALID,
  e_C_RESET,
  e_C_M_COUNT,
  e_C_LOCAL,
  e_C_COMMIT_IDX,
  e_C_INFO_SP_OFFSET,
  e_C_WRITE_COUNT,
  e_C_MEM_TYPE,
  e_C_DONE
} komodo_ctrl_enum;

typedef enum {
  e_commit_PC_SRC,
  e_commit_PC_DST,
  e_commit_PC_INST,
  e_commit_PC_DATA,
  e_commit_PC_ADDR
} komodo_commit_enum;

typedef enum {
  e_SC_TYPE,
  e_SC_IN1,
  e_SC_IN2,
  e_SC_FN,
  e_SC_OUT,
  e_SC_DATA,
  e_SC_DONE,
  e_SC_COUNT
} komodo_change_conf_e;

typedef enum {
  e_OP_INTR,
  e_OP_MEM_WR,
  e_OP_MEM_RD,
  e_OP_ALU,
  e_OP_MEM_XA_ADD
} komodo_conf_op_e;

typedef enum {
  e_IN_DATA_MU,
  e_IN_ADDR_MU,
  e_IN_CONST,
  e_IN_LOC1,
  e_IN_LOC2,
  e_IN_QUEUE,
  e_IN_DATA_RESP,
  e_IN_LOC3,
  e_IN_LOC4,
  e_IN_LOC5,
  e_IN_LOC6,
  e_IN_COMPRESSED
} komodo_conf_in_e;

typedef enum {
  e_OUT_LOC1,
  e_OUT_LOC2,
  e_OUT_QUEUE,
  e_OUT_DATA,
  e_OUT_ADDR,
  e_OUT_INTR,
  e_OUT_LOC3,
  e_OUT_LOC4,
  e_OUT_LOC5,
  e_OUT_LOC6,
  e_DONE
} komodo_conf_out_e;

typedef enum {
  e_ALU_ADD,
  e_ALU_SUB,
  e_ALU_SL,
  e_ALU_SR,
  e_ALU_SLT,
  e_ALU_SEQ,
  e_ALU_AND,
  e_ALU_OR,
  e_ALU_XOR,
  e_ALU_NOP
} komodo_conf_alu_e;

#endif
