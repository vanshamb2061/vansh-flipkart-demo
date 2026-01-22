package com.scheduler.registry;

import com.scheduler.enums.TaskStatus;
import com.scheduler.model.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TaskRegistry {
    
    private final Map<String, Task> tasks;

    public TaskRegistry() {
        this.tasks = new ConcurrentHashMap<>();
    }

    public void register(Task task) {
        if (tasks.containsKey(task.getTaskId())) {
            throw new IllegalArgumentException("Task already registered: " + task.getTaskId());
        }
        tasks.put(task.getTaskId(), task);
    }

    public Task getTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return task;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public void clear() {
        tasks.clear();
    }
}
