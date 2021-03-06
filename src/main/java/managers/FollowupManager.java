package managers;

import database.mappers.FollowupConnector;
import database.mappers.UserConnector;
import database.mappers.intermediateClassMappers.AnswerToHistoryConnector;
import models.Answer;
import models.Followup;
import models.Question;

import java.util.ArrayList;

/**
 * Diese Klasse dient als Mittelstück der Applikation wenn es um Objekte der Klasse Followup geht.
 * <p>
 * Wenn ein solches Objekt verändert, gelesen oder etwas damit gemacht werden soll. Dann muss diese Klasse dafür aufgerufen werden.
 * <p>
 * Für mehr Informationen, siehe die Javadoc von {@link managers.BasicManager}
 *
 * @author Roman Eichenberger
 */
public class FollowupManager implements BasicManager<Followup>{

    private FollowupConnector followupConnector;
    private AnswerToHistoryConnector answerToHistoryConnector;
    private UserConnector userConnector;

    /**
     * Dieser Konstruktor soll offiziell gebraucht werden.
     */
    public FollowupManager(){

        followupConnector = new FollowupConnector();
        userConnector = new UserConnector();
        answerToHistoryConnector = new AnswerToHistoryConnector();
    }

    public int createFollowup(Followup followup){
        int id = followupConnector.create(followup);
        followup.setId(id);
        answerToHistoryConnector.storeCurrentRunToFollowup(followup);

        return id;
    }

    @Override
    public Followup add(){
        return null;
    }

    @Override
    public Followup update(Followup followup) {

        return null;
    }

    @Override
    public Followup get(int id) {
        return followupConnector.read(id);
    }

    @Override
    public ArrayList<Followup> getAll() {
        return null;
    }

    @Override
    public void remove(int id) {
        followupConnector.delete(id);
        answerToHistoryConnector.deleteFromFollowup(id);
    }

    public ArrayList<Followup> getFollowupsOfUser(int userId){

        return followupConnector.readFromUser(userId);
    }

    public ArrayList<Question> getYesAnsweredQuestionOfFollowup(Followup followup){
        return answerToHistoryConnector.getYesAnsweredQuestionsFromFollowup(followup);
    }

    public ArrayList<Answer> getInformationQuestionsOfFollowup(Followup followup){
        return answerToHistoryConnector.getInformationAnswersFromFollowup(followup);
    }

}
