package com.maiaga;

import com.google.gson.Gson;

/**
 * Created by lukas on 6/26/2017.
 */

public class ThrowResult {
    String time;
    double maxSpeed;
    double maxAltitude;
    double distance;
    float duration;

    @Override
    public String toString() {
        Gson g = new Gson();
        return g.toJson(this);
    }

    public static ThrowResult fromString(String str) {
        Gson g = new Gson();
        return g.fromJson(str, ThrowResult.class);
    }
}
