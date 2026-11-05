
package com.example.barber_shop.data.repository;

import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.model.SearchResult;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SearchRepository {

    private SearchRepository() {
    }

    public static List<SearchResult> getAllResults() {

        List<SearchResult> results = new ArrayList<>();

        // ========================================
        // 1. DỮ LIỆU CHI NHÁNH
        // ========================================

        for (Branch branch :
                BranchRepository.getSampleBranches()) {

            results.add(new SearchResult(
                    branch.getId(),
                    SearchResult.Type.BRANCH,
                    branch.getName(),
                    branch.getAddress(),
                    "Giờ hoạt động: " + branch.getWorkingHours(),
                    branch.getImageResId()
            ));
        }

        // ========================================
        // 2. DỮ LIỆU DỊCH VỤ
        // ========================================

        NumberFormat moneyFormatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        for (SalonService service :
                ServiceRepository.getSampleServices()) {

            String description = String.format(
                    Locale.getDefault(),
                    "%d phút · %s đ",
                    service.getDurationMinutes(),
                    moneyFormatter.format(service.getPriceVnd())
            );

            results.add(new SearchResult(
                    service.getId(),
                    SearchResult.Type.SERVICE,
                    service.getName(),
                    service.getCategory(),
                    description,
                    service.getImageResId()
            ));
        }

        // ========================================
        // 3. DỮ LIỆU BARBER
        // ========================================

        for (Barber barber :
                BarberRepository.getSampleBarbers()) {

            Branch branch = BranchRepository.findById(
                    barber.getBranchId()
            );

            String branchName = branch != null
                    ? branch.getName()
                    : "Chưa có thông tin chi nhánh";

            String description = String.format(
                    Locale.getDefault(),
                    "%d năm kinh nghiệm · ★ %.1f",
                    barber.getExperienceYears(),
                    barber.getRating()
            );

            results.add(new SearchResult(
                    barber.getId(),
                    SearchResult.Type.BARBER,
                    barber.getName(),
                    barber.getSpecialty() + " · " + branchName,
                    description,
                    barber.getImageResId()
            ));
        }

        return results;
    }
}

