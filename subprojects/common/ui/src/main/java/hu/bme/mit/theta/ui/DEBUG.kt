package hu.bme.mit.theta.ui

object DEBUG {
  const val enabled = true; // INFO: build time constant
  
  private fun getLoc(): String {
    val stackTrace = Thread.currentThread().stackTrace
    val caller = stackTrace[3]
    val fileName = caller.fileName
    val lineNumber = caller.lineNumber
    return "$fileName:$lineNumber"
  }

  fun debug(str: String) {
    if(!enabled) return;
    println(TUI.debug("${getLoc()} $str"))
  }

  fun type(obj: Any, full: Boolean = false) {
    if(!enabled) return;
    val stackTrace = Thread.currentThread().stackTrace
    val caller = stackTrace[2]
    val fileName = caller.fileName
    val lineNumber = caller.lineNumber
    if(full){
      println(TUI.debug("${getLoc()} Object runtime type: ${obj.javaClass.name}"));
    } else {
      println(TUI.debug("${getLoc()} Object runtime type: ${obj::class.simpleName}"));
    }
  }
}
