# Learn Performance Test - Product API

## Mô tả ngắn
API CRUD quản lý sản phẩm. Entity `Product` quan hệ ManyToOne với `Merchant` qua cột `merchant_id`. Mapping DTO sử dụng MapStruct.

- Base URL: `http://localhost:8080`
- Tiền tố API: `/api/products`
- Content-Type: `application/json`

## DTO hiện tại
- `ProductRequest`
  - `id` (Long, optional - bỏ qua khi tạo)
  - `name` (String, required)
  - `description` (String, optional)
  - `price` (Number/Decimal, required)
  - `merchantId` (Long, required)
  - `isDeleted` (Boolean, optional, default `false`)

- `ProductDto` (response)
  - `id` (Long)
  - `name` (String)
  - `description` (String)
  - `price` (Number/Decimal)
  - `merchantId` (Long)
  - `isDeleted` (Boolean)
  - `createdAt` (String, ISO)
  - `updatedAt` (String, ISO)

## Endpoints

### 1) Tạo sản phẩm
```
POST /api/products
```
Request body:
```json
{
  "name": "iPhone 15 Pro",
  "description": "A17 Pro, Titanium",
  "price": 25000000.00,
  "merchantId": 1,
  "isDeleted": false
}
```
Response 200 (ProductDto):
```json
{
  "id": 10,
  "name": "iPhone 15 Pro",
  "description": "A17 Pro, Titanium",
  "price": 25000000.00,
  "merchantId": 1,
  "isDeleted": false,
  "createdAt": "2025-08-22T10:00:00",
  "updatedAt": "2025-08-22T10:00:00"
}
```
Curl:
```bash
curl -X POST http://localhost:8080/v1/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name":"iPhone 15 Pro",
    "description":"A17 Pro, Titanium",
    "price":25000000.00,
    "merchantId":1,
    "isDeleted":false
  }'
```

### 2) Cập nhật sản phẩm
```
PUT /api/products/{id}
```
Request body (cùng schema với tạo):
```json
{
  "name": "iPhone 15 Pro (2025)",
  "description": "A17 Pro, Titanium",
  "price": 26000000.00,
  "merchantId": 1,
  "isDeleted": false
}
```
Response 200 (ProductDto) trả về bản ghi sau cập nhật.
Curl:
```bash
curl -X PUT http://localhost:8080/v1/api/products/10 \
  -H "Content-Type: application/json" \
  -d '{
    "name":"iPhone 15 Pro (2025)",
    "description":"A17 Pro, Titanium",
    "price":26000000.00,
    "merchantId":1,
    "isDeleted":false
  }'
```

### 3) Xóa sản phẩm
```
DELETE /api/products/{id}
```
Response 204 No Content.
Curl:
```bash
curl -X DELETE http://localhost:8080/v1/api/products/10
```

### 4) Lấy chi tiết sản phẩm
```
GET /api/products/{id}
```
Response 200 (ProductDto):
```json
{
  "id": 10,
  "name": "iPhone 15 Pro",
  "description": "A17 Pro, Titanium",
  "price": 25000000.00,
  "merchantId": 1,
  "isDeleted": false,
  "createdAt": "2025-08-22T10:00:00",
  "updatedAt": "2025-08-22T10:00:00"
}
```
Curl:
```bash
curl http://localhost:8080/v1/api/products/10
```

### 5) Danh sách sản phẩm (phân trang)
```
GET /api/products
```
Query params chuẩn Spring Data:
- `page` (mặc định 0)
- `size` (mặc định 20)
- `sort` (ví dụ `name,asc` hoặc nhiều sort: `name,asc&sort=price,desc`)

Ví dụ:
```
GET /api/products?page=0&size=10&sort=createdAt,desc
```
Response 200 (Page<ProductDto>) ví dụ rút gọn:
```json
{
  "content": [
    {
      "id": 10,
      "name": "iPhone 15 Pro",
      "description": "A17 Pro, Titanium",
      "price": 25000000.00,
      "merchantId": 1,
      "isDeleted": false,
      "createdAt": "2025-08-22T10:00:00",
      "updatedAt": "2025-08-22T10:00:00"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": { "sorted": true, "unsorted": false, "empty": false },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```
Curl:
```bash
curl "http://localhost:8080/v1/api/products?page=0&size=10&sort=createdAt,desc"
```

### 6) Tìm sản phẩm theo khoảng thời gian tạo
```
GET /api/products/by-date-range
```
Query params:
- `startDate` (String, required) - Ngày bắt đầu (ISO 8601 format: yyyy-MM-ddTHH:mm:ss)
- `endDate` (String, required) - Ngày kết thúc (ISO 8601 format: yyyy-MM-ddTHH:mm:ss)
- `page` (mặc định 0)
- `size` (mặc định 20)
- `sort` (ví dụ `createdAt,desc`)

Ví dụ:
```
GET /api/products/by-date-range?startDate=2025-01-01T00:00:00&endDate=2025-12-31T23:59:59&page=0&size=10&sort=createdAt,desc
```
Response 200 (Page<ProductDto>) tương tự như endpoint danh sách sản phẩm:
```json
{
  "content": [
    {
      "id": 10,
      "name": "iPhone 15 Pro",
      "description": "A17 Pro, Titanium",
      "price": 25000000.00,
      "merchantId": 1,
      "isDeleted": false,
      "createdAt": "2025-08-22T10:00:00",
      "updatedAt": "2025-08-22T10:00:00"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": { "sorted": true, "unsorted": false, "empty": false },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```
