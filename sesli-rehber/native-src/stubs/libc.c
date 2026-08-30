#define F(x) __attribute__((visibility("default"))) void x(void){}
F(calloc) F(free) F(memset) F(memcpy) F(pthread_create) F(pthread_join) F(snprintf) F(fopen) F(fwrite) F(fclose)
