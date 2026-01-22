package com.scheduler;

import com.scheduler.enums.Priority;
import com.scheduler.enums.TaskStatus;
import com.scheduler.model.Task;
import com.scheduler.model.WorkerNode;
import com.scheduler.queue.TaskQueue;
import com.scheduler.registry.TaskRegistry;
import com.scheduler.registry.WorkerRegistry;
import com.scheduler.service.SchedulerService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DistributedTaskScheduler {
    
    private final TaskQueue queue;
    private final TaskRegistry taskRegistry;
    private final WorkerRegistry workerRegistry;
    private final SchedulerService service;
    private long currentTime;

    public DistributedTaskScheduler() {
        this.queue = new TaskQueue();
        this.taskRegistry = new TaskRegistry();
        this.workerRegistry = new WorkerRegistry();
        this.currentTime = 0;
        this.service = new SchedulerService(
                queue, taskRegistry, workerRegistry);
    }

    public void registerWorker(String nodeId, int cpu, int memory, int speed) {
        WorkerNode worker = new WorkerNode(nodeId, cpu, memory, speed);
        workerRegistry.register(worker);
        service.assignQueued(currentTime);
    }

    public List<WorkerInfo> listWorkers() {
        return workerRegistry.getAllWorkers().stream()
                .map(WorkerInfo::from)
                .collect(Collectors.toList());
    }

    public void simulateWorkerFailure(String nodeId) {
        service.handleWorkerFailure(nodeId, currentTime);
    }

    public void submitTask(String taskId, int cpu, int memory, int executionTime, Priority priority) {
        Task task = new Task(taskId, cpu, memory, executionTime, priority);
        service.submitTask(task, currentTime);
    }

    public void submitTask(TaskConfig config) {
        submitTask(config.taskId, config.cpu, config.memory, config.executionTime, config.priority);
    }

    public void submitTasks(List<TaskConfig> configs) {
        for (TaskConfig config : configs) {
            submitTask(config);
        }
    }

    public List<TaskInfo> listTasks() {
        return taskRegistry.getAllTasks().stream()
                .map(TaskInfo::from)
                .collect(Collectors.toList());
    }

    public boolean cancelTask(String taskId) {
        return service.cancelTask(taskId, currentTime);
    }

    public void waitFor(long seconds) {
        currentTime += seconds;
        service.processCompletedTasks(currentTime);
    }

    public void simulateTaskTimeout(String taskId, long elapsedTime) {
        service.simulateTaskTimeout(taskId, elapsedTime, currentTime);
    }

    public WorkerInfo autoScale() {
        WorkerNode worker = service.autoScale(currentTime);
        return worker != null ? WorkerInfo.from(worker) : null;
    }

    public static class TaskConfig {
        public String taskId;
        public int cpu;
        public int memory;
        public int executionTime;
        public Priority priority = Priority.MEDIUM;

        public TaskConfig(String taskId, int cpu, int memory, int executionTime) {
            this.taskId = taskId;
            this.cpu = cpu;
            this.memory = memory;
            this.executionTime = executionTime;
        }

        public TaskConfig(String taskId, int cpu, int memory, int executionTime, Priority priority) {
            this(taskId, cpu, memory, executionTime);
            this.priority = priority;
        }
    }

    public static class WorkerInfo {
        public String nodeId;
        public int cpu;
        public int memory;
        public int speed;
        public String status;

        static WorkerInfo from(WorkerNode worker) {
            WorkerInfo info = new WorkerInfo();
            info.nodeId = worker.getNodeId();
            info.cpu = worker.getTotalCpu();
            info.memory = worker.getTotalMemory();
            info.speed = worker.getProcessingSpeed();
            info.status = worker.getStatus().name().toLowerCase();
            return info;
        }

        @Override
        public String toString() {
            return String.format("{ nodeId: \"%s\", cpu: %d, memory: %d, speed: %d, status: \"%s\" }",
                    nodeId, cpu, memory, speed, status);
        }
    }

    public static class TaskInfo {
        public String taskId;
        public String status;
        public String assignedTo;

        static TaskInfo from(Task task) {
            TaskInfo info = new TaskInfo();
            info.taskId = task.getTaskId();
            info.status = task.getStatus().name().toLowerCase();
            info.assignedTo = task.getAssignedTo();
            return info;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{ taskId: \"").append(taskId).append("\"");
            sb.append(", status: \"").append(status).append("\"");
            if (assignedTo != null && !status.equals("queued") && !status.equals("cancelled")) {
                sb.append(", assignedTo: \"").append(assignedTo).append("\"");
            }
            sb.append(" }");
            return sb.toString();
        }
    }
}
