package com.scheduler.model;

import com.scheduler.enums.Priority;
import com.scheduler.enums.TaskStatus;

import java.util.Objects;

/**
 * Represents a unit of work that can be scheduled and executed in the distributed system.
 * 
 * INTERVIEW TALKING POINTS:
 * - Immutable requirements: CPU, memory, execution time, and priority cannot change after creation
 * - Mutable state: Status, assignment, and timing information change during execution
 * - Resource modeling: Captures both computational (CPU) and memory requirements
 * - Priority support: Enables QoS and SLA management
 * - Retry mechanism: Built-in retry count for fault tolerance
 * 
 * @INTERVIEW_QUESTION: Why make resource requirements immutable?
 * @ANSWER: Prevents resource conflicts during execution and simplifies capacity planning
 * @INTERVIEW_QUESTION: Why track both CPU and memory?
 * @ANSWER: Real-world tasks require both computational resources and memory, enabling accurate resource allocation
 */
public class Task {
    // Immutable core properties
    private final String taskId;
    private final int cpuRequirement;
    private final int memoryRequirement;
    private final int executionTime;
    private final Priority priority;
    
    // Mutable execution state
    private TaskStatus status;
    private String assignedTo;
    private long startTime;
    private int retryCount;

    /**
     * Creates a new task with specified requirements.
     * 
     * @param taskId Unique identifier for this task
     * @param cpuRequirement CPU cores needed (must be positive)
     * @param memoryRequirement Memory in MB needed (must be positive)
     * @param executionTime Estimated execution time in seconds (must be positive)
     * @param priority Task priority level for scheduling
     * 
     * @INTERVIEW_QUESTION: Why validate parameters in constructor?
     * @ANSWER: Fail-fast principle - catch invalid input early rather than during execution
     * @INTERVIEW_QUESTION: Why use Objects.requireNonNull for taskId?
     * @ANSWER: Explicit null checking with clear error messages, better than NPE later
     */
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
    
    // Getter methods for immutable properties
    
    /**
     * Gets the unique identifier for this task.
     * 
     * @return task ID string
     * 
     * @INTERVIEW_QUESTION: Why use String for task ID?
     * @ANSWER: Human-readable, flexible format, can include timestamps, UUIDs, or business identifiers
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Gets the CPU cores required for this task.
     * 
     * @return CPU requirement count
     * 
     * @INTERVIEW_QUESTION: Why track CPU requirements?
     * @ANSWER: Enables intelligent scheduling based on available worker capacity and prevents overloading
     */
    public int getCpuRequirement() {
        return cpuRequirement;
    }

    /**
     * Gets the memory requirement in MB.
     * 
     * @return memory requirement in megabytes
     * 
     * @INTERVIEW_QUESTION: Why track memory separately from CPU?
     * @ANSWER: Tasks can be CPU-intensive or memory-intensive; tracking both enables optimal worker selection
     */
    public int getMemoryRequirement() {
        return memoryRequirement;
    }

    /**
     * Gets the estimated execution time in seconds.
     * 
     * @return execution time in seconds
     * 
     * @INTERVIEW_QUESTION: Why track execution time?
     * @ANSWER: Enables timeout detection, performance monitoring, and scheduling optimization
     */
    public int getExecutionTime() {
        return executionTime;
    }

    /**
     * Gets the priority level of this task.
     * 
     * @return task priority
     * 
     * @INTERVIEW_QUESTION: Why use Priority enum?
     * @ANSWER: Type safety and ability to add priority-related behavior in the future
     */
    public Priority getPriority() {
        return priority;
    }

    /**
     * Gets the current status of this task.
     * 
     * @return current task status
     * 
     * @INTERVIEW_QUESTION: Why expose status as mutable?
     * @ANSWER: Status changes during task lifecycle (QUEUED -> ASSIGNED -> COMPLETED)
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Gets the ID of the worker this task is assigned to.
     * 
     * @return worker ID or null if not assigned
     * 
     * @INTERVIEW_QUESTION: Why return null for unassigned tasks?
     * @ANSWER: Clear indication that task is not currently running on any worker
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    // State management methods
    
    /**
     * Sets the current status of this task.
     * 
     * @param status new task status
     * 
     * @INTERVIEW_QUESTION: Why allow status changes from outside?
     * @ANSWER: SchedulerService needs to update task status during lifecycle management
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Assigns this task to a specific worker.
     * 
     * @param workerId ID of the worker node
     * 
     * @INTERVIEW_QUESTION: Why track assignment separately from status?
     * @ANSWER: Status tells what phase, assignment tells where it's running - different concerns
     */
    public void setAssignedTo(String workerId) {
        this.assignedTo = workerId;
    }

    /**
     * Sets the start time for task execution.
     * 
     * @param startTime timestamp when task began execution
     * 
     * @INTERVIEW_QUESTION: Why track start time?
     * @ANSWER: Enables timeout detection, performance metrics, and execution duration calculation
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Increments the retry count for this task.
     * 
     * @INTERVIEW_QUESTION: Why track retry count?
     * @ANSWER: Enables fault tolerance by limiting retry attempts and detecting problematic tasks
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Resets task state for reassignment after failure.
     * 
     * @INTERVIEW_QUESTION: Why have a reset method?
     * @ANSWER: Enables task recovery after worker failures while preserving retry history
     */
    public void resetForReassignment() {
        this.status = TaskStatus.QUEUED;
        this.assignedTo = null;
        this.startTime = -1;
    }

    /**
     * Checks if task execution is complete based on current time.
     * 
     * @param currentTime current simulation time
     * @return true if execution is complete, false otherwise
     * 
     * @INTERVIEW_QUESTION: Why use startTime = -1 as sentinel?
     * @ANSWER: Clear indication that task hasn't started execution yet
     */
    public boolean isExecutionComplete(long currentTime) {
        if (startTime < 0 || status != TaskStatus.ASSIGNED) {
            return false;
        }
        return (currentTime - startTime) >= executionTime;
    }

    // Object methods for proper equality and hashing
    
    /**
     * Checks equality based on task ID.
     * 
     * @INTERVIEW_QUESTION: Why only compare task ID?
     * @ANSWER: Task ID should be unique; comparing all fields could cause issues with state changes
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(taskId, task.taskId);
    }

    /**
     * Generates hash code based on task ID.
     * 
     * @INTERVIEW_QUESTION: Why use task ID for hash code?
     * @ANSWER: Ensures consistent hashing and works with equals() implementation
     */
    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    /**
     * String representation for debugging and logging.
     * 
     * @INTERVIEW_QUESTION: Why customize toString?
     * @ANSWER: Provides useful debugging information showing task ID, status, and assignment
     */
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
