package com.keevol.keenotes.desk

import animatefx.animation.{FadeInUp, FlipInY}
import com.jfoenix.controls.JFXSnackbar.SnackbarEvent
import com.jfoenix.controls.{JFXButton, JFXSnackbar, JFXSnackbarLayout}
import com.keevol.javafx.utils.Platforms._
import com.keevol.javafx.utils.{AnchorPanes, Icons, Images, Stages}
import com.keevol.keenotes.KeeNoteCard
import com.keevol.keenotes.desk.KeeNotesFXApplication.{makeClickable, version}
import com.keevol.keenotes.desk.controls.InProgressMask
import com.keevol.keenotes.desk.domains.Note
import com.keevol.keenotes.desk.repository.NoteRepository
import com.keevol.keenotes.desk.settings.Settings
import com.keevol.keenotes.desk.utils.{CenterLabel, FontStringConverter}
import com.keevol.keenotes.splash.SplashScreenLoader
import com.sun.javafx.application.LauncherImpl
import fr.brouillard.oss.cssfx.CSSFX
import javafx.application.{Application, Platform}
import javafx.beans.binding.{Bindings, ObjectBinding}
import javafx.beans.property.SimpleBooleanProperty
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.geometry.{Insets, Pos}
import javafx.scene.control._
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.{Cursor, Node, Parent, Scene}
import javafx.stage.{Stage, WindowEvent}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.{CustomTextField, TextFields}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.atomic.AtomicReference
import java.util.{Date, Locale, ResourceBundle}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author fq@keevol.com
 */
class KeeNotesFXApplication extends Application {

  val logger: Logger = LoggerFactory.getLogger(classOf[KeeNotesFXApplication])
  val defaultFont: Font = Font.font("Aria")
  val texts: ResourceBundle = ResourceBundle.getBundle("bundles/gui", Locale.getDefault)

  val settings = new Settings(texts)
  val fontStringConverter = new FontStringConverter()
  val noteFontProperty: ObjectBinding[Font] = Bindings.createObjectBinding(() => {
    logger.info(s"create font from string: ${settings.fontProperty.get()}")
    fontStringConverter.fromString(settings.fontProperty.get())
  }, settings.fontProperty)

  val repository = new NoteRepository(settings)

  val mask = new InProgressMask
  val inProgressProperty = new SimpleBooleanProperty(false)

  var primaryStage: Stage = _
  var primaryScene: Scene = _
  var preferenceScene: Scene = _
  var insightScene: Scene = _

  val textArea = new TextArea()
  textArea.setWrapText(true)
  textArea.setCursor(Cursor.TEXT);
  textArea.setMinHeight(100)
  textArea.setPrefHeight(100)
  textArea.setMaxHeight(100)
  logger.info(s"sync font of note-taking field to ${settings.fontProperty.get()}")
  textArea.fontProperty().bind(noteFontProperty)

  val noteList = new VBox(5)

  val so = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
  so.setPrefWidth(300)
  so.setLeft(Icons.SEARCH)

  val action: () => Unit = () => {
    val keyword = StringUtils.trimToEmpty(so.getText)
    noteList.getChildren.clear()
    if (StringUtils.isNotEmpty(keyword)) {
      val notes = repository.search(keyword)
      notes.map(note => tile(note.channel, note.content, note.dt)).foreach(noteList.getChildren.add)
    } else {
      loadNoteAsPerLimit().map(note => tile(note.channel, note.content, note.dt)).foreach(noteList.getChildren.add)
    }
  }
  so.asInstanceOf[CustomTextField].getRight.setOnMouseClicked(_ => action())
  so.setOnAction(_ => action())
  so.setOnKeyReleased(_ => {
    if (StringUtils.isEmpty(StringUtils.trimToEmpty(so.getText()))) {
      action()
    }
  })

  val snackBar = new JFXSnackbar()
  snackBar.setPrefWidth(400)

  override def start(stage: Stage): Unit = {
    primaryStage = stage

    primaryScene = setupPrimaryScene()
    preferenceScene = setupPreferenceScene()
    insightScene = setupInsightScene()

    primaryStage.setScene(primaryScene)
    primaryStage.setOnShown(e => textArea.requestFocus())
    val versionString = if (StringUtils.isEmpty(StringUtils.trimToEmpty(version.get()))) "" else s"(${version.get()})"
    primaryStage.setTitle(s"KeeNotes Desk$versionString")
    primaryStage.getIcons.add(Images.from("/images/logo.png"))
    val WIDTH = 520
    val HEIGHT = 700
    primaryStage.setWidth(WIDTH)
    primaryStage.setMinWidth(WIDTH)
    primaryStage.setMaxWidth(WIDTH)
    primaryStage.setHeight(HEIGHT)
    primaryStage.setMinHeight(HEIGHT)
    if (settings.fullScreenOnStartProperty.get()) {
      primaryStage.setFullScreen(true)
    }

    val closeHandler: EventHandler[WindowEvent] = e => {
      logger.info("Close Request Received, start closing the application...")
      logger.info("Platform.exit()...")
      Platform.exit()
      logger.info("System.exit(0)...")
      System.exit(0)
    }

    primaryStage.setOnCloseRequest(e => {
      logger.info("attach close handler to primaryStage...")
      closeHandler.handle(e)
    })

    primaryStage.show()
    Stages.center(primaryStage)

    CSSFX.start()
  }

