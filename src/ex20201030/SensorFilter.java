package ex20201030;
/* -------------------------------------------- */
/*                                              */
/* This solution is submitted by:               */
/*                                              */
/* Write your name here!                        */
/*                                              */
/* -------------------------------------------- */

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


/* -------------------------------------------- */
/* Class representing one sample. Do not alter  */
/* this class.                                  */
/* -------------------------------------------- */

class Sample {
    private int id;
    private double value;
    private long timestamp;

    /**
     * Constructor
     */
    public Sample(int id, double value) {
        this.id = id;
        this.value = value;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the id (0,1,2) of the sensor
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the sampled (distance) value.
     */
    public double getDistance() {
        return value;
    }

    /**
     * Returns the sample timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }
}


/* -------------------------------------------- */
/* Complete the DistanceSensing monitor below.  */
/* Modify the method declarations if needed,    */
/* but not in a way that changes their call     */
/* signature (name, arguments, return value).   */
/* Use the monitor support built into standard  */
/* Java in your solution.                       */
/* In order to use this class you need to       */
/* write a subclass that implements the         */
/* dangerAlert() method. This is not a part of  */
/* the assignment. Instead, see the simulation  */
/* classes below for an example of how this     */
/* could be done.                               */
/* -------------------------------------------- */

abstract class DistanceSensing {

    private static final double DANGER_DIST = 0.8;

    /* Add your own attributes here */
    List<Sample> samples;
    boolean safe;
    boolean restore;

    /**
     * Constructor
     */
    public DistanceSensing() {
        samples = new ArrayList<>();
        safe = true;
        restore = false;
        new DangerThread().start();
    }

    /**
     * Stores a new sensor value in the monitor.
     * This method never blocks.
     */
    public synchronized void put(Sample s) {
        samples.add(s);
        if (safe) safe = isSafe();
        if (restore) restore = !isSafe();
        notifyAll();
    }

    /**
     * Blocks until sensor values at least as new as time
     * are available from all sensors anf then returns
     * the sample with smallest value.
     */
    public synchronized Sample getMinimum(long time) {
        while (samples.isEmpty() || samples.get(samples.size() - 1).getTimestamp() < time) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Sample min = samples.get(0);
        for (Sample s : samples) {
            min = s.getDistance() < min.getDistance() ? s : min;
        }
        notifyAll();
        return min;
    }

    /**
     * Automatically called whenever it is detected that
     * any of the current sensor values are lower than
     * DANGER_DIST. Only one invokation of this method can
     * be active at any time, i.e., if the method is called
     * it can not be called again until the first call has
     * finished. A new call also requires that all sensor
     * values returns to a safe state (> DANGER_DIST)
     * before it can be called again. A call to the method
     * is allowed to take a long time, e.g., waiting for an
     * operator to acknowledge an alarm.
     */
    protected abstract void dangerAlert();


    /* Add your private methods/inner classes here if your */
    /* solution requires any.                              */

    private Sample minimum(long time) {
        Sample minimum = null;
        int valid = 0;
        for (Sample s : samples) {
            if (s.getTimestamp() >= time) {
                valid++;
                if (minimum == null) {
                    minimum = s;
                }
                if (minimum.getTimestamp() > s.getTimestamp()) {
                    minimum = s;
                }
            }
        }
        if (valid < 3) return null;
        return minimum;
    }

    private boolean isSafe() {
        for (Sample s : samples)
            if (s.getDistance() < DANGER_DIST)
                return false;
        return true;
    }

    private synchronized void awaitUnsafe() {
        while (safe) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void awaitSafe() {
        if (!isSafe()) {
            while (restore) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        safe = true;
    }

    private class DangerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                awaitUnsafe();
                dangerAlert();
                awaitSafe();
            }
        }
    }
}



/* -------------------------------------------- */
/* Put your own classes below if your solution  */
/* requires any additional classes.             */
/* Hint: Do not make the classes public - that  */
/* would prevent the compiler from compiling    */
/* the file in a proper way.                    */
/* -------------------------------------------- */









/* ------------------- Simulation classes  -----------------------------
   Only used for testing the solution above.

   Do not alter these classes!

   Output is presented on standard output in a very simplicit form.

   Press Enter to acknowledge alarms.

*/

public class SensorFilter {

    public static void main(String[] args) {
        DistanceSensing mon = new TestDistanceSensing();
        for (int i = 0; i < 3; i++) {
            new SensorThread(i, mon).start();
        }
        new DistanceWatcher(mon).start();
    }
}


class TestDistanceSensing extends DistanceSensing {
    public void dangerAlert() {
        System.out.println();
        System.out.println("DANGER: distance warning! Press Enter to acknowledge.");
        System.out.println();
        // System.console().readLine(); Can cause NullPointerException in some configurations
        new Scanner(System.in).nextLine();
        System.out.println("Distance warning acknowledged.");
        System.out.println();
    }
}

class SensorThread extends Thread {
    private int id;
    private DistanceSensing mon;

    public SensorThread(int id, DistanceSensing mon) {
        this.id = id;
        this.mon = mon;
    }

    public void run() {
        Random r = new Random();
        while (true) {
            try {
                Thread.sleep(2000 + r.nextInt(1000));
            } catch (InterruptedException e) {
                throw new Error("Impossible!");
            }
            Sample s = new Sample(id, 0.7 + r.nextDouble());
            System.out.println("New sensor data available: " + s.getDistance() +
                    " from sensor " + id);
            mon.put(s);
        }
    }
}

class DistanceWatcher extends Thread {
    private DistanceSensing mon;

    public DistanceWatcher(DistanceSensing mon) {
        this.mon = mon;
    }

    public void run() {
        while (true) {
            Sample s = mon.getMinimum(System.currentTimeMillis());
            System.out.println("Minimum distance now: " + s.getDistance() +
                    " from sensor " + s.getId());
        }
    }
}
