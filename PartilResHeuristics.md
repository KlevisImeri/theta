### Ides how to stop

Good thinking — detecting **same CEX loops** and **no precision improvement** are both solid heuristics. Here are a few more heuristics commonly used in CEGAR-like frameworks:
1. **Precision saturation**
   * If refinement keeps making only *tiny* or *repeated* changes that don’t actually progress, you can bail out.
   * E.g., measure “distance” between precisions (size of predicates, automaton states, etc.), and if growth stalls, stop.
2. **Proof/ARG stabilization**
   * If the abstract reachability graph stops changing structurally between iterations, you’re essentially cycling. That’s a good point to produce a partial result.
3. **Iteration/time budget**
   * Put a hard cap on iterations or elapsed time per config. This avoids pathological cases where a single config eats all resources.
4. **Refinement oscillation**
   * If precision keeps *changing back and forth* (like toggling predicates or domains), that’s a refinement oscillation → partial result.
5. **Solver instability detection**
   * If refinement calls repeatedly fail or trigger exceptions (solver returns unknown, timeouts, etc.), downgrade to partial.
6. **CEX complexity growth**
   * If the counterexample gets longer or structurally more complex across iterations without resolution, you may be chasing an unrefinable path. Stop early.
7. **Precision explosion guard**
   * If the precision becomes too large (e.g., too many predicates, too big automaton), you risk performance collapse. Emit partial and move on.
8. **User-configurable thresholds**
   * Let users tune heuristics like *max iterations per config*, *max precision size*, *max refinement attempts*, etc. This gives flexibility depending on the domain.
So in short, beyond “same CEX” and “no precision change,” I’d suggest:
* **stagnation (ARG/precision not evolving)**,
* **oscillation (back and forth refinements)**,
* **bounded resources (time, iterations, precision size)**.

---

You've identified two excellent heuristics for stopping CEGAR and returning partial results. Here are several more effective heuristics you could implement:

## 1. **Time-based Stopping**
```java
private final long timeLimitMs; // Configurable time limit

// In check() method:
if (stopwatch.elapsed(TimeUnit.MILLISECONDS) > timeLimitMs) {
    logger.write(Level.MAINSTEP, "Time limit exceeded, returning partial result%n");
    abstractor.unroll(proof, prec);
    return SafetyResult.partial(proof, getStats.get());
}
```

## 2. **Iteration Limit**
```java
private final int maxIterations; // Configurable iteration limit

// In the loop:
if (statsHolder.iteration >= maxIterateons) {
    logger.write(Level.MAINSTEP, "Iteration limit reached, returning partial result%n");
    abstractor.unroll(proof, prec);
    return SafetyResult.partial(proof, getStats.get());
}
```

## 3. **Precision Size Growth**
```java
private final double maxPrecisionGrowthFactor; // e.g., 2.0 for 2x growth

// Track precision size:
int currentPrecisionSize = calculatePrecisionSize(prec);
if (currentPrecisionSize > initialPrecisionSize * maxPrecisionGrowthFactor) {
    logger.write(Level.MAINSTEP, "Precision grew too large, returning partial result%n");
    abstractor.unroll(proof, prec);
    return SafetyResult.partial(proof, getStats.get());
}
```

## 4. **Memory Usage Monitoring**
```java
private final long memoryLimitMB; // Configurable memory limit

private boolean isMemoryLimitExceeded() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    return usedMemoryMB > memoryLimitMB;
}

// In the loop:
if (isMemoryLimitExceeded()) {
    logger.write(Level.MAINSTEP, "Memory limit exceeded, returning partial result%n");
    abstractor.unroll(proof, prec);
    return SafetyResult.partial(proof, getStats.get());
}
```

## 5. **Progress Stagnation Detection**
```java
private final int stagnationThreshold; // e.g., 3 iterations without progress

private int stagnationCounter = 0;
private int lastRefinedStates = 0;

// After refinement:
int currentRefinedStates = countRefinedStates(refinerResult);
if (currentRefinedStates == lastRefinedStates) {
    stagnationCounter++;
    if (stagnationCounter >= stagnationThreshold) {
        logger.write(Level.MAINSTEP, "Progress stagnated, returning partial result%n");
        abstractor.unroll(proof, prec);
        return SafetyResult.partial(proof, getStats.get());
    }
} else {
    stagnationCounter = 0;
    lastRefinedStates = currentRefinedStates;
}
```

