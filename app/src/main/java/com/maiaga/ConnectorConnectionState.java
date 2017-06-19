package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ConnectorConnectionState {
    // Initial state with no user action
    ReadyToConnect("Ready to connect"),
    // During connection
    Connecting("Connecting..."),
    // Impossible to connect
    CantConnect("Cannot connect"),
    // Connected
    Connected("Connected");

    private String string;

    ConnectorConnectionState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
