//package com.keevol.keenotes.desk
//
//import javafx.application.Application
//import javafx.scene.Scene
//import javafx.scene.shape.Rectangle
//import javafx.stage.Stage
//
//class PreviewSampler extends Application {
//  override def start(stage: Stage): Unit = {
//
//    import javafx.scene.layout.StackPane
//    import javafx.scene.paint.Color
//    val pane = new NoteList
//
//    stage.setScene(new Scene(pane))
//    stage.setWidth(1024)
//    stage.setHeight(768)
//    stage.show()
//
//  }
//}
//
//object PreviewSampler {
//  def main(args: Array[String]): Unit = {
//    Application.launch(classOf[PreviewSampler], args: _*)
//  }
//}
//
//object PreviewIDERunner {
//  def main(args: Array[String]): Unit = {
//    PreviewSampler.main(args)
//  }
//}