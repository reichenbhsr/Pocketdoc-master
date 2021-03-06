package models;

import database.mappers.QuestionConnector;
import database.mappers.intermediateClassMappers.AnswerToActionSuggestionScoreDistributionConnector;
import database.mappers.intermediateClassMappers.AnswerToDiagnosisScoreDistributionConnector;
import models.intermediateClassModels.AnswerToActionSuggestionScoreDistribution;
import models.intermediateClassModels.AnswerToDiagnosisScoreDistribution;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * Entity, also eins zu eins Abbildung der Datenbanktabelle answers
 *
 * @author Oliver Frischknecht
 */

public class Answer {

    private int id;

    private Set<AnswerToActionSuggestionScoreDistribution> answerToActionSuggestionScoreDistributions;

    private Set<AnswerToDiagnosisScoreDistribution> answerToDiagnosisScoreDistributions;

    private Set<Question> dependencyFrom;

    private Question answerYesOf;

    private Question answerNoOf;

    public Answer() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<AnswerToActionSuggestionScoreDistribution> getAnswerToActionSuggestionScoreDistributions() {

        if (answerToActionSuggestionScoreDistributions == null)
        {
            AnswerToActionSuggestionScoreDistributionConnector ac = new AnswerToActionSuggestionScoreDistributionConnector();
            answerToActionSuggestionScoreDistributions = ac.readSetOfAnswer(id);
        }

        return answerToActionSuggestionScoreDistributions;
    }

    public void setAnswerToActionSuggestionScoreDistributions(Set<AnswerToActionSuggestionScoreDistribution> answerToActionSuggestionScoreDistributions) {
        this.answerToActionSuggestionScoreDistributions = answerToActionSuggestionScoreDistributions;
    }

    public Set<AnswerToDiagnosisScoreDistribution> getAnswerToDiagnosisScoreDistributions() {

        if (answerToDiagnosisScoreDistributions == null)
        {
            AnswerToDiagnosisScoreDistributionConnector ac = new AnswerToDiagnosisScoreDistributionConnector();
            answerToDiagnosisScoreDistributions = ac.readSetOfAnswer(id);
        }

        return answerToDiagnosisScoreDistributions;
    }

    public void setAnswerToDiagnosisScoreDistributions(Set<AnswerToDiagnosisScoreDistribution> answerToDiagnosisScoreDistributions) {
        this.answerToDiagnosisScoreDistributions = answerToDiagnosisScoreDistributions;
    }

    public int getScoreForDiagnosis(Diagnosis diagnosis){
        AnswerToDiagnosisScoreDistributionConnector ac = new AnswerToDiagnosisScoreDistributionConnector();
        return ac.readScoreForDiagnosisFromAnswer(this, diagnosis);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Answer) {
            return ((Answer) obj).getId() == id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public Set<Question> getDependencyFrom() {

        if (dependencyFrom == null)
        {
            QuestionConnector qc = new QuestionConnector();
            dependencyFrom = qc.readDependendQuestions(id);
        }

        return dependencyFrom;
    }
    public void setDependencyFrom(Set<Question> dependencyFrom) {
        this.dependencyFrom = dependencyFrom;
    }

    public Question getAnswerYesOf() {

        if (answerYesOf == null)
        {
            QuestionConnector qc = new QuestionConnector();
            answerYesOf = qc.readAnswerYesOf(id);
        }

        return answerYesOf;
    }

    public void setAnswerYesOf(Question answerYesOf) {
        this.answerYesOf = answerYesOf;
    }

    public Question getAnswerNoOf() {

        if (answerNoOf == null)
        {
            QuestionConnector qc = new QuestionConnector();
            answerNoOf = qc.readAnswerNoOf(id);
        }

        return answerNoOf;
    }

    public void setAnswerNoOf(Question answerNoOf) {
        this.answerNoOf = answerNoOf;
    }

    /**
     * Gibt die Frage zu dieser Antwort zurück (egal ob Ja oder Nein)
     *
     * @return Question
     */
    public Question getAnswerOf() {
        if (getAnswerYesOf() == null) {
            return getAnswerNoOf();
        } else {
            return getAnswerYesOf();
        }
    }
}
