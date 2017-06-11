package com.maiaga;

import java.util.Date;

/**
 * Created by lukas on 6/9/2017.
 */

public class LogItem {
    @Override
    public String toString() {
        if(validGps)
            return dateTime + ";" +
                    lat + ";" +
                    lng + ";" +
                    alt + ";" +
                    spd;
        else
            return dateTime + "invalid GPS";
    }

    public double lat, lng, alt, spd;
    public boolean validGps;
    public Date dateTime;
}