## 6. **Solver Timeout Frequency**
```java
private final int maxSolverTimeouts; // e.g., 3 timeouts allowed

private int solverTimeoutCount = 0;

// Catch solver timeouts specifically:
} catch (SolverTimeoutException e) {
    solverTimeoutCount++;
    if (solverTimeoutCount >= maxSolverTimeouts) {
        logger.write(Level.MAINSTEP, "Too many solver timeouts, returning partial result%n");
        abstractor.unroll(proof, prec);
        return SafetyResult.partial(proof, getStats.get());
    }
    // Optionally continue or return partial
}
```

## 7. **Combined Heuristic Configuration**
You could create a configurable strategy:

```java
public class CegarStoppingCriteria {
    private final Boolean enabled;
    private final Long timeLimitMs;
    private final Integer maxIterations;
    private final Double maxPrecisionGrowth;
    private final Long memoryLimitMB;
    private final Integer stagnationThreshold;
    private final Integer maxSolverTimeouts;
    
    // Getters and builder pattern
}
```

## Implementation Approach
1. **Make heuristics configurable** through constructor parameters
2. **Use a combination** of multiple heuristics for robustness
3. **Log which heuristic triggered** the partial result for debugging
4. **Provide sensible defaults** but allow customization

The best approach depends on your specific use case, but a combination of time limit, iteration limit, and precision growth detection usually works well in practice.

---

That's an excellent question! Deciding when to gracefully exit with a partial result in CEGAR can significantly improve the overall efficiency of a multi-configuration solver. You've identified two very good heuristics:

1.  **Duplicate CEX Detection:** If the refiner consistently produces the same counterexample (or a very similar one that leads to the same refinement strategy), it suggests that the current abstraction/refinement strategy might be stuck. This is a strong indicator that progress isn't being made, and it's a good time to get a partial result and move on.

2.  **Precision Stagnation:** If the precision (`prec`) does not change after a refinement step, it means the abstraction is not getting more precise, and thus, the analysis is likely stuck. This is a critical sign of non-progress.

Here are some other good heuristics you could consider for deciding when to stop and produce a partial result:

### Heuristics Based on Resource Limits:

