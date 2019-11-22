#define _GNU_SOURCE

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/shm.h>
#include <string.h>
#include <signal.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include "varanus.h"

static unsigned char *afl_area_ptr;

int __libc_start_main(int (*main) (int,char **,char **),
                      int argc,char **ubp_av,
                      void (*init) (void),
                      void (*fini)(void),
                      void (*rtld_fini)(void),
                      void (*stack_end)) {
  int (*original__libc_start_main)(int (*main) (int,char **,char **),
                                   int argc,char **ubp_av,
                                   void (*init) (void),
                                   void (*fini)(void),
                                   void (*rtld_fini)(void),
                                   void (*stack_end));

  //printf("Leila is preloading\n");
  
  int shm_id = -1;
  char* id_str = getenv("__AFL_SHM_ID");
  if (id_str) {
    shm_id = atoi(id_str);
    //printf("shm_id: 0x%x \n", shm_id);
  }
  else {
    printf("Killed the process\n");
    kill(getpid(), SIGTERM);
    exit(1);
  }
  
  afl_area_ptr = shmat(shm_id, NULL, 0);
  //printf("afl_area_ptr: 0x%lx \n", (long unsigned int*) afl_area_ptr);
  if (afl_area_ptr == (void*)-1) {
    kill(getpid(), SIGTERM);
    exit(1);
  }

  memset(afl_area_ptr, 0, 0x10000);
  
  komodo_reset_all();
  komodo_set_local_reg(0, (long unsigned int*)(afl_area_ptr));
  komodo_set_local_reg(1, shm_id);
  komodo_set_local_reg(2, (long unsigned int*)(0x0));
  
  komodo_enable_all();
  
  //printf("local1: 0x%lx, local2: 0x%lx, local3: 0x%lx\n", komodo_info_sp_offset(0), komodo_info_sp_offset(1), komodo_info_sp_offset(2));
  
  original__libc_start_main = dlsym(RTLD_NEXT,"__libc_start_main");
  return original__libc_start_main(main,argc,ubp_av,
                                   init,fini,rtld_fini,stack_end);
}
