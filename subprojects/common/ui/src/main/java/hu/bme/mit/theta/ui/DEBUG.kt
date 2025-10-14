package hu.bme.mit.theta.ui

object DEBUG {
  const val enabled = true; // INFO: build time constant

  fun type(obj: Any, full: Boolean = false) {
    if(!enabled) return;
    val stackTrace = Thread.currentThread().stackTrace
    val caller = stackTrace[2]
    val fileName = caller.fileName
    val lineNumber = caller.lineNumber
    if(full){
      TUI.cyan("[$fileName:$lineNumber] Object runtime type: ${obj.javaClass.name}");
    } else {
      TUI.cyan("[$fileName:$lineNumber] Object runtime type: ${obj::class.simpleName}")
    }
  }
}
