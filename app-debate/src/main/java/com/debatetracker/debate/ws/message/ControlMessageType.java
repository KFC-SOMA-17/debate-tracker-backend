package com.debatetracker.debate.ws.message;

public enum ControlMessageType {
    START,
    STOP,
    ;

    public boolean isStart() {
        return this == START;
    }

    public boolean isStop() {
        return this == STOP;
    }
}
