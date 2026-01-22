package com.scheduler.model;

import com.scheduler.enums.Priority;
import com.scheduler.enums.TaskStatus;

import java.util.Objects;

public class Task {
    private final String taskId;
    private final int cpuRequirement;
    private final int memoryRequirement;
    private final int executionTime;
    private final Priority priority;
    
    private TaskStatus status;
    private String assignedTo;
    private long startTime;
    private int retryCount;

    public Task(String taskId, int cpuRequirement, int memoryRequirement, int executionTime, Priority priority) {
        this.taskId = Objects.requireNonNull(taskId, "Task ID cannot be null");
        if (cpuRequirement <= 0) {
            throw new IllegalArgumentException("CPU requirement must be positive");
        }
        this.cpuRequirement = cpuRequirement;
        if (memoryRequirement <= 0) {
            throw new IllegalArgumentException("Memory requirement must be positive");
        }
        this.memoryRequirement = memoryRequirement;
        if (executionTime <= 0) {
            throw new IllegalArgumentException("Execution time must be positive");
        }
        this.executionTime = executionTime;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.status = TaskStatus.QUEUED;
        this.assignedTo = null;
        this.startTime = -1;
        this.retryCount = 0;
    }
    
    public String getTaskId() {
        return taskId;
    }

    public int getCpuRequirement() {
        return cpuRequirement;
    }

    public int getMemoryRequirement() {
        return memoryRequirement;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public Priority getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setAssignedTo(String workerId) {
        this.assignedTo = workerId;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void resetForReassignment() {
        this.status = TaskStatus.QUEUED;
        this.assignedTo = null;
        this.startTime = -1;
    }

    public boolean isExecutionComplete(long currentTime) {
        if (startTime < 0 || status != TaskStatus.ASSIGNED) {
            return false;
        }
        return (currentTime - startTime) >= executionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ taskId: \"").append(taskId).append("\"");
        sb.append(", status: \"").append(status.name().toLowerCase()).append("\"");
        if (assignedTo != null) {
            sb.append(", assignedTo: \"").append(assignedTo).append("\"");
        }
        sb.append(" }");
        return sb.toString();
    }

}
