package com.scheduler.model;

import com.scheduler.enums.WorkerStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a worker node in the distributed system that can execute tasks.
 * 
 * INTERVIEW TALKING POINTS:
 * - Resource management: Tracks both total and used resources for capacity planning
 * - Thread safety: Uses ConcurrentHashMap for running tasks in concurrent environment
 * - Resource allocation: Prevents over-allocation through canAccommodate() checks
 * - Failure handling: Clean resource release on worker failure
 * - Load tracking: Maintains list of currently running tasks
 * 
 * @INTERVIEW_QUESTION: Why track both total and used resources?
 * @ANSWER: Enables intelligent task assignment by checking available capacity before allocation
 * @INTERVIEW_QUESTION: Why use ConcurrentHashMap for running tasks?
 * @ANSWER: Thread-safe access for concurrent task execution and monitoring
 */
public class WorkerNode {
    // Immutable worker properties
    private final String nodeId;
    private final int totalCpu;
    private final int totalMemory;
    private final int processingSpeed;
    
    // Mutable state
    private WorkerStatus status;
    private int usedCpu;
    private int usedMemory;
    private final Map<String, Task> runningTasks;

    /**
     * Creates a new worker node with specified capacity.
     * 
     * @param nodeId Unique identifier for this worker
     * @param totalCpu Total CPU cores available
     * @param totalMemory Total memory in MB available
     * @param processingSpeed Relative processing speed (higher = faster)
     * 
     * @INTERVIEW_QUESTION: Why validate all parameters?
     * @ANSWER: Ensures worker nodes have valid capacity and prevents configuration errors
     * @INTERVIEW_QUESTION: What does processingSpeed represent?
     * @ANSWER: Relative speed factor used for task assignment optimization (faster workers get priority)
     */
    public WorkerNode(String nodeId, int totalCpu, int totalMemory, int processingSpeed) {
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        if (totalCpu <= 0) {
            throw new IllegalArgumentException("CPU capacity must be positive");
        }
        this.totalCpu = totalCpu;
        if (totalMemory <= 0) {
            throw new IllegalArgumentException("Memory capacity must be positive");
        }
        this.totalMemory = totalMemory;
        if (processingSpeed <= 0) {
            throw new IllegalArgumentException("Processing speed must be positive");
        }
        this.processingSpeed = processingSpeed;
        this.status = WorkerStatus.ACTIVE;
        this.usedCpu = 0;
        this.usedMemory = 0;
        this.runningTasks = new ConcurrentHashMap<>();
    }

    // Getter methods for immutable properties
    
    /**
     * Gets the unique identifier for this worker node.
     * 
     * @return worker node ID
     * 
     * @INTERVIEW_QUESTION: Why use String for node ID?
     * @ANSWER: Human-readable, can include region, rack, or instance information
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Gets the total CPU capacity of this worker.
     * 
     * @return total CPU cores
     * 
     * @INTERVIEW_QUESTION: Why expose total capacity?
     * @ANSWER: Enables monitoring and capacity planning across the cluster
     */
    public int getTotalCpu() {
        return totalCpu;
    }

    /**
     * Gets the total memory capacity of this worker.
     * 
     * @return total memory in MB
     */
    public int getTotalMemory() {
        return totalMemory;
    }

    /**
     * Gets the processing speed factor of this worker.
     * 
     * @return processing speed (higher = faster)
     * 
     * @INTERVIEW_QUESTION: How is processing speed used?
     * @ANSWER: Used by SchedulerService to prioritize faster workers for task assignment
     */
    public int getProcessingSpeed() {
        return processingSpeed;
    }

    /**
     * Gets the current status of this worker.
     * 
     * @return worker status
     * 
     * @INTERVIEW_QUESTION: Why is status mutable?
     * @ANSWER: Workers can fail and become inactive, requiring status updates
     */
    public WorkerStatus getStatus() {
        return status;
    }

    // Resource management methods
    
    /**
     * Gets the available CPU capacity.
     * 
     * @return available CPU cores
     * 
     * @INTERVIEW_QUESTION: Why calculate available resources dynamically?
     * @ANSWER: Real-time capacity checking for accurate task assignment decisions
     */
    public int getAvailableCpu() {
        return totalCpu - usedCpu;
    }

    /**
     * Gets the available memory capacity.
     * 
     * @return available memory in MB
     */
    public int getAvailableMemory() {
        return totalMemory - usedMemory;
    }

