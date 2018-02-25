package org.cwi.examine

import javafx.application.Application
import org.cwi.examine.main.MainView
import tornadofx.App

/** Launch entry point. */
fun main(args: Array<String>) {
    Application.launch(ExamineApp::class.java, *args)
}

/** eXamine application. */
class ExamineApp : App(MainView::class) {

    init {
        setUserAgentStylesheet(javaClass.getResource("UserAgentStylesheet.css").toExternalForm())
    }

}