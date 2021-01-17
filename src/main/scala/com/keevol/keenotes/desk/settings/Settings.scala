package com.keevol.keenotes.desk.settings

import com.dlsc.formsfx.model.structure.Field
import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Group, Setting}
import com.dlsc.preferencesfx.view.PreferencesFxDialog
import javafx.beans.property.{SimpleBooleanProperty, SimpleDoubleProperty, SimpleIntegerProperty, SimpleLongProperty, SimpleObjectProperty, SimpleStringProperty}
import javafx.scene.text.Font

class Settings {

  val localStoreOnlyProperty = new SimpleBooleanProperty(true)

  val noteRelayServerProperty = new SimpleStringProperty("")
  val tokenProperty = new SimpleStringProperty("")
  val connectTimeoutProperty = new SimpleIntegerProperty(30000)
  val readTimeoutProperty = new SimpleIntegerProperty(30000)

  val sqliteFileProperty = new SimpleStringProperty(s"${System.getProperty("user.home")}/keenotes.sqlite3")

  val syncCommandProperty = new SimpleStringProperty("")

  val noteDisplayLimitProperty = new SimpleIntegerProperty()
  val fontProperty = new SimpleStringProperty("Serif")

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
      Group.of("GUI Settings",
        Setting.of("Note Display Num.", noteDisplayLimitProperty),
        Setting.of("Font", Field.ofStringType(fontProperty).render(new SimpleFontControl()), fontProperty)
      )

    )).buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .saveSettings(true)

  val f = preferencesFX.getClass.getDeclaredField("preferencesFxDialog")
  f.setAccessible(true)
  f.get(preferencesFX).asInstanceOf[PreferencesFxDialog].getStylesheets.add("/css/style.css")

}