    /**
     * Gets an unmodifiable view of currently running tasks.
     * 
     * @return collection of running tasks
     * 
     * @INTERVIEW_QUESTION: Why return unmodifiable collection?
     * @ANSWER: Prevents external modification while allowing read access for monitoring
     */
    public Collection<Task> getRunningTasks() {
        return Collections.unmodifiableCollection(runningTasks.values());
    }

    /**
     * Sets the status of this worker.
     * 
     * @param status new worker status
     * 
     * @INTERVIEW_QUESTION: Why allow external status changes?
     * @ANSWER: WorkerRegistry needs to mark workers as inactive during failure scenarios
     */
    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    /**
     * Checks if this worker is active and available for task assignment.
     * 
     * @return true if active, false otherwise
     * 
     * @INTERVIEW_QUESTION: Why have a convenience method?
     * @ANSWER: Simplifies common check and improves code readability
     */
    public boolean isActive() {
        return status == WorkerStatus.ACTIVE;
    }

    /**
     * Checks if this worker can accommodate a task with given resource requirements.
     * 
     * @param cpuRequired CPU cores required
     * @param memoryRequired Memory in MB required
     * @return true if worker can accommodate the task, false otherwise
     * 
     * @INTERVIEW_QUESTION: Why check both status and resources?
     * @ANSWER: Prevents assigning tasks to inactive workers or workers with insufficient capacity
     */
    public boolean canAccommodate(int cpuRequired, int memoryRequired) {
        return isActive() && 
               getAvailableCpu() >= cpuRequired && 
               getAvailableMemory() >= memoryRequired;
    }

    /**
     * Allocates resources for a task and starts tracking it.
     * 
     * @param task task to allocate resources for
     * @return true if allocation successful, false if insufficient resources
     * 
     * @INTERVIEW_QUESTION: Why return boolean instead of throwing exception?
     * @ANSWER: Allows scheduler to try other workers when allocation fails
     * @INTERVIEW_QUESTION: Why check canAccommodate again?
     * @ANSWER: Double-check prevents race conditions in concurrent environment
     */
    public boolean allocateResources(Task task) {
        if (!canAccommodate(task.getCpuRequirement(), task.getMemoryRequirement())) {
            return false;
        }
        
        usedCpu += task.getCpuRequirement();
        usedMemory += task.getMemoryRequirement();
        runningTasks.put(task.getTaskId(), task);
        return true;
    }

    /**
     * Releases resources for a completed task.
     * 
     * @param task task to release resources for
     * 
     * @INTERVIEW_QUESTION: Why check if task exists before releasing?
     * @ANSWER: Prevents negative resource usage if task was already removed
     */
    public void releaseResources(Task task) {
        if (runningTasks.remove(task.getTaskId()) != null) {
            usedCpu = Math.max(0, usedCpu - task.getCpuRequirement());
            usedMemory = Math.max(0, usedMemory - task.getMemoryRequirement());
        }
    }

    /**
     * Releases all resources and returns list of affected tasks.
     * Used during worker failure scenarios.
     * 
     * @return list of tasks that were running on this worker
     * 
     * @INTERVIEW_QUESTION: Why return the list of tasks?
     * @ANSWER: Enables scheduler to reassign failed tasks to other workers
     * @INTERVIEW_QUESTION: Why reset used resources to 0?
     * @ANSWER: Clean state for potential worker recovery or replacement
     */
    public List<Task> releaseAllResources() {
        List<Task> tasks = new ArrayList<>(runningTasks.values());
        runningTasks.clear();
        usedCpu = 0;
        usedMemory = 0;
        return tasks;
    }

    // Object methods for proper equality and hashing
    
    /**
     * Checks equality based on node ID.
     * 
     * @INTERVIEW_QUESTION: Why only compare node ID?
     * @ANSWER: Node ID should be unique; comparing all fields could cause issues with state changes
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerNode that = (WorkerNode) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    /**
     * Generates hash code based on node ID.
     * 
     * @INTERVIEW_QUESTION: Why use node ID for hash code?
     * @ANSWER: Ensures consistent hashing and works with equals() implementation
     */
    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    /**
     * String representation for debugging and monitoring.
     * 
     * @INTERVIEW_QUESTION: What information is most important for debugging?
     * @ANSWER: Node ID, capacities, and status - key information for troubleshooting
     */
    @Override
    public String toString() {
        return String.format("{ nodeId: \"%s\", cpu: %d, memory: %d, speed: %d, status: \"%s\" }",
                nodeId, totalCpu, totalMemory, processingSpeed, status.name().toLowerCase());
    }

}
