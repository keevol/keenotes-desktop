package com.keevol.keenotes.desk

import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Group, Setting}
import com.dlsc.preferencesfx.view.PreferencesFxDialog
import javafx.beans.property.{SimpleIntegerProperty, SimpleStringProperty}

class Settings {
  val noteRelayServerProperty = new SimpleStringProperty("")
  val tokenProperty = new SimpleStringProperty("")
  val connectTimeoutProperty = new SimpleIntegerProperty(30000)
  val readTimeoutProperty = new SimpleIntegerProperty(30000)

  val sqliteFileProperty = new SimpleStringProperty(s"${System.getProperty("user.home")}/keenotes.sqlite3")

  val syncCommandProperty = new SimpleStringProperty("")

  val preferencesFX: PreferencesFx = PreferencesFx.of(getClass,
    Category.of("KeeNotes Preferences",
      Group.of("Relay Server",
        Setting.of("Note Server", noteRelayServerProperty),
        Setting.of("Token", tokenProperty),
        Setting.of("Connect Timeout", connectTimeoutProperty),
        Setting.of("Read Timeout", readTimeoutProperty)),
      Group.of("Keenotes Storage",
        Setting.of("Keenotes Sqlite", sqliteFileProperty),
        Setting.of("rsync note command", syncCommandProperty))
    )).buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .saveSettings(true)

  val f = preferencesFX.getClass.getDeclaredField("preferencesFxDialog")
  f.setAccessible(true)
  f.get(preferencesFX).asInstanceOf[PreferencesFxDialog].getStylesheets.add("/css/style.css")

}