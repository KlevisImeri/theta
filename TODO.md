### Witness Validation Format 
file:///home/klevis/Downloads/978-3-031-66149-5_11.pdf

### Correctness Witness
```yaml
1 - entry_type : invariant_set
2   metadata : <...>
3   content :
4 - invariant :
5       type : loop_invariant #location_invariant
6       location :
7           file_name : "inv-a.c"
8           line : 11
9           column : 1 #optional (default first char) 
10          function : main #optional
11      value : "s <= i *255 && 0 <= i && i <= 255 && n <= 255"
12      format : c_expression
```
For `loop_invariant` you have to point to the beginning of the loop.

### Semantics 
The correctness witness is valid if it fulfills the following requirements.
- Each `loop_invariant` must always hold immediately before evaluating the condition of the corresponding loop.
- Each `location_invariant` must always hold immediately before evaluating the corresponding statement or declaration.
- The specification must be satisfied for all program executions.
- No invariant evaluation causes undefined behavior and no undefined behavior occurs during any execution of the program.

### Violation Witness
Violation witness is a sequence of waypoints that have to be passed
by the executions.

