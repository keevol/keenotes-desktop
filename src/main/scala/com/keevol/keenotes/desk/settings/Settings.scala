package com.keevol.keenotes.desk.settings

import com.dlsc.formsfx.model.structure.Field
import com.dlsc.formsfx.model.util.ResourceBundleService
import com.dlsc.preferencesfx.PreferencesFx
import com.dlsc.preferencesfx.model.{Category, Group, Setting}
import com.dlsc.preferencesfx.view.PreferencesFxDialog
import javafx.beans.property.{SimpleBooleanProperty, SimpleIntegerProperty, SimpleStringProperty}

import java.util.ResourceBundle

/**
 * @author fq@keevol.com
 */
class Settings(val texts: ResourceBundle) {
  val rbs = new ResourceBundleService(texts)

  val localStoreOnlyProperty = new SimpleBooleanProperty(true)
  val sqliteFileProperty = new SimpleStringProperty(s"${System.getProperty("user.home")}/keenotes.sqlite3")

  val noteRelayServerProperty = new SimpleStringProperty("")
  val tokenProperty = new SimpleStringProperty("")
  val connectTimeoutProperty = new SimpleIntegerProperty(30000)
  val readTimeoutProperty = new SimpleIntegerProperty(30000)
  val syncCommandProperty = new SimpleStringProperty("")

  val fullScreenOnStartProperty = new SimpleBooleanProperty(false)
  val noteDisplayLimitProperty = new SimpleIntegerProperty()
  val fontProperty = new SimpleStringProperty("Serif, 14.0, Regular")

  val basicGroup: Group = Group.of("label.local.config",
    Setting.of("label.sqlite.location", sqliteFileProperty),
  )

  val uiGroup: Group = Group.of("label.ui.section.title",
    Setting.of("label.ui.fullscreen.onstart", fullScreenOnStartProperty),
    Setting.of("label.ui.note.display.num", noteDisplayLimitProperty),
    Setting.of("label.ui.note.font", Field.ofStringType(fontProperty).render(new SimpleFontControl()), fontProperty)
  )

  val preferencesFX: PreferencesFx = PreferencesFx.of(getClass, Category.of("label.preference.title", basicGroup, uiGroup))
    .buttonsVisibility(true)
    .debugHistoryMode(true)
    .instantPersistent(true)
    .i18n(rbs)
    .saveSettings(true)

  attachCss(preferencesFX)

  def attachCss(pref: PreferencesFx): Unit = {
    val f = pref.getClass.getDeclaredField("preferencesFxDialog")
    f.setAccessible(true)
    f.get(pref).asInstanceOf[PreferencesFxDialog].getStylesheets.add("/css/style.css")
  }
}