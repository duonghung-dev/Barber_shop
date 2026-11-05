
package com.example.barber_shop.data.repository;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.SalonService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ServiceRepository {

    private ServiceRepository() {
    }

    public static List<SalonService> getSampleServices() {

        List<SalonService> services = new ArrayList<>();

        services.add(new SalonService(
                "DV001",
                "Cắt tóc nam",
                "Cắt tóc",
                "Tư vấn và cắt tóc phù hợp với khuôn mặt.",
                30,
                100000,
                R.drawable.ic_splash_scissors
        ));

        services.add(new SalonService(
                "DV002",
                "Cắt tóc + Gội",
                "Cắt tóc",
                "Cắt tóc kết hợp gội và sấy hoàn thiện.",
                45,
                150000,
                R.drawable.ic_splash_scissors
        ));

        services.add(new SalonService(
                "DV003",
                "Tạo kiểu Pomade",
                "Tạo kiểu",
                "Tạo kiểu tóc bằng sản phẩm chuyên dụng.",
                20,
                80000,
                R.drawable.ic_splash_scissors
        ));

        services.add(new SalonService(
                "DV004",
                "Uốn tạo kiểu",
                "Tạo kiểu",
                "Tạo độ phồng và định hình kiểu tóc.",
                90,
                350000,
                R.drawable.ic_splash_scissors
        ));

        services.add(new SalonService(
                "DV005",
                "Gội đầu thư giãn",
                "Chăm sóc",
                "Làm sạch da đầu và massage thư giãn.",
                25,
                70000,
                R.drawable.ic_splash_scissors
        ));

        services.add(new SalonService(
                "DV006",
                "Chăm sóc tóc",
                "Chăm sóc",
                "Dưỡng và phục hồi tóc cơ bản.",
                50,
                180000,
                R.drawable.ic_splash_scissors
        ));

        return services;
    }

    // Lọc dịch vụ theo chi nhánh mẫu
    public static List<SalonService> getServicesForBranch(
            String branchId
    ) {
        if (branchId == null) {
            return getSampleServices();
        }

        Set<String> serviceIds;

        switch (branchId) {

            case "CN001":
                serviceIds = new HashSet<>(
                        Arrays.asList(
                                "DV001", "DV002", "DV003",
                                "DV004", "DV005", "DV006"
                        )
                );
                break;

            case "CN002":
                serviceIds = new HashSet<>(
                        Arrays.asList(
                                "DV001", "DV002",
                                "DV003", "DV005"
                        )
                );
                break;

            case "CN003":
                serviceIds = new HashSet<>(
                        Arrays.asList(
                                "DV001", "DV003", "DV004",
                                "DV005", "DV006"
                        )
                );
                break;

            default:
                return new ArrayList<>();
        }

        List<SalonService> result = new ArrayList<>();

        for (SalonService service : getSampleServices()) {
            if (serviceIds.contains(service.getId())) {
                result.add(service);
            }
        }

        return result;
    }

    public static SalonService findById(String serviceId) {

        if (serviceId == null) {
            return null;
        }

        for (SalonService service : getSampleServices()) {

            if (service.getId().equals(serviceId)) {
                return service;
            }
        }

        return null;
    }

}
