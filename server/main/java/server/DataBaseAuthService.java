package server;

import java.sql.*;

public class DataBaseAuthService implements AuthService{
    private static Connection connection;
    private static Statement stmt;
   // private static PreparedStatement psInsert;

    public static boolean connect() throws ClassNotFoundException, SQLException {
        try{
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:mainChat.db");
        stmt = connection.createStatement();
        return true;
    } catch (Exception e){
        return false;
        }
    }
    public static void desconnect(){
        try {
            stmt.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


       @Override
        public String getNicknameByLoginAndPassword(String login , String password) {
            String nickname = null;
            try {
                ResultSet rs = stmt.executeQuery("SELECT * FROM clients WHERE (login=" + "'" + login + "') AND (password="+ "'"+password + "')");
                while (rs.next()) {
                    nickname = rs.getString("nickname");
                }
                rs.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return nickname;
        }

        @Override
        public boolean registration(String login, String password, String nickname) {


            try {
                ResultSet rs = stmt.executeQuery("SELECT * FROM clients WHERE (login='" + login + "') OR (nickname='" + nickname + "')");
                String nickN=null;
                while (rs.next()) {
                     nickN = rs.getString("nickname");
                }
                rs.close();
                System.out.println("nickname "+nickN+ " существует");
                if (nickN == null) {
                    stmt.executeUpdate("INSERT INTO clients (login,password,nickname) VALUES ('" + login + "', '" + password + "', '" + nickname + "')");

                    return true;
                } else {
                    return false;
                }

            } catch (SQLException throwables) {
                throwables.printStackTrace();
                return false;
            }
        }

    @Override
    public boolean changeNickname(String nickname, String newNickname) {
        try {
            ResultSet rsNickname = stmt.executeQuery("SELECT * FROM clients WHERE nickname='" + newNickname + "'");
            String otherNickname = null;
            while (rsNickname.next()) {
                otherNickname = rsNickname.getString("nickname");
            }
            rsNickname.close();
            if (otherNickname!= null) {
                return false;
            }else {
                stmt.executeUpdate("UPDATE clients SET nickname='" + newNickname + "' WHERE nickname='" + nickname + "'");
                return true;
            }
        }catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }
}


