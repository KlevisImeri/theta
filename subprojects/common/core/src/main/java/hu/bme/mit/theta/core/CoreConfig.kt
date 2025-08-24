package hu.bme.mit.theta.core


object CoreConfig {
  @JvmField
  var printPrefixNotation: Boolean = true  // for ANTLR tor parse the xcfa you need PrefixNotation == true
  @JvmField
  var printBeautifulSymbols: Boolean = false // if true, you cannot parse xcfa with current ANTLR grammar
}
