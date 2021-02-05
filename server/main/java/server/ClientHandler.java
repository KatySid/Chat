package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketPermission;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client " + nickname + " connected " + socket.getRemoteSocketAddress());
                                    socket.setSoTimeout(0);
                                    //Здесь должна быть отправка истории 100 последних сообщений.
                                    //sendMsg(getMsgForNick (nickname);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже авторизовались ");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            throw new RuntimeException("client disconnected");

                        }
                        if (str.startsWith(Command.REG)) {
                            String[] tokens = str.split("\\s");
                            if (tokens.length < 4) {
                                continue;
                            }
                            boolean isRegistered = server.getAuthService().registration(tokens[1], tokens[2], tokens[3]);
                            if (isRegistered) {
                                sendMsg(Command.REG_OK);
                            } else {
                                sendMsg(Command.REG_NO);
                            }
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            break;
                        }
                        if(str.startsWith(Command.CH_NICK)){
                            String clientNick;
                            String[] token = str.split("\\s+", 2);
                            if(token.length!=2 && token[1].contains(" ")){
                                sendMsg("Ник не должен содержать пробелы");
                                continue;
                            }
                            if (server.getAuthService().changeNickname(this.nickname, token[1])){
                                sendMsg("Ваш ник изменен на "+ token[1]);
                                sendMsg(Command.CH_TITLE +" "+ token[1]);
                                this.nickname = token[1];
                                server.broadcastClientList();
                            }else{
                                sendMsg("Ник "+ token[1]+ " существует");
                            }
                        }
                        if (str.startsWith(Command.PRV_MSG)) {
                            String addressMessage;
                            String privateMsg;
                            String[] msgArray = str.split("\\s", 3);
                            addressMessage = msgArray[1];
                            privateMsg = msgArray[2];
                            server.sendPrivateMsg(this, addressMessage, privateMsg);
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                }catch (SocketTimeoutException e){
                    sendMsg(Command.END);
                } catch (RuntimeException e){
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("client disconnected");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
