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
    
    private static boolean REFRESH_IN_PROGRESS;
    private static boolean INIT_COMPLETE;
    
    private double delta;
    private long lastTime;
    private long currentTime;
    // This array is shared with the adventure core for initialization
    private final ArrayList<HabitualRefresher> refreshersLoadingBay;
    // This array is for actually working with the refreshers once
    // initialization has completed.
    private HabitualRefresherProfile[] profiles;
    
    
    public RefreshThread(ArrayList<HabitualRefresher> refreshers) {
        super();
        this.refreshersLoadingBay = refreshers;
        REFRESH_IN_PROGRESS = false;
        INIT_COMPLETE = false;
        delta = 0;
        lastTime = 0;
        currentTime = 0;
    }
    
    @Override
    public void run() {
        profiles = new HabitualRefresherProfile[refreshersLoadingBay.size()];
        for (int i = 0; i < profiles.length; i++) {
            profiles[i] = new HabitualRefresherProfile(refreshersLoadingBay.get(i));
        }
        
        lastTime = System.nanoTime();
        INIT_COMPLETE = true;
        while (true) {
            if (!pauseRequested()) {
                currentTime = System.nanoTime();
                delta += (double)(currentTime - lastTime) * NANOSECONDS_TO_INTERVAL;
                lastTime = currentTime;
                if (delta >= 1) {
                    REFRESH_IN_PROGRESS = true;
                    if (DO_DEBUG) System.out.println("Running bulk refresh for a possible " + refreshersLoadingBay.size() + " items...");
                    int refreshCount = 0;
                    for (HabitualRefresherProfile profile : profiles) {
                        if (profile.refresher.needsRefresh()) {
                            SwingWorker refreshWorker = new SwingWorker<Void, Void>() {
                                @Override
                                public Void doInBackground() {
                                    profile.refresher.handleRefresh();
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
    
    private static boolean pauseRequested() {
        waitForInit();
        RefreshThread SINGLETON = JessAdventureCore.REFRESH_THREAD;
        for (HabitualRefresherProfile profile : SINGLETON.profiles) {
            if (profile.requestsPause) return true;
        }
        return false;
    }
    
    private static HabitualRefresherProfile getProfile(HabitualRefresher claimant) {
        waitForInit();
        RefreshThread SINGLETON = JessAdventureCore.REFRESH_THREAD;
        for (HabitualRefresherProfile profile : SINGLETON.profiles) {
            if (profile.refresher == claimant) return profile;
        }
        throw new RuntimeException(
                "A HabitualRefresher for "
                        + claimant.getClass().getName()
                        + " was not filed at start.");
    }
    
    public static void startPause(HabitualRefresher claimant) {
        HabitualRefresherProfile profile = getProfile(claimant);
        if (profile.requestsPause) {
            // Safety check against absent-minded pause code
            throw new RuntimeException("The refresh thread is already paused; "
                    + "cannot request a second pause before the first is ended.");
        }
        waitForMe();
        profile.requestsPause = true;
    }
    
    public static void endPause(HabitualRefresher claimant) {
        HabitualRefresherProfile profile = getProfile(claimant);
        if (!profile.requestsPause) {
            // Safety check against absent-minded pause code
            throw new RuntimeException("The refresh thread is already ended; "
                    + "cannot request an end-pause before one has started.");
        }
        
        // Clear the delta when ending a pause, becuase there may be other
        // bulk operations coming after this one, and we don't want the thread
        // to interrupt them until we're absolutely sure we are done with
        // the operation.
        JessAdventureCore.REFRESH_THREAD.delta = 0;
        profile.requestsPause = false;
    }
    
    public static void waitForMe() {
        while (REFRESH_IN_PROGRESS) {
            // Just chillin' until the refresh is done...
            // Note that this will be called by an outside thread to
            // handle waiting for this one.
        }
        
        // Exit wait
    }
    
    private static void waitForInit() {
        while (!INIT_COMPLETE) {
            // Just chillin' until the thread is actually ready for action.
        }
        
        // Exit wait
    }
}
