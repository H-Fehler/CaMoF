package camof.modeexecution;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public class Event implements Comparable<Event>{

    LocalDateTime eventStart;

    LocalDateTime eventEnd;
    String type;

    Object eventObject;

    public Event(String type, LocalDateTime eventStart, Object eventObject){
        this.type=type;
        this.eventStart=eventStart;
        this.eventObject=eventObject;
    }

    public Event(String type, LocalDateTime eventStart, LocalDateTime eventEnd, Object eventObject){
        this.type=type;
        this.eventStart=eventStart;
        this.eventObject=eventObject;
        this.eventEnd=eventEnd;
    }

    @Override
    public int compareTo(@NotNull Event event) {
        if(event.eventStart.isBefore(this.eventStart)){
            return 1;
        }else if(event.eventStart.isAfter(this.eventStart)){
            return -1;
        }else{
            return 0;
        }
    }

    public LocalDateTime getEventStart() {
        return eventStart;
    }

    public void setEventStart(LocalDateTime eventStart) {
        this.eventStart = eventStart;
    }

    public LocalDateTime getEventEnd() {
        return eventEnd;
    }

    public void setEventEnd(LocalDateTime eventEnd) {
        this.eventEnd = eventEnd;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getEventObject() {
        return eventObject;
    }

    public void setEventObject(Object eventObject) {
        this.eventObject = eventObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventStart, event.eventStart) && Objects.equals(type, event.type) && Objects.equals(eventObject, event.eventObject);
    }

}
