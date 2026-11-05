
package com.example.barber_shop.data.model;

public class Branch {

    private final String id;
    private final String name;
    private final String address;
    private final String workingHours;
    private final double rating;
    private final int imageResId;

    public Branch(
            String id,
            String name,
            String address,
            String workingHours,
            double rating,
            int imageResId
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.workingHours = workingHours;
        this.rating = rating;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public double getRating() {
        return rating;
    }

    public int getImageResId() {
        return imageResId;
    }
}

