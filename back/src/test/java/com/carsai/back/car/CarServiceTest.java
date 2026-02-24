package com.carsai.back.car;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.carsai.back.car.dto.CarDto;
import com.carsai.back.car.dto.CarSearchCriteria;
import com.carsai.back.car.dto.SearchResult;
import com.carsai.back.common.dto.PagedResponse;
import com.carsai.back.common.exception.CarNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    // ===== search() тесты =====

    @Test
    @SuppressWarnings("unchecked")
    void search_shouldReturnPagedResult() {
        // given
        Car car = buildCar(2_500_000, BodyType.SEDAN, EngineType.PETROL);
        when(carRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(car)));

        CarSearchCriteria criteria = CarSearchCriteria.builder()
                .priceMax(3_000_000)
                .bodyType(BodyType.SEDAN)
                .build();

        // when
        PagedResponse<CarDto> result = carService.search(criteria, 1, 20);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).bodyType()).isEqualTo(BodyType.SEDAN);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_shouldCapPerPageAt50() {
        // given — клиент запрашивает 100 на странице, должно получить max 50
        when(carRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        carService.search(CarSearchCriteria.builder().build(), 1, 100);

        // then — PageRequest должен быть создан с size=50, не 100
        verify(carRepository).findAll(any(Specification.class),
                argThat((PageRequest pr) -> pr.getPageSize() == 50));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_shouldBuildSpecification_withAllCriteria() {
        // given — передаём все критерии, убеждаемся что specification не null и запрос вызван
        when(carRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        CarSearchCriteria criteria = CarSearchCriteria.builder()
                .priceMin(1_000_000)
                .priceMax(3_000_000)
                .bodyType(BodyType.SUV)
                .engineType(EngineType.PETROL)
                .brand("Toyota")
                .yearMin(2020)
                .yearMax(2024)
                .seats(5)
                .transmission(Transmission.AUTOMATIC)
                .drive(DriveType.AWD)
                .minPower(150)
                .maxPower(250)
                .maxFuelConsumption(BigDecimal.valueOf(9.0))
                .build();

        // when
        carService.search(criteria, 1, 20);

        // then — метод поиска должен быть вызван ровно один раз
        verify(carRepository, times(1))
                .findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_shouldBuildEmptySpecification_whenNoCriteria() {
        // given — пустые критерии → Specification.where(null) → SELECT * FROM cars
        when(carRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        carService.search(CarSearchCriteria.builder().build(), 1, 20);

        // then
        verify(carRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    // ===== getById() тесты =====

    @Test
    void getById_shouldReturnCar_whenExists() {
        // given
        UUID id = UUID.randomUUID();
        Car car = buildCar(2_000_000, BodyType.HATCHBACK, EngineType.DIESEL);
        car.setId(id);

        when(carRepository.findById(id)).thenReturn(Optional.of(car));

        // when
        CarDto result = carService.getById(id);

        // then
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.bodyType()).isEqualTo(BodyType.HATCHBACK);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        // given
        UUID id = UUID.randomUUID();
        when(carRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> carService.getById(id))
                .isInstanceOf(CarNotFoundException.class);
    }

    // ===== getAllBrands() тесты =====

    @Test
    void getAllBrands_shouldReturnSortedList() {
        // given
        when(carRepository.findDistinctBrands())
                .thenReturn(List.of("Hyundai", "Kia", "Mazda", "Toyota"));

        // when
        List<String> brands = carService.getAllBrands();

        // then
        assertThat(brands).containsExactly("Hyundai", "Kia", "Mazda", "Toyota");
    }

    // ===== searchForChat() тесты =====

    @Test
    @SuppressWarnings("unchecked")
    void searchForChat_shouldReturnSearchResult() {
        // given
        Car car = buildCar(3_000_000, BodyType.SUV, EngineType.PETROL);
        when(carRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(car)));
        when(carRepository.count(any(Specification.class))).thenReturn(5L);

        CarSearchCriteria criteria = CarSearchCriteria.builder()
                .priceMax(3_500_000)
                .bodyType(BodyType.SUV)
                .transmission(Transmission.AUTOMATIC)
                .build();

        // when
        SearchResult result = carService.searchForChat(criteria, 10);

        // then
        assertThat(result.count()).isEqualTo(5); // totalCount из БД
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).bodyType()).isEqualTo(BodyType.SUV);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchForChat_shouldCapLimitAt10() {
        // given — LLM не должен получать больше 10 результатов
        when(carRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(carRepository.count(any(Specification.class))).thenReturn(0L);

        // when
        carService.searchForChat(CarSearchCriteria.builder().build(), 50);

        // then — PageRequest должен быть создан с size=10, не 50
        verify(carRepository).findAll(any(Specification.class),
                argThat((PageRequest pr) -> pr.getPageSize() == 10));
    }

    // ===== Вспомогательный метод =====

    private Car buildCar(int price, BodyType bodyType, EngineType engineType) {
        return Car.builder()
                .id(UUID.randomUUID())
                .brand("Toyota")
                .model("Test")
                .year(2023)
                .price(price)
                .bodyType(bodyType)
                .engineType(engineType)
                .engineVolume(BigDecimal.valueOf(2.0))
                .powerHp(150)
                .transmission(Transmission.AUTOMATIC)
                .drive(DriveType.FWD)
                .seats(5)
                .fuelConsumption(BigDecimal.valueOf(7.5))
                .build();
    }
}
