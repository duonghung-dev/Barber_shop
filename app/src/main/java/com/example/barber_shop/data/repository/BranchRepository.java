
package com.example.barber_shop.data.repository;

import androidx.annotation.Nullable;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Branch;

import java.util.ArrayList;
import java.util.List;

public final class BranchRepository {

    private BranchRepository() {
        // Không khởi tạo trực tiếp class này
    }

    // Lấy danh sách chi nhánh mẫu
    public static List<Branch> getSampleBranches() {

        List<Branch> branches = new ArrayList<>();

        branches.add(new Branch(
                "CN001",
                "Barber Shop - Chi nhánh 01",
                "Địa chỉ minh họa 01",
                "08:00 - 21:00",
                4.8,
                R.drawable.ic_guest_nav_branch
        ));

        branches.add(new Branch(
                "CN002",
                "Barber Shop - Chi nhánh 02",
                "Địa chỉ minh họa 02",
                "08:30 - 21:30",
                4.7,
                R.drawable.ic_guest_nav_branch
        ));

        branches.add(new Branch(
                "CN003",
                "Barber Shop - Chi nhánh 03",
                "Địa chỉ minh họa 03",
                "09:00 - 22:00",
                4.9,
                R.drawable.ic_guest_nav_branch
        ));

        return branches;
    }

    // Tìm một chi nhánh dựa vào ID
    @Nullable
    public static Branch findById(String branchId) {

        if (branchId == null) {
            return null;
        }

        for (Branch branch : getSampleBranches()) {

            if (branch.getId().equals(branchId)) {
                return branch;
            }
        }

        return null;
    }
}

