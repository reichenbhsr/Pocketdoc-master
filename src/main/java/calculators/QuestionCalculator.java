package calculators;

import managers.*;
import models.*;
import models.intermediateClassModels.AnswerToDiagnosisScoreDistribution;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Klasse dient zur Berechnung der nächste sinnvollste Frage.
 * <p>
 * Es wird auch überprüft ob die Fragerunde fertig ist laut Einstellungen.
 *
 * @author Nathan Bourquin
 */
public class QuestionCalculator {

    private final DiagnosisCalculator diagnosisCalculator;
    private QuestionManager questionManager;
    private HistoryManager historyManager;
    private Diagnosis topDiagnosis;
    private SettingManager settingManager;
    private FollowupManager followupManager;
    private static ArrayList<Question> informationQuestions;
    private static ArrayList<Question> remainingQuestions;
    private static Stack<Question> remainingForcedQuestions;
    private static ArrayList<Answer> followupInformationAnswers;
    private static Queue<Question> followupQuestions;
    private static Queue<Question> answeredFollowupQuestions;
    private static Followup currentFollowup;

    /**
     * Dieser Konstruktor soll offiziell gebraucht werden.
     */
    public QuestionCalculator() {
        this.questionManager = new QuestionManager();
        diagnosisCalculator = new DiagnosisCalculator();
        historyManager = new HistoryManager();
        settingManager = new SettingManager();
        followupManager = new FollowupManager();
    }

    /**
     * Dieser Konstruktor wird zum Testen gebraucht.
     * <p>
     * Damit können die Manager gefaked werden, somit wird nicht wirklich auf die Datenbank zugegriffen
     *
     * @param questionManager Ein QuestionManager oder eine Ableitung davon.
     * @param historyManager Ein HistoryManager oder eine Ableitung davon.
     * @param answerManager Ein AnswerManager oder eine Ableitung davon.
     * @param settingManager Ein SettingManager oder eine Ableitung davon.
     * @param diagnosisManager Ein DiagnosisManager oder eine Ableitung davon.
     */
    public QuestionCalculator(QuestionManager questionManager, HistoryManager historyManager, AnswerManager answerManager, SettingManager settingManager, DiagnosisManager diagnosisManager) {
        this.questionManager = questionManager;
        this.historyManager = historyManager;
        this.settingManager = settingManager;
        diagnosisCalculator = new DiagnosisCalculator(historyManager, answerManager, diagnosisManager);
    }

    /**
     * Initialisiert ein Followup, um dieses durchzuführen
     *
     * @param followup Followup, welches durchgeführt werden soll.
     */
    public void initFollowup(Followup followup){

        followupQuestions = new LinkedList<Question>();
        answeredFollowupQuestions = new LinkedList<Question>();
        currentFollowup = followupManager.get(followup.getId());

        ArrayList<Question> questions = followupManager.getYesAnsweredQuestionOfFollowup(followup);
        followupInformationAnswers = followupManager.getInformationQuestionsOfFollowup(followup);

        for(Question q: questions)
            followupQuestions.add(q);

    }

