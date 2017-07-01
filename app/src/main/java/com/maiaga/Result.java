package com.maiaga;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import static java.lang.System.out;

/**
 * Created by lukas on 6/26/2017.
 */

public class Result {
    double maxSpeed;
    double maxAltitude;
    double distance;
    float duration;

    @Override
    public String toString() {
        Gson g = new Gson();
        return g.toJson(this);
    }

    public static Result fromString(String str) {
        Gson g = new Gson();
        return g.fromJson(str, Result.class);
    }
}
