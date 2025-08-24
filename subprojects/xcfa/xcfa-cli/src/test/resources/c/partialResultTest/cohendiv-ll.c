/*
  Cohen's integer division
  returns x % y
  http://www.cs.upc.edu/~erodri/webpage/polynomial_invariants/cohendiv.htm
*/
extern void abort(void);
extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "cohendiv-ll.c", 8, "reach_error"); }
extern int __VERIFIER_nondet_int(void);

int counter = 0;
int main() {
    int x, y;
    long long q, r, a, b;

    x = __VERIFIER_nondet_int();
    y = __VERIFIER_nondet_int();

    if(!(y >= 1)) {abort();}

    q = 0;
    r = x;
    a = 0;
    b = 0;

    while (counter++<1) {
        if (!(b == y*a)) {
            reach_error();
        }
        if (!(x == q*y + r)) {
            reach_error();
        }

        if (!(r >= y))
            break;
        a = 1;
        b = y;

        while (counter++<1) {
            if (!(b == y*a)) {
                reach_error();
            }
            if (!(x == q*y + r)) {
                reach_error();
            }
            if (!(r >= 0)) {
                reach_error();
            }

            if (!(r >= 2 * b))
                break;

            if (!(r >= 2 * y * a)) {
                reach_error();
            }

            a = 2 * a;
            b = 2 * b;
        }
        r = r - b;
        q = q + a;
    }

    if (!(x == q*y + r)) {
        reach_error();
    }

    return 0;
}
