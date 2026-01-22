package com.scheduler.enums;

/**
 * Defines the operational status of worker nodes in the distributed scheduler.
 * 
 * INTERVIEW TALKING POINTS:
 * - Worker lifecycle management: Tracks availability for task assignment
 * - Fault tolerance: INACTIVE status enables failure detection and recovery
 * - Load balancing: Only ACTIVE workers receive new task assignments
 * - Health monitoring: Status changes reflect worker health and availability
 * 
 * @INTERVIEW_QUESTION: Why track worker status?
 * @ANSWER: Enables fault tolerance, prevents assigning tasks to failed workers, and supports auto-scaling
 * @INTERVIEW_QUESTION: Why only two states?
 * @ANSWER: Simplicity - ACTIVE means available for work, INACTIVE means unavailable (failed, maintenance, etc.)
 */
public enum WorkerStatus {
    /**
     * Worker is active and available for task assignment.
     * Normal operating state where the worker can receive and execute tasks.
     * Only workers with this status are considered for new task assignments.
     */
    ACTIVE,
    
    /**
     * Worker is inactive and unavailable for task assignment.
     * Used when worker fails, is under maintenance, or is being decommissioned.
     * Tasks assigned to this worker are reassigned to other active workers.
     */
    INACTIVE
}
