# Distributed Task Scheduler

A robust distributed task scheduling system built in Java that handles task assignment, worker management, fault tolerance, and auto-scaling.

## System Architecture

The system follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│                  (DistributedTaskScheduler)                 │
├─────────────────────────────────────────────────────────────┤
│                    Service Layer                            │
│                   (SchedulerService)                        │
├─────────────────────────────────────────────────────────────┤
│                    Registry Layer                           │
│              (TaskRegistry, WorkerRegistry)                 │
├─────────────────────────────────────────────────────────────┤
│                    Queue Layer                              │
│                    (TaskQueue)                              │
├─────────────────────────────────────────────────────────────┤
│                    Model Layer                              │
│                (Task, WorkerNode)                           │
└─────────────────────────────────────────────────────────────┘
```

The architecture implements:
- **Event-driven scheduling** with time-based task execution
- **Priority-based queuing** with multi-level priority queues
- **Resource-aware assignment** considering CPU, memory, and processing speed
- **Fault tolerance** with worker failure detection and task reassignment
- **Auto-scaling** capabilities for dynamic resource management

## Core Components

### 1. DistributedTaskScheduler
- **Main facade class** providing the public API
- **Coordinates** between all components
- **Manages** simulation time and state
- **Provides** task and worker information methods

### 2. SchedulerService
- **Core business logic** for task assignment and execution
- **Implements** the task assignment algorithm
- **Handles** worker failures and task timeouts
- **Manages** task lifecycle transitions

### 3. TaskQueue
- **Priority-based queue** with three levels (HIGH, MEDIUM, LOW)
- **Thread-safe** implementation using ReentrantLock
- **FIFO ordering** within each priority level
- **Efficient** enqueue/dequeue operations

### 4. TaskRegistry & WorkerRegistry
- **Centralized storage** for tasks and workers
- **Thread-safe** operations using ConcurrentHashMap
- **Provides** lookup and filtering capabilities
- **Manages** worker auto-scaling

### 5. Model Classes
- **Task**: Represents work units with resource requirements
- **WorkerNode**: Represents compute resources with capacity tracking

## Task Assignment Algorithm

The system implements a **fastest-worker-first** algorithm with resource constraints:

### Algorithm Steps:
1. **Resource Filtering**: Filter active workers that can accommodate task requirements
2. **Speed Ranking**: Select worker with highest processing speed
3. **Resource Allocation**: Allocate CPU and memory to the task
4. **Queue Fallback**: Queue task if no suitable worker available

### Pseudocode:
```java
function assignTask(task):
    availableWorkers = filter(activeWorkers, canAccommodate(task))
    if availableWorkers.isEmpty():
        queue.enqueue(task)
        return false
    
    bestWorker = max(availableWorkers, processingSpeed)
    allocateResources(bestWorker, task)
    return true
```

### Priority Handling:
- **HIGH priority** tasks are dequeued first
- **MEDIUM priority** tasks are processed after all HIGH tasks
- **LOW priority** tasks are processed last
- **FIFO ordering** is maintained within each priority level

## Task Lifecycle

Tasks progress through the following states:

```
┌─────────┐    Submit    ┌─────────┐    Assign    ┌─────────┐
│ QUEUED  │ ──────────► │ ASSIGNED│ ──────────► │COMPLETED│
└─────────┘             └─────────┘             └─────────┘
    │                       │                       │
    │ Cancel/Timeout         │ Timeout/Failure        │
    ▼                       ▼                       ▼
