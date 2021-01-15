package com.keevol.keenotes.desk

import com.keevol.javafx.utils.Icons
import com.keevol.keenotes.KeeNoteCard
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

class PreviewSampler extends Application {

  override def start(stage: Stage): Unit = {

    val card = new KeeNoteCard()
    card.title.setText("Roof over your head - 新加坡和东盟大陆与亚洲大陆相连")
    card.content.setText(
      """
        |#CNCN E103 - Want a new or different #Kubernetes job, or is your company looking for skilled workers? Check out the new careers board from
        |@learnk8s
        |. https://youtube.com/watch?v=0RksmQNPcUs&t=503  ☕️☕️☕️☕️
        |标普500指数在2020年收于3,756点，高盛预计该指数在今年将上涨14%，在2022年将进一步上涨7%，达到4,600点。
        |""".stripMargin)

    stage.setScene(new Scene(card))
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