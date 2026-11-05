
package com.example.barber_shop.data.repository;

import androidx.annotation.Nullable;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Barber;

import java.util.ArrayList;
import java.util.List;

public final class BarberRepository {

    private BarberRepository() {
    }

    // ========================================
    // 1. DANH SÁCH BARBER MẪU
    // ========================================

    public static List<Barber> getSampleBarbers() {

        List<Barber> barbers = new ArrayList<>();

        barbers.add(new Barber(
                "B001",
                "Nguyễn Minh Anh",
                "CN001",
                "Cắt tóc nam",
                5,
                4.8,
                R.drawable.ic_guest_nav_account
        ));

        barbers.add(new Barber(
                "B002",
                "Trần Gia Bảo",
                "CN001",
                "Tạo kiểu",
                4,
                4.7,
                R.drawable.ic_guest_nav_account
        ));

        barbers.add(new Barber(
                "B003",
                "Lê Hoàng Nam",
                "CN002",
                "Chăm sóc tóc",
                6,
                4.9,
                R.drawable.ic_guest_nav_account
        ));

        barbers.add(new Barber(
                "B004",
                "Phạm Quốc Huy",
                "CN002",
                "Cắt tóc nam",
                3,
                4.6,
                R.drawable.ic_guest_nav_account
        ));

        barbers.add(new Barber(
                "B005",
                "Đỗ Thanh Phong",
                "CN003",
                "Tạo kiểu",
                7,
                4.9,
                R.drawable.ic_guest_nav_account
        ));

        barbers.add(new Barber(
                "B006",
                "Võ Minh Khang",
                "CN003",
                "Cắt tóc nam",
                5,
                4.8,
                R.drawable.ic_guest_nav_account
        ));

        return barbers;
    }

    // ========================================
    // 2. TÌM BARBER THEO ID
    // ========================================

    @Nullable
    public static Barber findById(String barberId) {

        if (barberId == null) {
            return null;
        }

        for (Barber barber : getSampleBarbers()) {

            if (barber.getId().equals(barberId)) {
                return barber;
            }
        }

        return null;
    }

    // ========================================
    // 3. LỌC BARBER THEO CHI NHÁNH
    // ========================================

    public static List<Barber> getBarbersForBranch(
            String branchId
    ) {

        List<Barber> result = new ArrayList<>();

        if (branchId == null) {
            return result;
        }

        for (Barber barber : getSampleBarbers()) {

            if (barber.getBranchId().equals(branchId)) {
                result.add(barber);
            }
        }

        return result;
    }
}