┌─────────┐             ┌─────────┐
│CANCELLED│             │ FAILED  │
└─────────┘             └─────────┘
```

### State Transitions:
- **QUEUED → ASSIGNED**: When suitable worker becomes available
- **ASSIGNED → COMPLETED**: When execution time elapses
- **ASSIGNED → QUEUED**: When worker fails or task times out
- **QUEUED/ASSIGNED → CANCELLED**: When cancellation is requested
- **ASSIGNED → FAILED**: After multiple retry attempts

## Key Design Decisions

### 1. Priority-Based Multi-Queue Design
- **Decision**: Separate queues for each priority level
- **Rationale**: Ensures priority guarantees while maintaining simplicity
- **Trade-off**: Additional memory overhead vs. O(1) priority operations

### 2. Fastest-Worker-First Assignment
- **Decision**: Prioritize processing speed over load balancing
- **Rationale**: Minimizes task completion time
- **Trade-off**: Potential resource starvation on slower workers

### 3. Immutable Task Requirements
- **Decision**: Task resource requirements are final
- **Rationale**: Simplifies resource management and prevents conflicts
- **Trade-off**: Less flexibility for dynamic resource adjustment

### 4. Thread-Safe Components
- **Decision**: Use ConcurrentHashMap and ReentrantLock
- **Rationale**: Support concurrent access in distributed environment
- **Trade-off**: Slight performance overhead vs. safety guarantees

### 5. Auto-Scaling with Fixed Configuration
- **Decision**: New workers get standardized resources (2 CPU, 4 Memory, 10 Speed)
- **Rationale**: Simplifies auto-scaling logic
- **Trade-off**: Less adaptive to varying workload patterns

## Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                DistributedTaskScheduler                    │
│─────────────────────────────────────────────────────────────│
│ - queue: TaskQueue                                         │
│ - taskRegistry: TaskRegistry                               │
│ - workerRegistry: WorkerRegistry                           │
│ - service: SchedulerService                                │
│ - currentTime: long                                        │
│─────────────────────────────────────────────────────────────│
│ + registerWorker(nodeId, cpu, memory, speed)              │
│ + submitTask(taskId, cpu, memory, time, priority)         │
│ + cancelTask(taskId)                                       │
│ + simulateWorkerFailure(nodeId)                             │
│ + autoScale()                                              │
│ + waitFor(seconds)                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  SchedulerService                           │
│─────────────────────────────────────────────────────────────│
│ - queue: TaskQueue                                         │
│ - taskRegistry: TaskRegistry                               │
│ - workerRegistry: WorkerRegistry                           │
│─────────────────────────────────────────────────────────────│
│ + submitTask(task, currentTime)                            │
│ + tryAssignTask(task, currentTime)                         │
│ + processCompletedTasks(currentTime)                        │
│ + handleWorkerFailure(workerId, currentTime)              │
│ + cancelTask(taskId, currentTime)                          │
│ + autoScale(currentTime)                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   TaskQueue     │  │  TaskRegistry   │  │ WorkerRegistry  │
│─────────────────│  │─────────────────│  │─────────────────│
│ - highPriority  │  │ - tasks: Map    │  │ - workers: Map  │
│ - mediumPriority│  │─────────────────│  │ - autoScaleCtr  │
│ - lowPriority   │  │ + register(task)│  │─────────────────│
│ - lock: Reentrant│ │ + getTask(id)   │  │ + register(worker)│
│─────────────────│  │ + getAllTasks() │  │ + getWorker(id)  │
│ + enqueue(task) │  └─────────────────┘  │ + getActive()    │
│ + dequeue()     │                       │ + markFailed(id)│
│ + remove(task)  │                       │ + autoScale()    │
│ + size()        │                       └─────────────────┘
│ + isEmpty()     │
└─────────────────┘
                              │
                              ▼
┌─────────────────┐  ┌─────────────────┐
│      Task       │  │   WorkerNode    │
│─────────────────│  │─────────────────│
│ - taskId: String│  │ - nodeId: String│
│ - cpuReq: int   │  │ - totalCpu: int │
│ - memReq: int   │  │ - totalMem: int │
│ - execTime: int│  │ - speed: int    │
│ - priority: enum│  │ - status: enum  │
│ - status: enum  │  │ - usedCpu: int  │
│ - assignedTo: S │  │ - usedMem: int  │
│ - startTime: lng│  │ - runningTasks  │
│ - retryCount: i │  │─────────────────│
│─────────────────│  │ + canAccommodate│
│ + isExecutionC │  │ + allocate(task)│
│ + resetForReass │  │ + release(task) │
│ + incrementRetry│  │ + releaseAll()  │
└─────────────────┘  └─────────────────┘
```

