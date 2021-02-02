package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegController {

    public PasswordField passwordField;
    public Button cancelBtn;
    private Controller controller;
    private Stage regStage;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @FXML
    private TextField loginField;
    @FXML
    private TextField nicknameFild;
    @FXML
    private TextArea textArea;

    @FXML
    public void tryToReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameFild.getText().trim();
        controller.tryToReg(login, password, nickname);
    }
    public void regOK(){
        textArea.appendText("Регистрация прошла успешно\n");
        loginField.clear();
        passwordField.clear();
        nicknameFild.clear();
    }
    public void regNO(){
        textArea.appendText("Не удалось зарегистрироваться\n");
        loginField.clear();
        passwordField.clear();
        nicknameFild.clear();
    }
    @FXML
    public void closedWindow(ActionEvent actionEvent) {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
