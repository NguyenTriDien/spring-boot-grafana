# 🧪 Performance Testing Lab Guide

## Mục tiêu

Học viên sẽ sử dụng **JMeter** để tạo tải lên các API có lỗi performance, sau đó quan sát **Grafana Dashboard** để phát hiện, phân tích nguyên nhân gốc rễ và hiểu cách nhận biết từng loại lỗi trong thực tế.

---

## Chuẩn bị

1. **Khởi động hệ thống:**
   ```bash
   docker compose up -d --build
   ```

2. **Truy cập Grafana:** [http://localhost:3000](http://localhost:3000)
   - User: `admin` / Pass: `admin`
   - Vào Dashboard → **Tester Overview v2**

3. **Base URL API:** `http://localhost:8082`

4. **Reset sau mỗi bài lab:** Gọi API reset để xóa sạch tài nguyên bị leak
   ```bash
   curl -X POST http://localhost:8082/v1/api/lab/reset
   ```

---

## Sơ đồ Dashboard Grafana

```
┌─────────────────────────┬─────────────────────────┐
│   App CPU Usage (%)     │  App Heap RAM Usage (%)  │   ← Hàng 1: Tài nguyên App
├─────────────────────────┼─────────────────────────┤
│   API TPS (req/s)       │  API Response Time       │   ← Hàng 2: Hiệu năng API
├────────────┬────────────┼─────────────────────────┤
│  DB CPU %  │  DB RAM %  │  Database Connections    │   ← Hàng 3: Tài nguyên Database
├─────────────────────────┼─────────────────────────┤
│  HikariCP Connection    │  JVM Threads             │   ← Hàng 4: Chi tiết nội bộ App
│  Pool                   │  (Live/Blocked/Waiting)  │
├─────────────────────────┴─────────────────────────┤
│  Top 10 Slowest SQL Queries                       │   ← Hàng 5: SQL chậm nhất
└───────────────────────────────────────────────────┘
```

---

## Bài 1: CPU Overload

### API: `GET /v1/api/lab/cpu-heavy`

### 🔍 Nguyên nhân gốc
Code thực hiện **tính toán rất nặng** ngay trên thread xử lý request:
- Tạo mảng 2 triệu phần tử random
- Sắp xếp (sort) toàn bộ mảng — độ phức tạp O(n·log·n)
- Hash SHA-256 lặp lại 100 lần

Trong thực tế, lỗi này thường xảy ra khi:
- Xử lý thuật toán phức tạp trực tiếp trong API (mã hóa, nén, xử lý ảnh)
- Vòng lặp quá lớn không kiểm soát
- Regular Expression phức tạp (ReDoS)

### 🎯 Cách nhận biết
- Response time của API **tăng cao** (>1-2 giây)
- Khi tăng số lượng request đồng thời, **tất cả API khác cũng bị chậm** (vì CPU bị chiếm hết)
- App trả lời đúng kết quả nhưng **rất chậm**

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **App CPU Usage (%)** | 📈 Tăng vọt lên **80-100%** khi có request |
| **API TPS** | 📉 TPS thấp vì mỗi request mất nhiều thời gian |
| **API Response Time** | 📈 p99 tăng cao (>1s) |

### 🧪 Cách test bằng JMeter
- Thread Group: **20 threads**, Ramp-up: 5s, Loop: 50
- HTTP Request: `GET http://localhost:8082/v1/api/lab/cpu-heavy`

---

## Bài 2: Memory Leak

### API: `GET /v1/api/lab/memory-leak`

### 🔍 Nguyên nhân gốc
Mỗi request **thêm 1MB dữ liệu vào một biến static** (biến tồn tại suốt vòng đời ứng dụng). Dữ liệu này **không bao giờ được giải phóng** vì biến static luôn giữ tham chiếu → Garbage Collector không thể dọn.

Trong thực tế, lỗi này thường xảy ra khi:
- Lưu dữ liệu vào `static List/Map` mà quên xóa
- Event listener được đăng ký nhưng không bao giờ hủy
- Cache tự implement không có cơ chế eviction
- File/Stream mở nhưng không close

### 🎯 Cách nhận biết
- RAM **chỉ tăng, KHÔNG BAO GIỜ giảm** dù đã ngừng gửi request
- Sau một thời gian, app bị **OutOfMemoryError** và crash
- Garbage Collection chạy nhưng **không giải phóng được bộ nhớ**
- Khác biệt so với API bình thường: API bình thường RAM tăng rồi giảm (GC dọn được)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **App Heap RAM Usage (%)** | 📈 **Tăng đều đặn như bậc thang**, mỗi bậc = 1 request. Không bao giờ giảm |
| **App CPU Usage (%)** | 📈 Có thể tăng nhẹ do GC chạy liên tục cố gắng thu hồi bộ nhớ |

### 🧪 Cách test bằng JMeter
- Thread Group: **5 threads**, Ramp-up: 2s, Loop: 100
- HTTP Request: `GET http://localhost:8082/v1/api/lab/memory-leak`
- ⚠️ **Sau khi test xong, gọi reset API** để tránh crash app

---

## Bài 3: Database Connection Exhaustion

### API: `GET /v1/api/lab/connection-leak`

### 🔍 Nguyên nhân gốc
Code lấy connection từ Connection Pool (HikariCP) **nhưng không bao giờ trả lại** (không gọi `connection.close()`). Mỗi request "mượn" 1 connection rồi giữ vĩnh viễn → Pool cạn kiệt → request mới phải chờ → timeout.

Trong thực tế, lỗi này thường xảy ra khi:
- Dùng `DataSource.getConnection()` trực tiếp mà quên close trong `finally`
- Exception xảy ra trước khi `connection.close()` được gọi
- Transaction bị treo (long-running transaction)
- Connection pool size quá nhỏ cho lượng traffic

### 🎯 Cách nhận biết
- Những request đầu tiên **nhanh bình thường**
- Sau khi pool đầy (~100 connections), request mới bị **timeout 30s** rồi lỗi
- Lỗi điển hình: `Connection is not available, request timed out after 30000ms`
- Restart app thì hết lỗi (vì connection được trả lại)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **HikariCP Connection Pool** | 📈 **Active (in use) tăng dần** từ 50 → 80 → 100. **Idle (available) giảm về 0**. **Pending (waiting) xuất hiện** |
| **Database Connections** | 📈 Active/Connected tăng dần tương ứng |
| **API Response Time** | 📈 Đột ngột tăng lên **30s** (timeout) khi pool cạn |
| **API TPS** | 📉 Giảm mạnh → gần 0 khi pool hết |

### 🧪 Cách test bằng JMeter
- Thread Group: **5 threads**, Ramp-up: 2s, Loop: 30
- HTTP Request: `GET http://localhost:8082/v1/api/lab/connection-leak`
- ⚠️ **Sau khi test xong, gọi reset API** để trả lại connections

---

## Bài 4: N+1 Query Problem

### API: `GET /v1/api/lab/n-plus-one`

### 🔍 Nguyên nhân gốc
Code thực hiện **1 câu SQL lấy danh sách 500 products**, sau đó **lặp qua từng product và query riêng lẻ thông tin merchant** → tổng cộng **501 câu SQL** cho 1 request (1 + N).

```
SQL 1:   SELECT * FROM products LIMIT 500           ← 1 query
SQL 2:   SELECT * FROM merchants WHERE id = 1       ← cho product 1
SQL 3:   SELECT * FROM merchants WHERE id = 2       ← cho product 2
...
SQL 501: SELECT * FROM merchants WHERE id = 3       ← cho product 500
```

Trong thực tế, lỗi này thường xảy ra khi:
- Dùng ORM (Hibernate/JPA) với quan hệ `@ManyToOne` mà không dùng `JOIN FETCH`
- Vòng lặp gọi DB trong service layer
- Lazy loading kích hoạt trong vòng lặp

### 🎯 Cách nhận biết
- Response time **cao bất thường** so với lượng data trả về (chỉ 500 records mà mất >1s)
- Response body chứa trường `total_queries: 501` — đây là dấu hiệu rõ ràng
- Nếu có query log, sẽ thấy **hàng trăm câu SELECT giống nhau** lặp lại

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **API Response Time** | 📈 Response time cao (>500ms) dù data không lớn |
| **Database CPU** | 📈 Tăng nhẹ do phải xử lý nhiều câu SQL |
| **Top 10 Slowest SQL** | Thấy câu `SELECT * FROM merchants WHERE id = ?` xuất hiện với tần suất rất cao |
| **API TPS** | 📉 TPS thấp hơn bình thường |

### 🧪 Cách test bằng JMeter
- Thread Group: **10 threads**, Ramp-up: 3s, Loop: 20
- HTTP Request: `GET http://localhost:8082/v1/api/lab/n-plus-one`

---

## Bài 5: Slow SQL Query

### API: `GET /v1/api/lab/slow-query`

### 🔍 Nguyên nhân gốc
Code thực thi câu SQL **CROSS JOIN** (tích Descartes) kết hợp với hàm `SLEEP()`:
```sql
SELECT p1.name, p2.name, SLEEP(0.01) 
FROM products p1 CROSS JOIN products p2 
LIMIT 300
```
- `CROSS JOIN` tạo ra **N × N** tổ hợp (rất nặng cho DB engine)
- `SLEEP(0.01)` → mỗi dòng ngủ 10ms → 300 dòng × 10ms = **~3 giây**

Trong thực tế, lỗi này thường xảy ra khi:
- Query thiếu index → full table scan trên bảng lớn
- JOIN sai điều kiện → cartesian product (CROSS JOIN ngầm)
- Subquery có tương quan (correlated subquery) chạy lặp
- `SELECT *` trên bảng có cột TEXT/BLOB lớn

### 🎯 Cách nhận biết
- Response time **rất cao và ổn định** (luôn ~3s, không phụ thuộc vào load)
- Database CPU tăng cao hơn bình thường
- Không có lỗi kết quả, chỉ **chậm**

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **API Response Time** | 📈 p99 **luôn ở mức ~3s**, rất ổn định (không random) |
| **Database CPU** | 📈 Tăng rõ rệt khi có nhiều request |
| **Database Connections** | Running/Pending tăng (connection bị giữ lâu) |
| **Top 10 Slowest SQL** | Câu CROSS JOIN xuất hiện ở top đầu |

### 🧪 Cách test bằng JMeter
- Thread Group: **5 threads**, Ramp-up: 2s, Loop: 10
- HTTP Request: `GET http://localhost:8082/v1/api/lab/slow-query`

---

## Bài 6: Thread Starvation (Deadlock/Bottleneck)

### API: `GET /v1/api/lab/thread-blocked`

### 🔍 Nguyên nhân gốc
Code sử dụng `synchronized` trên **một object duy nhất** (global lock), kết hợp `Thread.sleep(3000)`. Hậu quả:
- **Chỉ 1 thread được xử lý tại 1 thời điểm**
- Tất cả thread khác phải **xếp hàng chờ** (blocked state)
- Nếu có 20 request đồng thời → request cuối phải chờ 60 giây!

```java
synchronized (GLOBAL_LOCK) {   // ← Chỉ 1 thread vào được
    Thread.sleep(3000);         // ← Giữ lock 3 giây
}
```

Trong thực tế, lỗi này thường xảy ra khi:
- Dùng `synchronized` trên shared resource mà xử lý lâu
- Deadlock giữa 2+ threads (lock A chờ lock B, lock B chờ lock A)
- Thread pool bị cạn vì task chạy quá lâu
- External API call bị timeout trong synchronized block

### 🎯 Cách nhận biết
- TPS **cực thấp** (~0.3 req/s) dù CPU và RAM còn thừa
- Response time **tăng tuyến tính** theo số request đang chờ (3s, 6s, 9s, 12s...)
- Nhiều request bị **timeout**
- CPU, RAM, DB đều bình thường → điểm nghẽn ở **logic code**

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **API TPS** | 📉 **Tối đa chỉ ~0.3 req/s** (1 request / 3 giây) |
| **API Response Time** | 📈 p99 tăng **liên tục** (3s → 30s → 60s...) |
| **JVM Threads** | 📈 **Blocked Threads tăng vọt** = số thread đang chờ lock |
| **App CPU Usage** | ⚖️ **Bình thường** (vì thread chỉ ngủ, không tính toán) |

### 🧪 Cách test bằng JMeter
- Thread Group: **20 threads**, Ramp-up: 1s, Loop: 5
- HTTP Request: `GET http://localhost:8082/v1/api/lab/thread-blocked`

---

## Bài 7: Unbounded Cache (OOM Cache)

### API: `GET /v1/api/lab/unbounded-cache?key=xxx`

### 🔍 Nguyên nhân gốc
Code cache dữ liệu vào `ConcurrentHashMap` tĩnh với key là tham số từ user. Mỗi key khác nhau tạo **1 entry ~100KB**, nhưng **không có cơ chế xóa** (no eviction, no TTL, no max size).

Nếu gọi với 10,000 key khác nhau → cache chiếm ~1GB RAM.

Trong thực tế, lỗi này thường xảy ra khi:
- Tự implement cache bằng `HashMap` mà không giới hạn
- Cache key chứa dữ liệu từ user input (vô hạn key)
- Quên cấu hình `maxSize` hoặc `TTL` cho cache library (Guava, Caffeine)
- Session storage không có timeout

### 🎯 Cách nhận biết  
- RAM tăng **tỷ lệ thuận** với số lượng key duy nhất
- **Giống memory-leak** nhưng khác ở nguyên nhân: leak do bug code, cache do thiếu cấu hình
- API trả về trường `cache_entries` cho thấy cache liên tục tăng
- Nếu gọi lại cùng key → nhanh (cache hit). Key mới → RAM tăng thêm

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **App Heap RAM Usage (%)** | 📈 Tăng đều, giống memory-leak. Tốc độ tăng phụ thuộc vào số key mới |
| **App CPU Usage** | ⚖️ Bình thường (cache lookup nhanh) |
| **API Response Time** | ⚖️ Nhanh (vì data lấy từ cache) — đây là điểm khác memory-leak |

### 🧪 Cách test bằng JMeter
- Thread Group: **10 threads**, Ramp-up: 3s, Loop: 200
- HTTP Request: `GET http://localhost:8082/v1/api/lab/unbounded-cache?key=${__Random(1,100000)}`
- ⚠️ Dùng `${__Random()}` để tạo key khác nhau mỗi lần

---

## Bài 8: Oversized Response Payload

### API: `GET /v1/api/lab/large-payload`

### 🔍 Nguyên nhân gốc
API trả về **100,000 records** (200 products × 500 lần nhân bản) kèm dữ liệu padding → response body **~50MB**. 

Vấn đề:
- Server phải **serialize toàn bộ data** vào JSON trước khi gửi → tốn RAM
- Network phải truyền tải ~50MB → tốn bandwidth, client chờ lâu
- Nhiều request đồng thời → RAM bùng nổ (10 request × 50MB = 500MB)

Trong thực tế, lỗi này thường xảy ra khi:
- API trả về toàn bộ records mà không phân trang (pagination)
- `SELECT *` thay vì chọn đúng cột cần thiết
- Trả về nested object quá sâu (entity → related entities → ...)
- Không giới hạn `page size` từ phía backend

### 🎯 Cách nhận biết
- Response time **rất cao** (>5 giây)
- RAM tăng **đột ngột** khi có request (không tăng dần như leak)
- RAM **giảm lại** sau khi response hoàn thành (khác memory-leak)
- Response body cực lớn (có thể dùng DevTools Network tab để thấy)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **App Heap RAM Usage (%)** | 📈 **Tăng đột ngột** khi có request, **giảm xuống** sau khi xong (khác memory-leak) |
| **API Response Time** | 📈 Rất cao (>5s), p99 có thể >10s |
| **App CPU Usage** | 📈 Tăng (do JSON serialization) |
| **API TPS** | 📉 Rất thấp vì mỗi request mất nhiều giây |

### 🧪 Cách test bằng JMeter
- Thread Group: **5 threads**, Ramp-up: 2s, Loop: 5
- HTTP Request: `GET http://localhost:8082/v1/api/lab/large-payload`

---

## Bài 9 & 10: So sánh với API Healthy

### API: `GET /v1/api/lab/healthy` và `GET /v1/api/lab/healthy-cached`

### 🔍 Mục đích
Dùng 2 API này làm **baseline** (chuẩn so sánh):
- `/healthy`: Query đơn giản `SELECT COUNT(*)` — nhanh, nhẹ
- `/healthy-cached`: Giống healthy nhưng **có Redis cache** → lần gọi thứ 2+ không truy cập DB

### 📊 Kết quả mong đợi trên Grafana
| Panel | healthy | healthy-cached |
|-------|---------|----------------|
| **API TPS** | Cao (~200+ req/s) | Rất cao (~500+ req/s) |
| **API Response Time** | Thấp (<50ms) | Cực thấp (<5ms) |
| **App CPU** | Ổn định | Ổn định |
| **App RAM** | Ổn định | Ổn định |

---

## Reset sau khi demo

```bash
# Reset tất cả resource bị leak (memory, connections, cache)
curl -X POST http://localhost:8082/v1/api/lab/reset

# Hoặc restart container
docker compose restart app
```

---

## Bảng tổng kết

| # | API | Loại lỗi | Panel Grafana chính | Dấu hiệu đặc trưng |
|---|-----|----------|--------------------|--------------------|
| 1 | cpu-heavy | CPU Overload | App CPU Usage | CPU tăng vọt 80-100% |
| 2 | memory-leak | Memory Leak | App Heap RAM | RAM tăng bậc thang, không giảm |
| 3 | connection-leak | Connection Exhaustion | HikariCP Pool | Active tăng, Idle giảm về 0 |
| 4 | n-plus-one | N+1 Query | Response Time + Slow SQL | 501 queries cho 1 request |
| 5 | slow-query | Slow SQL | Response Time + DB CPU | p99 luôn ~3s cố định |
| 6 | thread-blocked | Thread Starvation | JVM Threads + TPS | Blocked threads tăng, TPS ~0.3 |
| 7 | unbounded-cache | Cache OOM | App Heap RAM | RAM tăng theo số key, response nhanh |
| 8 | large-payload | Oversized Response | App Heap RAM + Response Time | RAM tăng đột ngột rồi giảm |
| 9 | healthy | ✅ Baseline | Tất cả | Mọi chỉ số bình thường |
| 10 | healthy-cached | ✅ Baseline + Cache | Tất cả | Response time cực thấp |

---

## Bảng bài tập (Học viên điền sau khi làm lab)

| API | Lỗi phát hiện được | Panel Grafana nào thấy rõ nhất | Nguyên nhân gốc | Cách khắc phục đề xuất |
|-----|--------------------|-----------------------------|----------------|----------------------|
| cpu-heavy | | | | |
| memory-leak | | | | |
| connection-leak | | | | |
| n-plus-one | | | | |
| slow-query | | | | |
| thread-blocked | | | | |
| unbounded-cache | | | | |
| large-payload | | | | |
