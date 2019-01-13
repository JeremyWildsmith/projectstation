package com.jevaengine.spacestation;

import java.io.Serializable;

public enum DamageSeverity implements Serializable {
    None(0),
    VeryMinor(1),
    Minor(2),
    Serious(3),
    VerySerious(4),
    Critical(5);

    private final int rating;

    DamageSeverity(int rating) {
        this.rating = rating;
    }

    public boolean isAtLeast(DamageSeverity severity) {
        return rating >= severity.rating;
    }

}