## Sequence Diagrams

### Task Submission and Assignment

```
Client    DistributedTaskScheduler    SchedulerService    TaskRegistry    TaskQueue    WorkerNode
  │                │                        │                 │              │             │
  │ submitTask()   │                        │                 │              │             │
  ├──────────────►│                        │                 │              │             │
  │                │ submitTask(task, time) │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │                        │ register(task) │              │             │
  │                │                        ├──────────────►│              │             │
  │                │                        │                 │              │             │
  │                │ tryAssignTask()        │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │ findFastestWorker()    │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │                        │                 │              │             │
  │                │                        │                 │              │             │
  │                │    WORKER AVAILABLE    │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │ allocateResources()    │                 │              │             │
  │                ├────────────────────────────────────────────────────────►│             │
  │                │                        │                 │              │             │
  │                │ setStatus(ASSIGNED)    │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │ setAssignedTo()        │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │ setStartTime()         │                 │              │             │
  │                ├──────────────────────►│                 │              │             │
  │                │                        │                 │              │             │
  │                │◄───────────────────────│                 │              │             │
  │◄───────────────│                        │                 │              │             │
```

### Worker Failure and Task Reassignment

```
Client    DistributedTaskScheduler    SchedulerService    WorkerRegistry    WorkerNode    TaskQueue    Task
  │                │                        │                 │              │             │        │
  │ simulateFail() │                        │                 │              │             │        │
  ├──────────────►│                        │                 │              │             │        │
  │                │ handleWorkerFailure()  │                 │              │             │        │
  │                ├──────────────────────►│                 │              │             │        │
  │                │                        │ markFailed(id)  │              │             │        │
  │                │                        ├──────────────►│              │             │        │
  │                │                        │                 │ setStatus()  │             │        │
  │                │                        │                 ├────────────►│             │        │
  │                │                        │                 │ releaseAll() │             │        │
  │                │                        │                 ├────────────►│             │        │
  │                │                        │◄───────────────│              │             │        │
  │                │                        │ affectedTasks   │              │             │        │
  │                │◄───────────────────────│                 │              │             │        │
  │                │                        │                 │              │             │        │
  │                │    FOR EACH AFFECTED TASK               │              │             │        │
  │                ├──────────────────────►│                 │              │             │        │
  │                │ resetForReassignment()│                 │              │             │        │
  │                ├─────────────────────────────────────────────────────────────────────────►│
  │                │ incrementRetryCount() │                 │              │             │        │
  │                ├─────────────────────────────────────────────────────────────────────────►│
  │                │ tryAssignTask()        │                 │              │             │        │
  │                ├──────────────────────►│                 │              │             │        │
  │                │                        │                 │              │             │        │
  │                │    NEW WORKER AVAILABLE                 │              │             │        │
  │                ├──────────────────────────────────────────────────────────────────────►│
  │                │                        │                 │              │             │        │
  │                │◄───────────────────────│                 │              │             │        │
  │◄───────────────│                        │                 │              │             │        │
```

## Complexity Analysis

### Time Complexity

| Operation | Best Case | Average Case | Worst Case |
|-----------|-----------|--------------|------------|
| Task Submission | O(1) | O(log n) | O(n) |
| Task Assignment | O(1) | O(log m) | O(m) |
| Task Completion | O(k) | O(k) | O(k) |
| Worker Failure | O(p) | O(p) | O(p) |
| Task Cancellation | O(1) | O(1) | O(n) |

Where:
- n = total number of tasks
- m = number of active workers
- k = number of running tasks on a worker
- p = number of tasks on failed worker

### Space Complexity

| Component | Space Complexity |
|-----------|------------------|
| TaskRegistry | O(n) |
| WorkerRegistry | O(m) |
| TaskQueue | O(q) |
| WorkerNode | O(k) per worker |

