package uk.gov.hmcts.tasks.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.hmcts.tasks.model.TaskStatus;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
