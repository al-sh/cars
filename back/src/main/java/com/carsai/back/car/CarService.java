package com.carsai.back.car;

import com.carsai.back.car.dto.CarDto;
import com.carsai.back.car.dto.CarSearchCriteria;
import com.carsai.back.car.dto.CarShortDto;
import com.carsai.back.car.dto.SearchResult;
import com.carsai.back.common.dto.PagedResponse;
import com.carsai.back.common.exception.CarNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_LLM_RESULTS = 10;

    private final CarRepository carRepository;

    public PagedResponse<CarDto> search(CarSearchCriteria criteria, int page, int perPage) {
        Specification<Car> spec = buildSpecification(criteria);
        Page<Car> cars = carRepository.findAll(spec,
                PageRequest.of(page - 1, Math.min(perPage, MAX_PAGE_SIZE),
                        Sort.by("price").ascending()));
        return PagedResponse.from(cars, CarDto::from);
    }

    public CarDto getById(UUID id) {
        return carRepository.findById(id)
                .map(CarDto::from)
                .orElseThrow(() -> new CarNotFoundException(id));
    }

    public List<String> getAllBrands() {
        return carRepository.findDistinctBrands();
    }

    /**
     * Поиск для MessageService: возвращает упрощённый результат для форматирования LLM.
     */
    public SearchResult searchForChat(CarSearchCriteria criteria, int limit) {
        Specification<Car> spec = buildSpecification(criteria);

        List<Car> cars = carRepository.findAll(spec,
                PageRequest.of(0, Math.min(limit, MAX_LLM_RESULTS),
                        Sort.by("price").ascending()))
                .getContent();

        long totalCount = carRepository.count(spec);

        return SearchResult.builder()
                .count((int) totalCount)
                .items(cars.stream().map(this::toShortDto).toList())
                .build();
    }

    private Specification<Car> buildSpecification(CarSearchCriteria criteria) {
        Specification<Car> spec = Specification.where(null);

        if (criteria.getPriceMin() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), criteria.getPriceMin()));
        }
        if (criteria.getPriceMax() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), criteria.getPriceMax()));
        }
        if (criteria.getBodyType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("bodyType"), criteria.getBodyType()));
        }
        if (criteria.getEngineType() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("engineType"), criteria.getEngineType()));
        }
        if (criteria.getBrand() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("brand")), criteria.getBrand().toLowerCase()));
        }
        if (criteria.getYearMin() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("year"), criteria.getYearMin()));
        }
        if (criteria.getYearMax() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("year"), criteria.getYearMax()));
        }
        if (criteria.getSeats() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("seats"), criteria.getSeats()));
        }
        if (criteria.getTransmission() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("transmission"), criteria.getTransmission()));
        }
        if (criteria.getDrive() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("drive"), criteria.getDrive()));
        }
        if (criteria.getMinPower() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("powerHp"), criteria.getMinPower()));
        }
        if (criteria.getMaxPower() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("powerHp"), criteria.getMaxPower()));
        }
        if (criteria.getMaxFuelConsumption() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fuelConsumption"), criteria.getMaxFuelConsumption()));
        }

        return spec;
    }

    private CarShortDto toShortDto(Car car) {
        return new CarShortDto(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getYear(),
                car.getPrice(),
                car.getBodyType(),
                car.getEngineType(),
                car.getPowerHp(),
                car.getTransmission(),
                car.getDrive()
        );
    }
}
