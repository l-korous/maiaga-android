package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ProcessorConnectionState {
    // While connector not connected, or connecting.
    None(""),
    // Connected, data not being fetched
    TryingToFetchData("Connected, trying to fetch data..."),
    // Good state, fetching data, all fine
    FetchingDataGps("GPS data coming in..."),
    // As above, data being fetched, no GPS signal
    FetchingDataNoGps("Waiting for GPS data..."),
    // As above, no data being fetched, but we thing BT will just kick back in when in range
    FetchingDataNoDataTemporary("Bad bluetooth signal, get closer to MAIAGA device...");

    private String string;

    ProcessorConnectionState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
