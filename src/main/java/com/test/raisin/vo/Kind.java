package com.test.raisin.vo;

public enum Kind {
    JOINED("joined"),
    ORPHANED("orphaned"),
    DEFECTIVE("defective");

    public final String kind;

    Kind(String kind) {
        this.kind = kind;
    }

}
