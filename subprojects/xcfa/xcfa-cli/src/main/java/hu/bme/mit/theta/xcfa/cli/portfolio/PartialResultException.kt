package hu.bme.mit.theta.xcfa.cli.portfolio

/**
 * A dedicated exception to signal that a partial result was found and the portfolio
 * should transition to the next analysis node.
 */
class PartialResultException : Exception("A usable partial result was found, transitioning to the next analysis.")
