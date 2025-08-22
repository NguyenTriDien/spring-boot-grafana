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
curl -X POST http://localhost:8080/api/products \
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
curl -X PUT http://localhost:8080/api/products/10 \
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
curl -X DELETE http://localhost:8080/api/products/10
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
curl http://localhost:8080/api/products/10
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
curl "http://localhost:8080/api/products?page=0&size=10&sort=createdAt,desc"
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
Truy cập: `http://localhost:8080/api/products` 