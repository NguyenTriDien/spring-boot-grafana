# Demo Spring Boot Product API

## Mô tả
API CRUD cho quản lý sản phẩm với kiến trúc: Controller -> Service -> Repository -> Entity -> DTO

## Cấu trúc Project
```
src/main/java/com/example/demo/
├── controller/
│   └── ProductController.java
├── service/
│   └── ProductService.java
├── repository/
│   └── ProductRepository.java
├── entity/
│   └── Product.java
├── dto/
│   ├── ProductDto.java
│   ├── CreateProductDto.java
│   └── UpdateProductDto.java
└── exception/
    └── GlobalExceptionHandler.java
```

## Cấu hình Database
Cập nhật file `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## API Endpoints

### 1. Lấy tất cả sản phẩm
```
GET /api/products
```

### 2. Lấy sản phẩm theo ID
```
GET /api/products/{id}
```

### 3. Tạo sản phẩm mới
```
POST /api/products
Content-Type: application/json

{
    "name": "Tên sản phẩm",
    "description": "Mô tả sản phẩm",
    "price": 100000.00
   
}
```

### 4. Cập nhật sản phẩm
```
PUT /api/products/{id}
Content-Type: application/json

{
    "name": "Tên sản phẩm mới",
    "price": 120000.00,
    "stockQuantity": 60
}
```

### 5. Xóa sản phẩm (Soft Delete)
```
DELETE /api/products/{id}
```

### 6. Tìm kiếm sản phẩm theo tên
```
GET /api/products/search?name=keyword
```

### 7. Lấy sản phẩm theo danh mục
```
GET /api/products/category/{category}
```

### 8. Lấy sản phẩm theo khoảng giá
```
GET /api/products/price-range?minPrice=100000&maxPrice=500000
```

### 9. Lấy sản phẩm có số lượng tồn kho thấp
```
GET /api/products/low-stock?threshold=10
```

### 10. Kích hoạt sản phẩm
```
PATCH /api/products/{id}/activate
```

### 11. Vô hiệu hóa sản phẩm
```
PATCH /api/products/{id}/deactivate
```

## Validation Rules

### CreateProductDto
- `name`: Bắt buộc, không được để trống
- `price`: Bắt buộc, phải lớn hơn 0
- `stockQuantity`: Bắt buộc, phải lớn hơn hoặc bằng 0

### UpdateProductDto
- `price`: Nếu có, phải lớn hơn 0
- `stockQuantity`: Nếu có, phải lớn hơn hoặc bằng 0

## Chạy ứng dụng

### Cách 1: Chạy trực tiếp với Maven

1. Cài đặt dependencies:
```bash
mvn clean install
```

2. Chạy ứng dụng:
```bash
mvn spring-boot:run
```

3. Truy cập API tại: `http://localhost:8080/api/products`

### Cách 2: Chạy với Docker Compose (Khuyến nghị)

1. Build và chạy toàn bộ stack:
```bash
# Build application
mvn clean package -DskipTests

# Chạy với Docker Compose
docker-compose up -d
```

2. Truy cập các service:
- **Spring Boot API**: http://localhost:8080/api/products
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **MySQL**: localhost:3306

3. Dừng services:
```bash
docker-compose down
```

4. Xóa volumes (dữ liệu):
```bash
docker-compose down -v
```

### Cấu hình Docker

- **MySQL**: Database `demo_db` với user `demo_user`/`demo_password`
- **Prometheus**: Monitor Spring Boot app tại `/actuator/prometheus`
- **Grafana**: Dashboard mẫu cho Spring Boot metrics
- **Health Checks**: Tự động kiểm tra sức khỏe của các service

## Tính năng

- ✅ CRUD operations đầy đủ
- ✅ Validation với Bean Validation
- ✅ Soft Delete
- ✅ Tìm kiếm và lọc sản phẩm
- ✅ Global Exception Handling
- ✅ Cross-Origin support
- ✅ Transactional support
- ✅ Lombok để giảm boilerplate code
- ✅ Docker containerization
- ✅ MySQL database integration
- ✅ Prometheus monitoring
- ✅ Grafana dashboards
- ✅ Health checks
- ✅ Automated deployment scripts

## Monitoring & Observability

### Prometheus Metrics
Spring Boot app expose metrics tại `/actuator/prometheus` bao gồm:
- HTTP request metrics
- JVM memory và thread metrics
- Database connection pool metrics
- Custom business metrics

### Grafana Dashboards
Dashboard mẫu bao gồm:
- HTTP Request Rate
- Response Time
- JVM Memory Usage
- Thread States
- Database Connections
- Error Rate

### Health Checks
- Spring Boot Actuator health endpoint
- MySQL connection health
- Container health checks 