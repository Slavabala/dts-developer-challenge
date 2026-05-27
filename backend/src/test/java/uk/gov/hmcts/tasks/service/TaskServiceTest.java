package uk.gov.hmcts.tasks.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.tasks.dto.CreateTaskRequest;
import uk.gov.hmcts.tasks.dto.TaskResponse;
import uk.gov.hmcts.tasks.exception.TaskNotFoundException;
import uk.gov.hmcts.tasks.model.Task;
import uk.gov.hmcts.tasks.model.TaskStatus;
import uk.gov.hmcts.tasks.repository.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        sampleTask = Task.builder()
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
    void createTask_shouldReturnCreatedTask() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Review case file");
        request.setDescription("Review the documents for case #1234");
        request.setDueDate(LocalDateTime.now().plusDays(3));

        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskResponse response = taskService.createTask(request);

        assertThat(response.getTitle()).isEqualTo("Review case file");
        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldUseProvidedStatus_whenSupplied() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Urgent task");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setDueDate(LocalDateTime.now().plusDays(1));

        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        taskService.createTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void createTask_shouldDefaultToTodo_whenStatusNotSupplied() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Routine task");
        request.setDueDate(LocalDateTime.now().plusDays(1));

        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        taskService.createTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void getAllTasks_shouldReturnEmptyList_whenNoTasksExist() {
        when(taskRepository.findAllByOrderByDueDateAsc()).thenReturn(List.of());

        List<TaskResponse> tasks = taskService.getAllTasks();

        assertThat(tasks).isEmpty();
    }

    @Test
    void getTaskById_shouldReturnTask_whenTaskExists() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(sampleTask));

        TaskResponse response = taskService.getTaskById(taskId);

        assertThat(response.getId()).isEqualTo(taskId);
        assertThat(response.getTitle()).isEqualTo("Review case file");
    }

    @Test
    void getTaskById_shouldThrowTaskNotFoundException_whenTaskDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(taskRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(missingId))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    @Test
    void getAllTasks_shouldReturnAllTasksOrderedByDueDate() {
        when(taskRepository.findAllByOrderByDueDateAsc()).thenReturn(List.of(sampleTask));

        List<TaskResponse> tasks = taskService.getAllTasks();

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getTitle()).isEqualTo("Review case file");
    }

    @Test
    void updateTaskStatus_shouldReturnUpdatedTask() {
        Task updatedTask = Task.builder()
                .id(taskId)
                .title(sampleTask.getTitle())
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(sampleTask.getDueDate())
                .createdAt(sampleTask.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        TaskResponse response = taskService.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateTaskStatus_shouldThrowTaskNotFoundException_whenTaskDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(taskRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTaskStatus(missingId, TaskStatus.DONE))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deleteTask_shouldDeleteTask_whenTaskExists() {
        when(taskRepository.existsById(taskId)).thenReturn(true);

        taskService.deleteTask(taskId);

        verify(taskRepository).deleteById(taskId);
    }

    @Test
    void deleteTask_shouldThrowTaskNotFoundException_whenTaskDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(taskRepository.existsById(missingId)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(missingId))
                .isInstanceOf(TaskNotFoundException.class);
    }
}
