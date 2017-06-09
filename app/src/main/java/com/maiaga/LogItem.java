package com.maiaga;

/**
 * Created by lukas on 6/9/2017.
 */

public class LogItem {
    @Override
    public String toString() {
        return date + " " + time + "   " +
                "Lat: " + lat +
                "Long: " + lng +
                "Alt: " + alt +
                "Speed: " + speed;
    }
    double lat, lng, alt, speed;
    long date, time;
}
