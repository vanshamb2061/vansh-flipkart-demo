package com.scheduler;

import com.scheduler.enums.Priority;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        testCase1();
        testCase2();
        testCase3();
        testCase4();
        testCase5();
        testCase6();
        testCase7();
        testCase8();
        testCase9();
    }

    private static void testCase1() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 4, 16, 5);
        scheduler.registerWorker("W2", 8, 32, 10);
        System.out.println("Test Case 1: Register Worker Nodes");
        System.out.println(scheduler.listWorkers());
        System.out.println();
    }

    private static void testCase2() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 4, 16, 5);
        scheduler.registerWorker("W2", 8, 32, 10);
        
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 8, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 4, 16, 20)
        ));
        
        System.out.println("Test Case 2: Submit Task and Assign it to Best Worker");
        System.out.println(scheduler.listTasks());
        System.out.println();
    }

    private static void testCase3() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 4, 16, 5);
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 8, 10)
        ));
        
        System.out.println("Test Case 3: Task Execution & Completion");
        scheduler.waitFor(10);
        System.out.println(scheduler.listTasks());
        System.out.println();
        
        DistributedTaskScheduler scheduler2 = new DistributedTaskScheduler();
        scheduler2.registerWorker("W1", 2, 8, 5);
        
        scheduler2.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 4, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 2, 4, 5)
        ));
        
        System.out.println(scheduler2.listTasks());
        System.out.println();
    }

    private static void testCase4() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 6, 32, 5);
        scheduler.registerWorker("W2", 8, 32, 10);
        
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 8, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 4, 16, 20)
        ));
        
        System.out.println("Test Case 4: Worker Failure & Task Reassignment");
        System.out.println(scheduler.listTasks());
        scheduler.simulateWorkerFailure("W2");
        System.out.println(scheduler.listTasks());
        System.out.println(scheduler.listWorkers());
        System.out.println();
    }

    private static void testCase5() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 2, 16, 5);
        
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 4, 10, Priority.LOW),
            new DistributedTaskScheduler.TaskConfig("T2", 2, 4, 5, Priority.HIGH)
        ));
        
        System.out.println("Test Case 5: Task Prioritization");
        System.out.println(scheduler.listTasks());
        System.out.println();
    }

    private static void testCase6() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 2, 8, 5);
        
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 4, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 2, 4, 5)
        ));
        
        System.out.println("Test Case 6: Auto Scaling of Worker Nodes");
        System.out.println(scheduler.listTasks());
        scheduler.autoScale();
        System.out.println(scheduler.listTasks());
        System.out.println(scheduler.listWorkers());
        System.out.println();
    }

    private static void testCase7() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 2, 16, 5);
        
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 4, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 2, 4, 10)
        ));
        
        System.out.println("Test Case 7: Task Timeout & Reassignment");
        System.out.println(scheduler.listTasks());
        scheduler.simulateTaskTimeout("T1", 13);
        System.out.println(scheduler.listTasks());
        System.out.println();
    }

    private static void testCase8() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 4, 16, 5);
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 4, 10)
        ));
        
        System.out.println("Test Case 8: Task Cancellation");
        scheduler.cancelTask("T1");
        System.out.println(scheduler.listTasks());
        System.out.println();
    }

    private static void testCase9() {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler();
        scheduler.registerWorker("W1", 4, 16, 5);
        
        scheduler.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 4, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 2, 4, 5)
        ));
        
        System.out.println("Test Case 9: Parallel task execution");
        scheduler.waitFor(10);
        System.out.println(scheduler.listTasks());
        System.out.println();
        
        DistributedTaskScheduler scheduler2 = new DistributedTaskScheduler();
        scheduler2.registerWorker("W1", 4, 16, 5);
        scheduler2.registerWorker("W2", 8, 32, 10);
        
        scheduler2.submitTasks(Arrays.asList(
            new DistributedTaskScheduler.TaskConfig("T1", 2, 8, 10),
            new DistributedTaskScheduler.TaskConfig("T2", 4, 16, 20)
        ));
        
        System.out.println(scheduler2.listTasks());
        scheduler2.waitFor(12);
        scheduler2.simulateWorkerFailure("W2");
        System.out.println(scheduler2.listTasks());
        System.out.println(scheduler2.listWorkers());
        System.out.println();
    }
}
