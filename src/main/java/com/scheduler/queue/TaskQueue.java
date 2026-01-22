package com.scheduler.queue;

import com.scheduler.model.Task;
import com.scheduler.enums.Priority;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Priority-based task queue that manages multiple queues for different priority levels.
 * 
 * INTERVIEW TALKING POINTS:
 * - Multi-queue design: Separate queues for each priority level ensure O(1) priority operations
 * - Thread safety: Uses ReentrantLock for atomic operations across multiple queues
 * - FIFO ordering: Maintains fairness within each priority level
 * - Priority semantics: HIGH > MEDIUM > LOW with strict ordering
 * - Concurrent access: ConcurrentLinkedQueue for lock-free individual queue operations
 * 
 * @INTERVIEW_QUESTION: Why use separate queues instead of a priority heap?
 * @ANSWER: O(1) enqueue/dequeue for each priority, simpler implementation, and guaranteed priority ordering
 * @INTERVIEW_QUESTION: Why use ReentrantLock instead of synchronized?
 * @ANSWER: More flexible locking, better performance, and ability to interrupt waiting threads
 */
public class TaskQueue {
    
    // Separate queues for each priority level
    private final Queue<Task> highPriorityQueue;
    private final Queue<Task> mediumPriorityQueue;
    private final Queue<Task> lowPriorityQueue;
    
    // Lock for atomic operations across all queues
    private final ReentrantLock lock;

    /**
     * Creates a new priority-based task queue.
     * 
     * @INTERVIEW_QUESTION: Why use ConcurrentLinkedQueue for individual queues?
     * @ANSWER: Lock-free operations for individual queue access, combined with external lock for coordination
     * @INTERVIEW_QUESTION: Why initialize queues in constructor?
     * @ANSWER: Ensures queues are always available and prevents null pointer exceptions
     */
    public TaskQueue() {
        this.highPriorityQueue = new ConcurrentLinkedQueue<>();
        this.mediumPriorityQueue = new ConcurrentLinkedQueue<>();
        this.lowPriorityQueue = new ConcurrentLinkedQueue<>();
        this.lock = new ReentrantLock();
    }

    /**
     * Adds a task to the appropriate priority queue.
     * 
     * @param task task to be queued
     * 
     * @INTERVIEW_QUESTION: Why lock the entire enqueue operation?
     * @ANSWER: Ensures atomicity when selecting the correct queue and prevents race conditions
     * @INTERVIEW_QUESTION: Why use try-finally for lock management?
     * @ANSWER: Guarantees lock is always released, even if an exception occurs
     */
    public void enqueue(Task task) {
        lock.lock();
        try {
            getQueueForPriority(task.getPriority()).add(task);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes and returns the highest priority task available.
     * 
     * @return Optional containing the task, or empty if all queues are empty
     * 
     * @INTERVIEW_QUESTION: Why check queues in priority order?
     * @ANSWER: Ensures high-priority tasks are always processed before lower priority ones
     * @INTERVIEW_QUESTION: Why return Optional instead of null?
     * @ANSWER: Explicit indication of empty queue, prevents NPE, and follows modern Java practices
     */
    public Optional<Task> dequeue() {
        lock.lock();
        try {
            Task task = highPriorityQueue.poll();
            if (task == null) {
                task = mediumPriorityQueue.poll();
            }
            if (task == null) {
                task = lowPriorityQueue.poll();
            }
            return Optional.ofNullable(task);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a specific task from its priority queue.
     * 
     * @param task task to be removed
     * @return true if task was removed, false if not found
     * 
     * @INTERVIEW_QUESTION: Why is remove operation important?
     * @ANSWER: Enables task cancellation and queue management without disrupting other tasks
     * @INTERVIEW_QUESTION: Why return boolean?
     * @ANSWER: Allows caller to know if removal was successful for error handling
     */
    public boolean remove(Task task) {
        lock.lock();
        try {
            return getQueueForPriority(task.getPriority()).remove(task);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the total number of tasks across all priority queues.
     * 
     * @return total queue size
     * 
     * @INTERVIEW_QUESTION: Why calculate size dynamically?
     * @ANSWER: Always returns current state, avoids synchronization issues with cached values
     */
    public int size() {
        lock.lock();
        try {
            return highPriorityQueue.size() + 
                   mediumPriorityQueue.size() + 
                   lowPriorityQueue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if all priority queues are empty.
     * 
     * @return true if no tasks are queued, false otherwise
     * 
     * @INTERVIEW_QUESTION: Why have a separate isEmpty method?
     * @ANSWER: More efficient than checking size() == 0, and provides clearer intent
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Removes all tasks from all priority queues.
     * 
     * @INTERVIEW_QUESTION: Why provide a clear method?
     * @ANSWER: Useful for system shutdown, testing, and queue reset scenarios
     */
    public void clear() {
        lock.lock();
        try {
            highPriorityQueue.clear();
            mediumPriorityQueue.clear();
            lowPriorityQueue.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the appropriate queue for a given priority level.
     * 
     * @param priority task priority
     * @return queue for that priority level
     * 
     * @INTERVIEW_QUESTION: Why use switch expression here?
     * @ANSWER: Modern Java syntax, more concise, and ensures all priority levels are handled
     */
    private Queue<Task> getQueueForPriority(Priority priority) {
        return switch (priority) {
            case HIGH -> highPriorityQueue;
            case MEDIUM -> mediumPriorityQueue;
            case LOW -> lowPriorityQueue;
        };
    }
}
