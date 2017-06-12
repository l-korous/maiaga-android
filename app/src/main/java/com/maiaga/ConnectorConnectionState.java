package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ConnectorConnectionState {
    // Initial state with no user action
    InitializedReadyToConnect,
    // During connection
    Connecting,
    // Impossible to connect
    CantConnect,
    // Connected
    Connected
}
