package com.maiaga;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
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
    private static final int disonnectedLimitInSeconds = 20;
    private static final int milisecondsForFloatingAverages = 1500;
    private static final int speedForThrow = 1;
    private static final int speedForAfterThrow = 1;
    private static final int minThrowDurationInMiliseconds = 2000;

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
        if(mRunningThread != null)
            mRunningThread.interrupt();
        mStop = false;
        mRunningThread = Thread.currentThread();
        mProcessorConnectionState = ProcessorConnectionState.TryingToFetchData;
        sendMessage("processorConnectionState", mProcessorConnectionState.toString());
        mThrowState = NoThrow;
        while(!Thread.currentThread().isInterrupted() && !mStop)
        {
            try
            {
                final int bytesAvailable = mInStream.available();
                if(!mStop && bytesAvailable > 0)
                {
                    final byte[] packetBytes = new byte[bytesAvailable];
                    mInStream.read(packetBytes);
                    for(int i=0;i<bytesAvailable;i++) {
                        encode(packetBytes[i]);
                    }
                }
                if(!mStop && newDataAvailable())
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

                boolean isConnectionStateChanged = !mStop && udpateConnectionStateReturnIfChanged();
                if(!mStop && isConnectionStateChanged && mThrowState == NoThrow) {
                    sendMessage("processorConnectionState", mProcessorConnectionState.toString());
                }
                if(!mStop && mProcessorConnectionState == ProcessorConnectionState.FetchingDataGps && mThrowState != ResultsAvailable) {
                    boolean isThrowStateChanged = udpateThrowStateReturnIfChanged();
                    if (isThrowStateChanged) {
                        sendMessage("processorThrowState", mThrowState.toString());
                        if (mThrowState == AfterThrow) {
                            mThrowState = ResultsAvailable;
                            ThrowResult throwResult = getResults();
                            String resultString = throwResult.toString();
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
                Thread.sleep(900);
            }
            catch (IOException e) {
                if(!mStop)
                    handleDisconnect();
                e.printStackTrace();
            } catch (InterruptedException e) {
                if(!mStop)
                    handleDisconnect();
                e.printStackTrace();
            }
        }
    }

    private double getDistanceFromLatLonInKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = 112000 * (lat2 - lat1);
        double dLon = 112000 * (lon2 - lon1);
        double d = Math.sqrt(dLat * dLat + dLon * dLon);
        return d;
    }

    private double deg2rad(double deg) {
        return deg * (Math.PI/180);
    }

    private ThrowResult getResults() {
        ThrowResult throwResult = new ThrowResult();
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date now = new Date();
        throwResult.time = sdfDate.format(now);
        throwResult.duration = (mThrowEndLogItem.dateTime.getTime() - mThrowStartLogItem.dateTime.getTime()) / 1000;
        throwResult.maxAltitude = 0.;
        throwResult.maxSpeed = mThrowStartLogItem.spd;
        for(LogItem logItem : log) {
            if(logItem.dateTime.getTime() > mThrowStartTime && logItem.dateTime.getTime() <= mThrowEndTime) {
                throwResult.maxAltitude = Math.max(logItem.alt - mThrowStartLogItem.alt, throwResult.maxAltitude);
                throwResult.maxSpeed = Math.max(logItem.spd, throwResult.maxSpeed);
            }
        }
        throwResult.distance = getDistanceFromLatLonInKm(mThrowStartLogItem.lat, mThrowStartLogItem.lng, mThrowEndLogItem.lat, mThrowEndLogItem.lng);
        return throwResult;
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
        mStop = true;
        if(mRunningThread != null)
            mRunningThread.interrupt();

        log.clear();
        mLastDataDateTime = null;

        mProcessorConnectionState = None;
        sendMessage("processorConnectionState", mProcessorConnectionState.toString());

        resetThrow();
    }

    public void resetThrow(){
        mThrowState = NoThrow;
        mThrowStartLogItem = null;
        mThrowEndLogItem = null;
        sendMessage("processorThrowState", mThrowState.toString());
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
    private Thread mRunningThread;
}
