#include "varanus.h"

#define komodo_set_config(comparator, action, data) \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, ((uint64_t) action << 32 | comparator), data, 5, 11, 12);

#define komodo_set_mask(comparator, action, data) \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, ((uint64_t) action << 32 | comparator), data, 0, 11, 12);


#define komodo_ctrl(comparator, action) \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, ((uint64_t) action << 32 | comparator), 0, 1, 11, 12);

#define komodo_ctrl_wr(comparator, action, data) \
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, ((uint64_t) action << 32 | comparator), data, 1, 11, 12);

#define komodo_ctrl_rd(comparator, action, out) \
  ROCC_INSTRUCTION(XCUSTOM_KOMODO, out, ((uint64_t) action << 32 | comparator), 0, 1);

void komodo_action_config(uint32_t comparator, act_conf_table_t * action) {
  komodo_set_config(comparator, e_SC_TYPE, action->op_type);
  komodo_set_config(comparator, e_SC_IN1, action->in1);
  komodo_set_config(comparator, e_SC_IN2, action->in2);
  komodo_set_config(comparator, e_SC_FN, action->fn);
  komodo_set_config(comparator, e_SC_OUT, action->out);
  komodo_set_config(comparator, e_SC_DATA, action->data);
  komodo_set_config(comparator, e_SC_DONE, 0);
}

void komodo_pattern(uint32_t comparator, mask_t * mask) {
  komodo_set_mask(comparator, e_SM_PC_SRC_CARE, mask->care.pc_src);
  komodo_set_mask(comparator, e_SM_PC_DST_CARE, mask->care.pc_dst);
  komodo_set_mask(comparator, e_SM_INST_CARE, mask->care.inst);
  komodo_set_mask(comparator, e_SM_RD_CARE, mask->care.rd);
  komodo_set_mask(comparator, e_SM_DATA_CARE, mask->care.data);
  komodo_set_mask(comparator, e_SM_PC_SRC_DCARE, mask->dont_care.pc_src);
  komodo_set_mask(comparator, e_SM_PC_DST_DCARE, mask->dont_care.pc_dst);
  komodo_set_mask(comparator, e_SM_INST_DCARE, mask->dont_care.inst);
  komodo_set_mask(comparator, e_SM_RD_DCARE, mask->dont_care.rd);
  komodo_set_mask(comparator, e_SM_DATA_DCARE, mask->dont_care.data);
}

void komodo_enable(uint32_t comparator) {
  komodo_ctrl(comparator, e_C_VALID);
}

void komodo_disable(uint32_t comparator) {
  komodo_ctrl(comparator, e_C_INVALID);
}

void komodo_reset_val(uint32_t comparator) {
  komodo_ctrl(comparator, e_C_RESET);
}

void komodo_match_count(uint32_t comparator, data_t count, xlen_t * addr) {
  komodo_ctrl_wr(comparator, e_C_M_COUNT, count);
}

void komodo_set_local_reg(uint32_t index, xlen_t *addr)
{
  komodo_ctrl_wr(index, e_C_LOCAL, addr);
}

void komodo_set_mem_typ(xlen_t type) {
  komodo_ctrl_wr(0, e_C_MEM_TYPE, type);
}

void komodo_set_commit_index(uint32_t comparator, data_t index) {
  komodo_ctrl_wr(comparator, e_C_COMMIT_IDX, index);
}

uint64_t komodo_info_sp_offset(uint32_t index) {
  uint64_t out;
  komodo_ctrl_rd(index, e_C_INFO_SP_OFFSET, out);
  return out;
}

uint64_t komodo_done_info() {
  uint64_t out;
  komodo_ctrl_rd(0, e_C_DONE, out);
  return out;
}

void komodo_mem_cmd(uint64_t  addr, uint64_t data) {
  ROCC_INSTRUCTION_0_R_R(XCUSTOM_KOMODO, addr, data, 11, 11, 12);
}

uint64_t komodo_read_mask(uint32_t comparator, uint64_t action) {
  uint64_t out;
  ROCC_INSTRUCTION(XCUSTOM_KOMODO, out, ((uint64_t) action << 32 | comparator), 0, 12);
  return out;
}

uint64_t komodo_read_conf(uint32_t comparator, uint64_t action) {
  uint64_t out;
  ROCC_INSTRUCTION(XCUSTOM_KOMODO, out, ((uint64_t) action << 32 | comparator), 0, 13);
  return out;
}

uint64_t komodo_read_commit_index(uint32_t comparator) {
  uint64_t out;
  ROCC_INSTRUCTION(XCUSTOM_KOMODO, out, ((uint64_t) comparator), 0, 14);
  return out;
}
