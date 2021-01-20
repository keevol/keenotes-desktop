package com.keevol.keenotes.desk.settings

import com.dlsc.formsfx.model.structure.Field
import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Group, Setting}
import com.dlsc.preferencesfx.view.PreferencesFxDialog
import javafx.beans.InvalidationListener
import javafx.beans.property.{SimpleBooleanProperty, SimpleIntegerProperty, SimpleStringProperty}

/**
 * @author fq@keevol.com
 */
class Settings {

  val localStoreOnlyProperty = new SimpleBooleanProperty(true)
  val sqliteFileProperty = new SimpleStringProperty(s"${System.getProperty("user.home")}/keenotes.sqlite3")

  val noteRelayServerProperty = new SimpleStringProperty("")
  val tokenProperty = new SimpleStringProperty("")
  val connectTimeoutProperty = new SimpleIntegerProperty(30000)
  val readTimeoutProperty = new SimpleIntegerProperty(30000)
  val syncCommandProperty = new SimpleStringProperty("")

  val noteDisplayLimitProperty = new SimpleIntegerProperty()
  val fontProperty = new SimpleStringProperty("Serif, 14.0, Regular")


  val basicGroup: Group = Group.of("KeeNotes Local Configuration",
    Setting.of("Local Store Only", localStoreOnlyProperty),
    Setting.of("KeeNotes Sqlite", sqliteFileProperty),
  )

  val basicGroupLocal: Group = Group.of("KeeNotes Local Configuration",
    Setting.of("Local Store Only", localStoreOnlyProperty),
    Setting.of("KeeNotes Sqlite", sqliteFileProperty),
  )

  val remoteGroup: Group = Group.of("Remote Relay Server Configuration",
    Setting.of("Note Server", noteRelayServerProperty),
    Setting.of("Token", tokenProperty),
    Setting.of("Connect Timeout", connectTimeoutProperty),
    Setting.of("Read Timeout", readTimeoutProperty),
    Setting.of("rsync note command", syncCommandProperty))

  val uiGroup: Group = Group.of("GUI Settings",
    Setting.of("Note Display Num.", noteDisplayLimitProperty),
    Setting.of("Font", Field.ofStringType(fontProperty).render(new SimpleFontControl()), fontProperty)
  )
  val uiGroupLocal: Group = Group.of("GUI Settings",
    Setting.of("Note Display Num.", noteDisplayLimitProperty),
    Setting.of("Font", Field.ofStringType(fontProperty).render(new SimpleFontControl()), fontProperty)
  )

  val category: Category = Category.of("KeeNotes Preferences", basicGroup, uiGroup, remoteGroup)
  val categoryLocal: Category = Category.of("KeeNotes Preferences", basicGroupLocal, uiGroupLocal)

  val preferencesFX: PreferencesFx = PreferencesFx.of(getClass, category)
    .buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .saveSettings(true)

  val preferencesFXLocal: PreferencesFx = PreferencesFx.of(getClass, categoryLocal)
    .buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .saveSettings(true)

  attachCss(preferencesFX)
  attachCss(preferencesFXLocal)

  def attachCss(pref: PreferencesFx): Unit = {
    val f = pref.getClass.getDeclaredField("preferencesFxDialog")
    f.setAccessible(true)
    f.get(pref).asInstanceOf[PreferencesFxDialog].getStylesheets.add("/css/style.css")
  }
}