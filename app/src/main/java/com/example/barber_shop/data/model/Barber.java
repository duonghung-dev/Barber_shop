
package com.example.barber_shop.data.model;

public class Barber {

    private final String id;
    private final String name;
    private final String branchId;
    private final String specialty;
    private final int experienceYears;
    private final double rating;
    private final int imageResId;

    public Barber(
            String id,
            String name,
            String branchId,
            String specialty,
            int experienceYears,
            double rating,
            int imageResId
    ) {
        this.id = id;
        this.name = name;
        this.branchId = branchId;
        this.specialty = specialty;
        this.experienceYears = experienceYears;
        this.rating = rating;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getSpecialty() {
        return specialty;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public double getRating() {
        return rating;
    }

    public int getImageResId() {
        return imageResId;
    }
}
