package com.maiaga;

/**
 * Created by lukas on 6/11/2017.
 */

public enum ThrowState {
    NoThrow(App.getContext().getString(R.string.no_throw)),
    InThrow(App.getContext().getString(R.string.in_throw)),
    AfterThrow(App.getContext().getString(R.string.after_throw)),
    // The Processor is never physically in this state, it reports it is and then it switches to None right away.
    ResultsAvailable(App.getContext().getString(R.string.results_available));

    private String string;

    ThrowState(String name){string = name;}

    public String toHumanReadableString() {
        return string;
    }
}
