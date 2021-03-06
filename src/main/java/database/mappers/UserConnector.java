package database.mappers;

import models.User;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;

/**
 * Created by Roman on 02.10.2015.
 */
public class UserConnector extends DatabaseConnector {

    public int createUser(User user)
    {

        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            java.util.Date now = new java.util.Date();
            String SQL = "SELECT id FROM Users where id = " + user.getId() + ";";
            ResultSet res = stmt.executeQuery(SQL);

            if (res.next())
                System.out.println("User already exists!");
            else{
                // Create User
                stmt = connection.createStatement();
                SQL = "INSERT INTO Users (name, password, email, gender, age_category, is_admin, lang, last_activity, history) VALUES (" +
                        (user.getName() == null ? null : "'" + user.getName() + "'") + "," +
                        (user.getPassword() == null ? null : "'" + user.getPassword() + "'") + "," +
                        (user.getEmail() == null ? null : "'" + user.getPassword() + "'") + "," +
                        "'" + user.getGender() + "'" + "," +
                        "'" + user.getAgeCategory() + "'" + "," +
                        "'" + false + "'" + "," +
                        "'" + user.getLanguage().getId() + "'" + "," +
                        "'" + new Timestamp(now.getTime()) + "'" + "," +
                        "'" + user.getHistory().getId() + "');";

                int rows = stmt.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
                int id = 0;
                if (rows > 0)
                {
                    ResultSet set = stmt.getGeneratedKeys();
                    if (set.next())
                        id = set.getInt(1);
                }
                user.setId(id);
                return id;
            }
        }
        catch (SQLException ex){
            System.out.println("SQL Error createUser");
        }

        return -1;
    }

    public User updateUser(User user)
    {
        try {
            establishConnection();

            Statement stmt = connection.createStatement();
            java.util.Date now = new java.util.Date();
            String SQL = "UPDATE Users SET " +
                            " name=" + (user.getName() == null ? null : "'" + user.getName() + "'") + "," +
                            ((user.getPassword() == null || user.getPassword().equals("")) ? "" : " password=" + "'" + user.getPassword() + "'" +",") +
                            " email=" + "'" + user.getEmail() + "'" +","+
                            " gender=" + "'" + user.getGender() + "'" +","+
                            " age_category=" + "'" + user.getAgeCategory() + "'" +","+
                            " is_temporary=" + "'" + user.isTemporary() + "'" +","+
                            " last_activity=" + "'" + new Timestamp(now.getTime()) + "'" +","+
                            " lang=" + "'" + user.getLanguage().getId() + "'" +","+
                            " history='" + user.getHistory().getId() +"' "+
                            " WHERE id =" + user.getId() + ";";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error updateUser");
        }

        return user;
    }

    public void updateLastActivity(int userId)
    {
        try {
            establishConnection();

            Statement stmt = connection.createStatement();
            java.util.Date now = new java.util.Date();

            String SQL = "UPDATE Users SET " +
                    " last_activity=" + "'" + new Date(now.getTime()) + "'" +
                    " WHERE id =" + userId + ";";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error update last activity user");
        }
    }

    public User getUser(int userId)
    {
        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM Users WHERE id='"+ userId + "';";

            ResultSet set = stmt.executeQuery(SQL);

            if (set.next())
            {
                User user = new User();
                user.setId(set.getInt("id"));
                user.setName(set.getString("name"));
                user.setPassword(set.getString("password"));
                user.setEmail(set.getString("email"));
                user.setGender(set.getInt("gender"));
                user.setAgeCategory(set.getInt("age_category"));
                user.setHistoryId(set.getInt("history"));
                user.setIsAdmin(set.getBoolean("is_admin"));
                user.setTemporary(set.getBoolean("is_temporary"));
                user.setLanguageId(set.getInt("lang"));

                return user;
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error getUser");
        }

        return null;
    }

    public ArrayList<User>  readAll()
    {

        ArrayList<User> users = new ArrayList<User>();

        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM Users;";

            ResultSet set = stmt.executeQuery(SQL);

            User user;
            while (set.next())
            {
                user = new User();
                user.setId(set.getInt("id"));
                user.setName(set.getString("name"));
                user.setPassword(set.getString("password"));
                user.setEmail(set.getString("email"));
                user.setGender(set.getInt("gender"));
                user.setAgeCategory(set.getInt("age_category"));
                user.setHistoryId(set.getInt("history"));
                user.setIsAdmin(set.getBoolean("is_admin"));
                user.setTemporary(set.getBoolean("is_temporary"));
                user.setLanguageId(set.getInt("lang"));

                users.add(user);
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error readAll User");
        }

        return users;
    }

    public boolean hasMailAdress(String address){

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT name FROM users WHERE email='" + address + "';";

            ResultSet set = stmt.executeQuery(SQL);

            if(set.next())
                return true;

        }
        catch (SQLException ex){
            System.out.println("SQL Error has mailaddress");
        }

        return false;
    }

    public boolean checkPassword(String address, String password)
    {
        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT name FROM users WHERE email='" + address + "' AND " +
                         "password = '" + password  + "';";

            ResultSet set = stmt.executeQuery(SQL);

            if(set.next())
                return true;

        }
        catch (SQLException ex){
            System.out.println("SQL Error has mailaddress");
        }

        return false;
    }

    public User addPasswordRestoreToken(String address)
    {
        SecureRandom random = new SecureRandom();
        String token = new BigInteger(130, random).toString(32);
        User user = null;

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "UPDATE users SET password_restore_token = '" + token + "' WHERE email = '" + address + "';";

            int rows = stmt.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);

            if (rows > 0)
            {
                SQL = "SELECT * FROM users WHERE email = '" + address +"';";
                stmt = connection.createStatement();

                ResultSet set = stmt.executeQuery(SQL);

                if (set.next())
                {
                    user = new User();
                    user.setId(set.getInt("id"));
                    user.setName(set.getString("name"));
                    user.setEmail(set.getString("email"));
                    user.setLanguageId(set.getInt("lang"));
                    user.setPassword(set.getString("password"));
                    user.setPasswordRestoreToken(token);
                }

                set.close();

            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error add password restore token");
        }

        return user;
    }

    public void delete(int userId)
    {
        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "DELETE FROM Users WHERE id ='" + userId + "';";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error delete User");
        }
    }

    public void deleteTemporaryUsers()
    {
        try{

            establishConnection();
            java.util.Date border = new java.util.Date(new java.util.Date().getTime() - 1000 * 60 * 30);

            Statement stmt = connection.createStatement();
            String SQL = "DELETE FROM users WHERE is_temporary AND last_activity <  '" + border + "';";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error delete temporary users");
        }
    }

}
