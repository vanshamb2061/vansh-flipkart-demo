package com.scheduler.registry;

import com.scheduler.enums.WorkerStatus;
import com.scheduler.model.Task;
import com.scheduler.model.WorkerNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Centralized repository for managing worker nodes in the distributed scheduler.
 * 
 * INTERVIEW TALKING POINTS:
 * - Worker lifecycle management: Tracks active and inactive workers
 * - Auto-scaling support: Generates unique IDs for dynamically created workers
 * - Thread-safe operations: ConcurrentHashMap for concurrent access
 * - Failure handling: Manages worker status changes and resource cleanup
 * - Load monitoring: Provides visibility into worker capacity and utilization
 * 
 * @INTERVIEW_QUESTION: Why use AtomicInteger for auto-scaling counter?
 * @ANSWER: Thread-safe counter for generating unique worker IDs in concurrent environment
 * @INTERVIEW_QUESTION: Why track workers separately from tasks?
 * @ANSWER: Different lifecycle and management patterns - workers are long-lived, tasks are transient
 */
public class WorkerRegistry {
    
    // Thread-safe storage for all workers
    private final Map<String, WorkerNode> workers;
    
    // Atomic counter for generating unique auto-scaled worker IDs
    private final AtomicInteger autoScaleCounter;

    /**
     * Creates a new worker registry.
     * 
     * @INTERVIEW_QUESTION: Why initialize counter to 1?
     * @ANSWER: Starts numbering from 1, more intuitive than 0 for human-readable IDs
     */
    public WorkerRegistry() {
        this.workers = new ConcurrentHashMap<>();
        this.autoScaleCounter = new AtomicInteger(1);
    }

    /**
     * Registers a new worker node in the system.
     * 
     * @param worker worker node to be registered
     * @throws IllegalArgumentException if worker ID already exists
     * 
     * @INTERVIEW_QUESTION: Why prevent duplicate worker IDs?
     * @ANSWER: Ensures unique identification and prevents conflicts in task assignment
     * @INTERVIEW_QUESTION: Why not use putIfAbsent like TaskRegistry?
     * @ANSWER: Workers are typically registered during setup, duplicates indicate configuration errors
     */
    public void register(WorkerNode worker) {
        if (workers.containsKey(worker.getNodeId())) {
            throw new IllegalArgumentException("Worker already registered: " + worker.getNodeId());
        }
        workers.put(worker.getNodeId(), worker);
    }

    /**
     * Retrieves a worker node by its unique identifier.
     * 
     * @param nodeId unique worker identifier
     * @return the worker node with the given ID
     * @throws IllegalArgumentException if worker not found
     * 
     * @INTERVIEW_QUESTION: Why throw exception for missing workers?
     * @ANSWER: Worker references should always be valid; missing worker indicates system inconsistency
     */
    public WorkerNode getWorker(String nodeId) {
        WorkerNode worker = workers.get(nodeId);
        if (worker == null) {
            throw new IllegalArgumentException("Worker not found: " + nodeId);
        }
        return worker;
    }

