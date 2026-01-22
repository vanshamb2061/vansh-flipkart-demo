package com.scheduler.enums;

/**
 * Defines the priority levels for tasks in the distributed scheduler.
 * 
 * INTERVIEW TALKING POINTS:
 * - Why use enums: Type safety, compile-time checking, better than constants
 * - Priority weighting: Higher numbers = higher priority for easy comparison
 * - Extensibility: Easy to add new priority levels (URGENT, CRITICAL)
 * - Design decision: Three levels provide good balance between simplicity and functionality
 * 
 * @INTERVIEW_QUESTION: Why not use integers directly?
 * @ANSWER: Enums provide type safety, prevent invalid values, and allow methods like getWeight()
 */
public enum Priority {
    /**
     * High priority tasks - processed first
     * Used for critical operations, user-facing features, or time-sensitive work
     */
    HIGH(3),
    
    /**
     * Medium priority tasks - normal processing order
     * Default priority for most tasks when no specific priority is specified
     */
    MEDIUM(2),
    
    /**
     * Low priority tasks - processed when no higher priority tasks exist
     * Used for background processing, maintenance tasks, or batch operations
     */
    LOW(1);

    private final int weight;

    /**
     * Constructor for priority levels.
     * 
     * @param weight Numerical weight for comparison (higher = more important)
     * 
     * @INTERVIEW_QUESTION: Why store weight separately?
     * @ANSWER: Allows for flexible priority comparison and potential for dynamic priority adjustment
     */
    Priority(int weight) {
        this.weight = weight;
    }

    /**
     * Gets the numerical weight of this priority level.
     * 
     * @return weight value for comparison operations
     * 
     * @INTERVIEW_QUESTION: Why expose weight as a method?
     * @ANSWER: Encapsulates the internal representation while allowing comparison logic
     */
    public int getWeight() {
        return weight;
    }
}
