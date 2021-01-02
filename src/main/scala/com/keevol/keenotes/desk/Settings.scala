package com.keevol.keenotes.desk

import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Group, Setting}
import javafx.beans.property.{SimpleIntegerProperty, SimpleStringProperty}

class Settings {
  val noteRelayServerProperty = new SimpleStringProperty("")
  val tokenProperty = new SimpleStringProperty("")
  val connectTimeoutProperty = new SimpleIntegerProperty(30000)
  val readTimeoutProperty = new SimpleIntegerProperty(30000)

  val preferencesFX: PreferencesFx = PreferencesFx.of(getClass,
    Category.of("KeeNotes Preferences",
      Group.of("Relay Server",
        Setting.of("Note Server", noteRelayServerProperty),
        Setting.of("Token", tokenProperty),
        Setting.of("Connect Timeout", connectTimeoutProperty),
        Setting.of("Read Timeout", readTimeoutProperty))
    )).buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .saveSettings(true)

}