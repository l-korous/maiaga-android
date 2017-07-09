package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ProcessorConnectionState {
    // While connector not connected, or connecting.
    None(""),
    // Connected, data not being fetched
    TryingToFetchData(App.getContext().getString(R.string.trying_to_fetch_data)),
    // Good state, fetching data, all fine
    FetchingDataGps(App.getContext().getString(R.string.fetching_data_gps)),
    // As above, data being fetched, no GPS signal
    FetchingDataNoGps(App.getContext().getString(R.string.fetching_data_no_gps)),
    // As above, no data being fetched, but we thing BT will just kick back in when in range
    FetchingDataNoDataTemporary(App.getContext().getString(R.string.fetching_data_no_data_temporary));

    private String string;

    ProcessorConnectionState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