Where:
- q = number of queued tasks
- k = number of tasks per worker

### Overall System Complexity
- **Time**: O(n + m) for most operations
- **Space**: O(n + m + q) total system memory

## Features Summary

### Core Features
- ✅ **Priority-based Task Scheduling** (HIGH, MEDIUM, LOW)
- ✅ **Resource-aware Assignment** (CPU, Memory constraints)
- ✅ **Fastest-worker-first Algorithm**
- ✅ **Worker Failure Detection** and Task Reassignment
- ✅ **Task Timeout Handling** with Automatic Retry
- ✅ **Task Cancellation** Support
- ✅ **Auto-scaling** of Worker Nodes
- ✅ **Parallel Task Execution**
- ✅ **Thread-safe Operations**

### Advanced Features
- ✅ **Multi-level Priority Queues**
- ✅ **Resource Utilization Tracking**
- ✅ **Task Retry Mechanism**
- ✅ **Real-time Status Monitoring**
- ✅ **Comprehensive Test Suite** (9 test cases)

## Project Structure

```
src/main/java/com/scheduler/
├── Main.java                          # Demo application with 9 test cases
├── DistributedTaskScheduler.java       # Main facade class
├── enums/
│   ├── Priority.java                  # Task priority enumeration
│   ├── TaskStatus.java               # Task status enumeration
│   └── WorkerStatus.java             # Worker status enumeration
├── model/
│   ├── Task.java                     # Task entity class
│   └── WorkerNode.java               # Worker node entity class
├── queue/
│   └── TaskQueue.java                # Priority-based task queue
├── registry/
│   ├── TaskRegistry.java             # Task storage and lookup
│   └── WorkerRegistry.java           # Worker storage and management
└── service/
    └── SchedulerService.java         # Core scheduling logic
```

## Test Cases Overview

The system includes 9 comprehensive test cases:

### Test Case 1: Worker Registration
- Registers multiple workers with different configurations
- Validates worker listing functionality

### Test Case 2: Task Submission and Assignment
- Submits tasks with varying resource requirements
- Demonstrates automatic assignment to best-fit workers

### Test Case 3: Task Execution and Completion
- Shows task lifecycle from submission to completion
- Demonstrates parallel execution capability

### Test Case 4: Worker Failure and Reassignment
- Simulates worker node failure
- Validates automatic task reassignment to available workers

### Test Case 5: Task Prioritization
- Submits tasks with different priorities
- Validates priority-based execution order

### Test Case 6: Auto-scaling
- Demonstrates automatic worker creation when queue is full
- Shows resource allocation to new workers

### Test Case 7: Task Timeout and Reassignment
- Simulates task execution timeout
- Validates automatic task retry and reassignment

### Test Case 8: Task Cancellation
- Cancels tasks in different states
- Validates resource cleanup and queue management

### Test Case 9: Complex Scenario
- Combines multiple features: parallel execution, failures, reassignment
- Demonstrates system behavior under complex conditions

## Interview Talking Points

### System Design Strengths
1. **Modular Architecture**: Clear separation of concerns with layered design
2. **Scalability**: Thread-safe components support concurrent operations
3. **Fault Tolerance**: Comprehensive failure handling with automatic recovery
4. **Resource Management**: Efficient resource allocation and utilization tracking
5. **Priority Handling**: Multi-level queue ensures QoS guarantees

### Technical Implementation Highlights
1. **Concurrent Programming**: Proper use of ConcurrentHashMap and ReentrantLock
2. **Algorithm Design**: Efficient fastest-worker-first assignment with O(log m) complexity
3. **State Management**: Clean state transitions with proper lifecycle handling
4. **Error Handling**: Robust exception handling and recovery mechanisms
5. **Testing Strategy**: Comprehensive test coverage for all major scenarios

### Performance Considerations
1. **Time Complexity**: Optimized for common operations (O(1) to O(log n))
2. **Space Efficiency**: Minimal memory overhead with efficient data structures
3. **Concurrency**: Lock-free operations where possible, minimal lock contention
4. **Resource Utilization**: Efficient CPU and memory allocation strategies

