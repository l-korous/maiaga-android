package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ThrowState {
    NoThrow("Waiting for a throw..."),
    InThrow("Flying..."),
    AfterThrow("Cooool");

    private String string;

    ThrowState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
