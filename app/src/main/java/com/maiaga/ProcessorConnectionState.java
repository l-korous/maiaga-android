package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ProcessorConnectionState {
    // Connected, data not being fetched
    TryingToFetchData,
    // Good state, fetching data, all fine
    FetchingDataGps,
    // As above, data being fetched, no GPS signal
    FetchingDataNoGps,
    // As above, no data being fetched, but we thing BT will just kick back in when in range
    FetchingDataNoDataTemporary,
    // As above, no data being fetched, and we think BT connection broken
    FetchingDataNoDataShouldReconnect,
    // Reconnecting - probably the same as FetchingDataNoDataShouldReconnectNoThrow or FetchingDataNoDataShouldReconnectInThrow
    Reconnecting
}