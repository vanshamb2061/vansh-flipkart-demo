package com.scheduler.service;

import com.scheduler.enums.Priority;
import com.scheduler.enums.TaskStatus;
import com.scheduler.model.Task;
import com.scheduler.model.WorkerNode;
import com.scheduler.queue.TaskQueue;
import com.scheduler.registry.TaskRegistry;
import com.scheduler.registry.WorkerRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SchedulerService {
    
    private final TaskQueue queue;
    private final TaskRegistry taskRegistry;
    private final WorkerRegistry workerRegistry;

    public SchedulerService(TaskQueue queue, 
                           TaskRegistry taskRegistry,
                           WorkerRegistry workerRegistry) {
        this.queue = queue;
        this.taskRegistry = taskRegistry;
        this.workerRegistry = workerRegistry;
    }

    public void submitTask(Task task, long currentTime) {
        taskRegistry.register(task);
        tryAssignTask(task, currentTime);
    }

    public boolean tryAssignTask(Task task, long currentTime) {
        List<WorkerNode> workers = workerRegistry.getActiveWorkers();
        Optional<WorkerNode> bestWorker = findFastestWorker(task, workers);
        
        if (bestWorker.isPresent()) {
            assignToWorker(task, bestWorker.get(), currentTime);
            return true;
        } else {
            // Always add to queue if no worker available
            task.setStatus(TaskStatus.QUEUED);
            queue.enqueue(task);
            return false;
        }
    }

    private Optional<WorkerNode> findFastestWorker(Task task, List<WorkerNode> workers) {
        return workers.stream()
                .filter(WorkerNode::isActive)
                .filter(worker -> worker.canAccommodate(
                        task.getCpuRequirement(), 
                        task.getMemoryRequirement()))
                .max(Comparator.comparingInt(WorkerNode::getProcessingSpeed));
    }

    private void assignToWorker(Task task, WorkerNode worker, long currentTime) {
        if (worker.allocateResources(task)) {
            task.setStatus(TaskStatus.ASSIGNED);
            task.setAssignedTo(worker.getNodeId());
            task.setStartTime(currentTime);
            queue.remove(task);
        }
    }

    public List<Task> processCompletedTasks(long currentTime) {
        List<Task> completed = new ArrayList<>();
        
        for (WorkerNode worker : workerRegistry.getActiveWorkers()) {
            List<Task> toComplete = new ArrayList<>();
            
            for (Task task : worker.getRunningTasks()) {
                if (task.isExecutionComplete(currentTime)) {
                    toComplete.add(task);
                }
            }
            
            for (Task task : toComplete) {
                completeTask(task, worker);
                completed.add(task);
            }
        }
        
        assignQueued(currentTime);
        return completed;
    }

    private void completeTask(Task task, WorkerNode worker) {
        task.setStatus(TaskStatus.COMPLETED);
        worker.releaseResources(task);
    }

    public void assignQueued(long currentTime) {
        // Process tasks in priority order using the queue's dequeue method
        while (true) {
            Optional<Task> taskOpt = queue.dequeue();
            if (taskOpt.isEmpty()) {
                break;
            }
            
            Task task = taskOpt.get();
            List<WorkerNode> workers = workerRegistry.getActiveWorkers();
            Optional<WorkerNode> bestWorker = findFastestWorker(task, workers);
            
            if (bestWorker.isPresent()) {
                assignToWorker(task, bestWorker.get(), currentTime);
            } else {
                // Put it back in queue if no worker available
                queue.enqueue(task);
                break;
            }
        }
    }

    public List<Task> handleWorkerFailure(String workerId, long currentTime) {
        List<Task> affected = workerRegistry.markWorkerFailed(workerId);
        List<Task> reassigned = new ArrayList<>();
        
        for (Task task : affected) {
            if (task.getStatus() == TaskStatus.ASSIGNED) {
                task.resetForReassignment();
                task.incrementRetryCount();
                
                if (tryAssignTask(task, currentTime)) {
                    reassigned.add(task);
                }
            }
        }
        
        return reassigned;
    }

    private void timeoutTask(Task task, WorkerNode worker) {
        worker.releaseResources(task);
        task.resetForReassignment();
        task.incrementRetryCount();
        queue.enqueue(task);
    }

    public void simulateTaskTimeout(String taskId, long elapsedTime, long currentTime) {
        Task task = taskRegistry.getTask(taskId);
        
        if (task.getStatus() != TaskStatus.ASSIGNED) {
            return;
        }
        
        String workerId = task.getAssignedTo();
        WorkerNode worker = workerRegistry.getWorker(workerId);
        
        long timeoutThreshold = (long) (task.getExecutionTime() * 1.2);
        
        if (elapsedTime >= timeoutThreshold) {
            timeoutTask(task, worker);
            // Update currentTime to reflect the elapsed time for proper reassignment
            assignQueued(currentTime + elapsedTime);
        }
    }

    public boolean cancelTask(String taskId, long currentTime) {
        Task task = taskRegistry.getTask(taskId);
        
        switch (task.getStatus()) {
            case QUEUED:
                queue.remove(task);
                task.setStatus(TaskStatus.CANCELLED);
                return true;
                
            case ASSIGNED:
                String workerId = task.getAssignedTo();
                WorkerNode worker = workerRegistry.getWorker(workerId);
                worker.releaseResources(task);
                task.setStatus(TaskStatus.CANCELLED);
                task.setAssignedTo(null);
                assignQueued(currentTime);
                return true;
                
            case COMPLETED:
            case CANCELLED:
            case FAILED:
                return false;
                
            default:
                return false;
        }
    }

    public WorkerNode autoScale(long currentTime) {
        if (queue.isEmpty()) {
            return null;
        }
        
        WorkerNode newWorker = workerRegistry.autoScaleWorker();
        
        // Try to assign queued tasks to the new worker
        assignQueued(currentTime);
        
        return newWorker;
    }
}
