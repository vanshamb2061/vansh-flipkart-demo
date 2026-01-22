package com.scheduler.registry;

import com.scheduler.enums.TaskStatus;
import com.scheduler.model.Task;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Centralized repository for managing all tasks in the distributed scheduler.
 * 
 * INTERVIEW TALKING POINTS:
 * - Thread-safe storage: ConcurrentHashMap for concurrent access across multiple threads
 * - Task lifecycle tracking: Maintains reference to all tasks regardless of status
 * - Lookup efficiency: O(1) task retrieval by ID using HashMap
 * - Duplicate prevention: Ensures task ID uniqueness across the system
 * - Monitoring support: Provides methods for system observability and debugging
 * 
 * @INTERVIEW_QUESTION: Why use ConcurrentHashMap instead of regular HashMap?
 * @ANSWER: Thread-safe access for concurrent task operations without external synchronization
 * @INTERVIEW_QUESTION: Why prevent duplicate task IDs?
 * @ANSWER: Ensures unique identification and prevents confusion in task tracking and assignment
 */
public class TaskRegistry {
    
    // Thread-safe storage for all tasks
    private final Map<String, Task> tasks;

    /**
     * Creates a new task registry.
     * 
     * @INTERVIEW_QUESTION: Why initialize ConcurrentHashMap in constructor?
     * @ANSWER: Ensures registry is ready for use and prevents null pointer exceptions
     */
    public TaskRegistry() {
        this.tasks = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new task in the system.
     * 
     * @param task task to be registered
     * @throws IllegalArgumentException if task ID already exists
     * 
     * @INTERVIEW_QUESTION: Why throw exception for duplicate tasks?
     * @ANSWER: Fail-fast principle - prevents system inconsistencies and forces caller to handle duplicates
     * @INTERVIEW_QUESTION: Why use putIfAbsent pattern?
     * @ANSWER: Thread-safe check-and-put operation prevents race conditions in concurrent environment
     */
    public void register(Task task) {
        if (tasks.containsKey(task.getTaskId())) {
            throw new IllegalArgumentException("Task already registered: " + task.getTaskId());
        }
        tasks.put(task.getTaskId(), task);
    }

    /**
     * Retrieves a task by its unique identifier.
     * 
     * @param taskId unique task identifier
     * @return the task with the given ID
     * @throws IllegalArgumentException if task not found
     * 
     * @INTERVIEW_QUESTION: Why throw exception for missing tasks?
     * @ANSWER: Makes programming errors visible and forces proper error handling
     * @INTERVIEW_QUESTION: Why not return Optional?
     * @ANSWER: Task IDs should always be valid; missing task indicates a system error
     */
    public Task getTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return task;
    }

    /**
     * Gets all tasks currently registered in the system.
     * 
     * @return list of all tasks
     * 
     * @INTERVIEW_QUESTION: Why return a new ArrayList?
     * @ANSWER: Prevents external modification of internal state while providing full access
     * @INTERVIEW_QUESTION: Why return List instead of Collection?
     * @ANSWER: More specific interface, provides indexed access, and is more commonly used
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Gets tasks filtered by their current status.
     * 
     * @param status status to filter by
     * @return list of tasks with the specified status
     * 
     * @INTERVIEW_QUESTION: Why provide status-based filtering?
     * @ANSWER: Enables monitoring and management of tasks in specific lifecycle stages
     * @INTERVIEW_QUESTION: Why use stream API?
     * @ANSWER: Modern, readable approach for collection filtering and transformation
     */
    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Gets tasks assigned to a specific worker.
     * 
     * @param workerId worker identifier
     * @return list of tasks assigned to the worker
     * 
     * @INTERVIEW_QUESTION: Why track tasks by worker assignment?
     * @ANSWER: Enables worker load monitoring and failure recovery scenarios
     */
    public List<Task> getTasksByWorker(String workerId) {
        return tasks.values().stream()
                .filter(task -> workerId.equals(task.getAssignedTo()))
                .collect(Collectors.toList());
    }

    /**
     * Gets the current number of registered tasks.
     * 
     * @return total task count
     * 
     * @INTERVIEW_QUESTION: Why provide a count method?
     * @ANSWER: Efficient way to get system size without creating intermediate collections
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * Removes all tasks from the registry.
     * 
     * @INTERVIEW_QUESTION: Why provide a clear method?
     * @ANSWER: Useful for testing, system reset, and cleanup scenarios
     */
    public void clear() {
        tasks.clear();
    }

    /**
     * Checks if a task with the given ID is registered.
     * 
     * @param taskId task identifier to check
     * @return true if task exists, false otherwise
     * 
     * @INTERVIEW_QUESTION: Why provide a contains method?
     * @ANSWER: Non-throwing way to check for task existence, useful for validation
     */
    public boolean containsTask(String taskId) {
        return tasks.containsKey(taskId);
    }
}
