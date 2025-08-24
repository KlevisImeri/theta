extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "multivar_1-1.c", 3, "reach_error"); }
extern unsigned int __VERIFIER_nondet_uint(void);
extern _Bool __VERIFIER_nondet_bool(void);

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

int main(void) {
  unsigned int x = __VERIFIER_nondet_uint();
  unsigned int y = x;
  unsigned int z = 0;

  // Constrain the initial value of x to prevent trivial solutions
  // and ensure the loop is entered.
  if (x >= 1024) {
    x = 0;
    y = 0;
  }

  while (x < 1024) {
    unsigned int i = __VERIFIER_nondet_uint();

    // Introduce a more complex, conditional update to y
    if (__VERIFIER_nondet_bool()) {
      y += i + 1;
    } else {
      y += i - 1;
    }

    // Introduce a non-linear relationship involving a third variable
    x += z;
    z++;
  }

  // The assertion will now fail due to the more complex and divergent updates to x and y.
  __VERIFIER_assert(x == y);
}