  def setupPrimaryScene(): Scene = {
    val layout = new BorderPane()
    layout.setTop(header())
    layout.setCenter(notePane())
    layout.setBottom(footer())

    val stackPane = new StackPane()
    stackPane.getChildren.add(layout)

    createSceneWithStyle(stackPane)
  }

  def setupPreferenceScene(): Scene = {
    val layout = new VBox(5)
    layout.setPadding(new Insets(10))

    val doubleClickHandler: EventHandler[MouseEvent] = e => {
      if (e.getClickCount > 1) {
        primaryStage.setScene(primaryScene)
      }
    }

    val prefPane = settings.preferencesFX.getView
    prefPane.setOnMouseClicked(doubleClickHandler)
    layout.getChildren.add(prefPane)
    VBox.setVgrow(prefPane, Priority.ALWAYS)

    val l = new CenterLabel(texts.getString("label.double.click.go.back"), true)
    l.setOnMouseClicked(doubleClickHandler)
    layout.getChildren.add(l)
    layout.getChildren.add(footer())

    val root = new StackPane(layout)
    createSceneWithStyle(root)
  }

  def setupInsightScene(): Scene = {
    // TODO
    null
  }

  def createSceneWithStyle(root: Parent): Scene = {
    val scene = new Scene(root)
    scene.getStylesheets.add("/css/style.css")
    scene.setFill(Color.web("#464646"))
    scene
  }


  def notePane() = {
    val vbox = new VBox()

    val textPane = new StackPane(textArea)
    snackBar.registerSnackbarContainer(textPane)
    VBox.setMargin(textPane, new Insets(0, 10, 10, 10))
    vbox.getChildren.add(textPane)

    val submit = new Button(texts.getString("button.submit"))
    makeClickable(submit)
    submit.setFont(defaultFont)
    submit.disableProperty().bind(Bindings.createBooleanBinding(() => StringUtils.isEmpty(StringUtils.trimToEmpty(textArea.getText())), textArea.textProperty()).or(inProgressProperty))
    submit.setOnAction(e => {
      val content = StringUtils.trimToEmpty(textArea.getText)
      val ch = "keenotes-desktop"
      if (StringUtils.isNotEmpty(content)) {
        mask.setWidth(400)
        mask.show()
        Stages.center(mask, primaryStage)

        textArea.setEditable(false)
        //        submit.setDisable(true)
        inProgressProperty.set(true)
        submit.getScene.setCursor(Cursor.WAIT)
        Future {
          try {
            repository.insert(new Note(content = content, channel = ch, dt = new Date()))

            ui {
              textArea.clear()
              noteList.getChildren.add(0, tile(ch, content)) // desc order
              info("Note Added!")
            }

          } catch {
            case t: Throwable => {
              logger.error(ExceptionUtils.getStackTrace(t))
              Platform.runLater(() => error(ExceptionUtils.getStackTrace(t)))
            }
          } finally {
            Platform.runLater(() => {
              textArea.setEditable(true)
              //              submit.setDisable(false)
              inProgressProperty.set(false)
              submit.getScene.setCursor(Cursor.DEFAULT)
              mask.close()
            })
          }
        }

      }
    })

    vbox.getChildren.add(new AnchorPane(submit))
    AnchorPane.setLeftAnchor(submit, 10)
    AnchorPane.setRightAnchor(submit, 10)


    val noteListWrapper = new ScrollPane(noteList)
    noteListWrapper.setFitToWidth(true)

    val ap = new AnchorPane(noteListWrapper)
    AnchorPanes.stickAllEdges(noteListWrapper)
    VBox.setMargin(ap, new Insets(10, 10, 5, 10))
    VBox.setVgrow(ap, Priority.ALWAYS)
    vbox.getChildren.add(ap)

    noteListWrapper.setContent(new CenterLabel(texts.getString("text.loading.message"), true, 52))

    Future {

      val notes = loadNoteAsPerLimit()
      logger.info("note count at load: {}", notes.size)
      for (note <- notes) {
        ui {
          noteList.getChildren.add(tile(note.channel, note.content, note.dt))
        }
      }

      ui {
        new FadeInUp(noteList).setSpeed(0.5).play()
        noteListWrapper.setContent(noteList)
      }
    }

    vbox
  }

