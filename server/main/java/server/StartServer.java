package server;

import java.sql.SQLException;

public class StartServer {
    public static void main(String[] args) {

        try {
            new Server();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
