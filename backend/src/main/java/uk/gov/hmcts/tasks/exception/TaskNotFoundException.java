package uk.gov.hmcts.tasks.exception;

import java.util.UUID;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID id) {
        super("Task not found with id: " + id);
    }
}