    /**
     * Diese Methode wird gebraucht um die nächste sinnvollste Frage zu berechnen
     * <p>
     * 1. Zuerst wird überprüft ob die letzte gestellte Frage eine abhängige Frage hat. Diese muss dann unbedingt als nächstes gefragt werden
     * <p>
     * 2. Dann werden alle Fragen geholt und davon werden die schon gestellten Fragen entfernt, sowie ihre die Fragen die davon Abhängig sind.
     * <p>
     * 3. Es wird mit der in Punkt 2. erstellte Liste mehrere Ranglisten erstellt. Dann wird geschaut welche Rangliste die grösste Auswirkung hat. Dise Frage wird ist die beste Frage.
     * <p>
     * 4. Es wird überprüft ob diese beste Frage abhängigkeiten hat. Die höchste Abhängigkeit wird als neue beste Frage genommen
     * <p>
     * 5. Die Settings werden überprüft. (Ist eine Diagnose schon lange an erster Stelle?)
     * <p>
     * 6. die beste Frage wird zurückgegeben
     *
     * @param user der Benutzer der die Fragen beantwortet. Diesem User ist die History referenziert.
     * @return Die bestemögliche Frage, oder null falls die alle Fragen schon gestellt wurden oder die Diagnosensuche fertig ist
     */
    public Question getNextQuestion(User user) {
        History history = historyManager.getAndFetch(user.getHistory());
        Question bestQuestion;

        if (remainingQuestions == null){
            informationQuestions = getInformaticQuestions(); // RE
            remainingQuestions = getQuestionsToAsk(); // RE
            remainingForcedQuestions = new Stack<Question>();
            answerTypedQuestions(user);
            if (followupInformationAnswers != null && followupInformationAnswers.size() > 0)
                answerInformationsForFollowup(user);
        }

        topDiagnosis = diagnosisCalculator.getDiagnosis(); // RE

        if (followupQuestions == null){
            // Gibts abhängige Fragen, die als nächsts gestellt werden müssen?
            if (user.getHistory().getLastAnswer() != null){
                Question forcedQuestion = checkForceDependentQuestion(user.getHistory().getLastAnswer());

                if (forcedQuestion != null)
                    return forcedQuestion;
            }


            /*
            Beste Frage herausfinden
             */
            bestQuestion = getBestQuestion(remainingQuestions, user);

            if (bestQuestion == null) {
                bestQuestion = remainingQuestions.get(0);
            }

            /*
            Ist die beste Frage abhängig? (Wenn ja soll diese gestellt werden)
             */
            bestQuestion = getQuestionDependence(bestQuestion);

            /*
            Settings überprüfen
             */
            if (checkSettings(user, history)) {
                return null;
            }
        }
        else if (followupQuestions.size() == 0){

            // Followup Fragen gestellt, Diagnose prüfen

            followupQuestions = null;
            if (topDiagnosis.equals(currentFollowup.getDiagnosis()))
                bestQuestion = null; // Diagnose ist gleich, wieder stellen
            else
                bestQuestion = getNextQuestion(user); // Diagnose Unterschiedlich, mit normalem Durchlauf fortsetzen.

        }
        else{
            // Followup wird durchgeführt
            do{
                bestQuestion = followupQuestions.poll();
                if (bestQuestion != null)
                    answeredFollowupQuestions.add(bestQuestion);
            }while(bestQuestion != null && !remainingQuestions.contains(bestQuestion));

            if (bestQuestion == null)
                bestQuestion = getNextQuestion(user);
        }

        return bestQuestion;
    }

    /**
     * Diese Methode berechnet die restlichen Fragen und Scores neu, wenn bspw. eine bereits beantwortete Frage neu beantwortet wird.
     */
    public void recalculate(User user){

        Set<Answer> answers = user.getHistory().getAnswers();

        informationQuestions = getInformaticQuestions(); // RE
        remainingQuestions = getQuestionsToAsk(); // RE
        remainingForcedQuestions = new Stack<Question>();

        if (followupQuestions != null){
            for(Question question: followupQuestions)
                answeredFollowupQuestions.add(question);
        }

        if (answeredFollowupQuestions != null)
        {
            followupQuestions = answeredFollowupQuestions;
            answeredFollowupQuestions = new LinkedList<Question>();
        }

        DiagnosisCalculator.reset();

        for(Answer answer: answers){
            diagnosisCalculator.addAnswerToRankingList(answer);
            if (informationQuestions.size() > 0)
                informationQuestions = cleanOutDependentQuestions(informationQuestions, answer);

            remainingQuestions = cleanOutDependentQuestions(remainingQuestions, answer);

            if (followupQuestions != null){
                if (followupQuestions.remove(answer.getAnswerOf()))
                    answeredFollowupQuestions.add(answer.getAnswerOf());
            }
        }

        if (followupQuestions != null)
            followupQuestions.remove();

    }

    /**
     * Beantwortet die aktuelle Frage, um deren Scores hinzuzufügen und Vorbereitungen für die nächste Frage zu treffen.
     * @param question Beantwortete Frage
     * @param answer Gegebene Antwort
     */
    public void addAnswer(Question question, Answer answer){

        if (answer.getId() > -1){
            diagnosisCalculator.addAnswerToRankingList(answer);

            if (informationQuestions.size() > 0)
                informationQuestions = cleanOutDependentQuestions(informationQuestions, answer);

            remainingQuestions = cleanOutDependentQuestions(remainingQuestions, answer);
        }
        else {

            if (informationQuestions.size() > 0)
                informationQuestions = cleanOutDependentQuestions(informationQuestions, question);

            remainingQuestions = cleanOutDependentQuestions(remainingQuestions, question);

        }

    }

