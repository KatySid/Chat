package client;

import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8190;

    private boolean authenticated;
    private String nickname;

    private Stage stage;
    private Stage regStage;
    private RegController regController;
    private File historyFile;
    private FileWriter writeToFile;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(()->{
            stage = (Stage) textArea.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent windowEvent) {
                    System.out.println("bye");
                    if (socket!=null && !socket.isClosed()){
                        try {
                            out.writeUTF(Command.END);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });

        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());


            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.startsWith(Command.AUTH_OK)) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                createFileHistory();
                                returnHistory();
                                break;
                            }

                            if (str.equals(Command.END)) {
                                System.out.println("client disconnected");
                                throw new RuntimeException("server disconnected us");
                            }
                            if(str.startsWith(Command.REG_OK)){
                            regController.regOK();
                            }
                            if (str.startsWith(Command.REG_NO)){
                            regController.regNO();
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if(str.startsWith("/")){
                            if (str.equals(Command.END)) {
                                System.out.println("client disconnected");
                                break;
                            }
                            if (str.startsWith(Command.CLIENT_LIST)){
                                String[] tokens = str.split("\\s");
                                Platform.runLater(()-> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientList.getItems().add(tokens[i]);
                                    }
                                });

                            }
                            if (str.startsWith(Command.CH_TITLE)){
                                String[] token = str.split("\\s");
                                nickname = token[1];
                                writeToFile.close();
                                createFileHistory();
                                setTitle(nickname);
                             }
                        }
                        else {
                            textArea.appendText(str + "\n");
                            writeToFile.write(str+"\r\n");
                            writeToFile.flush();
                        }

                    }
                }catch (RuntimeException e){
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthenticated(false);
                    try {
                        socket.close();
                        writeToFile.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMsg(ActionEvent actionEvent) {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String msg = String.format("%s %s %s",Command.AUTH, loginField.getText().trim(), passwordField.getText().trim());

        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        if (nickname.equals("")) {
            Platform.runLater(()->{
                stage.setTitle("GeekChat");
            });
        } else {
            Platform.runLater(()->{
            stage.setTitle(String.format("GeekChat [ %s ]", nickname));
            });
        }
    }

    public void clientListClicked(MouseEvent mouseEvent) {
        String reciever = clientList.getSelectionModel().getSelectedItem();
        textField.setText(String.format("%s %s ", Command.PRV_MSG, reciever));
    }

    public void registration(ActionEvent actionEvent) {
    if(regStage == null){
        createRegStage();
    }
    regStage.show();
    }

    private void createRegStage() {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("GeekChat registration");
            regStage.setScene(new Scene(root, 400, 350));
            regController = fxmlLoader.getController();
            regController.setController(this);
            regStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
     public void tryToReg(String login, String password, String nickname){
         if (socket == null || socket.isClosed()) {
             connect();
         }
        String msg = String.format("%s %s %s %s", Command.REG, login, password, nickname);
         try {
             out.writeUTF(msg);
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
    private void createFileHistory(){
        File folder = new File("history_clients");
        System.out.println(folder.getName());
        if(!folder.exists()){
        folder.mkdir();
        }
        historyFile = new File(String.format("%s/history_%s.txt", folder.getName(), this.nickname));
        try {
            if (!historyFile.exists()) {
                    historyFile.createNewFile();
                }
                writeToFile = new FileWriter( historyFile, true);

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void returnHistory(){
        try{
            List<String> hist = Files.readAllLines( historyFile.getAbsoluteFile().toPath());
            if(hist.size()<=100 && hist.size()>0){
               for (int i = 0; i < hist.size(); i++) {
                    textArea.appendText(hist.get(i)+"\n");
                }
              }
            if (hist.size()>100) {
                for (int i = hist.size()-100; i < hist.size(); i++) {
                    textArea.appendText(hist.get(i)+"\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
