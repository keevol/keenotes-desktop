package com.keevol.keenotes.desk

import com.keevol.javafx.utils.Platforms._
import com.keevol.javafx.utils.{AnchorPanes, Icons, Stages}
import com.keevol.keenotes.KeeNoteCard
import com.keevol.keenotes.desk.KeeNotesFXApplication.{makeClickable, makeNonClickable}
import com.keevol.keenotes.desk.controls.InProgressMask
import com.keevol.keenotes.desk.domains.Note
import com.keevol.keenotes.desk.repository.NoteRepository
import com.keevol.keenotes.desk.settings.Settings
import com.keevol.keenotes.desk.utils.{FontStringConverter, SimpleProcessLoggerFactory}
import fr.brouillard.oss.cssfx.CSSFX
import javafx.application.{Application, Platform}
import javafx.beans.binding.Bindings
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.{Insets, Pos}
import javafx.scene.control._
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.{Cursor, Node, Parent, Scene}
import javafx.stage.Stage
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.{CustomTextField, TextFields}
import org.kordamp.ikonli.javafx.FontIcon
import org.slf4j.{Logger, LoggerFactory}

import java.awt.SplashScreen
import java.lang
import java.util.Date
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process.Process


class KeeNotesFXApplication extends Application {

  val logger: Logger = LoggerFactory.getLogger(classOf[KeeNotesFXApplication])

  val settings = new Settings()
  val fontStringConverter = new FontStringConverter()

  val repository = new NoteRepository(settings)

  val mask = new InProgressMask

  var primaryStage: Stage = _

  val stackPane = new StackPane()

  val textArea = new TextArea()
  textArea.setWrapText(true)
  textArea.setCursor(Cursor.TEXT);
  textArea.setMinHeight(100)
  textArea.setPrefHeight(100)
  textArea.setMaxHeight(100)
  logger.info(s"set font of note-taking field to ${settings.fontProperty.get()}")
  textArea.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
  textArea.fontProperty().bind(Bindings.createObjectBinding(() => fontStringConverter.fromString(settings.fontProperty.get()), settings.fontProperty))

  val noteList = new VBox(5)

  val so = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
  so.setPrefWidth(300)
  so.setLeft(Icons.SEARCH)
  logger.info(s"set font of search field to ${settings.fontProperty.get()}")
  so.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
  settings.fontProperty.addListener(_ => {
    logger.info(s"set font for search field on font change to : ${settings.fontProperty.get()}")
    so.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
  })

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

  override def init(): Unit = {
    super.init()

    if (SplashScreen.getSplashScreen != null)
      SplashScreen.getSplashScreen.close()
  }

  override def start(stage: Stage): Unit = {
    primaryStage = stage

    val layout = new BorderPane()
    layout.setTop(header())
    layout.setCenter(notePane())
    layout.setBottom(footer())
    stackPane.getChildren.add(layout)

    stage.setScene(setupSceneWith(stackPane))
    stage.setOnShown(e => textArea.requestFocus())
    stage.setTitle(s"KeeNotes Desk")
    val WIDTH = 400
    val HEIGHT = 600
    stage.setWidth(WIDTH)
    stage.setMinWidth(WIDTH)
    stage.setMaxWidth(WIDTH)
    stage.setHeight(HEIGHT)
    stage.setMinHeight(HEIGHT)
    //    stage.initStyle(StageStyle.UTILITY)
    stage.setOnCloseRequest(e => {
      logger.info("Close Request Received, start closing the application...")
      logger.info("Platform.exit()...")
      Platform.exit()
      logger.info("System.exit(0)...")
      System.exit(0)
    })
    stage.show()
    Stages.center(stage)

    CSSFX.start()
  }


  def setupSceneWith(pane: Parent): Scene = {
    val scene = new Scene(pane)
    scene.getStylesheets.add("/css/style.css")
    scene.setFill(Color.web("#464646"))
    scene
  }


