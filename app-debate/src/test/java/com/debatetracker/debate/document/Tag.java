package com.debatetracker.debate.document;

public enum Tag {

    DEBATE_API("Debate API"),
    ;

    private final String displayName;

    Tag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
