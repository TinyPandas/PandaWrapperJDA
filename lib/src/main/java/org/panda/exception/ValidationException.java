package org.panda.exception;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ValidationException extends Exception {

    public String fieldName;
    public String validationMessage;
    public String eventMessage;

    public ValidationException(String eventMessage) {
        this.eventMessage = eventMessage;
    }

    public ValidationException(String fieldName, String validationMessage) {
        this.fieldName = fieldName;
        this.validationMessage = validationMessage;
    }

    public String toEventResponse() {
        if (Objects.nonNull(eventMessage) && !eventMessage.isEmpty()) {
            return eventMessage;
        }

        return String.format("%s %s", fieldName, validationMessage);
    }
}