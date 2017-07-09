package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ConnectorConnectionState {
    // Initial state with no user action
    ReadyToConnect(App.getContext().getString(R.string.ready_to_connect)),
    // During connection
    Connecting(App.getContext().getString(R.string.connecting)),
    // Impossible to connect
    CantConnect(App.getContext().getString(R.string.cant_connect)),
    // Connected
    Connected(App.getContext().getString(R.string.connected));

    private String string;

    ConnectorConnectionState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
