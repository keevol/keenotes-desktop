package com.keevol.keenotes.desk.utils

import animatefx.animation.Bounce
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.{HBox, Priority, Region}
import javafx.scene.text.Font

class CenterLabel(text: String, dynamicBounce: Boolean = false, fontSize: Int = 11) extends HBox {
  //  setStyle("-fx-background-color: #494949;")

  val placeholder1 = new Region
  HBox.setHgrow(placeholder1, Priority.ALWAYS)

  val placeholder2 = new Region
  HBox.setHgrow(placeholder2, Priority.ALWAYS)

  val l = new Label(text)
  l.setFont(Font.font(fontSize))
  getChildren.addAll(placeholder1, l, placeholder2)
  HBox.setMargin(l, new Insets(3))

  if (dynamicBounce) {
    new Bounce(l)
      .setCycleCount(Int.MaxValue)
      .setSpeed(0.5)
      .setResetOnFinished(true)
      .play()
  }
}