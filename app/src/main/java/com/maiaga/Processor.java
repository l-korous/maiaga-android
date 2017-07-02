package com.maiaga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.maiaga.ProcessorConnectionState.None;
import static com.maiaga.ThrowState.AfterThrow;
import static com.maiaga.ThrowState.InThrow;
import static com.maiaga.ThrowState.NoThrow;
import static com.maiaga.ThrowState.ResultsAvailable;

public class Processor implements Runnable {
    private static final int allowFirstGpsRecordsInvalid = 5;
    private static final int temporaryDisonnectLimitInSeconds = 5;
    private static final int disonnectedLimitInSeconds = 30;
    private static final int milisecondsForFloatingAverages = 2500;
    private static final int speedForThrow = 2;
    private static final int speedForAfterThrow = 1;
    private static final int minThrowDurationInMiliseconds = 4000;

    Processor(Handler handler) {
        mHandler = handler;
        mThrowState = NoThrow;
        mProcessorConnectionState = None;
        mThrowStartLogItem = null;
        mThrowEndLogItem = null;
        lastLogItem = null;
        mStop = false;
        mLastDataDateTime = null;

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
        sendMessage("processorConnectionState", mProcessorConnectionState.toString());
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

                boolean isConnectionStateChanged = udpateConnectionStateReturnIfChanged();
                if(isConnectionStateChanged && mThrowState == NoThrow) {
                    sendMessage("processorConnectionState", mProcessorConnectionState.toString());
                }
                if(mProcessorConnectionState == ProcessorConnectionState.FetchingDataGps && mThrowState != ResultsAvailable) {
                    boolean isThrowStateChanged = udpateThrowStateReturnIfChanged();
                    if (isThrowStateChanged) {
                        sendMessage("processorThrowState", mThrowState.toString());
                        if (mThrowState == AfterThrow) {
                            mThrowState = ResultsAvailable;
                            Result result = getResults();
                            String resultString = result.toString();
                            sendMessage("processorThrowState", mThrowState.toString(), resultString);
                            mThrowStartLogItem = null;
                            log.clear();
                            mLastDataDateTime = null;
                            mThrowEndLogItem = null;
                        }
                    }
                    else if(mThrowState == NoThrow && isConnectionStateChanged) {
                        Thread.sleep(1000);
                        sendMessage("processorThrowState", mThrowState.toString());
                    }
                }
            }
            catch (IOException e) {
                handleDisconnect();
                e.printStackTrace();
            } catch (InterruptedException e) {
                handleDisconnect();
                e.printStackTrace();
            }
        }
    }

    private double getDistanceFromLatLonInKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = deg2rad(lat2-lat1);
        double dLon = deg2rad(lon2-lon1);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d;
    }

    private double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }

    private Result getResults() {
        Result result = new Result();
        result.duration = (mThrowEndLogItem.dateTime.getTime() - mThrowStartLogItem.dateTime.getTime()) / 1000;
        result.maxAltitude = 0.;
        result.maxSpeed = mThrowStartLogItem.spd;
        for(LogItem logItem : log) {
            if(logItem.dateTime.getTime() > mThrowStartTime && logItem.dateTime.getTime() <= mThrowEndTime) {
                result.maxAltitude = Math.max(logItem.alt - mThrowStartLogItem.alt, result.maxAltitude);
                result.maxSpeed = Math.max(logItem.spd, result.maxSpeed);
            }
        }
        result.distance = getDistanceFromLatLonInKm(mThrowStartLogItem.lat, mThrowStartLogItem.lng, mThrowEndLogItem.lat, mThrowEndLogItem.lng);
        return result;
    }

    private double getCurrentSpeed() {
        float speedAverage = 0;
        int i = 1, j = 0, logSize = log.size();
        Date now = new Date();

        while(i <= logSize) {
            LogItem logItem = log.get(logSize - i);
            if(now.getTime() - logItem.dateTime.getTime() > milisecondsForFloatingAverages)
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
            if(now.getTime() - logItem.dateTime.getTime() > milisecondsForFloatingAverages)
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
            if(now.getTime() - logItem.dateTime.getTime() > milisecondsForFloatingAverages)
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
            if(now.getTime() - logItem.dateTime.getTime() > milisecondsForFloatingAverages)
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
        if (mProcessorConnectionState == ProcessorConnectionState.None) {
            Log.e("WRONG Processor State", "This should not happen.");
            return false;
        }

        if(mLastDataDateTime == null) {
            if (mProcessorConnectionState != ProcessorConnectionState.TryingToFetchData) {
                mProcessorConnectionState = ProcessorConnectionState.TryingToFetchData;
                return true;
            } else
                return false;
        }

        Date now = new Date();
        long lastDataBefore = getDateDiff(mLastDataDateTime, now, TimeUnit.SECONDS);

        if(lastDataBefore >= disonnectedLimitInSeconds)
            handleDisconnect();

        if(lastDataBefore > temporaryDisonnectLimitInSeconds && lastDataBefore < disonnectedLimitInSeconds) {
            switch(mProcessorConnectionState) {
                case TryingToFetchData:
                case FetchingDataGps:
                case FetchingDataNoGps:
                    mProcessorConnectionState = ProcessorConnectionState.FetchingDataNoDataTemporary;
                    return true;
                case FetchingDataNoDataTemporary:
                    return false;
            }
        }
        if(!lastLogItem.allValid()) {
            switch(mProcessorConnectionState) {
                case TryingToFetchData:
                    if(log.size() < allowFirstGpsRecordsInvalid)
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
        Date now = new Date();

        if(mThrowState == NoThrow && getCurrentSpeed() > speedForThrow) {
            mThrowState = InThrow;
            mThrowStartLogItem = lastLogItem;
            mThrowStartTime = now.getTime();
            Log.i("THROW START", String.valueOf(mThrowStartTime));
            return true;
        }

        if(mThrowState == InThrow && getCurrentSpeed() < speedForAfterThrow && (now.getTime() - mThrowStartTime) > minThrowDurationInMiliseconds) {
            mThrowEndLogItem = lastLogItem;
            Log.i("THROW END", String.valueOf(now.getTime()));
            Log.i("THROW DIFF", String.valueOf((now.getTime() - mThrowStartTime)));
            mThrowState = AfterThrow;
            mThrowEndTime = now.getTime();
            return true;
        }
        return false;
    }

    private void handleDisconnect() {
        stop();
        sendMessage("processorToConnector", "reconnect");
    }

    public void stop() {
        Thread.currentThread().interrupt();
        mStop = true;
        mThrowState = NoThrow;
        log.clear();
        mThrowStartLogItem = null;
        mThrowEndLogItem = null;
        mLastDataDateTime = null;
        mProcessorConnectionState = None;
    }

    public void resetThrow(){
        mThrowState = NoThrow;
        if(mProcessorConnectionState == ProcessorConnectionState.FetchingDataGps)
            sendMessage("processorThrowState", mThrowState.toString());
        else
            sendMessage("processorConnectionState", mProcessorConnectionState.toString());
    }


    public void setStream(InputStream inStream) {
        mInStream = inStream;
    }

    private void sendMessage(String key, String data) {
        // We do not send changed connection states or data, but we do allow to send processorToConnector messages
        // processorThrowState messages are not sent in this throwState anyway
        if((key == "processorConnectionState" || key == "processorData") && mThrowState == ResultsAvailable)
            return;
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("key", key);
        bundle.putString("data", data);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void sendMessage(String key, String data, String subData) {
        // We do not send changed connection states or data, but we do allow to send processorToConnector messages
        // processorThrowState messages are not sent in this throwState anyway
        if((key == "processorConnectionState" || key == "processorData") && mThrowState == ResultsAvailable)
            return;
        Message msg = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("key", key);
        bundle.putString("data", data);
        bundle.putString("subData", subData);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private ArrayList<LogItem> log;
    private LogItem lastLogItem;
    private LogItem mThrowStartLogItem;
    private LogItem mThrowEndLogItem;
    private long mThrowStartTime;
    private long mThrowEndTime;
    private ProcessorConnectionState mProcessorConnectionState;
    private ThrowState mThrowState;
    private Handler mHandler;
    public boolean mStop;
    private InputStream mInStream;
    private Date mLastDataDateTime;
}
