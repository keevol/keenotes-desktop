package com.keevol.keenotes.desk

import com.dlsc.formsfx.model.structure.Field
import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Setting}
import com.keevol.keenotes.desk.settings.SimpleFontControl
import com.keevol.keenotes.desk.utils.FontStringConverter
import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Scene
import javafx.scene.control.{Button, Label}
import javafx.scene.layout.VBox
import javafx.scene.text.{Font, FontWeight}
import javafx.stage.Stage
import org.apache.commons.lang3.StringUtils

class PreviewSampler extends Application {

  val fontStringConverter = new FontStringConverter()
  val fontProperty = new SimpleStringProperty("Serif")


  override def start(stage: Stage): Unit = {

    val pref = PreferencesFx.of(getClass, Category.of("Test", Setting.of("Font", Field.ofStringType(fontProperty).render(new SimpleFontControl()), fontProperty))).instantPersistent(true)

    val layout = new VBox(20)
    val l = new Label()
    l.setFont(fontStringConverter.fromString(fontProperty.getValue))

    val btn = new Button("Open")
    btn.setOnAction(e => {
      pref.show()
    })

    fontProperty.addListener((_, _, newValue) => {
      val f = fontStringConverter.fromString(newValue)

      println("set font to button text after font change...")
      l.setText(newValue)
      l.setFont(f) // setFont如果要多次调用， 需要在目标组件上先调用setFont设置一个默认的值， 否则后面任何时候再设置就不work。
    })
    layout.getChildren.addAll(l, btn)

    //    val card = new KeeNoteCard()
    //    card.title.setText("Roof over your head - 新加坡和东盟大陆与亚洲大陆相连")
    //    card.content.setText(
    //      """
    //        |#CNCN E103 - Want a new or different #Kubernetes job, or is your company looking for skilled workers? Check out the new careers board from
    //        |@learnk8s
    //        |. https://youtube.com/watch?v=0RksmQNPcUs&t=503  ☕️☕️☕️☕️
    //        |标普500指数在2020年收于3,756点，高盛预计该指数在今年将上涨14%，在2022年将进一步上涨7%，达到4,600点。
    //        |""".stripMargin)

    stage.setScene(new Scene(layout))
    stage.getScene.getStylesheets.add("/css/style.css")
    stage.setWidth(1024)
    stage.setHeight(768)
    stage.show()

  }
}

object PreviewSampler {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[PreviewSampler], args: _*)
  }
}

object PreviewIDERunner {
  def main(args: Array[String]): Unit = {
    PreviewSampler.main(args)
  }
}