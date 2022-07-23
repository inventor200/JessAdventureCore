/*
 * The MIT License
 *
 * Copyright 2022 Joseph Cramsey.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package joeyproductions.jessadventurecore.ui;

import java.util.ArrayList;
import java.util.ListIterator;
import javax.swing.SwingWorker;

/**
 * A thread which refreshes implementations of HabitualRefresher at some FPS.
 * The purpose of this is to allow for repaints and rebuilds after
 * bulk-operations, without having to do a repaint/rebuild after every
 * sub-operation in the bulk order.
 * 
 * For most situations, this will make a negligible difference, but in
 * situations where a sizeable screen of text is procedurally-generated, this
 * could make quite an impact.
 * 
 * @author Joseph Cramsey
 */
public class RefreshThread extends Thread {
    
    private static final int FRAMES_PER_SECOND = 10;
    private static final double NANOSECONDS_TO_INTERVAL = (double)FRAMES_PER_SECOND / (double)1E+9;
    private static final boolean DO_DEBUG = false;
    
    private static boolean PAUSE_FOR_OPERATION;
    private static boolean REFRESH_IN_PROGRESS;
    
    private double delta;
    private long lastTime;
    private long currentTime;
    private final ArrayList<HabitualRefresher> refreshers;
    
    public RefreshThread(ArrayList<HabitualRefresher> refreshers) {
        super();
        this.refreshers = refreshers;
        PAUSE_FOR_OPERATION = false;
        REFRESH_IN_PROGRESS = false;
        delta = 0;
        lastTime = 0;
        currentTime = 0;
    }
    
    @Override
    public void run() {
        lastTime = System.nanoTime();
        while (true) {
            if (!PAUSE_FOR_OPERATION) {
                currentTime = System.nanoTime();
                delta += (double)(currentTime - lastTime) * NANOSECONDS_TO_INTERVAL;
                lastTime = currentTime;
                if (delta >= 1) {
                    REFRESH_IN_PROGRESS = true;
                    if (DO_DEBUG) System.out.println("Running bulk refresh for a possible " + refreshers.size() + " items...");
                    ListIterator<HabitualRefresher> iter = this.refreshers.listIterator();
                    int refreshCount = 0;
                    while (iter.hasNext()) {
                        HabitualRefresher refresher = iter.next();
                        if (refresher.needsRefresh()) {
                            SwingWorker refreshWorker = new SwingWorker<Void, Void>() {
                                @Override
                                public Void doInBackground() {
                                    refresher.handleRefresh();
                                    return null;
                                }
                            };
                            refreshWorker.execute();
                            refreshCount++;
                        }
                    }
                    if (DO_DEBUG) System.out.println("Refreshed " + refreshCount + " items.");
                    delta = 0;
                    REFRESH_IN_PROGRESS = false;
                }
            }
        } 
    }
    
    public static void startPause() {
        if (PAUSE_FOR_OPERATION) {
            // Safety check against absent-minded pause code
            throw new RuntimeException("The refresh thread is already paused; "
                    + "cannot request a second pause before the first is ended.");
        }
        waitForMe();
        PAUSE_FOR_OPERATION = true;
    }
    
    public static void endPause() {
        if (!PAUSE_FOR_OPERATION) {
            // Safety check against absent-minded pause code
            throw new RuntimeException("The refresh thread is already ended; "
                    + "cannot request an end-pause before one has started.");
        }
        
        // Clear the delta when ending a pause, becuase there may be other
        // bulk operations coming after this one, and we don't want the thread
        // to interrupt them until we're absolutely sure we are done with
        // the operation.
        JessAdventureCore.REFRESH_THREAD.delta = 0;
        PAUSE_FOR_OPERATION = false;
    }
    
    public static void waitForMe() {
        while (REFRESH_IN_PROGRESS) {
            // Just chillin' until the refresh is done...
            // Note that this will be called by an outside thread to
            // handle waiting for this one.
        }
        
        // Exit wait
    }
}
