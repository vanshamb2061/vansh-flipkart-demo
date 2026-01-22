package com.scheduler.enums;

/**
 * Represents the complete lifecycle states of a task in the distributed scheduler.
 * 
 * INTERVIEW TALKING POINTS:
 * - State machine pattern: Tasks transition through well-defined states
 * - Finite states: Ensures predictable behavior and easier debugging
 * - State validation: Prevents invalid state transitions
 * - Monitoring: Each state represents a different phase for observability
 * 
 * @INTERVIEW_QUESTION: Why use an enum instead of constants?
 * @ANSWER: Type safety, compile-time checking, and ability to add behavior to states
 * @INTERVIEW_QUESTION: Why track task states?
 * @ANSWER: Enables monitoring, debugging, fault tolerance, and proper resource management
 */
public enum TaskStatus {
    /**
     * Task is queued and waiting for worker assignment.
     * Initial state after submission when no suitable worker is immediately available.
     * Task is stored in priority queue and will be assigned when resources become available.
     */
    QUEUED,
    
    /**
     * Task has been assigned to a worker and is currently executing.
     * Resources have been allocated and the task is running on a specific worker node.
     * This is the active state where the task is consuming CPU and memory resources.
     */
    ASSIGNED,
    
    /**
     * Task has completed successfully.
     * Final state indicating successful execution.
     * Resources have been released and task is no longer consuming system resources.
     */
    COMPLETED,
    
    /**
     * Task was cancelled by user request or system intervention.
     * Terminal state for tasks that were intentionally stopped.
     * Resources are properly cleaned up and task is not retried.
     */
    CANCELLED,
    
    /**
     * Task failed after multiple retry attempts.
     * Terminal state indicating permanent failure.
     * Used when tasks exceed retry limits or encounter unrecoverable errors.
     */
    FAILED
}
