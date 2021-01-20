package com.keevol.keenotes.desk.settings

import javafx.scene.Node
import javafx.scene.control.{ButtonType, Dialog}
/**
 * @author fq@keevol.com
 */
class SettingsDialog extends Dialog {

  setTitle("Settings")

  val dp = getDialogPane()
  dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

  dp.getStylesheets.add("/css/style.css")

  def setContent(content: Node) = {
    dp.setContent(content)
  }

}