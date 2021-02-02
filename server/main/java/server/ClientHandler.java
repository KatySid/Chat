package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals(Command.END)) {
                            sendMsg(Command.END);
                            System.out.println("client disconnected");
                            break;
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
                } catch (RuntimeException e){
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
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
