# Car Catalog Service

> **Зависимости:** CONTEXT.md, contracts/api.md, contracts/types.md, infrastructure/database.md

Сервис управления справочником автомобилей.

---

## Ответственность

- Хранение справочника автомобилей
- Поиск и фильтрация по параметрам
- Предоставление данных для MessageService

---

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                      Controller                          │
│                    CarController                         │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                       Service                            │
│                     CarService                           │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                     Repository                           │
│                   CarRepository                          │
└─────────────────────────┬───────────────────────────────┘
                          │
                          ▼
                     PostgreSQL
```

---

## Слой Controller

### CarController

```java
@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
public class CarController {
    
    private final CarService carService;
    
    @GetMapping
    public PagedResponse<CarDto> searchCars(
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(required = false) BodyType bodyType,
        @RequestParam(required = false) EngineType engineType,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) Integer minYear,
        @RequestParam(required = false) Integer maxYear,
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
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .bodyType(bodyType)
            .engineType(engineType)
            .brand(brand)
            .minYear(minYear)
            .maxYear(maxYear)
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
```

---

## Слой Service

### CarService

```java
@Service
@RequiredArgsConstructor
public class CarService {
    
    private final CarRepository carRepository;
    
    public PagedResponse<CarDto> search(CarSearchCriteria criteria, int page, int perPage) {
        Specification<Car> spec = buildSpecification(criteria);
        
        Page<Car> cars = carRepository.findAll(spec,
            PageRequest.of(page - 1, Math.min(perPage, 50), 
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
     * Метод для MessageService.
     * Возвращает упрощённый результат для форматирования LLM.
     */
    public SearchResult searchForChat(CarSearchCriteria criteria, int limit) {
        Specification<Car> spec = buildSpecification(criteria);
        
        List<Car> cars = carRepository.findAll(spec,
            PageRequest.of(0, Math.min(limit, 10), Sort.by("price").ascending()))
            .getContent();
        
        long totalCount = carRepository.count(spec);
        
        return ToolSearchResult.builder()
            .count((int) totalCount)
            .items(cars.stream()
                .map(this::toShortDto)
                .toList())
            .build();
    }
    
    private Specification<Car> buildSpecification(CarSearchCriteria criteria) {
        Specification<Car> spec = Specification.where(null);
        
        if (criteria.minPrice() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
        }
        
        if (criteria.maxPrice() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
        }
        
        if (criteria.bodyType() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("bodyType"), criteria.bodyType()));
        }
        
        if (criteria.engineType() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("engineType"), criteria.engineType()));
        }
        
        if (criteria.brand() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(cb.lower(root.get("brand")), criteria.brand().toLowerCase()));
        }
        
        if (criteria.minYear() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("year"), criteria.minYear()));
        }
        
        if (criteria.maxYear() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("year"), criteria.maxYear()));
        }
        
        if (criteria.seats() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("seats"), criteria.seats()));
        }
        
        if (criteria.transmission() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("transmission"), criteria.transmission()));
        }
        
        if (criteria.drive() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("drive"), criteria.drive()));
        }
        
        if (criteria.minPower() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("powerHp"), criteria.minPower()));
        }
        
        if (criteria.maxPower() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("powerHp"), criteria.maxPower()));
        }
        
        if (criteria.maxFuelConsumption() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("fuelConsumption"), 
                    criteria.maxFuelConsumption()));
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
```

---

## DTO

### CarSearchCriteria

См. `contracts/types.md` — единый источник истины.

### SearchResult

Для возврата в LLM:

```java
@Builder
public record ToolSearchResult(
    int count,
    List<CarShortDto> items
) {}

public record CarShortDto(
    UUID id,
    String brand,
    String model,
    int year,
    int price,
    BodyType bodyType,
    EngineType engineType,
    int powerHp,
    Transmission transmission,
    DriveType drive
) {}
```

---

## Repository

```java
@Repository
public interface CarRepository extends JpaRepository<Car, UUID>, 
                                       JpaSpecificationExecutor<Car> {
    
    @Query("SELECT DISTINCT c.brand FROM Car c ORDER BY c.brand")
    List<String> findDistinctBrands();
}
```

---

## Entity

```java
@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String brand;
    
    @Column(nullable = false)
    private String model;
    
    @Column(nullable = false)
    private int year;
    
    @Column(nullable = false)
    private int price;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false)
    private BodyType bodyType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "engine_type", nullable = false)
    private EngineType engineType;
    
    @Column(name = "engine_volume")
    private BigDecimal engineVolume;
    
    @Column(name = "power_hp")
    private int powerHp;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transmission transmission;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriveType drive;
    
    @Column(nullable = false)
    private int seats;
    
    @Column(name = "fuel_consumption")
    private BigDecimal fuelConsumption;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

---

## Наполнение справочника

### Источники данных

Для MVP — ручное наполнение или импорт из CSV/JSON.

### Seed Data

```sql
-- Примеры автомобилей для тестирования
INSERT INTO cars (id, brand, model, year, price, body_type, engine_type, 
                  engine_volume, power_hp, transmission, drive, seats, 
                  fuel_consumption, description) VALUES

-- Кроссоверы
('uuid-1', 'Toyota', 'RAV4', 2023, 3500000, 'SUV', 'PETROL', 
 2.5, 199, 'AUTOMATIC', 'AWD', 5, 8.1, 'Популярный семейный кроссовер'),
 
('uuid-2', 'Mazda', 'CX-5', 2023, 3200000, 'SUV', 'PETROL', 
 2.5, 194, 'AUTOMATIC', 'AWD', 5, 7.8, 'Стильный и управляемый'),

-- Седаны
('uuid-3', 'Toyota', 'Camry', 2023, 3000000, 'SEDAN', 'PETROL', 
 2.5, 200, 'AUTOMATIC', 'FWD', 5, 8.5, 'Бизнес-седан'),

-- Хэтчбеки
('uuid-4', 'Volkswagen', 'Golf', 2022, 2500000, 'HATCHBACK', 'PETROL', 
 1.4, 150, 'AUTOMATIC', 'FWD', 5, 6.5, 'Компактный городской автомобиль'),

-- Электромобили
('uuid-5', 'Tesla', 'Model 3', 2023, 4500000, 'SEDAN', 'ELECTRIC', 
 NULL, 283, 'AUTOMATIC', 'AWD', 5, NULL, 'Запас хода 500 км');
```

---

## Индексы

```sql
-- Для частых фильтров
CREATE INDEX idx_cars_price ON cars(price);
CREATE INDEX idx_cars_body_type ON cars(body_type);
CREATE INDEX idx_cars_engine_type ON cars(engine_type);
CREATE INDEX idx_cars_brand ON cars(brand);
CREATE INDEX idx_cars_year ON cars(year);

-- Составной индекс для типичных запросов
CREATE INDEX idx_cars_search ON cars(body_type, engine_type, price);
```

---

## Конфигурация

```yaml
# application.yml

car-catalog:
  default-page-size: 20
  max-page-size: 50
  max-results-for-llm: 10
```