  def placeholder() = {
    val r = new Region
    HBox.setHgrow(r, Priority.ALWAYS)
    r
  }

  def header() = {
    val hbox = new HBox(3)
    hbox.setAlignment(Pos.CENTER)

    HBox.setMargin(so, new Insets(10, 0, 10, 10))

    val stats = new JFXButton("", Icons.from("fa-bar-chart:21:aqua"))
    stats.setOnAction(e => {
      // TODO add analysis to notes
    })

    val settingBtn = new JFXButton("", Icons.from("fa-gear:21:aqua"))
    settingBtn.setOnAction(e => {
      new FlipInY(preferenceScene.getRoot).play()
      primaryStage.setScene(preferenceScene)
    })
    HBox.setMargin(settingBtn, new Insets(10, 10, 10, 0))

    hbox.getChildren.addAll(so, placeholder(), stats, settingBtn)

    hbox
  }

  def footer() = {
    val hbox = new HBox(10)
    hbox.setStyle("-fx-background-color: #494949;")

    val placeholder1 = new Region
    HBox.setHgrow(placeholder1, Priority.ALWAYS)

    val placeholder2 = new Region
    HBox.setHgrow(placeholder2, Priority.ALWAYS)

    val copyright = new Label("© KEEVOL Consulting @keevol.com")
    copyright.setFont(defaultFont)
    hbox.getChildren.addAll(placeholder1, copyright, placeholder2)
    HBox.setMargin(copyright, new Insets(3))
    hbox
  }

  def tile(channel: String, content: String, dt: Date = new Date()) = {
    val card = new KeeNoteCard
    card.title.setText(channel + s"@${DateFormatUtils.format(dt, "yyyy-MM-dd HH:mm:ss")}")
    //    card.title.fontProperty().bind(noteFontProperty)
    card.content.setText(content)
    card.content.fontProperty().bind(noteFontProperty)

    card.closeBtn.setOnAction(e => {
      val alert = new Alert(Alert.AlertType.CONFIRMATION, "你确定？\nAre you sure to delete the note?", ButtonType.YES, ButtonType.NO)
      alert.getDialogPane.getStylesheets.add("/css/style.css")
      val result = alert.showAndWait().orElse(ButtonType.NO)
      try {
        if (ButtonType.YES.equals(result)) {
          if (repository.delete(content, channel) > 0) {
            noteList.getChildren.remove(card)
            logger.info(s"delete note successfully: content=$content and channel=$channel")
          } else {
            logger.warn(s"删除记录失败（delete note failed）: conent=$content and channel=$channel")
          }
        }
      } finally {
        e.consume()
      }
    })
    card
  }

  def info(message: String) = {
    snackBar.enqueue(new SnackbarEvent(new JFXSnackbarLayout(message), PseudoClass.getPseudoClass("info-toast")))
  }

  def error(message: String) = {
    Notifications.create().darkStyle().title("Error").text(message).owner(primaryStage).showError()
  }

  def loadNoteAsPerLimit(): List[Note] = {
    val limitCnt = if (settings.noteDisplayLimitProperty.get() < 1) Int.MaxValue else settings.noteDisplayLimitProperty.get()
    repository.load(limitCnt)
  }

}


object KeeNotesFXApplication {

  val version: AtomicReference[String] = new AtomicReference[String]()


  def makeClickable(node: Node) = {
    node.setOnMouseEntered(e => {
      node.getScene.setCursor(Cursor.HAND)
    })
    node.setOnMouseExited(e => {
      node.getScene.setCursor(Cursor.DEFAULT)
    })
  }

  def makeNonClickable(node: Node) = {
    node.setOnMouseEntered(e => {
      node.getScene.setCursor(Cursor.DEFAULT)
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
    //    Application.launch(classOf[KeeNotesFXApplication], args: _*)
    LauncherImpl.launchApplication(classOf[KeeNotesFXApplication], classOf[SplashScreenLoader], args)
  }
}

object KeeNotesFXApplicationLauncher {
  val logger: Logger = LoggerFactory.getLogger("KeeNotesFXApplicationLauncher")

  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler((t: Thread, e: Throwable) => logger.error(s"something goes wrong in thread: ${t.getName} with exception: \n ${ExceptionUtils.getStackTrace(e)}"))

    logger.info(s"""set -Dkeenotes.desk.version(Property)=${System.getProperty("keenotes.desk.version")} to window title""")
    KeeNotesFXApplication.version.set(System.getProperty("keenotes.desk.version"))

    KeeNotesFXApplication.main(args)
  }
}

