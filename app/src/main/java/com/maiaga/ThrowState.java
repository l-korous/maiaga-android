package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ThrowState {
    NoThrow("Waiting for a throw..."),
    InThrow("Flying..."),
    AfterThrow("Getting results..."),
    // The Processor is never physically in this state, it reports it is and then it switches to None right away.
    ResultsAvailable("Good job!");

    private String string;

    ThrowState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
