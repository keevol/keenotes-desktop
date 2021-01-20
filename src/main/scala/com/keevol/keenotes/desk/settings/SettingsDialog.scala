package com.keevol.keenotes.desk.settings

import javafx.beans.InvalidationListener
import javafx.scene.control.{ButtonType, Dialog}

/**
 * @author fq@keevol.com
 */
class SettingsDialog(settings: Settings) extends Dialog {

  setTitle("Settings")

  val dp = getDialogPane()
  dp.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL)

  dp.getStylesheets.add("/css/style.css")

  val switchListener: InvalidationListener = _ => {
    if (settings.localStoreOnlyProperty.get()) {
      dp.setContent(settings.preferencesFXLocal.getView)
    } else {
      dp.setContent(settings.preferencesFX.getView)
    }
  }
  settings.localStoreOnlyProperty.addListener(switchListener)
  switchListener.invalidated(settings.localStoreOnlyProperty)

}