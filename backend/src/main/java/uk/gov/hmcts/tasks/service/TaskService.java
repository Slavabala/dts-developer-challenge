package uk.gov.hmcts.tasks.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.tasks.dto.CreateTaskRequest;
import uk.gov.hmcts.tasks.dto.TaskResponse;
import uk.gov.hmcts.tasks.exception.TaskNotFoundException;
import uk.gov.hmcts.tasks.model.Task;
import uk.gov.hmcts.tasks.model.TaskStatus;
import uk.gov.hmcts.tasks.repository.TaskRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Cacheable(value = "tasks", key = "#id")
    public TaskResponse getTaskById(UUID id) {
        return taskRepository.findById(id)
                .map(TaskResponse::from)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Cacheable(value = "tasks", key = "'all'")
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAllByOrderByDueDateAsc()
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse updateTaskStatus(UUID id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(status);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }
}
