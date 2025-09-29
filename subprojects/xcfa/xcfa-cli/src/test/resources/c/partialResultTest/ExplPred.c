// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2016 Gilles Audemard
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2020 The SV-Benchmarks Community
//
// SPDX-License-Identifier: MIT

extern void abort(void) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));
extern void __assert_fail(const char *, const char *, unsigned int,
                          const char *) __attribute__((__nothrow__, __leaf__))
__attribute__((__noreturn__));
int __VERIFIER_nondet_int();
void reach_error() {
  __assert_fail("0", "AllInterval-015.c", 5, "reach_error");
}
void assume(int cond) {
  if (!cond)
    abort();
}
void __VERIFIER_assert(int cond) {
    if (!(cond)) {
    ERROR:
	{reach_error();}
    }
    return;
}
void main() {
 if (__VERIFIER_nondet_int()) {
   int i;
   for (i = __VERIFIER_nondet_int(); i < 1000000; i++);
   __VERIFIER_assert(i >= 1000000);
 } else {
   int x = 5;
   int y = 6;
   int r = x * y;
   __VERIFIER_assert(r >= x);
  }
}