    /**
     * Gets all worker nodes currently registered in the system.
     * 
     * @return list of all workers
     * 
     * @INTERVIEW_QUESTION: Why return a new ArrayList?
     * @ANSWER: Prevents external modification of internal state while providing full access
     */
    public List<WorkerNode> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }

    /**
     * Gets only the active worker nodes that can receive task assignments.
     * 
     * @return list of active workers
     * 
     * @INTERVIEW_QUESTION: Why filter only active workers?
     * @ANSWER: Inactive workers cannot receive tasks, filtering improves scheduler efficiency
     * @INTERVIEW_QUESTION: Why use stream API?
     * @ANSWER: Modern, readable approach for collection filtering and transformation
     */
    public List<WorkerNode> getActiveWorkers() {
        return workers.values().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());
    }

    /**
     * Gets workers filtered by their current status.
     * 
     * @param status status to filter by
     * @return list of workers with the specified status
     * 
     * @INTERVIEW_QUESTION: Why provide status-based filtering?
     * @ANSWER: Enables monitoring and management of workers in specific states
     */
    public List<WorkerNode> getWorkersByStatus(WorkerStatus status) {
        return workers.values().stream()
                .filter(worker -> worker.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Marks a worker as inactive and returns its running tasks for reassignment.
     * Used during worker failure scenarios.
     * 
     * @param nodeId worker identifier to mark as failed
     * @return list of tasks that were running on the failed worker
     * 
     * @INTERVIEW_QUESTION: Why return the list of affected tasks?
     * @ANSWER: Enables scheduler to reassign failed tasks to other available workers
     * @INTERVIEW_QUESTION: Why not remove the worker entirely?
     * @ANSWER: Worker might recover; keeping it allows for potential reactivation
     */
    public List<Task> markWorkerFailed(String nodeId) {
        WorkerNode worker = getWorker(nodeId);
        worker.setStatus(WorkerStatus.INACTIVE);
        return worker.releaseAllResources();
    }

    /**
     * Creates a new auto-scaled worker with default configuration.
     * 
     * @return the newly created worker node
     * 
     * @INTERVIEW_QUESTION: Why use fixed configuration for auto-scaled workers?
     * @ANSWER: Simplifies auto-scaling logic and provides predictable resource allocation
     * @INTERVIEW_QUESTION: Why generate IDs automatically?
     * @ANSWER: Ensures unique identification without requiring caller to specify IDs
     */
    public WorkerNode autoScaleWorker() {
        String nodeId = "W" + (workers.size() + autoScaleCounter.getAndIncrement());
        
        // Default configuration for auto-scaled workers
        WorkerNode worker = new WorkerNode(nodeId, 2, 4, 10);
        
        workers.put(nodeId, worker);
        return worker;
    }

    /**
     * Gets the current number of registered workers.
     * 
     * @return total worker count
     * 
     * @INTERVIEW_QUESTION: Why provide a count method?
     * @ANSWER: Efficient way to get cluster size without creating intermediate collections
     */
    public int getWorkerCount() {
        return workers.size();
    }

    /**
     * Gets the number of active workers.
     * 
     * @return count of active workers
     * 
     * @INTERVIEW_QUESTION: Why track active count separately?
     * @ANSWER: Quick way to assess available capacity without filtering entire collection
     */
    public int getActiveWorkerCount() {
        return (int) workers.values().stream()
                .filter(WorkerNode::isActive)
                .count();
    }

    /**
     * Checks if a worker with the given ID is registered.
     * 
     * @param nodeId worker identifier to check
     * @return true if worker exists, false otherwise
     * 
     * @INTERVIEW_QUESTION: Why provide a contains method?
     * @ANSWER: Non-throwing way to check for worker existence, useful for validation
     */
    public boolean containsWorker(String nodeId) {
        return workers.containsKey(nodeId);
    }

    /**
     * Removes all workers and resets the auto-scale counter.
     * 
     * @INTERVIEW_QUESTION: Why reset the auto-scale counter?
     * @ANSWER: Ensures clean state for testing or system restart scenarios
     */
    public void clear() {
        workers.clear();
        autoScaleCounter.set(1);
    }

    /**
     * Reactivates a previously inactive worker.
     * 
     * @param nodeId worker identifier to reactivate
     * @return true if worker was reactivated, false if not found or already active
     * 
     * @INTERVIEW_QUESTION: Why provide a reactivate method?
     * @ANSWER: Enables worker recovery after temporary failures or maintenance
     */
    public boolean reactivateWorker(String nodeId) {
        WorkerNode worker = workers.get(nodeId);
        if (worker != null && !worker.isActive()) {
            worker.setStatus(WorkerStatus.ACTIVE);
            return true;
        }
        return false;
    }
}
