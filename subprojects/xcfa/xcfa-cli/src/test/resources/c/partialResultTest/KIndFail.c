void reach_error(){}
extern unsigned char __VERIFIER_nondet_int(void);
extern void __VERIFIER_assert(int cond);
extern void abort(void);
void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}
int main() {
  int x = 0;
  int y = 0;
  int n = 10; // An arbitrary number

  while (x < n) {
    x = x + 1;
    y = y + x;
  }

  __VERIFIER_assert(2 * y == n * (n + 1));
}

// int main() {
//   unsigned char i = 1;
//   while (i < 5) {
//     i++; 
//   }
//   __VERIFIER_assert(i < 10);
// }
