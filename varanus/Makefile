DIR_BUILD  = build
DIR_MAIN_C = src/main/c
DIR_TEST_C = src/test/c
DIR_COSTOM_C = rocket-rocc-examples/rocc-software/src

DIR_LIB    = usr/lib
DIR_LIB_RV = usr/lib-rv

DIR_INCLUDE_C = $(DIR_COSTOM_C) $(DIR_MAIN_C) $(DIR_TEST_C) 

RV_TESTS = shadow_stack.c afl_ndisasm.c debug.c debug_break.c komodo_test.c
RV_TESTS_EXECUTABLES = $(RV_TESTS:%.c=$(DIR_BUILD)/%.rv)
RV_TESTS_DISASM      = $(RV_TESTS:%.c=$(DIR_BUILD)/%.rvS)

RV_TARGET     = /newSSD/leila/riscv_wo_c/bin/riscv64-unknown-linux-gnu
RV_GCC        = $(RV_TARGET)-gcc
RV_GPP        = $(RV_TARGET)-g++
RV_AR         = $(RV_TARGET)-ar
RV_OBJDUMP    = $(RV_TARGET)-objdump

RV_CFLAGS     = -Wall -march=rv64imafd -mabi=lp64d\
	$(addprefix -I,$(DIR_INCLUDE_C))
RV_LFLAGS     = -L$(DIR_LIB_RV) -lvaranus

# Varanus library
VARANUS_LIBS = $(DIR_BUILD)/libvaranus.a
VARANUS_LIBS_OBJECTS = $(DIR_BUILD)/komodo.rvo

vpath %.c $(DIR_MAIN_C) $(DIR_TEST_C)

.PHONY: default all clean

.PRECIOUS: $(DIR_BUILD)/%.rv

default: all

all: $(RV_TESTS_DISASM)

$(DIR_BUILD)/%.rv: %.c $(VARANUS_LIBS)
	$(RV_GCC) $(RV_CFLAGS) -static  $< $(RV_LFLAGS) -o $@

#	$(RV_GCC) $(RV_CFLAGS) $< $(RV_LFLAGS) -o $@

$(DIR_BUILD)/%.rvS: $(DIR_BUILD)/%.rv
	$(RV_OBJDUMP) -d $< > $@

$(DIR_BUILD)/%.rvo: %.c
	$(RV_GCC) $(RV_CFLAGS) -c $< -o $@

$(VARANUS_LIBS): $(VARANUS_LIBS_OBJECTS) $(DIR_MAIN_C)/varanus.h
	$(RV_AR) rcs $@ $(VARANUS_LIBS_OBJECTS)

clean:
	rm -rf build/*
