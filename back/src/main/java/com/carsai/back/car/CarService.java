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
     * Использует один запрос к БД — Page уже содержит totalElements.
     */
    public SearchResult searchForChat(CarSearchCriteria criteria, int limit) {
        Specification<Car> spec = buildSpecification(criteria);

        Page<Car> page = carRepository.findAll(spec,
                PageRequest.of(0, Math.min(limit, MAX_LLM_RESULTS),
                        Sort.by("price").ascending()));

        return SearchResult.builder()
                .count((int) page.getTotalElements())
                .items(page.getContent().stream().map(CarShortDto::from).toList())
                .build();
    }

    private Specification<Car> buildSpecification(CarSearchCriteria criteria) {
        return Specification
                .where(gte("price", criteria.getPriceMin()))
                .and(lte("price", criteria.getPriceMax()))
                .and(eq("bodyType", criteria.getBodyType()))
                .and(eq("engineType", criteria.getEngineType()))
                .and(eqIgnoreCase("brand", criteria.getBrand()))
                .and(gte("year", criteria.getYearMin()))
                .and(lte("year", criteria.getYearMax()))
                .and(eq("seats", criteria.getSeats()))
                .and(eq("transmission", criteria.getTransmission()))
                .and(eq("drive", criteria.getDrive()))
                .and(gte("powerHp", criteria.getPowerMin()))
                .and(lte("powerHp", criteria.getPowerMax()))
                .and(lte("fuelConsumption", criteria.getFuelConsumptionMax()))
                .and(gte("engineVolume", criteria.getEngineVolumeMin()))
                .and(lte("engineVolume", criteria.getEngineVolumeMax()));
    }

    private <T extends Comparable<T>> Specification<Car> gte(String field, T value) {
        return value == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(field), value);
    }

    private <T extends Comparable<T>> Specification<Car> lte(String field, T value) {
        return value == null ? null
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get(field), value);
    }

    private <T> Specification<Car> eq(String field, T value) {
        return value == null ? null
                : (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private Specification<Car> eqIgnoreCase(String field, String value) {
        return value == null ? null
                : (root, query, cb) -> cb.equal(cb.lower(root.get(field)), value.toLowerCase());
    }
}
