package com.test.raisin.vo;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("msg")
public class ResponseB {
    String id;
    String done;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDone() {
        return done;
    }

    public void setDone(String done) {
        this.done = done;
    }
}
