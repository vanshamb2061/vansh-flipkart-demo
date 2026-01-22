package com.scheduler.model;

import com.scheduler.enums.WorkerStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorkerNode {
    private final String nodeId;
    private final int totalCpu;
    private final int totalMemory;
    private final int processingSpeed;
    
    private WorkerStatus status;
    private int usedCpu;
    private int usedMemory;
    private final Map<String, Task> runningTasks;

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

    public String getNodeId() {
        return nodeId;
    }

    public int getTotalCpu() {
        return totalCpu;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public int getProcessingSpeed() {
        return processingSpeed;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public int getAvailableCpu() {
        return totalCpu - usedCpu;
    }

    public int getAvailableMemory() {
        return totalMemory - usedMemory;
    }

    public Collection<Task> getRunningTasks() {
        return Collections.unmodifiableCollection(runningTasks.values());
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == WorkerStatus.ACTIVE;
    }

    public boolean canAccommodate(int cpuRequired, int memoryRequired) {
        return isActive() && 
               getAvailableCpu() >= cpuRequired && 
               getAvailableMemory() >= memoryRequired;
    }

    public boolean allocateResources(Task task) {
        if (!canAccommodate(task.getCpuRequirement(), task.getMemoryRequirement())) {
            return false;
        }
        
        usedCpu += task.getCpuRequirement();
        usedMemory += task.getMemoryRequirement();
        runningTasks.put(task.getTaskId(), task);
        return true;
    }

    public void releaseResources(Task task) {
        if (runningTasks.remove(task.getTaskId()) != null) {
            usedCpu = Math.max(0, usedCpu - task.getCpuRequirement());
            usedMemory = Math.max(0, usedMemory - task.getMemoryRequirement());
        }
    }

    public List<Task> releaseAllResources() {
        List<Task> tasks = new ArrayList<>(runningTasks.values());
        runningTasks.clear();
        usedCpu = 0;
        usedMemory = 0;
        return tasks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerNode that = (WorkerNode) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return String.format("{ nodeId: \"%s\", cpu: %d, memory: %d, speed: %d, status: \"%s\" }",
                nodeId, totalCpu, totalMemory, processingSpeed, status.name().toLowerCase());
    }

}
