package com.keevol.keenotes.desk

import com.keevol.keenotes.desk.KeeNotesFXApplication.{clickable, success}
import javafx.application.{Application, Platform}
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, TextArea}
import javafx.scene.layout._
import javafx.scene.text.Font
import javafx.scene.{Cursor, Node, Scene}
import javafx.stage.{Stage, StageStyle}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.kordamp.ikonli.javafx.FontIcon
import org.slf4j.{Logger, LoggerFactory}


class KeeNotesFXApplication extends Application {

  val logger: Logger = LoggerFactory.getLogger(classOf[KeeNotesFXApplication])

  val settings = new Settings()

  override def start(stage: Stage): Unit = {
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
  }

  def notePane() = {
    val vbox = new VBox()

    val textArea = new TextArea()
    VBox.setVgrow(textArea, Priority.ALWAYS)
    VBox.setMargin(textArea, new Insets(0, 10, 10, 10))
    vbox.getChildren.add(textArea)

    val submit = new Button("Submit")
    clickable(submit)
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
            success()
          } else {
            logger.error(s"error: ${r.statusCode} - ${r.statusMessage}")
          }
        } catch {
          case t: Throwable => logger.error(ExceptionUtils.getStackTrace(t))
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
    settingIcon.setOnMouseClicked(e => {
      settings.preferencesFX.show(true)
    })
    clickable(settingIcon)

    HBox.setMargin(settingIcon, new Insets(10))

    hbox.getChildren.addAll(logoText, placeholder(), settingIcon)

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

}


object KeeNotesFXApplication {

  def clickable(node: Node) = {
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

