package com.maiaga;

import java.util.Date;

/**
 * Created by lukas on 6/9/2017.
 */

public class LogItem {
    @Override
    public String toString() {
        return dateTime + ", loc:" +
                (validLoc ? lat + "," + lng : "(invalid)") + ", alt:" +
                (validAlt ? alt : "(invalid)") + ", spd:" +
                (validSpd ? spd : "(invalid)");
    }

    public String toCsvRow() {
        return dateTime + ";" +
                lat + ";" +
                lng + ";" +
                alt + ";" +
                spd;
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
