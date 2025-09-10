package jp.readscape.consumer.domain.users.model;

public enum Gender {
    MALE("男性"),
    FEMALE("女性"),
    OTHER("その他"),
    NOT_DISCLOSED("非公開");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}