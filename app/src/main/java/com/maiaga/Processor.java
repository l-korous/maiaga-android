package com.maiaga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.maiaga.ThrowState.AfterThrow;
import static com.maiaga.ThrowState.InThrow;
import static com.maiaga.ThrowState.NoThrow;

public class Processor implements Runnable {
    Processor(Handler handler) {
        mHandler = handler;
        log = new ArrayList<LogItem>();
    }

    static {
        System.loadLibrary("tinyGps");
    }
    public native void encode(short s);

    public native double lat();
    public native double lng();
    public native double alt();
    public native double spd();
    public native long date();
    public native long time();
    public native boolean isValidLoc();
    public native boolean isValidSpd();
    public native boolean isValidAlt();
    public native boolean isValidDateTime();
    public native boolean newDataAvailable();

    @Override
    public void run() {
        mStop = false;
        mProcessorConnectionState = ProcessorConnectionState.TryingToFetchData;
        mThrowState = NoThrow;
        while(!Thread.currentThread().isInterrupted() && !mStop)
        {
            try
            {
                final int bytesAvailable = mInStream.available();
                if(bytesAvailable > 0)
                {
                    final byte[] packetBytes = new byte[bytesAvailable];
                    mInStream.read(packetBytes);
                    for(int i=0;i<bytesAvailable;i++) {
                        encode(packetBytes[i]);
                    }
                }
                if(newDataAvailable())
                {
                    LogItem logItem = new LogItem();
                    logItem.lat = lat();
                    logItem.lng = lng();
                    logItem.alt = alt();
                    logItem.spd = spd();
                    logItem.validLoc = isValidLoc();
                    logItem.validSpd = isValidSpd();
                    logItem.validAlt = isValidAlt();
                    logItem.dateTime = new Date();
                    processLogItem(logItem);
                    sendMessage("processorData", logItem.toString());
                }
            }

            catch (IOException ex)
            {
                mStop = true;
                if(mProcessorConnectionState != ProcessorConnectionState.FetchingDataNoDataShouldReconnect) {
                    mProcessorConnectionState = ProcessorConnectionState.FetchingDataNoDataShouldReconnect;
                    sendMessage("processorConnectionState", mProcessorConnectionState.toString());
                }
            }
            finally {
                boolean isConnectionStateChanged = udpateConnectionStateReturnIfChanged();
                if(isConnectionStateChanged) {
                    sendMessage("processorConnectionState", mProcessorConnectionState.toString());
                }
                if(mProcessorConnectionState == ProcessorConnectionState.FetchingDataGps) {
                    boolean isThrowStateChanged = udpateThrowStateReturnIfChanged();
                    if (isThrowStateChanged)
                        sendMessage("processorThrowState", mThrowState.toString());
                }
            }
        }
    }

    private double getCurrentSpeed() {
        float speedAverage = 0;
        int i = 1, j = 0, logSize = log.size();
        Date now = new Date();

        while(i <= logSize) {
            LogItem logItem = log.get(logSize - i);
            if(now.getTime() - logItem.dateTime.getTime() > 2500)
                break;
            if(logItem.validSpd) {
                speedAverage += logItem.spd;
                j++;
            }
            i++;
        }

        return (j == 0 ? 0.0 : (speedAverage / j));
    }

    private double getCurrentAltitude() {
        float altAverage = 0;
        int i = 1, j = 0, logSize = log.size();
        Date now = new Date();

        while(i <= logSize) {
            LogItem logItem = log.get(logSize - i);
            if(now.getTime() - logItem.dateTime.getTime() > 2500)
                break;
            if(logItem.validAlt) {
                altAverage += logItem.alt;
                j++;
            }
            i++;
        }

        return (j == 0 ? 0.0 : (altAverage / j));
    }

    private double getCurrentLatitude() {
        float latAverage = 0;
        int i = 1, j = 0, logSize = log.size();
        Date now = new Date();

        while(i <= logSize) {
            LogItem logItem = log.get(logSize - i);
            if(now.getTime() - logItem.dateTime.getTime() > 2500)
                break;
            if(logItem.validLoc) {
                latAverage += logItem.lat;
                j++;
            }
            i++;
        }

        return (j == 0 ? 0.0 : (latAverage / j));
    }

