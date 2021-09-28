import java.awt.Desktop
import java.net.URL

fun openBrowser(url: String) {
    val uri = URL(url).toURI()
    val osName by lazy(LazyThreadSafetyMode.NONE) { System.getProperty("os.name").lowercase() }
    val desktop = Desktop.getDesktop()

    when {
        Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE) ->
                desktop.browse(uri)
        "mac" in osName -> Runtime.getRuntime().exec("open $uri")
        "nix" in osName || "nux" in osName -> Runtime.getRuntime().exec("xdg-open $uri")
    }
}
