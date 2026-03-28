package com.example.defensemanagement.common;

public class StudentMaterialContext {

    public static final String SOURCE_PREFERRED_PDF = "PREFERRED_PDF";
    public static final String SOURCE_FIRST_AVAILABLE_PDF = "FIRST_AVAILABLE_PDF";
    public static final String SOURCE_METADATA_FALLBACK = "METADATA_FALLBACK";
    public static final String SOURCE_EMPTY = "EMPTY";

    private final String content;
    private final String source;
    private final String reason;

    public StudentMaterialContext(String content, String source, String reason) {
        this.content = content;
        this.source = source;
        this.reason = reason;
    }

    public String getContent() {
        return content;
    }

    public String getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }
}
