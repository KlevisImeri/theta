package hu.bme.mit.theta.analysis.algorithm

class AlgorithmTimeoutException(message: String) : RuntimeException(message)

fun abortIfTimedOut(message: String = "Delulu Message") { 
  // println("trying to abort!!! at $message")
  if (Thread.currentThread().isInterrupted()) {
    throw AlgorithmTimeoutException("The algorithm ran out of time!")
  }
}
