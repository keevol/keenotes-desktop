package com.keevol.keenotes.desk

import com.keevol.javafx.controls.Card
import com.keevol.javafx.utils.Icons
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.{HBox, StackPane, VBox}

class NoteList(app: KeeNotesFXApplication) extends StackPane {
  val topbar: HBox = setupTopBar()
  val listPane: VBox = setupNoteList()
  val bottomBar: HBox = setupBottomBar()

  val mainLayout = new VBox(10)
  mainLayout.getChildren.addAll(topbar, listPane, bottomBar)
  mainLayout.setPadding(new Insets(10))

  getChildren.add(mainLayout)


  def setupTopBar(): HBox = {
    val addNewNoteIcon = Icons.from(Icons.PLUS_LIT)
    addNewNoteIcon.setOnMouseEntered(e => {
      e.getSource.asInstanceOf[Node].setScaleX(1.5)
      e.getSource.asInstanceOf[Node].setScaleY(1.5)
    })
    addNewNoteIcon.setOnMouseExited(e => {
      e.getSource.asInstanceOf[Node].setScaleX(1)
      e.getSource.asInstanceOf[Node].setScaleY(1)
    })
    addNewNoteIcon.setOnMouseClicked(e => {
      app.primaryStage.setScene(app.noteTakingScene)
    })


    val top = new HBox(10)
    top.getChildren.add(addNewNoteIcon)
    top
  }

  def setupBottomBar(): HBox = {

    val bottom = new HBox(10)
    bottom.getChildren.add(new Label("KEEVOL Consulting"))
    bottom
  }

  def setupNoteList(): VBox = {


    val noteList = new VBox(10)
    val card = new Card("Demo", content = "Nothing important")
    card.setStyle("-fx-border-color: blue; -fx-border-width: 1; ")

    noteList.getChildren.add(card)

    noteList
  }
}