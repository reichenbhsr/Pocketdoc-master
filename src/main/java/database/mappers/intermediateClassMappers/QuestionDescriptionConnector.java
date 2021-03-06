package database.mappers.intermediateClassMappers;

import database.mappers.DatabaseConnector;
import models.intermediateClassModels.QuestionDescription;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Roman on 04.10.2015.
 */
public class QuestionDescriptionConnector extends DatabaseConnector{


    public int create(QuestionDescription questionDescription){

        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT id FROM question_descriptions WHERE id = " + questionDescription.getId() + ";";
            ResultSet res = stmt.executeQuery(SQL);

            if (res.next())
                System.out.println("Question description already exists!");
            else {

                stmt = connection.createStatement();
                SQL = "INSERT INTO question_descriptions (description, language, question) VALUES (" +
                        "" + (questionDescription.getDescription() == null ? null : "'" + questionDescription.getDescription() + "'") + "," +
                        "" + questionDescription.getLanguage().getId() + "," +
                        "" + questionDescription.getQuestion().getId() + ");";

                int rows = stmt.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
                int id = 0;
                if (rows > 0)
                {
                    ResultSet set = stmt.getGeneratedKeys();
                    if (set.next())
                        id = set.getInt(1);
                }
                questionDescription.setId(id);
                return id;
            }
        }
        catch (SQLException ex){
            System.out.println("SQL Error create Question description");
        }

        return -1;
    }

    public QuestionDescription update(QuestionDescription questionDescription){

        try {
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "UPDATE question_descriptions SET " +
                    "description=" + (questionDescription.getDescription() == null ? null : "'" + questionDescription.getDescription() + "'") + "," +
                    " language=" + questionDescription.getLanguage().getId() +","+
                    " question=" + questionDescription.getQuestion().getId() +" "+
                    " WHERE id =" + questionDescription.getId() + ";";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error update Question description");
        }

        return questionDescription;
    }

    public QuestionDescription read(int questionDescriptionId){

        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM question_descriptions WHERE id='"+ questionDescriptionId + "';";

            ResultSet set = stmt.executeQuery(SQL);

            if (set.next())
            {
                QuestionDescription questionDescription = new QuestionDescription();
                questionDescription.setId(set.getInt("id"));
                questionDescription.setDescription(set.getString("description"));
                questionDescription.setLanguageId(set.getInt("language"));
                questionDescription.setQuestionId(set.getInt("question"));

                return questionDescription;
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error read Question description");
        }

        return null;
    }

    public HashSet<QuestionDescription> readSetOfQuestion(int questionId){
        HashSet<QuestionDescription> set = new HashSet<QuestionDescription>();

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM question_descriptions WHERE question= " + questionId + " ;";

            ResultSet res = stmt.executeQuery(SQL);

            QuestionDescription description;
            while (res.next())
            {
                description = new QuestionDescription();
                description.setId(res.getInt("id"));
                description.setLanguageId(res.getInt("language"));
                description.setQuestionId(res.getInt("question"));
                description.setDescription(res.getString("description"));

                set.add(description);
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error read set of question descriptions");
        }

        return set;
    }

    public ArrayList<QuestionDescription> readAll(){

        ArrayList<QuestionDescription> questionDescriptions = new ArrayList<QuestionDescription>();

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM question_descriptions;";

            ResultSet set = stmt.executeQuery(SQL);

            QuestionDescription questionDescription;
            while (set.next())
            {
                questionDescription = new QuestionDescription();
                questionDescription.setId(set.getInt("id"));
                questionDescription.setDescription(set.getString("description"));
                questionDescription.setLanguageId(set.getInt("language"));
                questionDescription.setQuestionId(set.getInt("question"));

                questionDescriptions.add(questionDescription);
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error read all question descriptions");
        }

        return questionDescriptions;
    }

    public void delete(int questionDescriptionId){

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "DELETE FROM question_descriptions WHERE id ='" + questionDescriptionId + "';";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error delete Question description");
        }

    }

    public void deleteFromLanguage(int languageId){
        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "DELETE FROM question_descriptions WHERE language ='" + languageId + "';";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error delete Question description from language");
        }
    }

    public void deleteFromQuestion(int questionId){
        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "DELETE FROM question_descriptions WHERE question ='" + questionId + "';";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error delete Question description from question");
        }
    }

}
