package hu.bme.mit.theta.ui;

object TUI {
    const val RESET = "\u001b[0m"
    
    // Regular colors
    const val BLACK = "\u001b[30m"
    const val RED = "\u001b[31m"
    const val GREEN = "\u001b[32m"
    const val YELLOW = "\u001b[33m"
    const val BLUE = "\u001b[34m"
    const val MAGENTA = "\u001b[35m"
    const val CYAN = "\u001b[36m"
    const val WHITE = "\u001b[37m"
    
    // Light colors
    const val LIGHT_BLACK = "\u001b[90m"
    const val LIGHT_RED = "\u001b[91m"
    const val LIGHT_GREEN = "\u001b[92m"
    const val LIGHT_YELLOW = "\u001b[93m"
    const val LIGHT_BLUE = "\u001b[94m"
    const val LIGHT_MAGENTA = "\u001b[95m"
    const val LIGHT_CYAN = "\u001b[96m"
    const val LIGHT_WHITE = "\u001b[97m"
    
    // Background colors
    const val BG_BLACK = "\u001b[40m"
    const val BG_RED = "\u001b[41m"
    const val BG_GREEN = "\u001b[42m"
    const val BG_YELLOW = "\u001b[43m"
    const val BG_BLUE = "\u001b[44m"
    const val BG_MAGENTA = "\u001b[45m"
    const val BG_CYAN = "\u001b[46m"
    const val BG_WHITE = "\u001b[47m"
    
    // Light background colors
    const val BG_LIGHT_BLACK = "\u001b[100m"
    const val BG_LIGHT_RED = "\u001b[101m"
    const val BG_LIGHT_GREEN = "\u001b[102m"
    const val BG_LIGHT_YELLOW = "\u001b[103m"
    const val BG_LIGHT_BLUE = "\u001b[104m"
    const val BG_LIGHT_MAGENTA = "\u001b[105m"
    const val BG_LIGHT_CYAN = "\u001b[106m"
    const val BG_LIGHT_WHITE = "\u001b[107m"
    
    // Text styles
    const val BOLD = "\u001b[1m"
    const val DIM = "\u001b[2m"
    const val ITALIC = "\u001b[3m"
    const val UNDERLINE = "\u001b[4m"
    const val BLINK = "\u001b[5m"
    const val REVERSE = "\u001b[7m"
    const val HIDDEN = "\u001b[8m"
    const val STRIKETHROUGH = "\u001b[9m"

    // Regular color functions
    fun black(text: String) = "${BLACK}$text${RESET}"
    fun red(text: String) = "${RED}$text${RESET}"
    fun green(text: String) = "${GREEN}$text${RESET}"
    fun yellow(text: String) = "${YELLOW}$text${RESET}"
    fun blue(text: String) = "${BLUE}$text${RESET}"
    fun magenta(text: String) = "${MAGENTA}$text${RESET}"
    fun cyan(text: String) = "${CYAN}$text${RESET}"
    fun white(text: String) = "${WHITE}$text${RESET}"

    // Light color functions
    fun lightBlack(text: String) = "${LIGHT_BLACK}$text${RESET}"
    fun lightRed(text: String) = "${LIGHT_RED}$text${RESET}"
    fun lightGreen(text: String) = "${LIGHT_GREEN}$text${RESET}"
    fun lightYellow(text: String) = "${LIGHT_YELLOW}$text${RESET}"
    fun lightBlue(text: String) = "${LIGHT_BLUE}$text${RESET}"
    fun lightMagenta(text: String) = "${LIGHT_MAGENTA}$text${RESET}"
    fun lightCyan(text: String) = "${LIGHT_CYAN}$text${RESET}"
    fun lightWhite(text: String) = "${LIGHT_WHITE}$text${RESET}"

    // Background color functions
    fun bgBlack(text: String) = "${BG_BLACK}$text${RESET}"
    fun bgRed(text: String) = "${BG_RED}$text${RESET}"
    fun bgGreen(text: String) = "${BG_GREEN}$text${RESET}"
    fun bgYellow(text: String) = "${BG_YELLOW}$text${RESET}"
    fun bgBlue(text: String) = "${BG_BLUE}$text${RESET}"
    fun bgMagenta(text: String) = "${BG_MAGENTA}$text${RESET}"
    fun bgCyan(text: String) = "${BG_CYAN}$text${RESET}"
    fun bgWhite(text: String) = "${BG_WHITE}$text${RESET}"

    // Light background color functions
    fun bgLightBlack(text: String) = "${BG_LIGHT_BLACK}$text${RESET}"
    fun bgLightRed(text: String) = "${BG_LIGHT_RED}$text${RESET}"
    fun bgLightGreen(text: String) = "${BG_LIGHT_GREEN}$text${RESET}"
    fun bgLightYellow(text: String) = "${BG_LIGHT_YELLOW}$text${RESET}"
    fun bgLightBlue(text: String) = "${BG_LIGHT_BLUE}$text${RESET}"
    fun bgLightMagenta(text: String) = "${BG_LIGHT_MAGENTA}$text${RESET}"
    fun bgLightCyan(text: String) = "${BG_LIGHT_CYAN}$text${RESET}"
    fun bgLightWhite(text: String) = "${BG_LIGHT_WHITE}$text${RESET}"

    // Text style functions
    fun bold(text: String) = "${BOLD}$text${RESET}"
    fun underline(text: String) = "${UNDERLINE}$text${RESET}"
    fun italic(text: String) = "${ITALIC}$text${RESET}"
    fun blink(text: String) = "${BLINK}$text${RESET}"
    fun reverse(text: String) = "${REVERSE}$text${RESET}"

    // Combined styles example
    fun error(text: String) = "${BOLD}${RED}[ERROR]: $text${RESET}"
    fun warn(text: String) = "${BOLD}${YELLOW}[WARN]: $text${RESET}"
    fun success(text: String) = "${BOLD}${GREEN}[SUCCESS]: $text${RESET}"
    fun info(text: String) = "${BOLD}${BLUE}[INFO]: $text${RESET}"
}

