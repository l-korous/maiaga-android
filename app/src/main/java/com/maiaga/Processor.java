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
    public native boolean isValidGps();
    public native boolean newDataAvailable();

    @Override
    public void run() {
        mStop = false;
        mConnectionState = ConnectionState.Connecting;
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
                if(newDataAvailable()) {
                    LogItem logItem = new LogItem();
                    logItem.lat = lat();
                    logItem.lng = lng();
                    logItem.alt = alt();
                    logItem.spd = spd();
                    logItem.validGps = isValidGps();
                    Long currentDate = date();
                    Long currentTime = time();
                    if (currentTime > 10000000) {
                        try {
                            DateFormat df = new SimpleDateFormat("ddMMyyHHmmss");
                            logItem.dateTime = df.parse(Long.toString(currentDate) + (Long.toString(currentTime / 100)));
                        }
                        catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            DateFormat df = new SimpleDateFormat("ddMMyyHmmss");
                            logItem.dateTime = df.parse(Long.toString(currentDate) + (Long.toString(currentTime / 100)));
                        }
                        catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    processLogItem(logItem);

                    sendMessage("processorData", logItem.toString());
                }
            }

            catch (IOException ex)
            {
                mStop = true;
                if(mConnectionState != ConnectionState.FetchingDataNoDataShouldReconnect) {
                    mConnectionState = ConnectionState.FetchingDataNoDataShouldReconnect;
                    sendMessage("processorConnectionState", mConnectionState.toString());
                }
            }
            finally {
                boolean isConnectionStateChanged = udpateConnectionStateReturnIfChanged();
                if(mConnectionState == ConnectionState.FetchingDataGps) {
                    boolean isThrowStateChanged = udpateThrowStateReturnIfChanged();
                    if (isThrowStateChanged)
                        sendMessage("processorThrowState", mThrowState.toString());
                }
                else
                if(isConnectionStateChanged) {
                    sendMessage("processorConnectionState", mConnectionState.toString());
                }
            }
        }
    }

    private void processLogItem(LogItem logItem) {
        LogItem previousLogItem = log.get(log.size() - 1);
        if(previousLogItem.dateTime == logItem.dateTime) {
            if(previousLogItem.validGps)
                return;
            else
                log.remove(log.size() - 1);
        }
        else {
            log.add(logItem);
            lastLogItem = logItem;
            mLastDataDateTime = new Date();
        }
    }

    private static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }

    private boolean udpateConnectionStateReturnIfChanged() {
        if(mLastDataDateTime == null && mConnectionState != ConnectionState.TryingToFetchData) {
            mConnectionState = ConnectionState.TryingToFetchData;
            return true;
        }

        Date now = new Date();
        long lastDataBefore = getDateDiff(mLastDataDateTime, now, TimeUnit.SECONDS);

        if(lastDataBefore >= 30) {
            switch(mConnectionState) {
                case TryingToFetchData:
                case FetchingDataGps:
                case FetchingDataNoGps:
                case FetchingDataNoDataTemporary:
                    mConnectionState = ConnectionState.FetchingDataNoDataShouldReconnect;
                    return true;
            }
        }

        if(lastDataBefore > 1 && lastDataBefore < 30) {
            switch(mConnectionState) {
                case TryingToFetchData:
                case FetchingDataGps:
                case FetchingDataNoGps:
                    mConnectionState = ConnectionState.FetchingDataNoDataTemporary;
                    return true;
            }
        }
        if(!lastLogItem.validGps) {
            switch(mConnectionState) {
                case TryingToFetchData:
                case FetchingDataGps:
                case FetchingDataNoDataTemporary:
                    mConnectionState = ConnectionState.FetchingDataNoGps;
                    return true;
            }
        }

         if(mConnectionState != ConnectionState.FetchingDataGps) {
             mConnectionState = ConnectionState.FetchingDataGps;
             return true;
         }
         return false;
    }

    private boolean udpateThrowStateReturnIfChanged() {
        if(mThrowState == NoThrow) {
            boolean isMoving = lastLogItem.spd > 3;
            mThrowState = InThrow;
            return true;
        }

        if(mThrowState == InThrow) {
            boolean isMoving = lastLogItem.spd < 1;
            mThrowState = AfterThrow;
            return true;
        }
        return false;
    }

    public void stop() {
        mStop = true;
    }

    public void reset() {
        stop();
        log.clear();
        mConnectionState = ConnectionState.TryingToFetchData;
        mThrowState = NoThrow;
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
    private ConnectionState mConnectionState;
    private ThrowState mThrowState;
    private Handler mHandler;
    private boolean mStop;
    private InputStream mInStream;
    private Date mLastDataDateTime;
}
