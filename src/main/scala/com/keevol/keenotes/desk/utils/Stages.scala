package com.keevol.keenotes.desk.utils

import javafx.stage.Stage
import javafx.stage.Screen

object Stages {
  def center(stage: Stage) = {
    val primScreenBounds = Screen.getPrimary.getVisualBounds
    stage.setX((primScreenBounds.getWidth - stage.getWidth) / 2)
    stage.setY((primScreenBounds.getHeight - stage.getHeight) / 2)
  }

  def center(stage: Stage, parent: Stage) = {
    val primScreenBounds = Screen.getPrimary.getVisualBounds
    stage.setX((primScreenBounds.getWidth - stage.getWidth) / 2 + (parent.getWidth - stage.getWidth)/2)
    stage.setY((primScreenBounds.getHeight - stage.getHeight) / 2)
  }
}