### Extensibility Features
1. **Plugin Architecture**: Easy to add new scheduling algorithms
2. **Configuration**: Flexible worker and task configurations
3. **Monitoring**: Built-in status tracking and reporting
4. **Auto-scaling**: Dynamic resource management capabilities

## Interview Questions & Answers

### Q1: How does the system handle task priorities?
**A**: The system uses three separate priority queues (HIGH, MEDIUM, LOW) with FIFO ordering within each priority. During task assignment, the dequeue method checks queues in priority order, ensuring high-priority tasks are always processed first. This design provides O(1) priority operations while maintaining fairness within each priority level.

### Q2: What happens when a worker node fails?
**A**: When a worker fails, the system:
1. Marks the worker as INACTIVE
2. Releases all resources allocated to that worker
3. Resets affected tasks to QUEUED state
4. Increments retry count for each affected task
5. Attempts to reassign tasks to other available workers
6. Queues tasks if no suitable workers are available

### Q3: How does the auto-scaling mechanism work?
**A**: Auto-scaling is triggered when the task queue is not empty. The system creates a new worker with standardized resources (2 CPU, 4 Memory, 10 Speed), registers it in the worker registry, and immediately attempts to assign queued tasks to the new worker. This ensures dynamic resource allocation based on workload demand.

### Q4: What's the time complexity of task assignment?
**A**: Task assignment has O(log m) average complexity where m is the number of active workers. The algorithm filters workers by resource constraints (O(m)), then finds the maximum by processing speed (O(log m) using stream operations). In the best case with available workers, it's O(1), and worst case is O(m) when all workers need to be checked.

### Q5: How does the system ensure thread safety?
**A**: The system uses multiple concurrency mechanisms:
1. **ConcurrentHashMap** for task and worker registries
2. **ReentrantLock** for queue operations
3. **AtomicInteger** for auto-scaling counter
4. **Immutable task requirements** to prevent conflicts
5. **Thread-safe collections** throughout the system

### Q6: What's the strategy for task timeout handling?
**A**: Tasks timeout after 120% of their expected execution time. When timeout occurs:
1. Resources are released from the current worker
2. Task is reset to QUEUED state
3. Retry count is incremented
4. Task is re-enqueued for reassignment
5. System attempts immediate reassignment to available workers

### Q7: How does the fastest-worker-first algorithm work?
**A**: The algorithm:
1. Filters active workers that can accommodate task requirements
2. Selects worker with maximum processing speed
3. Allocates resources and assigns task
4. Falls back to queuing if no suitable worker exists
This minimizes task completion time but may lead to resource imbalance, which is mitigated by the auto-scaling feature.

### Q8: What are the trade-offs in the current design?
**A**: Key trade-offs include:
1. **Speed vs. Load Balancing**: Fastest-worker-first may underutilize slower workers
2. **Memory vs. Performance**: Separate priority queues use more memory but provide O(1) priority operations
3. **Simplicity vs. Flexibility**: Fixed auto-scaling configuration is simple but less adaptive
4. **Consistency vs. Availability**: Strong consistency with locks vs. eventual consistency

### Q9: How would you extend this system for production use?
**A**: Production enhancements would include:
1. **Distributed Coordination**: Using Zookeeper or etcd for cluster management
2. **Persistence**: Database storage for tasks and worker state
3. **Monitoring**: Metrics collection and alerting
4. **Load Balancing**: More sophisticated assignment algorithms
5. **Security**: Authentication and authorization mechanisms
6. **API**: REST/GRPC interfaces for external integration

### Q10: What's the role of the retry mechanism?
**A**: The retry mechanism handles transient failures by:
1. Tracking retry count per task
2. Resetting tasks to QUEUED state on failure
3. Attempting reassignment to different workers
4. Preventing infinite retry loops (could be enhanced with max retry limits)
This improves system reliability and handles temporary resource unavailability.
