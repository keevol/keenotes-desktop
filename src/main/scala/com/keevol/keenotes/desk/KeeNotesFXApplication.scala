package com.keevol.keenotes.desk

import com.keevol.javafx.utils.{Icons, ScrollPanes, Stages}
import com.keevol.keenotes.desk.KeeNotesFXApplication.{makeClickable, makeNonClickable}
import com.keevol.keenotes.desk.repository.NoteRepository
import eu.hansolo.tilesfx.Tile.SkinType
import eu.hansolo.tilesfx.TileBuilder
import fr.brouillard.oss.cssfx.CSSFX
import javafx.application.{Application, Platform}
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, TextArea}
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.scene.text.Font
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
import com.keevol.keenotes.desk.utils.SimpleProcessLoggerFactory
import javafx.beans.value.{ChangeListener, ObservableValue}

import java.lang


class KeeNotesFXApplication extends Application {

  val logger: Logger = LoggerFactory.getLogger(classOf[KeeNotesFXApplication])

  val settings = new Settings()

  val repository = new NoteRepository(settings)

  val mask = new InProgressMask

  var primaryStage: Stage = _

  val so = TextFields.createClearableTextField().asInstanceOf[CustomTextField]
  so.setPrefWidth(300)
  so.setLeft(Icons.SEARCH)
  so.setOnKeyReleased(e => {
    if (StringUtils.isNotEmpty(so.getText())) {
      println("do something with: " + so.getText) // TODO searching as filtering
    }
  })
  so.setOnAction(e => println("perform Searching As Filtering."))

  val stackPane = new StackPane()


  val noteList = new VBox(5)

  override def start(stage: Stage): Unit = {
    primaryStage = stage

    val layout = new BorderPane()
    layout.setTop(header())
    layout.setCenter(notePane())
    layout.setBottom(footer())
    stackPane.getChildren.add(layout)

    stage.setScene(setupSceneWith(stackPane))

    stage.setTitle("KeeNotes Desk")
    stage.setWidth(400)
    stage.setMaxWidth(400)
    stage.setHeight(600)
    stage.setMinHeight(600)
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
    scene
  }


  def notePane() = {
    val vbox = new VBox()

    val textArea = new TextArea()
    textArea.setWrapText(true)
    textArea.setCursor(Cursor.TEXT);
    //    VBox.setVgrow(textArea, Priority.ALWAYS)
    textArea.setMinHeight(100)
    textArea.setPrefHeight(100)
    textArea.setMaxHeight(100)
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


    //    val textTile = TileBuilder.create()
    //      .skinType(SkinType.TEXT)
    //      //      .prefSize(300, 300)
    //      .title("Text Tile")
    //      .text("Whatever text")
    //      .description("May the force be with you\n...always")
    //      .descriptionAlignment(Pos.TOP_LEFT)
    //      .textVisible(true)
    //      .build();
    //
    //    val textTile2 = TileBuilder.create()
    //      .skinType(SkinType.TEXT)
    //      //      .prefSize(300, 300)
    //      .title("Text Tile")
    //      .text("Whatever text")
    //      .description("May the force be with you ...always, LIve Long And Prosper")
    //      .descriptionAlignment(Pos.TOP_LEFT)
    //      //      .textVisible(true)
    //      .build();
    //
    //
    //    noteList.getChildren.addAll(textTile, textTile2, tile("keenotes-desk", "yosbits の webサーバーは利用可能な状態になっています。工事業者による点検の結果、光ケーブルの信号レベルが基準範囲内ですが、張り替え作業を行うということで張り替えと終端措置の置き換えをされました。よく問題が起こるところから順に置き換えるというやり方をしているとのことで様子を見ます。"))

    val noteListWrapper = ScrollPanes.wrap(noteList)
    VBox.setVgrow(noteListWrapper, Priority.ALWAYS)
    VBox.setMargin(noteListWrapper, new Insets(10))
    vbox.getChildren.add(noteListWrapper)

    Future {
      repository.load().foreach(note => ui { () =>
        noteList.getChildren.add(tile(note.channel, note.content, note.dt))
      })
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
    settingIcon.setIconLiteral("fa-gear:32:aqua")
    makeClickable(settingIcon)
    settingIcon.setOnMouseClicked(e => {
      settings.preferencesFX.show(true)
    })

    HBox.setMargin(settingIcon, new Insets(10))

    val sync = new FontIcon()
    sync.setIconLiteral("fa-refresh:32:aqua")
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

  def tile(channel: String, content: String, dt: Date = new Date()) = TileBuilder.create()
    .skinType(SkinType.TEXT)
    .title(channel + s"@${DateFormatUtils.format(dt, "yyyy-MM-dd HH:mm:ss")}")
    .titleColor(Color.web("#3383F8"))
    //    .text(DateFormatUtils.format(dt, "yyyy-MM-dd HH:mm:ss"))
    .description(content)
    .descriptionAlignment(Pos.CENTER_LEFT)
    //    .textColor(Color.web("gray"))
    .textVisible(true)
    .build();

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

