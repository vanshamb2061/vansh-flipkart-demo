package com.scheduler.registry;

import com.scheduler.enums.WorkerStatus;
import com.scheduler.model.Task;
import com.scheduler.model.WorkerNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WorkerRegistry {
    
    private final Map<String, WorkerNode> workers;
    private final AtomicInteger autoScaleCounter;

    public WorkerRegistry() {
        this.workers = new ConcurrentHashMap<>();
        this.autoScaleCounter = new AtomicInteger(1);
    }

    public void register(WorkerNode worker) {
        if (workers.containsKey(worker.getNodeId())) {
            throw new IllegalArgumentException("Worker already registered: " + worker.getNodeId());
        }
        workers.put(worker.getNodeId(), worker);
    }

    public WorkerNode getWorker(String nodeId) {
        WorkerNode worker = workers.get(nodeId);
        if (worker == null) {
            throw new IllegalArgumentException("Worker not found: " + nodeId);
        }
        return worker;
    }

    public List<WorkerNode> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }

    public List<WorkerNode> getActiveWorkers() {
        return workers.values().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());
    }

    public List<Task> markWorkerFailed(String nodeId) {
        WorkerNode worker = getWorker(nodeId);
        worker.setStatus(WorkerStatus.INACTIVE);
        return worker.releaseAllResources();
    }

    public WorkerNode autoScaleWorker() {
        String nodeId = "W" + (workers.size() + autoScaleCounter.getAndIncrement());
        
        WorkerNode worker = new WorkerNode(nodeId, 2, 4, 10);
        
        workers.put(nodeId, worker);
        return worker;
    }
    public void clear() {
        workers.clear();
        autoScaleCounter.set(1);
    }
}
