
package com.example.barber_shop.data.model;

public class SalonService {

    private final String id;
    private final String name;
    private final String category;
    private final String description;
    private final int durationMinutes;
    private final long priceVnd;
    private final int imageResId;

    public SalonService(
            String id,
            String name,
            String category,
            String description,
            int durationMinutes,
            long priceVnd,
            int imageResId
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.priceVnd = priceVnd;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public long getPriceVnd() {
        return priceVnd;
    }

    public int getImageResId() {
        return imageResId;
    }
}

