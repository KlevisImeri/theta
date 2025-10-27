void reach_error(){}
extern unsigned char __VERIFIER_nondet_uchar(void);
extern int __VERIFIER_nondet_int(void);

extern void abort(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}


int main() { 
  int n = __VERIFIER_nondet_uchar(), sn=0, i=1;
  while (i<=n) {
    sn = sn + 2;
    i++;
  }
  __VERIFIER_assert(sn==n*2 || sn == 0);
}
