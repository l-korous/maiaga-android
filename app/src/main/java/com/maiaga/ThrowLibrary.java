package com.maiaga;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 6/9/2017.
 */

public class ThrowLibrary {
    public static final String fileName = "throwLibrary";

    public static ThrowLibrary getSingleton() {
        if(mSingleton == null)
            mSingleton = new ThrowLibrary();

        return mSingleton;
    }

    public ThrowLibrary() {
        mThrowResultArray = new ArrayList<ThrowResult>();
        read();
    }

    public void add(String resultString) {
        mThrowResultArray.add(ThrowResult.fromString(resultString));
        write();
    }

    public void add(ThrowResult throwResult) {
        mThrowResultArray.add(throwResult);
        write();
    }

    private void write() {
        String toWrite = "";
        for(ThrowResult throwResult : mThrowResultArray){
            toWrite += throwResult.toString() + "\n";
        }

        try {
            File file = new File(App.getContext().getFilesDir() + File.separator + fileName);
            if(!file.exists())
                file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.append(toWrite);
            writer.flush();
            writer.close();
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read() {
        File file = new File(App.getContext().getFilesDir() + File.separator + fileName);
        try (FileInputStream fin = new FileInputStream(file)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                mThrowResultArray.add(ThrowResult.fromString(line));
            }
            reader.close();
            fin.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ThrowResult> mThrowResultArray;
    public static ThrowLibrary mSingleton;
}
