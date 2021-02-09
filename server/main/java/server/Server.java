package server;

import commands.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8190;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() throws SQLException, ClassNotFoundException {
        clients = new CopyOnWriteArrayList<>();

        if(!DataBaseAuthService.connect()){
            throw new RuntimeException("не удалось подключиться к БД");
        }
            authService = new DataBaseAuthService();
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started");

            while (true) {
                socket = server.accept();
                System.out.println("Client connected");

                ClientHandler current = new ClientHandler(this, socket);
            }

        
    } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DataBaseAuthService.desconnect();
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler clientHandler, String msg){
        String message = String.format("[ %s ]: %s", clientHandler.getNickname(), msg);
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }
    void subscribe(ClientHandler clientHandler){

        clients.add(clientHandler);
        broadcastClientList();
    }

    void unsubscribe(ClientHandler clientHandler){

        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void sendPrivateMsg(ClientHandler clientHandler, String addressMessage, String privateMsg) {
        for (ClientHandler c : clients) {
            if (c.getNickname().equals(addressMessage)) {
                String msg = String.format("[%s] to [%s] : %s", clientHandler.getNickname(), c.getNickname(), privateMsg);
                c.sendMsg(msg);
                clientHandler.sendMsg(msg);
                return;
            }
        }
        clientHandler.sendMsg(String.format("Client [%s] not found", addressMessage));
    }

    public boolean isLoginAuthenticated (String login){
        for (ClientHandler c: clients) {
            if(c.getLogin().equals(login)){
                return true;
            }
        }
        return false;

    }

    public void broadcastClientList(){
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);
        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }
        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }

    }

}
