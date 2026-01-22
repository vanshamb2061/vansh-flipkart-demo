package com.scheduler.queue;

import com.scheduler.model.Task;
import com.scheduler.enums.Priority;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class TaskQueue {
    
    private final Queue<Task> highPriorityQueue;
    private final Queue<Task> mediumPriorityQueue;
    private final Queue<Task> lowPriorityQueue;
    private final ReentrantLock lock;

    public TaskQueue() {
        this.highPriorityQueue = new ConcurrentLinkedQueue<>();
        this.mediumPriorityQueue = new ConcurrentLinkedQueue<>();
        this.lowPriorityQueue = new ConcurrentLinkedQueue<>();
        this.lock = new ReentrantLock();
    }

    public void enqueue(Task task) {
        lock.lock();
        try {
            getQueueForPriority(task.getPriority()).add(task);
        } finally {
            lock.unlock();
        }
    }

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

    public boolean remove(Task task) {
        lock.lock();
        try {
            return getQueueForPriority(task.getPriority()).remove(task);
        } finally {
            lock.unlock();
        }
    }

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

    public boolean isEmpty() {
        return size() == 0;
    }

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

    private Queue<Task> getQueueForPriority(Priority priority) {
        return switch (priority) {
            case HIGH -> highPriorityQueue;
            case MEDIUM -> mediumPriorityQueue;
            case LOW -> lowPriorityQueue;
        };
    }
}
