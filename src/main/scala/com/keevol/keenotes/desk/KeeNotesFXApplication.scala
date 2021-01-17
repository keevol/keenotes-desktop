package com.keevol.keenotes.desk

import com.keevol.javafx.controls.SimpleCard
import com.keevol.javafx.utils.{AnchorPanes, Icons, Platforms, ScrollPanes, Stages}
import com.keevol.keenotes.desk.KeeNotesFXApplication.{makeClickable, makeNonClickable}
import com.keevol.keenotes.desk.repository.NoteRepository
import eu.hansolo.tilesfx.Tile.SkinType
import eu.hansolo.tilesfx.TileBuilder
import fr.brouillard.oss.cssfx.CSSFX
import javafx.application.{Application, Platform}
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, ScrollPane, TextArea}
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontWeight}
import javafx.scene.{Cursor, Node, Parent, Scene}
import javafx.stage.{Stage, StageStyle}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.controlsfx.control.Notifications
import org.controlsfx.control.textfield.{CustomTextField, TextFields}
import org.kordamp.ikonli.javafx.FontIcon
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process.Process
import com.keevol.javafx.utils.Platforms._
import com.keevol.keenotes.KeeNoteCard
import com.keevol.keenotes.desk.controls.InProgressMask
import com.keevol.keenotes.desk.domains.Note
import com.keevol.keenotes.desk.settings.Settings
import com.keevol.keenotes.desk.utils.{FontStringConverter, SimpleProcessLoggerFactory}
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.util.StringConverter

import java.lang
import java.util.concurrent.atomic.AtomicInteger


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

  val noteList = new VBox(5)

  val so = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
  so.setPrefWidth(300)
  so.setLeft(Icons.SEARCH)
  //  so.setOnKeyReleased(e => {
  //    if (StringUtils.isNotEmpty(so.getText())) {
  //      println("do something with: " + so.getText)
  //    }
  //  })
  val action: () => Unit = () => {
    val keyword = StringUtils.trimToEmpty(so.getText)
    noteList.getChildren.clear()
    if (StringUtils.isNotEmpty(keyword)) {
      val notes = repository.search(keyword)
      notes.map(note => tile(note.channel, note.content, note.dt)).foreach(noteList.getChildren.add)
    } else {
      repository.load().map(note => tile(note.channel, note.content, note.dt)).foreach(noteList.getChildren.add)
    }
  }
  so.asInstanceOf[CustomTextField].getRight.setOnMouseClicked(_ => action())
  so.setOnAction(_ => action())

  override def start(stage: Stage): Unit = {
    primaryStage = stage

    val layout = new BorderPane()
    layout.setTop(header())
    layout.setCenter(notePane())
    layout.setBottom(footer())
    stackPane.getChildren.add(layout)

    stage.setScene(setupSceneWith(stackPane))
    stage.setOnShown(e => textArea.requestFocus())
    stage.setTitle("KeeNotes Desk")
    val WIDTH = 400
    val HEIGHT = 600
    stage.setWidth(WIDTH)
    stage.setMinWidth(WIDTH)
    stage.setMaxWidth(WIDTH)
    stage.setHeight(HEIGHT)
    stage.setMinHeight(HEIGHT)
    stage.initStyle(StageStyle.UTILITY)
    stage.setOnCloseRequest(e => {
      Platform.exit()
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
            val r = requests.post(settings.noteRelayServerProperty.getValue,
              headers = Map("Content-Type" -> "application/x-www-form-urlencoded"),
              params = Map("token" -> settings.tokenProperty.getValue, "channel" -> ch, "text" -> content),
              connectTimeout = settings.connectTimeoutProperty.getValue,
              readTimeout = settings.readTimeoutProperty.getValue)

            repository.insert(new Note(content = content, channel = ch, dt = new Date()))

            if (r.statusCode == 200) {
              Platform.runLater(() => {
                textArea.clear()

                noteList.getChildren.add(0, tile(ch, content)) // desc order
                info("Note Relayed!")
              })
            } else {
              val err = s"error: ${r.statusCode} - ${r.statusMessage}"
              logger.error(err)
              Platform.runLater(() => error(err))
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
    //    noteListWrapper.viewportBoundsProperty().addListener((_, _, arg2) => {
    //      val content = noteListWrapper.getContent();
    //      noteListWrapper.setFitToWidth(content.prefWidth(-1) < arg2.getWidth());
    //      noteListWrapper.setFitToHeight(content.prefHeight(-1) < arg2.getHeight());
    //    })
    noteListWrapper.setFitToWidth(true)

    val ap = new AnchorPane(noteListWrapper)
    AnchorPanes.stickAllEdges(noteListWrapper)
    VBox.setMargin(ap, new Insets(10, 10, 5, 10))
    VBox.setVgrow(ap, Priority.ALWAYS)
    vbox.getChildren.add(ap)
    //    VBox.setMargin(noteListWrapper, new Insets(10, 10, 5, 10))
    //    VBox.setVgrow(noteListWrapper, Priority.ALWAYS)
    //    vbox.getChildren.add(noteListWrapper)

    Future {
      val loadLimit = if (settings.noteDisplayLimitProperty.get() < 1) Int.MaxValue else settings.noteDisplayLimitProperty.get()
      val notes = repository.load(loadLimit)
      logger.info("note count at load: {}", notes.size)
      for (note <- notes) {
        Platforms.ui() {
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
    //        card.prefHeightProperty().bind(settings.cardPrefHeightProperty)
    card.content.setFont(fontStringConverter.fromString(settings.fontProperty.get()))
    Bindings.bindBidirectional(settings.fontProperty, card.content.fontProperty(), fontStringConverter)
    card
  }

  def info(message: String) = {
    Notifications.create().darkStyle().title("Success").text(message).owner(primaryStage).showInformation()
  }

  def error(message: String) = {
    Notifications.create().darkStyle().title("Error").text(message).owner(primaryStage).showError()
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

        sync.setIconColor(Color.BLUE)
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
  def main(args: Array[String]): Unit = {
    KeeNotesFXApplication.main(args)
  }
}

