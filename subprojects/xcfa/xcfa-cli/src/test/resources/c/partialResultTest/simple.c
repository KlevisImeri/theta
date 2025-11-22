extern void __assert_fail(const char *, const char *, unsigned int, const char *) __attribute__ ((__nothrow__ , __leaf__)) __attribute__ ((__noreturn__));
void reach_error() { __assert_fail("0", "nested_2.c", 13, "reach_error"); }

int main() {
	int a = 0;
	while(a < 6) {
    a++;
  }
	if(!(a == 6)) {
		reach_error();
	}
}