Curl:
```bash
curl "http://localhost:8080/v1/api/products/by-date-range?startDate=2025-01-01T00:00:00&endDate=2025-12-31T23:59:59&page=0&size=10&sort=createdAt,desc"
```

---

## Merchant API
- Tiền tố API: `/api/merchants`

### DTO
- `MerchantRequest`
  - `name` (String, required)

- `MerchantDto`
  - `id` (Long)
  - `name` (String)

### 1) Tạo merchant
```
POST /api/merchants
```
Request body:
```json
{ "name": "Apple Inc." }
```
Response 200 (MerchantDto):
```json
{ "id": 1, "name": "Apple Inc." }
```
Curl:
```bash
curl -X POST http://localhost:8080/v1/api/merchants \
  -H "Content-Type: application/json" \
  -d '{"name":"Apple Inc."}'
```

### 2) Cập nhật merchant
```
PUT /api/merchants/{id}
```
Request body:
```json
{ "name": "Apple" }
```
Response 200 (MerchantDto) trả về bản ghi sau cập nhật.
Curl:
```bash
curl -X PUT http://localhost:8080/v1/api/merchants/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Apple"}'
```

### 3) Xóa merchant
```
DELETE /api/merchants/{id}
```
Response 204 No Content.
Curl:
```bash
curl -X DELETE http://localhost:8080/v1/api/merchants/1
```

### 4) Lấy chi tiết merchant
```
GET /api/merchants/{id}
```
Response 200 (MerchantDto):
```json
{ "id": 1, "name": "Apple Inc." }
```
Curl:
```bash
curl http://localhost:8080/v1/api/merchants/1
```

### 5) Danh sách merchant (phân trang)
```
GET /api/merchants
```
Query params: `page`, `size`, `sort`

Ví dụ:
```
GET /api/merchants?page=0&size=10&sort=name,asc
```
Response 200 (Page<MerchantDto>) ví dụ rút gọn:
```json
{
  "content": [ { "id": 1, "name": "Apple Inc." } ],
  "pageable": { "pageNumber": 0, "pageSize": 10 },
  "totalElements": 1,
  "totalPages": 1
}
```
Curl:
```bash
curl "http://localhost:8080/v1/api/merchants?page=0&size=10&sort=name,asc"
```

## Ghi chú
- `price` được lưu ở DB dạng `DECIMAL(15,2)` và ở entity là `BigDecimal`.
- `merchantId` cần tồn tại trong bảng `merchants` (khóa ngoại).
- MapStruct tự động map `merchantId` ↔ `merchant.id` trong tầng entity/DTO.
- Một số endpoint có dùng cache (`@Cacheable`, `@CachePut`, `@CacheEvict`) với cache name `products` nếu đã bật Spring Cache/Redis.

## Khởi chạy nhanh
```bash
mvn clean package -DskipTests
docker-compose down
docker-compose up -d --build
```
Truy cập: `http://localhost:8080/v1/api/products` 

Lệnh monitor docker
```bash 
docker stats
```

## Các lệnh Docker hữu ích

### Quản lý container
```bash
# Xem danh sách container đang chạy
docker ps

# Xem tất cả container (kể cả đã dừng)
docker ps -a

# Dừng container
docker stop <container_id>

# Khởi động lại container
docker restart <container_id>

# Xóa container
docker rm <container_id>

# Xem logs của container
docker logs <container_id>

# Xem logs real-time
docker logs -f <container_id>

# Truy cập vào container đang chạy
docker exec -it <container_id> /bin/bash
```

### Quản lý images
```bash
# Xem danh sách images
docker images

# Xóa image
docker rmi <image_id>

# Xóa tất cả images không sử dụng
docker image prune -a

# Build image từ Dockerfile
docker build -t <tên_image> .
```

### Quản lý volumes và networks
```bash
# Xem danh sách volumes
docker volume ls

# Xem danh sách networks
docker network ls

# Xem thông tin chi tiết network
docker network inspect <network_name>
```

### Docker Compose
```bash
# Khởi động services
docker-compose up -d

# Dừng services
docker-compose down

# Xem logs của tất cả services
docker-compose logs

# Xem logs của service cụ thể
docker-compose logs <service_name>

# Rebuild và khởi động services
docker-compose up -d --build

# Xem trạng thái services
docker-compose ps

# Dừng và xóa volumes
docker-compose down -v
```

### Monitoring và Debugging
```bash
# Xem thống kê sử dụng tài nguyên
docker stats

# Xem thông tin chi tiết container
docker inspect <container_id>

# Xem thông tin chi tiết image
docker inspect <image_id>

# Xem lịch sử image
docker history <image_id>
```

### Dọn dẹp hệ thống
```bash
# Xóa tất cả container đã dừng
docker container prune

# Xóa tất cả images không sử dụng
docker image prune -a

# Xóa tất cả volumes không sử dụng
docker volume prune

# Xóa tất cả networks không sử dụng
docker network prune

# Dọn dẹp toàn bộ (containers, images, volumes, networks)
docker system prune -a --volumes
```

### Troubleshooting
```bash
# Kiểm tra disk space sử dụng bởi Docker
docker system df

# Xem thông tin hệ thống Docker
docker info

# Kiểm tra version Docker
docker version

# Xem events Docker real-time
docker events
```