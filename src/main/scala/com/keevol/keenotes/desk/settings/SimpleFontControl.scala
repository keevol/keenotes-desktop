package com.keevol.keenotes.desk.settings

import com.dlsc.formsfx.model.structure.StringField
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleControl
import com.keevol.keenotes.desk.utils.FontStringConverter
import javafx.geometry.Insets
import javafx.scene.control.{Button, TextField}
import javafx.scene.layout.{HBox, Priority, StackPane}
import javafx.scene.text.Font
import org.controlsfx.dialog.FontSelectorDialog

/**
 * a custom simple control for font with foot chooser
 *
 * @author fq@keevol.com
 */
class SimpleFontControl extends SimpleControl[StringField, StackPane] {

  var textField: TextField = _
  var fontChooseButton: Button = _

  val fontStringConverter = new FontStringConverter()

  override def initializeParts(): Unit = {
    super.initializeParts()

    node = new StackPane()

    textField = new TextField()
    textField.setEditable(false)
    fontChooseButton = new Button("Choose Font")
    fontChooseButton.setOnAction(e => {
      val dialog = new FontSelectorDialog(Font.getDefault)
      val p = dialog.showAndWait()
      if (p.isPresent) {
        val font = p.get()
        println("font.toString: " + font.toString)
        textField.setText(fontStringConverter.toString(font))
        field.persist()
      }
    })

    val hbox = new HBox(10)
    hbox.setPadding(new Insets(3))
    hbox.getChildren.addAll(textField, fontChooseButton)
    HBox.setHgrow(textField, Priority.ALWAYS)

    node.getChildren.add(hbox)

  }

  override def layoutParts(): Unit = {

  }

  override def setupBindings(): Unit = {
    super.setupBindings()
    // without this, PreferencesFX will throw exception.
    if (field.valueProperty.get == "null" || field.valueProperty.get == null) field.valueProperty.set("")

    field.valueProperty().bindBidirectional(textField.textProperty())
  }
}