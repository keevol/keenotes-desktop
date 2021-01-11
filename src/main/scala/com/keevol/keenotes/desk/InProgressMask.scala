package com.keevol.keenotes.desk

import com.keevol.javafx.utils.AnchorPanes
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.{Label, ProgressBar}
import javafx.scene.layout.{AnchorPane, VBox}
import javafx.stage.{Modality, Stage, StageStyle}

class InProgressMask extends Stage {
  val layout = new VBox(10)
  layout.setPadding(new Insets(10))

  val title = new Label("Processing...")

  val indicator = new ProgressBar()
  layout.getChildren.addAll(title, new AnchorPane(indicator))
  AnchorPanes.stickLeftRight(indicator)

  setScene(new Scene(layout))
  initModality(Modality.APPLICATION_MODAL)
  initStyle(StageStyle.TRANSPARENT)
}