  def notePane() = {
    val vbox = new VBox()

    VBox.setMargin(textArea, new Insets(0, 10, 10, 10))
    vbox.getChildren.add(textArea)

    val submit = new Button("Submit")
    makeClickable(submit)
    submit.setFont(Font.font("Arial Black", 11))
    submit.disableProperty().bind(Bindings.createBooleanBinding(() => StringUtils.isEmpty(StringUtils.trimToEmpty(textArea.getText())), textArea.textProperty()))
    submit.setOnAction(e => {
      val content = StringUtils.trimToEmpty(textArea.getText)
      val ch = "keenotes-desktop"
      if (StringUtils.isNotEmpty(content)) {
        mask.setWidth(400)
        mask.show()
        Stages.center(mask, primaryStage)

        textArea.setEditable(false)
        submit.setDisable(true)
        submit.getScene.setCursor(Cursor.WAIT)
        Future {
          try {
            repository.insert(new Note(content = content, channel = ch, dt = new Date()))

            if (!settings.localStoreOnlyProperty.get()) {
              val r = requests.post(settings.noteRelayServerProperty.getValue,
                headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
                params = Map("token" -> settings.tokenProperty.getValue, "channel" -> ch, "text" -> content),
                connectTimeout = settings.connectTimeoutProperty.getValue,
                readTimeout = settings.readTimeoutProperty.getValue)
              if (r.statusCode != 200) {
                throw new Exception(s"remote note relay error: ${r.statusCode} - ${r.statusMessage}")
              }
            }

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
              submit.setDisable(false)
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

    Future {
      val notes = loadNoteAsPerLimit()
      logger.info("note count at load: {}", notes.size)
      for (note <- notes) {
        ui {
          noteList.getChildren.add(tile(note.channel, note.content, note.dt))
        }
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
    val hbox = new HBox(10)
    hbox.setAlignment(Pos.CENTER)

    HBox.setMargin(so, new Insets(10))

    val settingIcon = new FontIcon()
    settingIcon.setIconLiteral("fa-gear:21:aqua")
    makeClickable(settingIcon)
    settingIcon.setOnMouseClicked(e => {
      settings.preferencesFX.show(true)
    })

    HBox.setMargin(settingIcon, new Insets(10))

    val sync = new FontIcon()
    sync.setIconLiteral("fa-refresh:21:aqua")
    settings.localStoreOnlyProperty.addListener(new ChangeListener[java.lang.Boolean] {
      override def changed(observable: ObservableValue[_ <: lang.Boolean], oldValue: lang.Boolean, newValue: lang.Boolean): Unit = updateStateOfSyncUI(sync, newValue)
    })
    updateStateOfSyncUI(sync, settings.localStoreOnlyProperty.get())

    hbox.getChildren.addAll(so, placeholder(), sync, settingIcon)

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
    copyright.setFont(Font.font("Arial Black", 9))
    hbox.getChildren.addAll(placeholder1, copyright, placeholder2)
    HBox.setMargin(copyright, new Insets(3))
    hbox
  }

  def tile(channel: String, content: String, dt: Date = new Date()) = {
    val card = new KeeNoteCard
    card.title.setText(channel + s"@${DateFormatUtils.format(dt, "yyyy-MM-dd HH:mm:ss")}")
    card.content.setText(content)
    card.content.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
    settings.fontProperty.addListener(_ => {
      card.title.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
      card.content.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
    })

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
    Notifications.create().darkStyle().title("Success").text(message).owner(primaryStage).showInformation()
  }

  def error(message: String) = {
    Notifications.create().darkStyle().title("Error").text(message).owner(primaryStage).showError()
  }

  def loadNoteAsPerLimit(): List[Note] = {
    val limitCnt = if (settings.noteDisplayLimitProperty.get() < 1) Int.MaxValue else settings.noteDisplayLimitProperty.get()
    repository.load(limitCnt)
  }

  def updateStateOfSyncUI(sync: FontIcon, newValue: lang.Boolean): Unit = {
    if (newValue) {
      sync.setIconColor(Color.GREY)
      makeNonClickable(sync)
      sync.setOnMouseClicked(null)
    } else {
      sync.setIconColor(Color.AQUA)
      makeClickable(sync)
      sync.setOnMouseClicked(e => {
        val processLogger = new SimpleProcessLoggerFactory

        mask.setWidth(400)
        mask.show()
        Stages.center(mask, primaryStage)

        sync.setIconColor(Color.GRAY)
        sync.getScene.setCursor(Cursor.WAIT)
        Future {
          try {
            if (StringUtils.isEmpty(StringUtils.trimToEmpty(settings.syncCommandProperty.get()))) throw new IllegalArgumentException(s"bad sync command: ${settings.syncCommandProperty}")
            val exitCode = Process(Seq("bash", "-c", settings.syncCommandProperty.get()), None, System.getenv().asScala.toSeq: _*) ! processLogger.logger
            exitCode match {
              case 0 => Platform.runLater(() => info("Note Synced Successfully."))
              case _ => {
                Platform.runLater(() => error(s"something goes wrong with exit code=$exitCode, check log for more information."))
                logger.error(processLogger.getConsoleOutput()._1 + "\n" + processLogger.getConsoleOutput()._2)
              }
            }
          } catch {
            case t: Throwable => {
              Platform.runLater(() => error(s"something goes wrong with exception thrown, check log for more information."))
              logger.error(ExceptionUtils.getStackTrace(t) + "\n" + processLogger.getConsoleOutput()._1 + "\n" + processLogger.getConsoleOutput()._2)
            }
          } finally {
            Platform.runLater(() => {
              sync.getScene.setCursor(Cursor.DEFAULT)
              sync.setIconColor(Color.AQUA)
              mask.close()
            })
          }
        }
      })
    }
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
    Application.launch(classOf[KeeNotesFXApplication], args: _*)
  }
}

object KeeNotesFXApplicationLauncher {
  val logger: Logger = LoggerFactory.getLogger("KeeNotesFXApplicationLauncher")

  def main(args: Array[String]): Unit = {
    Thread.setDefaultUncaughtExceptionHandler((t: Thread, e: Throwable) => logger.error(s"something goes wrong in thread: ${t.getName} with exception: \n ${ExceptionUtils.getStackTrace(e)}"))

    KeeNotesFXApplication.main(args)
  }
}

