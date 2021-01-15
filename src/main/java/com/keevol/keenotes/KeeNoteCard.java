package com.keevol.keenotes;

import com.keevol.javafx.utils.Icons;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class KeeNoteCard extends StackPane {

    @FXML
    public Label title;

    @FXML
    public Label content;

    @FXML
    public Button shareBtn;

    public KeeNoteCard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/keenote.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        shareBtn.setGraphic(Icons.from("fa-share-square-o:11:aqua"));
        shareBtn.setStyle("-fx-background-color: #464646;"); // make it transparent
        shareBtn.setTooltip(new Tooltip("Share this note."));

        content.setOnMouseClicked(event -> {
            if(event.getClickCount() > 1) {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(content.getText());
                Clipboard.getSystemClipboard().setContent(cc);
            }
        });
    }
}
