package com.maiaga;

import java.util.Date;

/**
 * Created by lukas on 6/9/2017.
 */

public class LogItem {
    @Override
    public String toString() {
        return dateTime.toString() + ", loc:" +
                (validLoc ? Double.toString(lat) + "," + Double.toString(lng) : "(invalid)") + ", alt:" +
                (validAlt ? Double.toString(alt) : "(invalid)") + ", spd:" +
                (validSpd ? Double.toString(spd) : "(invalid)");
    }

    public String toCsvRow() {
        return dateTime.toString() + ";" +
                Double.toString(lat) + ";" +
                Double.toString(lng) + ";" +
                Double.toString(alt) + ";" +
                Double.toString(spd);
    }

    public boolean betterOrEquivalentTo(LogItem other) {
        if(!other.validLoc && validLoc)
            return true;
        else {
            int noOfValid = (validLoc ? 1 : 0) + (validAlt ? 1 : 0) + (validSpd ? 1 : 0);
            int noOfValidOther = (other.validLoc ? 1 : 0) + (other.validAlt ? 1 : 0) + (other.validSpd ? 1 : 0);
            return noOfValid >= noOfValidOther;
        }
    }

    public boolean allValid() {
        return validSpd && validAlt && validLoc;
    }

    public double lat, lng, alt, spd;
    public boolean validLoc, validAlt, validSpd;
    public Date dateTime;
}
