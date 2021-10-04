package org.hhu.examine

import javafx.application.Application
import org.hhu.examine.main.MainView
import tornadofx.App

/** Launch entry point. */
fun main(args: Array<String>) {
    // Help argument.
    if (args.contains("--help") or args.contains("-h")) {
        println("eXamine is an interactive visualization application that requires a JavaFX GUI to run.")
        println("Place your data sets in a folder 'data-sets' of your current working directory and run eXamine without further arguments.")
        println("Data set file formats are described at https://github.com/AlBi-HHU/eXamine-stand-alone")
    } else {
        Application.launch(ExamineApp::class.java, *args)
    }
}

/** eXamine application. */
class ExamineApp : App(MainView::class) {

    init {
        setUserAgentStylesheet(javaClass.getResource("UserAgentStylesheet.css").toExternalForm())
    }

}