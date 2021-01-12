package com.keevol.keenotes.desk

import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Group, Setting}
import com.dlsc.preferencesfx.view.PreferencesFxDialog
import javafx.beans.property.{SimpleBooleanProperty, SimpleDoubleProperty, SimpleIntegerProperty, SimpleStringProperty}

class Settings {

  val localStoreOnlyProperty = new SimpleBooleanProperty(true)

  val noteRelayServerProperty = new SimpleStringProperty("")
  val tokenProperty = new SimpleStringProperty("")
  val connectTimeoutProperty = new SimpleIntegerProperty(30000)
  val readTimeoutProperty = new SimpleIntegerProperty(30000)

  val sqliteFileProperty = new SimpleStringProperty(s"${System.getProperty("user.home")}/keenotes.sqlite3")

  val syncCommandProperty = new SimpleStringProperty("")

  val cardPrefHeightProperty = new SimpleDoubleProperty(111)

  val preferencesFX: PreferencesFx = PreferencesFx.of(getClass,
    Category.of("KeeNotes Preferences",

      Group.of("Keenotes Local Configuration",
        Setting.of("Local Store Only", localStoreOnlyProperty),
        Setting.of("Keenotes Sqlite", sqliteFileProperty),
      ),

      Group.of("Remote Relay Server Configuration",
        Setting.of("Note Server", noteRelayServerProperty),
        Setting.of("Token", tokenProperty),
        Setting.of("Connect Timeout", connectTimeoutProperty),
        Setting.of("Read Timeout", readTimeoutProperty),
        Setting.of("rsync note command", syncCommandProperty)),
      Group.of("GUI Settings", Setting.of("Card Pref Height", cardPrefHeightProperty))

    )).buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .saveSettings(true)

  val f = preferencesFX.getClass.getDeclaredField("preferencesFxDialog")
  f.setAccessible(true)
  f.get(preferencesFX).asInstanceOf[PreferencesFxDialog].getStylesheets.add("/css/style.css")

}