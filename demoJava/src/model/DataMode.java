package model;

public enum DataMode {
    TEXT("văn bản"),
    FILE("tệp tin");

    private final String displayName;

    DataMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