    private double getCurrentLongitude() {
        float lngAverage = 0;
        int i = 1, j = 0, logSize = log.size();
        Date now = new Date();

        while(i <= logSize) {
            LogItem logItem = log.get(logSize - i);
            if(now.getTime() - logItem.dateTime.getTime() > 2500)
                break;
            if(logItem.validLoc) {
                lngAverage += logItem.lng;
                j++;
            }
            i++;
        }

        return (j == 0 ? 0.0 : (lngAverage / j));
    }

    private void processLogItem(LogItem logItem) {
        boolean addThisItem = true;

        if(!log.isEmpty()) {
            LogItem previousLogItem = log.get(log.size() - 1);
            if (previousLogItem.dateTime == logItem.dateTime) {
                if (previousLogItem.betterOrEquivalentTo(logItem))
                    addThisItem = false;
                else
                    log.remove(log.size() - 1);
            }
        }

        if(addThisItem) {
            log.add(logItem);
            lastLogItem = logItem;
        }
        mLastDataDateTime = new Date();
    }

    private static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    private boolean udpateConnectionStateReturnIfChanged() {
        if(mLastDataDateTime == null) {
            if (mProcessorConnectionState != ProcessorConnectionState.TryingToFetchData) {
                mProcessorConnectionState = ProcessorConnectionState.TryingToFetchData;
                return true;
            } else
                return false;
        }

        Date now = new Date();
        long lastDataBefore = getDateDiff(mLastDataDateTime, now, TimeUnit.SECONDS);

        if(lastDataBefore >= 30) {
            switch(mProcessorConnectionState) {
                case TryingToFetchData:
                case FetchingDataGps:
                case FetchingDataNoGps:
                case FetchingDataNoDataTemporary:
                    mProcessorConnectionState = ProcessorConnectionState.FetchingDataNoDataShouldReconnect;
                    return true;
                case FetchingDataNoDataShouldReconnect:
                    return false;
            }
        }

        if(lastDataBefore > 3 && lastDataBefore < 30) {
            switch(mProcessorConnectionState) {
                case TryingToFetchData:
                case FetchingDataGps:
                case FetchingDataNoGps:
                case FetchingDataNoDataShouldReconnect:
                    mProcessorConnectionState = ProcessorConnectionState.FetchingDataNoDataTemporary;
                    return true;
                case FetchingDataNoDataTemporary:
                    return false;
            }
        }
        if(!lastLogItem.allValid()) {
            switch(mProcessorConnectionState) {
                case TryingToFetchData:
                    if(log.size() < 5)
                        return false;
                case FetchingDataGps:
                case FetchingDataNoDataTemporary:
                    mProcessorConnectionState = ProcessorConnectionState.FetchingDataNoGps;
                    return true;
                case FetchingDataNoGps:
                    return false;
            }
        }

        if(mProcessorConnectionState != ProcessorConnectionState.FetchingDataGps) {
            mProcessorConnectionState = ProcessorConnectionState.FetchingDataGps;
            return true;
        }
        return false;
    }

    private boolean udpateThrowStateReturnIfChanged() {
        if(mThrowState == NoThrow && getCurrentSpeed() > 2) {
            mThrowState = InThrow;
            return true;
        }

        if(mThrowState == InThrow && getCurrentSpeed() < 1) {
            mThrowState = AfterThrow;
            return true;
        }
        return false;
    }

    public void stop() {
        mStop = true;
    }

    public void stopAndGetResults() {
        stop();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mThrowState = ThrowState.ResultsAvailable;
                sendMessage("processorThrowState", mThrowState.toString());
            }
        }, 2000);
    }

    public void stopAndReset() {
        stop();
        log.clear();
        if(mProcessorConnectionState != ProcessorConnectionState.TryingToFetchData) {
            mProcessorConnectionState = ProcessorConnectionState.TryingToFetchData;
            sendMessage("processorConnectionState", mProcessorConnectionState.toString());
        }

        if(mThrowState != NoThrow) {
            mThrowState = NoThrow;
            sendMessage("processorThrowState", mThrowState.toString());
        }
    }

    public void setStream(InputStream inStream) {
        mInStream = inStream;
    }

    private void sendMessage(String key, String data) {
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("key", key);
        bundle.putString("data", data);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private ArrayList<LogItem> log;
    private LogItem lastLogItem;
    private ProcessorConnectionState mProcessorConnectionState;
    private ThrowState mThrowState;
    private Handler mHandler;
    public boolean mStop;
    private InputStream mInStream;
    private Date mLastDataDateTime;
}
