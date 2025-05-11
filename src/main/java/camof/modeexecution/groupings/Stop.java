package camof.modeexecution.groupings;

import camof.modeexecution.Agent;
import camof.modeexecution.Coordinate;

import java.time.LocalDateTime;
import java.util.List;

public class Stop {

    LocalDateTime startTime;

    LocalDateTime endTime;

    Coordinate stopCoordinate;

    Stopreason reasonForStopping;

     List<Agent> personsInQuestion;

    public Stop() {
    }


    public Stop(LocalDateTime startTime, LocalDateTime endTime, Coordinate stopCoordinate, Stopreason reasonForStopping, List<Agent> personsInQuestion) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.stopCoordinate = stopCoordinate;
        this.reasonForStopping = reasonForStopping;
        this.personsInQuestion = personsInQuestion;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Coordinate getStopCoordinate() {
        return stopCoordinate;
    }

    public void setStopCoordinate(Coordinate stopCoordinate) {
        this.stopCoordinate = stopCoordinate;
    }

    public Stopreason getReasonForStopping() {
        return reasonForStopping;
    }

    public void setReasonForStopping(Stopreason reasonForStopping) {
        this.reasonForStopping = reasonForStopping;
    }

    public List<Agent> getPersonsInQuestion() {
        return personsInQuestion;
    }

    public void setPersonsInQuestion(List<Agent> personsInQuestion) {
        this.personsInQuestion = personsInQuestion;
    }
}