    /**
     * Diese Methode wird gebraucht um die Settings zu überprüfen
     * <p>
     * Im moment werden zwei Sachen überprüft:<br>
     * 1. Hat die erste Diagnose auf der Rangliste einen bestimmten Abstand auf der zweiten Diagnose?<br>
     * 2. Ist diese Diagnose eine bestimmte Zeit lang diesen Abstand?
     * <p>
     * Wenn diese Punkte zutreffen dann wurde eine Diagnose gefunden
     *
     * @param user    der Benutzer der die Fragen beantwortet. Diesem User ist die History referenziert.
     * @param history Das History (schon gefetcht)
     * @return true falls eine Diagnose gefunden wurde, sonst false.
     */
    private boolean checkSettings(User user, History history) {

        /*
        Aktuelle Diagnosen Rangliste holen
         */
        final TreeMap<Diagnosis, Integer> diagnosisRankingList = diagnosisCalculator.getSortedRankingList();

        if (diagnosisRankingList.size() > 0) {
            final int minDifference = Integer.parseInt(settingManager.get(SettingManager.MIN_DIFFERENCE).getValue());
            final Iterator<Map.Entry<Diagnosis, Integer>> iterator = diagnosisRankingList.entrySet().iterator();
            int firstScore = iterator.next().getValue();
            int secondScore = 0;

            /*
            Wenn die Rangliste mehrere einträge haben. Sonst wird 0 als secondscore genommen
             */
            if (iterator.hasNext()) {
                secondScore = iterator.next().getValue();
            }

            /*
            Ist der Mindestabstand erreicht?
             */
            if (firstScore - secondScore >= minDifference) {
                final Integer actualConsecutiveQuestions = history.getConsecutiveQuestions() + 1;
                final int consecutiveQuestions = Integer.parseInt(settingManager.get(SettingManager.CONSECUTIVE_QUESTIONS).getValue());

                /*
                Überprüfen ob
                 */
                if (actualConsecutiveQuestions >= consecutiveQuestions) {
                    //Counter  zurücksetzen, falls der User weiterfahren möchte und true zurückgegeben
                    history.setConsecutiveQuestions(0);
                    historyManager.update(history);
                    return true;
                } else {
                    //Counter erhöhen
                    history.setConsecutiveQuestions(actualConsecutiveQuestions);
                    historyManager.update(history);
                    return false;
                }
            } else {
                //Counter zurücksetzen
                history.setConsecutiveQuestions(0);
                historyManager.update(history);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Die Methode sucht ob die nextQuestion von anderen Fragen abhängig ist.
     * <p>
     * Wenn keine abhängig ist, dann wird die aktuell beste Frage zurückgegeben
     * <p>
     * Achtung diese Methode wird Rekursiv ausgeführt
     *
     * @param bestQuestion Die bisherig beste Frage
     * @return Die neue beste Frage
     */
    private Question getQuestionDependence(Question bestQuestion) {
        Answer answer = bestQuestion.getDependsOn();

        //wenn keine dependence besteht.
        if (answer == null) {
            return bestQuestion;
        }

        Question question = answer.getAnswerOf();
        if (question == null || (!remainingQuestions.contains(question) && !informationQuestions.contains(question))) {
            return bestQuestion;
        }

        //rekursiven aufruf
        return getQuestionDependence(question);
    }



    /**
     * Die Methode sucht alle Questions die schon gestellt wurden.
     * Dies Questions müssen nämlich nicht mehr gestellt werden.
     * <p>
     * Zudem werden alle dependencyfrom Fragen ebenfalls gelöscht.
     *
     * @param askedAnswers Alle Antworten die schon abgegeben wurden
     * @return ArrayList mit den sauberen Fragen
     */
    private ArrayList<Question> getCleanQuestions(Set<Answer> askedAnswers) {
        ArrayList<Question> questions = questionManager.getAll();
        if (questions == null) {
            return new ArrayList<Question>();
        }

        for (Answer answer : askedAnswers) {
            final Question answerOf = answer.getAnswerOf();
            questions.remove(answerOf);
        }

        for (Answer askedAnswer : askedAnswers) {
            Question question = askedAnswer.getAnswerOf();
            if (question == null) {
                continue;
            }
            cleanDependencies(question.getAnswerNo(), questions);
            cleanDependencies(question.getAnswerYes(), questions);

        }

        return questions;
    }

    /**
     * Gibt alle Fragen zurück, welche noch gefragt werden sollen.
     *
     * @return Liste aller zu fragenden Antworten.
     */
    private ArrayList<Question> getQuestionsToAsk(){    // FIXME RE
        ArrayList<Question> questions = questionManager.getAll();
        ArrayList<Question> questionsToAsk = new ArrayList<Question>();

        if (questions == null) {
            return new ArrayList<Question>();
        }

        for(Question question: questions){
            if (question.isSymptom())
                questionsToAsk.add(question);
        }

        return questionsToAsk;

    }

    /**
     * Gibt alle Informations Fragen zurück.
     * @return Liste aller Informations Fragen.
     */
    private ArrayList<Question> getInformaticQuestions(){   // FIXME RE
        ArrayList<Question> questions = questionManager.getAll();
        ArrayList<Question> questionsToAsk = new ArrayList<Question>();

        if (questions == null) {
            return new ArrayList<Question>();
        }

        for(Question question: questions){
            if (!question.isSymptom())
                questionsToAsk.add(question);
        }

        return questionsToAsk;
    }

    /**
     * Entfernt alle abhängigen Fragen, welche mit der aktuellen Antwort ausgeschlossen werden können, aus der Frageliste.
     *
     * @param questions Liste der zu reinigenden Fragen.
     * @param givenAnswer Gegebene Antwort
     * @return Gereinigte Fragenliste.
     */
    private ArrayList<Question> cleanOutDependentQuestions(ArrayList<Question> questions, Answer givenAnswer){

        ArrayList<Question> questionsToCheck = (ArrayList<Question>) questions.clone();
        Answer answerDependencyToRemove = givenAnswer.getAnswerOf().getNegativeAnswer(givenAnswer);

        questionsToCheck.remove(givenAnswer.getAnswerOf());

        for(Question q: answerDependencyToRemove.getDependencyFrom()){
            questionsToCheck.remove(q);
            questionsToCheck = cleanOutDependentQuestions(questionsToCheck, q.getAnswerYes());
            questionsToCheck = cleanOutDependentQuestions(questionsToCheck, q.getAnswerNo());
        }

        return questionsToCheck;
    }

    /**
     * Entfernt alle abhängigen Fragen, welche mit der Antwort "Weiss nicht" ausgeschlossen werden können, aus der Frageliste.
     * @param questions Liste der zu reinigenden Fragen.
     * @param answeredQuestion Frage, die mit "Weiss nicht" beantwortet wurde.
     * @return Gereinigte Fragenliste.
     */
    private ArrayList<Question> cleanOutDependentQuestions(ArrayList<Question> questions, Question answeredQuestion){

        ArrayList<Question> questionsToCheck = (ArrayList<Question>) questions.clone();
        Answer answerDependencyToRemove = answeredQuestion.getAnswerYes();

        questionsToCheck.remove(answeredQuestion);

        for(Question q: answerDependencyToRemove.getDependencyFrom()){
            questionsToCheck.remove(q);
            questionsToCheck = cleanOutDependentQuestions(questionsToCheck, q.getAnswerYes());
            questionsToCheck = cleanOutDependentQuestions(questionsToCheck, q.getAnswerNo());
        }

        return questionsToCheck;
    }

    /**
     * Prüft, ob eine Frage von der aktuellen Antwort abhängig ist, die zwangsläufig als nächstes gestellt werden muss.
     *
     * @param givenAnswer Gegebene Antwort
     * @return Frage, die als nächstes gestellt werden muss.
     */
    private Question checkForceDependentQuestion(Answer givenAnswer){   // RE

        for(Question q: givenAnswer.getDependencyFrom()){
            if (q.getForceDependentAsking())
                return q;
        }

        return null;
    }

    /**
     * Diese Methode entfernt einer Liste mit Fragen die Kette von Fragen die von einer Anwort abhängig sind.
     * <p>
     * Diese Methode wird Rekursiv aufgerufen um die Kette zu finden
     *
     * @param answer    Die Antwort deren Abhängigkeitskette gesucht und entfernt werden muss
     * @param questions Die Liste von Fragen die gesäubert werden muss.
     */
    private void cleanDependencies(Answer answer, ArrayList<Question> questions) {
        final Set<Question> dependencyFrom = answer.getDependencyFrom();

        if (dependencyFrom == null) {
            return;
        } else {
            for(Question q: dependencyFrom) {
                questions.remove(q);
                cleanDependencies(q.getAnswerNo(), questions);
                cleanDependencies(q.getAnswerYes(), questions);
            }

        }
    }


    /**
     * Diese Methode gibt die nächste Frage, nach der reihenfolge der Ids
     * <p>
     * Diese Methode wird nur gebraucht wenn getBestQuestion gebraucht wird.
     *
     * @param questions Die Fragen die noch nicht gestellt wurden
     * @param user      der User
     * @return die nächste Frage
     * @deprecated
     */
    private Question getOtherBestQuestion(final ArrayList<Question> questions, User user) {
        return questions.get(0);
    }

    /**
     * Die Methode überprüft mit welcher Frage, der Scoreunterschied am grössten wäre.
     * <p>
     * Dabei muss die aktuelle Diagnose NICHT zuoberst sein.
     * <p>
     * Es wird durch alle Fragen Durchiteriert, mit ihren Antworten wird eine neue virtuelle Rangliste erstellt.
     * Dabei wird den Scorunterschied mit der Frage jedes mal gespeichert, wenn der Unterschied grösser wird.
     *
     * @param questions Die Fragen die noch nicht gestellt wurden
     * @param user      der Benutzer der die Fragen beantwortet. Diesem User ist die History referenziert.
     * @return Die beste Frage oder null
     */
    private Question getBestQuestion(final ArrayList<Question> questions, User user) {
        Question bestQuestion = null;
        int highestDifference = 0;
        Map.Entry<Diagnosis, Integer> topEntry = diagnosisCalculator.getTopDiagnosis();
        Map.Entry<Diagnosis, Integer> secondEntry = diagnosisCalculator.getSecondDiagnosis();

        // RE: Prüft, ob es Informative Fragen hat. diese zu Erst stellen
        for(Question question: informationQuestions)
            if (!question.isSymptom())
                return question;


        int difference;
        if (topEntry != null){
            Diagnosis top = topEntry.getKey();
            Diagnosis second = secondEntry.getKey();
            int topValue = topEntry.getValue();
            int secondValue = secondEntry.getValue();

            for(Question question: questions){

                difference = calculateDifference(question, top, second, topValue, secondValue);

                if (highestDifference < difference){
                    highestDifference = difference;
                    bestQuestion = question;
                }

            }
        }

        return bestQuestion;
    }

    /**
     * Berechnet den Scoreunterschied zwischen der aktuellen Top Diagnose und der zweiten Diagnose.
     *
     * @param question Aktuelle Frage
     * @param top Top Diagnose
     * @param second Zweite Diagnose
     * @param topValue Score der Top Diagnose
     * @param secondValue Score der Zweitdiagnose
     * @return Scoredifferenz zwischen der Top und der Zweit Diagnose.
     */
    private int calculateDifference(Question question, Diagnosis top, Diagnosis second, int topValue, int secondValue){ // FIXME RE
        int tempDiff = 0;


        if (top == null || second == null)
            return 0;

        Answer answerNo = question.getAnswerNo();
        Answer answerYes = question.getAnswerYes();

        topValue += answerNo.getScoreForDiagnosis(top) + answerYes.getScoreForDiagnosis(top);
        secondValue += answerNo.getScoreForDiagnosis(second) + answerYes.getScoreForDiagnosis(second);

        return Math.abs(topValue - secondValue);

    }

    /**
     * Beantwortet die Informativen Fragen des Followups anhand des vorherigen Durchlaufs.
     * @param user
     */
    private void answerInformationsForFollowup(User user){

        for(Answer answer: followupInformationAnswers) {
            Question questionOfAnswer = answer.getAnswerOf();

            if (questionOfAnswer.getType() >= 10){
                if (answer.getAnswerYesOf() != null)
                    answer = getAnswerCorrectTimeQuestion(questionOfAnswer);
            }

            diagnosisCalculator.addAnswerToRankingList(answer);
            historyManager.addAnswerToHistory(answer, user.getHistory(), questionOfAnswer);
            informationQuestions = cleanOutDependentQuestions(informationQuestions, answer);
            informationQuestions.remove(answer);
        }

        followupInformationAnswers = null;

    }

    /**
     * Beantwortet bei einem Followup automatisch, wie lange eine Krankheit bereits besteht anhand der Angabe des Users beim
     * ersten Durchlauf und der verstrichenen Zeit, seit jenem Durchlauf.
     */
    private Answer getAnswerCorrectTimeQuestion(Question timeQuestion){


        Date lastRun = currentFollowup.getTimeStamp();
        Date now = new Date();
        long timespanInHours = (now.getTime() - lastRun.getTime()) / 3600000;

        switch(timeQuestion.getType()){
            case 10:
                timespanInHours += 0;
                break;
            case 11:
                timespanInHours += 2;
                break;
            case 12:
                timespanInHours += 4;
                break;
            case 13:
                timespanInHours += 24;
                break;
            case 14:
                timespanInHours += 72;
                break;
            case 15:
                timespanInHours += 168;
                break;
        }

        if (timespanInHours <= 2)
            return questionManager.getTypedQuestion(10).getAnswerYes();
        else if (timespanInHours > 2 && timespanInHours < 4)
            return questionManager.getTypedQuestion(11).getAnswerYes();
        else if (timespanInHours >= 4 && timespanInHours < 24 )
            return questionManager.getTypedQuestion(12).getAnswerYes();
        else if (timespanInHours >= 24 && timespanInHours < 72 )
            return questionManager.getTypedQuestion(13).getAnswerYes();
        else if (timespanInHours >= 72 && timespanInHours < 168 )
            return questionManager.getTypedQuestion(14).getAnswerYes();
        else if (timespanInHours >= 168 && timespanInHours < 504 )
            return questionManager.getTypedQuestion(15).getAnswerYes();

        return timeQuestion.getAnswerYes();
    }

    /**
     * Beantwortet typisierte Fragen automatisch (Aktuell Geschlecht und Alter)
     * @param user User Daten, anhand welcher die Antworten gegeben werden sollen.
     */
    private void answerTypedQuestions(User user){

        Question q;
        Answer givenAnswer;
        for(int i = informationQuestions.size() - 1; i > -1; i--){
            givenAnswer = null;
            q = informationQuestions.get(i);
            switch(q.getType()){
                case 1:
                    givenAnswer = (user.getGender() == 0) ? q.getAnswerYes() : q.getAnswerNo();
                    break;
                case 2:
                    givenAnswer = (user.getAgeCategory() == 0) ? q.getAnswerYes() : q.getAnswerNo();
                    break;
                case 3:
                    givenAnswer = (user.getAgeCategory() == 1) ? q.getAnswerYes() : q.getAnswerNo();
                    break;
                case 4:
                    givenAnswer = (user.getAgeCategory() == 2) ? q.getAnswerYes() : q.getAnswerNo();
                    break;
                default:
            }
            if (givenAnswer != null){
                diagnosisCalculator.addAnswerToRankingList(givenAnswer);
                historyManager.addAnswerToHistory(givenAnswer, user.getHistory(), givenAnswer.getAnswerOf());
                informationQuestions = cleanOutDependentQuestions(informationQuestions, givenAnswer);
                remainingQuestions = cleanOutDependentQuestions(remainingQuestions, givenAnswer);
                informationQuestions.remove(q);
                if (followupQuestions != null)
                    followupQuestions.remove(givenAnswer.getAnswerOf());
            }
        }

    }

    public static void reset(){ // FIXME RE

        informationQuestions = null;
        remainingQuestions = null;
        followupQuestions = null;
        answeredFollowupQuestions = null;
        currentFollowup = null;
        DiagnosisCalculator.reset();
    }
}
