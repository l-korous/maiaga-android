package com.maiaga;

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
        try {
            OutputStream outputStream = new ByteArrayOutputStream(1000);
            ObjectOutputStream out = new ObjectOutputStream(outputStream);
            out.writeObject(this);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Result fromString(String str) {
        Result result = new Result();
        try {
            InputStream stream = new ByteArrayInputStream(str.getBytes());
            ObjectInputStream objectInputStream = new ObjectInputStream(stream);
            result = (Result)objectInputStream.readObject();
            stream.close();
            objectInputStream.close();
            return result;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
