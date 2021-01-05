package com.keevol.keenotes.desk

import com.keevol.keenotes.desk.KeeNotesFXApplication.makeClickable
import com.keevol.keenotes.desk.utils.SimpleProcessLoggerFactory
import fr.brouillard.oss.cssfx.CSSFX
import javafx.application.{Application, Platform}
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, TextArea}
import javafx.scene.layout._
import javafx.scene.paint.{Color, Paint}
import javafx.scene.text.Font
import javafx.scene.{Cursor, Node, Scene}
import javafx.stage.{Stage, StageStyle}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.controlsfx.control.Notifications
import org.kordamp.ikonli.javafx.FontIcon
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.sys.process.Process


class KeeNotesFXApplication extends Application {

  val logger: Logger = LoggerFactory.getLogger(classOf[KeeNotesFXApplication])

  val settings = new Settings()

  var primaryStage: Stage = _

  override def start(stage: Stage): Unit = {
    primaryStage = stage

    val layout = new BorderPane()
    layout.setTop(header())
    layout.setCenter(notePane())
    layout.setBottom(footer())


    val scene = new Scene(layout)
    scene.getStylesheets.add("/css/style.css")
    stage.setScene(scene)
    stage.setWidth(400)
    stage.setMaxWidth(400)
    stage.setHeight(600)
    stage.setMaxHeight(600)
    stage.setResizable(false)
    stage.initStyle(StageStyle.UTILITY)
    stage.setOnCloseRequest(e => {
      Platform.exit()
      System.exit(0)
    })
    stage.show()

    CSSFX.start()
  }

  def notePane() = {
    val vbox = new VBox()

    val textArea = new TextArea()
    textArea.setWrapText(true)
    textArea.setCursor(Cursor.TEXT);
    VBox.setVgrow(textArea, Priority.ALWAYS)
    VBox.setMargin(textArea, new Insets(0, 10, 10, 10))
    vbox.getChildren.add(textArea)

    val submit = new Button("Submit")
    makeClickable(submit)
    submit.setFont(Font.font("Arial Black", 17))
    submit.setOnAction(e => {
      val content = StringUtils.trimToEmpty(textArea.getText)
      if (StringUtils.isNotEmpty(content)) {
        textArea.setEditable(false)
        submit.setDisable(true)
        submit.getScene.setCursor(Cursor.WAIT)
        try {
          val r = requests.post(settings.noteRelayServerProperty.getValue,
            headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
            params = Map("token" -> settings.tokenProperty.getValue, "channel" -> "keenotes-desktop", "text" -> content),
            connectTimeout = settings.connectTimeoutProperty.getValue,
            readTimeout = settings.readTimeoutProperty.getValue)
          if (r.statusCode == 200) {
            textArea.clear()
            info("Note Relayed!")
          } else {
            val err = s"error: ${r.statusCode} - ${r.statusMessage}"
            logger.error(err)
            error(err)
          }
        } catch {
          case t: Throwable => {
            logger.error(ExceptionUtils.getStackTrace(t))
            error(ExceptionUtils.getStackTrace(t))
          }
        } finally {
          textArea.setEditable(true)
          submit.setDisable(false)
          submit.getScene.setCursor(Cursor.DEFAULT)
        }
      }
    })

    vbox.getChildren.add(new AnchorPane(submit))
    AnchorPane.setLeftAnchor(submit, 10)
    AnchorPane.setRightAnchor(submit, 10)

    vbox
  }

  def placeholder() = {
    val r = new Region
    HBox.setHgrow(r, Priority.ALWAYS)
    r
  }

  def header() = {
    val hbox = new HBox(10)
    hbox.setAlignment(Pos.CENTER)

    val logoText = new Label("KeeNotes Desk")
    logoText.setFont(Font.font("Arial Black", 32))
    HBox.setMargin(logoText, new Insets(10))

    val settingIcon = new FontIcon()
    settingIcon.setIconLiteral("fa-gear:32:aqua")
    makeClickable(settingIcon)
    settingIcon.setOnMouseClicked(e => {
      settings.preferencesFX.show(true)
    })

    HBox.setMargin(settingIcon, new Insets(10))

    val sync = new FontIcon()
    sync.setIconLiteral("fa-refresh:32:aqua")
    makeClickable(sync)
    sync.setOnMouseClicked(e => {
      val processLogger = new SimpleProcessLoggerFactory

      sync.setIconColor(Color.BLUE)
      sync.getScene.setCursor(Cursor.WAIT)
      try {
        val exitCode = Process(Seq("bash", "-c", "***REMOVED***"), None, System.getenv().asScala.toSeq: _*) ! processLogger.logger
        exitCode match {
          case 0 => info("Note Synced Successfully.")
          case _ => {
            error(s"something goes wrong with exit code=$exitCode, check log for more information.")
            logger.error(processLogger.getConsoleOutput()._1 + "\n" + processLogger.getConsoleOutput()._2)
          }
        }
      } catch {
        case t: Throwable => {
          error(s"something goes wrong with exception thrown, check log for more information.")
          logger.error(processLogger.getConsoleOutput()._1 + "\n" + processLogger.getConsoleOutput()._2)
        }
      } finally {
        sync.getScene.setCursor(Cursor.DEFAULT)
        sync.setIconColor(Color.AQUA)
      }
    })

    hbox.getChildren.addAll(logoText, placeholder(), sync, settingIcon)

    hbox
  }

  def footer() = {
    val hbox = new HBox(10)

    val placeholder1 = new Region
    HBox.setHgrow(placeholder1, Priority.ALWAYS)

    val placeholder2 = new Region
    HBox.setHgrow(placeholder2, Priority.ALWAYS)

    val copyright = new Label("© KEEVOL Consulting @keevol.com")
    copyright.setFont(Font.font("Arial Black", 9))
    hbox.getChildren.addAll(placeholder1, copyright, placeholder2)
    HBox.setMargin(copyright, new Insets(10, 10, 10, 10))
    hbox
  }

  def info(message: String) = {
    Notifications.create().darkStyle().title("Success").text(message).owner(primaryStage).showInformation()
  }

  def error(message: String) = {
    Notifications.create().darkStyle().title("Error").text(message).owner(primaryStage).showError()
  }

}


object KeeNotesFXApplication {

  def makeClickable(node: Node) = {
    node.setOnMouseEntered(e => {
      node.getScene.setCursor(Cursor.HAND)
    })
    node.setOnMouseExited(e => {
      node.getScene.setCursor(Cursor.DEFAULT)
    })
  }

  def success() = {
    System.getProperty("os.name").toLowerCase match {
      case os if StringUtils.startsWith(os, "mac") => Runtime.getRuntime.exec(Array("osascript", "-e", """display notification "note sent successfully." with title "Success" subtitle "Success" sound name "Glass""""));
      case os if StringUtils.startsWith(os, "win") =>
      case _ =>
    }


  }

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[KeeNotesFXApplication], args: _*)
  }
}

object KeeNotesFXApplicationLauncher {
  def main(args: Array[String]): Unit = {
    KeeNotesFXApplication.main(args)
  }
}

