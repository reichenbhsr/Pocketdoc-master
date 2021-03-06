package managers;

import calculators.QuestionCalculator;
import models.*;

import java.util.Set;
import java.util.TreeMap;

/**
 * Diese Klasse dient als Mittelstück der Applikation wenn es um laufende Fragdurchläufe geht.
 * <p>
 * Dieser Manager ist anders als andere Manager. Es muss mehrere sachen Behandeln.
 *
 * @author Oliver Frischknecht
 */
public class RunManager {

    private QuestionManager questionManager;
    private DiagnosisManager diagnosisManager;
    private ActionSuggestionManager actionSuggestionManager;
    private HistoryManager historyManager;
    private UserManager userManager;
    private AnswerManager answerManager;
    private User user;

    /**
     * Dieser Manager muss ein User haben um zu funktionieren, da alle Methoden auf diesen Zugreifen müssen.
     *
     * @param user der Benutzer der die Fragen beantwortet. Diesem User ist die History referenziert.
     */
    public RunManager(User user) {
        userManager = new UserManager();
        answerManager = new AnswerManager();
        this.user = userManager.get(user.getId());
        questionManager = new QuestionManager();
        diagnosisManager = new DiagnosisManager();
        actionSuggestionManager = new ActionSuggestionManager();
        historyManager = new HistoryManager();
    }


    public void startFollowup(Followup followup){
        questionManager.startFollowup(followup);
    }

    /**
     * Dieser Konstruktor soll offiziell gebraucht werden.
     */
    public Question getNextQuestion() {
        return questionManager.next(user);
    }

    /**
     * Diese Methode wird aufgerufen um eine gegebenene Antwort dem History abzuspeichern.
     *
     * @param answer Die vom User beantwortete Antwort
     */
    public void addAnswer(Answer answer, Question question) {
        if (answer.getId() > -1)
        {
            answer = answerManager.get(answer.getId());
        }

        final History history = historyManager.getAndFetch(user.getHistory());
        question = questionManager.get(question.getId());

        boolean answerChanged = historyManager.changeGivenAnswer(question, user.getHistory());
        if (answerChanged)
            questionManager.recalculate(userManager.get(user.getId()));

        history.setLastAnswer(answer);
        historyManager.addAnswerToHistory(answer, history, question);
        historyManager.update(history);

        QuestionCalculator calc = new QuestionCalculator();
        calc.addAnswer(question, answer);
    }

    /**
     * Diese Methode wird aufgerufen um das Resultat des Fragendurchlauf in Form einer Diagnose zu holen.
     *
     * @return Die Diagnose
     */
    public Diagnosis getDiagnosis() {
        return diagnosisManager.diagnose(user);
    }

    /**
     * Diese Methode wird aufgerufen um das Resultat des Fragendurchlauf in Form einer Rangliste von Handlungsempfehlungen zu holen.
     *
     * @return Die Handlungsempfehlungen
     */
    public TreeMap<ActionSuggestion, Integer> getActionSuggestion() {
        return actionSuggestionManager.calculateActionSuggestions(user);
    }

    /**
     * Diese Methode wird aufgerufen um das Resultat des Fragendurchlauf in Form einer Liste von Handlungsempfehlungen zu holen.
     *
     * @return Die Handlungsempfehlungen
     */
    public TreeMap<Diagnosis, Integer> getDiagnosisRankingList() {
        return diagnosisManager.getDiagnosesRankingList(user);
    }


    /**
     * Diese Methode wird aufgerufen um ein Fragendurchlauf zu reseten.
     * <p>
     * Dabei müssen alle Antworten gelöscht werden, sowie verschiedene Eigenschaften eines Fragendurchlaufes.
     */
    public void deleteHistory() {
        final History history = historyManager.getAndFetch(user.getHistory());
        history.getAnswers().clear();
        history.setConsecutiveQuestions(0);
        history.setLastAnswer(null);
        history.setLastAnswerId(0);

        userManager.update(user);
        historyManager.clearAnswersForHistory(history);
        historyManager.update(history);
    }
}
