package database.mappers;

import models.Diagnosis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by Roman on 04.10.2015.
 */
public class DiagnosisConnector extends DatabaseConnector {

    public int create(Diagnosis diagnosis){

        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT id FROM diagnoses WHERE id = " + diagnosis.getId() + ";";
            ResultSet res = stmt.executeQuery(SQL);

            if (res.next())
                System.out.println("Diagnosis already exists!");
            else {

                stmt = connection.createStatement();
                SQL = "INSERT INTO diagnoses (name) VALUES (" +
                        (diagnosis.getName() == null ? null : "'" + diagnosis.getName() + "'") + ");";

                int rows = stmt.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
                int id = 0;
                if (rows > 0)
                {
                    ResultSet set = stmt.getGeneratedKeys();
                    if (set.next())
                        id = set.getInt(1);
                }
                diagnosis.setId(id);
                return id;
            }
        }
        catch (SQLException ex){
            System.out.println("SQL Error create Diagnosis");
        }

        return -1;
    }

    public Diagnosis update(Diagnosis diagnosis){

        try {
            establishConnection();

            Statement stmt = connection.createStatement();

            String SQL = "UPDATE diagnoses SET " +
                    "name=" + (diagnosis.getName() == null ? null : "'" + diagnosis.getName() + "'") + "" +
                    " WHERE id =" + diagnosis.getId() + ";";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error update Diagnosis");
        }

        return diagnosis;
    }

    public Diagnosis read(int diagnosisId){

        try{
            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM diagnoses WHERE id='"+ diagnosisId + "';";

            ResultSet set = stmt.executeQuery(SQL);

            if (set.next())
            {
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setId(set.getInt("id"));
                diagnosis.setName(set.getString("name"));

                return diagnosis;
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error read Diagnosis");
        }

        return null;
    }

    public ArrayList<Diagnosis> readAll(){

        ArrayList<Diagnosis> diagnoses = new ArrayList<Diagnosis>();

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "SELECT * FROM diagnoses ORDER BY name;";

            ResultSet set = stmt.executeQuery(SQL);

            Diagnosis diagnosis;
            while (set.next())
            {
                diagnosis = new Diagnosis();
                diagnosis.setId(set.getInt("id"));
                diagnosis.setName(set.getString("name"));

                diagnoses.add(diagnosis);
            }

        }
        catch (SQLException ex){
            System.out.println("SQL Error read all diagnoses");
        }

        return diagnoses;
    }

    public void delete(int diagnosisId){

        try{

            establishConnection();

            Statement stmt = connection.createStatement();
            String SQL = "DELETE FROM diagnoses WHERE id ='" + diagnosisId + "';";

            stmt.execute(SQL);
        }
        catch (SQLException ex){
            System.out.println("SQL Error delete diagnosis");
        }

    }

}
