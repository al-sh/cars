package com.carsai.back.car;

import com.carsai.back.car.dto.CarDto;
import com.carsai.back.car.dto.CarSearchCriteria;
import com.carsai.back.common.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @GetMapping
    public PagedResponse<CarDto> searchCars(
            @RequestParam(required = false) Integer priceMin,
            @RequestParam(required = false) Integer priceMax,
            @RequestParam(required = false) BodyType bodyType,
            @RequestParam(required = false) EngineType engineType,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false) Integer seats,
            @RequestParam(required = false) Transmission transmission,
            @RequestParam(required = false) DriveType drive,
            @RequestParam(required = false) Integer minPower,
            @RequestParam(required = false) Integer maxPower,
            @RequestParam(required = false) BigDecimal maxFuelConsumption,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage
    ) {
        CarSearchCriteria criteria = CarSearchCriteria.builder()
                .priceMin(priceMin)
                .priceMax(priceMax)
                .bodyType(bodyType)
                .engineType(engineType)
                .brand(brand)
                .yearMin(yearMin)
                .yearMax(yearMax)
                .seats(seats)
                .transmission(transmission)
                .drive(drive)
                .minPower(minPower)
                .maxPower(maxPower)
                .maxFuelConsumption(maxFuelConsumption)
                .build();

        return carService.search(criteria, page, perPage);
    }

    @GetMapping("/{id}")
    public CarDto getCar(@PathVariable UUID id) {
        return carService.getById(id);
    }

    @GetMapping("/brands")
    public List<String> getBrands() {
        return carService.getAllBrands();
    }
}
