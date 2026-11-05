
package com.example.barber_shop.data.model;

public class SearchResult {

    public enum Type {
        BRANCH,
        SERVICE,
        BARBER
    }

    private final String id;
    private final Type type;
    private final String title;
    private final String subtitle;
    private final String description;
    private final int imageResId;

    public SearchResult(
            String id,
            Type type,
            String title,
            String subtitle,
            String description,
            int imageResId
    ) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getTypeLabel() {

        switch (type) {
            case BRANCH:
                return "Chi nhánh";

            case SERVICE:
                return "Dịch vụ";

            case BARBER:
                return "Barber";

            default:
                return "";
        }
    }
}
