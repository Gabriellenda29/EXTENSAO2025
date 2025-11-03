package com.safemath.game;

import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

public class QuestionDialog {
    public static Boolean askQuestion(Window owner, Question q, String tipo) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Pergunta");
        dialog.setHeaderText("[" + tipo + "] " + q.text);
        dialog.initOwner(owner);

        Optional<String> result = dialog.showAndWait();
        if(result.isPresent()) {
            try {
                int answer = Integer.parseInt(result.get());
                return answer == q.answer;
            } catch(NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