*   **Time Limit:** This is a classic and very practical heuristic. If the CEGAR loop exceeds a predefined time limit for a single configuration, it should stop and return a partial result. This prevents a single difficult configuration from hogging all the resources.
*   **Iteration Limit:** Similar to a time limit, if the number of CEGAR iterations exceeds a certain threshold, it suggests that the problem is either very hard for the current configuration or the configuration is struggling to make progress.
*   **Memory Limit:** If the analysis starts consuming too much memory (e.g., the ARG grows excessively large, or the SMT solver's memory usage spikes), it's a good time to stop and yield a partial result. This prevents out-of-memory errors and allows other configurations to run.

### Heuristics Based on Refinement Characteristics:

*   **Refinement Size/Complexity:** If the size or complexity of the refinement (e.g., the number of new predicates added, or the depth of the new states explored) consistently becomes very small, it might indicate that the refiner is making very fine-grained adjustments that are not leading to significant progress. Conversely, if the refinement steps are *too* large and lead to very unstable behavior, that might also be a sign of trouble.
*   **Solver Timeout/Unsat Core Instability:** If the SMT solver used in the refiner frequently times out or struggles to find a minimal unsat core, it could indicate that the current path is computationally too expensive or ill-formed for the solver. This might be harder to monitor directly but could be inferred from frequent solver timeouts or exceptionally long solver calls.
*   **Increasing SMT Solver Time:** If the time spent in the SMT solver within the refiner continuously increases without a corresponding reduction in spurious counterexamples, it suggests diminishing returns.

### Heuristics Based on ARG Growth:

*   **ARG Size Stagnation:** If the Abstract Reachability Graph (ARG) size (number of states or transitions) stops growing significantly over multiple iterations, it could mean the abstractor has explored most of what it can with the current precision and isn't finding new paths.
*   **ARG Size Explosion:** Conversely, if the ARG size is growing exponentially, but without getting closer to a safe state, it might mean the abstraction is too coarse, and the refiner isn't adding strong enough predicates to prune the state space effectively.

### Combining Heuristics:

The most robust solution will likely involve a combination of these heuristics. For example:

*   "Stop if `(iteration_limit_reached OR time_limit_reached OR (precision_stagnated_for_X_iterations AND duplicate_cex_detected))`."

### Implementation Considerations:

*   **Configuration Parameters:** Make these thresholds (time limit, iteration limit, stagnation count) configurable so you can tune them for different types of problems or benchmarks.
*   **Monitoring:** You'll need to instrument your `Abstractor` and `Refiner` more deeply to collect metrics like ARG size, SMT solver times, and how many times the precision actually changed.

Let's illustrate some of these concepts.

Here's a visual representation of what precision stagnation might look like in a hypothetical CEGAR run: 

---

Looking at your CEGAR implementation, I can see you're already handling some good heuristics for when to stop and produce partial results. Let me suggest several additional heuristics that could help you decide when to abandon the current configuration and move to the next one with partial results:
Enhanced CEGAR Heuristics for Partial Results
Code 

Here are the key heuristics I recommend for deciding when to produce partial results:
Already Implemented (Good!)

    Infinite loop detection - Your monitor checkpoint approach
    Precision refinement failure - When precision can't be refined

Additional Heuristics to Consider:
1. Time-based Limits

    Set maximum time per configuration
    Adaptive timeouts based on problem complexity

2. Iteration Limits

    Maximum number of CEGAR iterations
    Prevents runaway refinement loops

3. Precision Stagnation

    Count consecutive iterations without precision changes
    More sophisticated than just checking if current == previous

4. Proof/ARG Size Explosion

    Monitor rapid growth in proof size
    Stop if proof grows beyond reasonable bounds without convergence

5. Refinement Progress Rate

    Track average iteration time
    Stop if iterations become significantly slower (diminishing returns)

6. Memory Pressure

    Monitor memory usage
    Stop before running out of memory

7. Consecutive Solver Failures

    Count timeouts or solver errors
    Stop if solvers consistently fail

8. Oscillation Detection

    Detect repeating patterns in precision refinement
    Stop if cycling between same precisions

9. Abstraction Quality Metrics

    Track spurious counterexample rates
    Stop if abstraction quality isn't improving

Implementation Strategy:

    Configure thresholds based on your problem domain
    Combine multiple heuristics - don't rely on just one
    Log which heuristic triggered for debugging
    Make thresholds adaptive based on problem characteristics
    Consider problem-specific metrics (e.g., program size, loop complexity)

The key insight is to fail fast when a configuration isn't making progress, while ensuring you still get useful partial results that can help the next configuration. Your monitor-based infinite loop detection is excellent - combining it with these additional heuristics should give you a robust system for deciding when to move on.

Would you like me to help you integrate any of these specific heuristics into your existing code?

--- 

Heuristics to trigger partial-result & bail-out
Repeated CEX — same or structurally similar counterexample seen N times (hash/path fingerprint).
No precision change — refinement returned a precision equal to previous for M consecutive iterations.
Refinements-without-progress — M refinements that do not eliminate any spurious CEX (or do not reduce number of spurious paths).
Time budget — wall-clock per config exceeded (e.g., 30s / 5min).
Iteration cap — iterations exceed K (simple safety net).
Solver failures / timeouts — repeated NotSolvable/solver exceptions or frequent timeouts.
**Diminishing returns** — improvements in measurable quality (ARG size, #abstract states, precision complexity) fall below ε over last T iterations.
Resource pressure — memory/GC spikes or abstract state explosion beyond threshold.
**CEX depth growth** — CEX length increases steadily without getting proven/ruled out (suggests divergence).
Refinement cost too high — average cost of a refinement step (time / SAT calls) exceeds threshold and progress is low.


Refiner Infeasibility:
If the refiner fails to process a Cex (e.g., due to complexity), return a partial result.

Unrefinable Counterexample:
When a Cex cannot be refined further (e.g., reaching a minimal precision), terminate.

Solver Failures:
Catch solver errors (e.g., timeouts, exceptions) and treat them as stopping conditions.

Coverage Threshold:
Stop when a significant portion of the state space is covered, even if not proven safe/unsafe.

```java
if (refinerResult.isSpurious() && prec.equals(refinerResult.asSpurious().getRefinedPrec())) {
    if (computePartialResult) {
        logger.write(Level.MAINSTEP, "Precision stagnation detected%n");
        abstractor.unroll(proof, prec);
        return SafetyResult.partial(proof, getStats.get());
    }
}
```
