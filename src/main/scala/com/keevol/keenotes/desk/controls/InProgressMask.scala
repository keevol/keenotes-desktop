package com.keevol.keenotes.desk.controls

import com.keevol.javafx.utils.AnchorPanes
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.{Label, ProgressBar}
import javafx.scene.layout.{AnchorPane, VBox}
import javafx.scene.paint.Color
import javafx.stage.{Modality, Stage, StageStyle}

class InProgressMask extends Stage {
  val layout = new VBox(10)
  layout.setPadding(new Insets(10))

  val title = new Label("Processing...")
  title.setStyle("-fx-text-fill: white;")

  val indicator = new ProgressBar()
  layout.getChildren.addAll(title, new AnchorPane(indicator))
  AnchorPanes.stickLeftRight(indicator)

  val scene = new Scene(layout)
  scene.setFill(Color.TRANSPARENT)
  scene.getStylesheets.add("/css/transparent.css")
  setScene(scene)
  initModality(Modality.APPLICATION_MODAL)
  initStyle(StageStyle.TRANSPARENT)

}