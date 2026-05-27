package uk.gov.hmcts.tasks.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.tasks.dto.CreateTaskRequest;
import uk.gov.hmcts.tasks.dto.TaskResponse;
import uk.gov.hmcts.tasks.dto.UpdateStatusRequest;
import uk.gov.hmcts.tasks.exception.TaskNotFoundException;
import uk.gov.hmcts.tasks.model.TaskStatus;
import uk.gov.hmcts.tasks.service.TaskService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    private ObjectMapper objectMapper;
    private TaskResponse sampleResponse;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        taskId = UUID.randomUUID();
        sampleResponse = TaskResponse.builder()
                .id(taskId)
                .title("Review case file")
                .description("Review the documents for case #1234")
                .status(TaskStatus.TODO)
                .dueDate(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createTask_shouldReturn201_withCreatedTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Review case file");
        request.setDueDate(LocalDateTime.now().plusDays(3));

        when(taskService.createTask(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Review case file"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_shouldReturn400_whenTitleIsMissing() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setDueDate(LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_shouldReturn400_whenDueDateIsMissing() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("No due date");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_shouldReturn400_whenDueDateIsInPast() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Past task");
        request.setDueDate(LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_shouldReturn201_withProvidedStatus() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("In progress task");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setDueDate(LocalDateTime.now().plusDays(2));

        TaskResponse inProgress = TaskResponse.builder()
                .id(taskId)
                .title("In progress task")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(taskService.createTask(any())).thenReturn(inProgress);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getTask_shouldReturn200_withTask() throws Exception {
        when(taskService.getTaskById(taskId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Review case file"));
    }

    @Test
    void getTask_shouldReturn404_whenTaskDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(taskService.getTaskById(missingId)).thenThrow(new TaskNotFoundException(missingId));

        mockMvc.perform(get("/api/tasks/{id}", missingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllTasks_shouldReturn200_withTaskList() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Review case file"));
    }

    @Test
    void getAllTasks_shouldReturn200_withEmptyList() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateStatus_shouldReturn400_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturn400_whenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturn200_withUpdatedTask() throws Exception {
        TaskResponse updated = TaskResponse.builder()
                .id(taskId)
                .title("Review case file")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskService.updateTaskStatus(eq(taskId), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteTask_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTask_shouldReturn404_whenTaskDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();
        doThrow(new TaskNotFoundException(missingId)).when(taskService).deleteTask(missingId);

        mockMvc.perform(delete("/api/tasks/{id}", missingId))
                .andExpect(status().isNotFound());
    }
}
