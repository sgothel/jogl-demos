/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package demos.newt.util;

import java.util.*;

public class TaskManager {
    private ThreadGroup threadGroup; 
    private volatile boolean shouldStop = false;
    private TaskWorker taskWorker = null;
    private Object taskWorkerLock = new Object();
    private ArrayList tasks = new ArrayList();
    private String name;

    public TaskManager(String name) {
        threadGroup = new ThreadGroup(name);
        this.name=name;
    }

    public String getName() { return name; }

    public ThreadGroup getThreadGroup() { return threadGroup; }

    public void start() {
        start(false);
    }

    /**
     * @param externalStimuli true indicates that another thread stimulates, 
     *                        ie. calls this TaskManager's run() loop method.
     *                        Hence no own thread is started in this case.
     *
     * @return The started Runnable, which handles the run-loop.
     *         Usefull in combination with externalStimuli=true,
     *         so an external stimuli can call it.
     */
    public Runnable start(boolean externalStimuli) {
        synchronized(taskWorkerLock) { 
            if(null==taskWorker) {
                taskWorker = new TaskWorker(name);
            }
            if(!taskWorker.isRunning()) {
                shouldStop = false;
                taskWorker.start(externalStimuli);
            }
            taskWorkerLock.notifyAll();
        }
        return taskWorker;
    }

    public void stop() {
        synchronized(taskWorkerLock) { 
            if(null!=taskWorker && taskWorker.isRunning()) {
                shouldStop = true;
            }
            taskWorkerLock.notifyAll();
        }
    }

    public void addTask(Runnable task) {
        if(task == null) {
            return;
        }
        synchronized(taskWorkerLock) {
            tasks.add(task);
            taskWorkerLock.notifyAll();
        }
    }

    public void removeTask(Runnable task) {
        if (task == null) {
            return;
        }
        synchronized(taskWorkerLock) {
            tasks.remove(task);
            taskWorkerLock.notifyAll();
        }
    }

    public Runnable[] getTasks() {
        Runnable[] res;
        synchronized(taskWorkerLock) {
            res = (Runnable[]) tasks.toArray();
        }
        return res;
    }

    public void waitOnWorker() {
        synchronized(taskWorkerLock) {
            if(null!=taskWorker && taskWorker.isRunning()) {
                try {
                    taskWorkerLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void waitUntilStopped() {
        synchronized(taskWorkerLock) {
            while(null!=taskWorker && taskWorker.isRunning()) {
                try {
                    taskWorkerLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class TaskWorker extends Thread {
        volatile boolean isRunning = false;
        volatile boolean externalStimuli = false;

        public TaskWorker(String name) {
            super(threadGroup, name);
        }

        public synchronized boolean isRunning() {
            return isRunning;
        }

        public void start(boolean externalStimuli) throws IllegalThreadStateException {
            synchronized(this) {
                this.externalStimuli = externalStimuli;
                isRunning = true;
            }
            if(!externalStimuli) {
                super.start();
            }
        }

        public void run() {
            while(!shouldStop) {
                try {
                    // prolog - lock stuff you may wanne do ..

                    ArrayList clonedTasks;

                    // wait for something todo ..
                    synchronized(taskWorkerLock) {
                        while(!shouldStop && tasks.size()==0) {
                            try {
                                taskWorkerLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        // make a safe clone of the list 
                        // since it may change while working on it
                        clonedTasks = (ArrayList) tasks.clone();
                    }

                    // do it for all tasks
                    for(Iterator i = clonedTasks.iterator(); i.hasNext(); ) {
                        Runnable task = (Runnable) i.next();
                        task.run();
                    }
                } catch (Throwable t) {
                    // handle errors ..
                    t.printStackTrace();
                } finally {
                    // epilog - unlock locked stuff
                }
                if(externalStimuli) break; // no loop if called by external stimuli
            }
            synchronized(this) {
                isRunning = !shouldStop;
            }
            if(!isRunning) {
                synchronized(taskWorkerLock) {
                    taskWorkerLock.notifyAll();
                }
            }
        }
    }
}

