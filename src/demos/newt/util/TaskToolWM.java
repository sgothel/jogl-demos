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

import com.jogamp.newt.*;

public class TaskToolWM {
    final static boolean externalStimuli;
    final static TaskManager eventMgr;
    final static Runnable eventMgrRunnable;
    final static TaskManager renderMgr;
    final static Runnable renderMgrRunnable;
    final static Map/*Window,Runnable*/ window2Event;
    final static Map/*Window,Set*/ window2Set;

    static {
        System.setProperty("java.awt.headless", "true");

        window2Event = new HashMap();
        window2Set = new HashMap();

        externalStimuli = Boolean.getBoolean("demos.newt.util.TaskToolWM.externalStimuli");

        eventMgr = new TaskManager("Event Manager");
        eventMgrRunnable = eventMgr.start(externalStimuli);

        renderMgr = new TaskManager("Render Manager");
        renderMgrRunnable = renderMgr.start(externalStimuli);

        /**
        String osName = System.getProperty("os.name");
        if (osNameLowerCase.startsWith("mac os x") ||
            osNameLowerCase.startsWith("darwin")) {
            TaskManagerMacOSX.registerEventRunnable(eventMgrRunnable);
            TaskManagerMacOSX.registerRenderRunnable(renderMgrRunnable);
        } */
    }

    private static class EventThread implements Runnable {
        Display display;

        EventThread(Display d) {
            display = d;
        }
        public void run() {
            try {
                // prolog - lock whatever you need

                // do it ..
                if(null!=display) {
                    display.dispatchMessages();
                }
            } catch (Throwable t) {
                // handle errors ..
                t.printStackTrace();
            } finally {
                // epilog - unlock locked stuff
            }
        }
    }

    public static boolean registerWindowEvent(Window w) {
        boolean res;
        synchronized(window2Event) {
            Runnable evt = (Runnable) window2Event.get(w);
            if(null==evt) {
                evt = new EventThread(w.getScreen().getDisplay());
                window2Event.put(w, evt);
                eventMgr.addTask(evt);
                res = true;
            } else {
                res = false;
            }
        }
        return res;
    }

    public static boolean unregisterWindowEvent(Window w) {
        boolean res;
        synchronized(window2Event) {
            Runnable evt = (Runnable) window2Event.remove(w);
            if(null!=evt) {
                eventMgr.removeTask(evt);
                res = true;
            } else {
                res = false;
            }
        }
        return res;
    }

    public static boolean isWindowEventRegistered(Window w) {
        boolean res;
        synchronized(window2Event) {
            res = null != window2Event.get(w);
        }
        return res;
    }

    public static void waitUntilWindowsUnregistered() {
        while(window2Event.size()>0) {
            eventMgr.waitOnWorker();
        }
    }

    public static void waitUntilWindowUnregistered(Window w) {
        while(isWindowEventRegistered(w)) {
            eventMgr.waitOnWorker();
        }
    }

    public static boolean addRenderTask(Window w, Runnable task) {
        boolean res;
        synchronized(window2Set) {
            Set s = (Set) window2Set.get(w);
            if(null==s) {
                s = new HashSet();
                window2Set.put(w, s);
            }
            if(s.add(task)) {
                renderMgr.addTask(task);
                res = true;
            } else {
                res = false;
            }
        }
        return res;
    }

    public static boolean removeRenderTasks(Window w) {
        boolean res;
        synchronized(window2Set) {
            Set s = (Set) window2Set.get(w);
            if(null==s) {
                res = false;
            } else {
                for(Iterator i=s.iterator(); i.hasNext(); ) {
                    Runnable task = (Runnable) i.next();
                    renderMgr.removeTask(task);
                    i.remove();
                }
                window2Set.remove(w);
                res = true;
            }
        }
        return res;
    }

    public static boolean removeRenderTask(Window w, Runnable task) {
        boolean res;
        synchronized(window2Set) {
            Set s = (Set) window2Set.get(w);
            if(null==s) {
                res = false;
            } else {
                if(s.remove(task)) {
                    renderMgr.removeTask(task);
                    if(s.size()==0) {
                        window2Set.remove(w);
                    }
                    res = true;
                } else {
                    res = false;
                }
            }
        }
        return res;
    }

    public static Runnable[] getRenderTasks(Window w) {
        Runnable[] res=null;
        synchronized(window2Set) {
            Set s = (Set) window2Set.get(w);
            if(null!=s) {
                res = (Runnable[])s.toArray();
            }
        }
        return res;
    }

    public static void exit(boolean systemExit) {
        eventMgr.stop();
        renderMgr.stop();
        if(systemExit) {
            System.exit(0);
        }
    }
}


