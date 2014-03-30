package org.util;

import java.util.LinkedList;

public class MyThreadPool {
    private Object waitLock = new Object();
    public Object getWaitLock() {
        return waitLock;
    }
    /**
    * list of threads for completing submitted tasks
    */
    private final LinkedList<MyThread> threads;
    /**
    * submitted task will be kept in this list untill they run by one of
    * threads in pool
    */
    private final LinkedList<Work> tasks;
    /**
    * shutDown flag to shut Down service
    */
    private volatile boolean shutDown;
    /**
    * ResultListener to get back the result of submitted tasks
    */
    private ResultListener resultListener;
    /**
    * initializes the threadPool by starting the threads threads will wait till
    * tasks are not submitted
    *
    * @param size
    * Number of threads to be created and maintained in pool
    * @param myResultListener
    * ResultListener to get back result
    */
    public MyThreadPool(int size, ResultListener myResultListener) {
        tasks = new LinkedList<Work>();
        threads = new LinkedList<MyThread>();
        shutDown = false;
        resultListener = myResultListener;
        for (int i = 0; i < size; i++) {
            MyThread myThread = new MyThread();
            myThread.setPool(this);
            threads.add(myThread);
            myThread.start();
        }
    }
    public ResultListener getResultListener() {
        return resultListener;
    }
    public void setResultListener(ResultListener resultListener) {
        this.resultListener = resultListener;
    }
    public boolean isShutDown() {
        return shutDown;
    }
    public int getThreadPoolSize() {
        return threads.size();
    }
    public synchronized Work removeFromQueue() {
        return tasks.poll();
    }
    public synchronized void addToTasks(Work callable) {
        tasks.add(callable);
    }
    /**
    * submits the task to threadPool. will not accept any new task if shutDown
    * is called Adds the task to the list and notify any waiting threads
    *
    * @param callable
    */
    public void submit(Work callable) {
        if (!shutDown) {
            addToTasks(callable);
            synchronized (this.waitLock) {
                waitLock.notify();
            }
            } else {
            System.out.println("task is rejected.. Pool shutDown executed");
        }
    }
    /**
    * Initiates a shutdown in which previously submitted tasks are executed,
    * but no new tasks will be accepted. Waits if there are unfinished tasks
    * remaining
    *
    */
    public void stop() {
        for (MyThread mythread : threads) {
            mythread.shutdown();
        }
        synchronized (this.waitLock) {
            waitLock.notifyAll();
        }
        for (MyThread mythread : threads) {
            try {
                mythread.join();
                } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}