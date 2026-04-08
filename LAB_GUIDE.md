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

## Hướng dẫn sử dụng file JMeter (.jmx)

File test plan đã được cấu hình sẵn tại: **`jmeter/performance-lab.jmx`**

### Mở file JMX

1. Khởi động **Apache JMeter** (phiên bản 5.x trở lên)
2. Vào **File → Open** → chọn file `jmeter/performance-lab.jmx`
3. Cấu trúc Test Plan sẽ hiển thị như sau:

```
📁 Performance Lab Test Plan
├── ⚙️ HTTP Request Defaults (localhost:8082)
├── 📊 View Results Tree
├── 📊 Summary Report
├── ❌ Bug 01 - CPU Overload          (disabled)
├── ❌ Bug 02 - Memory Leak           (disabled)
├── ❌ Bug 03 - Connection Leak       (disabled)
├── ❌ Bug 04 - N+1 Query             (disabled)
├── ❌ Bug 05 - Slow Query            (disabled)
├── ❌ Bug 06 - Thread Starvation     (disabled)
├── ❌ Bug 07 - Unbounded Cache       (disabled)
├── ❌ Bug 08 - Large Payload         (disabled)
├── ❌ Bug 09 - DB Row Lock           (disabled)
├── ❌ Bug 10 - Deadlock (Side A)     (disabled)
├── ❌ Bug 10 - Deadlock (Side B)     (disabled)
├── ❌ Bug 11 - Full Table Scan       (disabled)
├── ❌ Bug 12 - GC Pressure           (disabled)
├── ❌ Healthy - Baseline             (disabled)
├── ❌ Healthy - Cached Baseline      (disabled)
└── ❌ RESET - Cleanup                (disabled)
```

### Cách chạy từng bài Lab

**Bước 1:** Click phải vào Thread Group muốn test → chọn **Enable**

**Bước 2:** Nhấn nút **▶️ Start** (hoặc Ctrl+R) để bắt đầu test

**Bước 3:** Mở **Grafana Dashboard** ([http://localhost:3000](http://localhost:3000)) để quan sát

**Bước 4:** Sau khi test xong, nhấn **⏹ Stop** và **Disable** Thread Group vừa test

**Bước 5:** Nếu test các bài leak (Memory, Connection, Cache), **Enable** group **RESET - Cleanup** rồi chạy 1 lần để dọn dẹp

### Lưu ý quan trọng

| Lưu ý | Chi tiết |
|-------|---------|
| 🔴 **Chỉ enable 1 bug tại 1 thời điểm** | Nếu chạy nhiều bug cùng lúc, triệu chứng sẽ trộn lẫn, khó phân biệt trên Dashboard |
| 🔴 **Deadlock cần enable CẢ 2 group** | Bài Deadlock phải enable đồng thời **Side A** + **Side B** mới tái tạo được deadlock |
| 🟡 **Reset sau bài Leak** | Bài 2 (Memory), bài 3 (Connection), bài 7 (Cache) sẽ leak tài nguyên. Sau khi test xong **phải chạy RESET** |
| 🟢 **Thay đổi Host/Port** | Nếu app không chạy trên `localhost:8082`, sửa biến `BASE_HOST` và `BASE_PORT` trong **User Defined Variables** ở đầu Test Plan |
| 🟢 **Xem kết quả** | Click vào **View Results Tree** để xem chi tiết từng request. Click **Summary Report** để xem thống kê tổng hợp (TPS, Avg, Min, Max, Error%) |

### Bảng cấu hình Thread Group

| Thread Group | Threads | Ramp-up | Loop | Tổng requests |
|-------------|---------|---------|------|---------------|
| Bug 01 - CPU Overload | 20 | 5s | 50 | 1,000 |
| Bug 02 - Memory Leak | 5 | 2s | 100 | 500 |
| Bug 03 - Connection Leak | 5 | 2s | 30 | 150 |
| Bug 04 - N+1 Query | 10 | 3s | 20 | 200 |
| Bug 05 - Slow Query | 5 | 2s | 10 | 50 |
| Bug 06 - Thread Starvation | 20 | 1s | 5 | 100 |
| Bug 07 - Unbounded Cache | 10 | 3s | 200 | 2,000 |
| Bug 08 - Large Payload | 5 | 2s | 5 | 25 |
| Bug 09 - DB Row Lock | 10 | 1s | 3 | 30 |
| Bug 10 - Deadlock (A+B) | 10+10 | 1s | 5 | 100 |
| Bug 11 - Full Table Scan | 3 | 1s | 5 | 15 |
| Bug 12 - GC Pressure | 10 | 3s | 50 | 500 |
| Healthy Baseline | 20 | 5s | 100 | 2,000 |
| Healthy Cached | 20 | 5s | 100 | 2,000 |

### Quy trình demo đề xuất

```
1. Chạy "Healthy - Baseline" trước ──→ Quan sát Dashboard bình thường (baseline)
2. Disable Baseline, Enable Bug muốn demo ──→ Quan sát sự thay đổi trên Dashboard
3. Phân tích: Panel nào thay đổi? Thay đổi thế nào?
4. Dừng test, chạy RESET nếu cần
5. Lặp lại với Bug tiếp theo
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

## Bài 9: Database Row Lock Contention

### API: `GET /v1/api/lab/db-row-lock`

### 🔍 Nguyên nhân gốc
Code mở transaction, **UPDATE một row** rồi **giữ transaction mở 5 giây** trước khi commit. Trong thời gian này, InnoDB đặt **exclusive row lock** trên row đó → tất cả request khác muốn UPDATE cùng row phải **chờ** cho đến khi transaction hoàn thành.

```
Transaction 1: BEGIN → UPDATE id=1 → Sleep 5s → COMMIT (giữ lock 5s)
Transaction 2: BEGIN → UPDATE id=1 → ⏳ BLOCKED... chờ Transaction 1
Transaction 3: BEGIN → UPDATE id=1 → ⏳ BLOCKED... chờ Transaction 1 + 2
```

Trong thực tế, lỗi này thường xảy ra khi:
- Transaction thực hiện quá nhiều logic (gọi external API, tính toán) trong khi giữ lock
- Nhiều user cùng update 1 record hot (ví dụ: cập nhật số dư tài khoản chung)
- Batch job update hàng loạt records mà không chia nhỏ transaction
- `SELECT ... FOR UPDATE` giữ lock quá lâu

### 🎯 Cách nhận biết
- Request đầu tiên nhanh (~5s), request thứ 2 mất ~10s, thứ 3 mất ~15s (cộng dồn)
- Response time tăng **tuyến tính** theo số request đang chờ
- Khác thread-blocked: thread-blocked do code Java, db-row-lock do **MySQL InnoDB**
- Nếu mỗi request UPDATE row **khác nhau** → nhanh bình thường (chỉ cùng row mới bị)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **API Response Time** | 📈 Tăng tuyến tính: 5s, 10s, 15s, 20s... |
| **Database Connections** | 📈 Running/Pending tăng (connections giữ transaction mở) |
| **HikariCP Connection Pool** | 📈 Active tăng (connections bị giữ chờ lock) |
| **Database CPU** | ⚖️ Bình thường (MySQL chỉ chờ, không tính toán) |

### 🧪 Cách test bằng JMeter
- Thread Group: **10 threads**, Ramp-up: **1s** (quan trọng: gửi gần như đồng thời), Loop: 3
- HTTP Request: `GET http://localhost:8082/v1/api/lab/db-row-lock`

---

## Bài 10: Deadlock

### API: `GET /v1/api/lab/deadlock?side=A` và `GET /v1/api/lab/deadlock?side=B`

### 🔍 Nguyên nhân gốc
Hai transaction **khóa chéo nhau** (circular lock):

```
Transaction A: Lock row 1 → Sleep 1s → Cố lock row 2 → ❌ BỊ BLOCK (B đang giữ row 2)
Transaction B: Lock row 2 → Sleep 1s → Cố lock row 1 → ❌ BỊ BLOCK (A đang giữ row 1)
```

Cả hai đều chờ nhau → **DEADLOCK**! MySQL InnoDB phát hiện và **kill 1 transaction** (trả về lỗi), transaction còn lại hoàn thành.

Trong thực tế, lỗi này thường xảy ra khi:
- 2 API cùng update 2+ bảng/rows nhưng theo **thứ tự khác nhau**
- Transfer tiền: API A chuyển từ tài khoản 1→2, API B chuyển từ 2→1 đồng thời
- Batch processing + real-time API cùng chạm vào cùng dữ liệu
- INSERT/UPDATE với FOREIGN KEY trigger lock cascade

### 🎯 Cách nhận biết
- Một số request trả về **HTTP 500** với lỗi `Deadlock found when trying to get lock`
- Không phải tất cả request đều thất bại — MySQL chỉ kill **1 bên**, bên kia thành công
- Xuất hiện **ngẫu nhiên** khi traffic cao (khó reproduce khi test manual)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **API TPS** | 📉 TPS giảm, một số request bị lỗi → Grafana tính TPS thấp hơn |
| **API Response Time** | 📈 Có spike ~1-2s (thời gian chờ lock trước khi deadlock detect) |
| **Database CPU** | ⚖️ Bình thường |

### 🧪 Cách test bằng JMeter
Cần **2 Thread Group chạy đồng thời**:
- **Thread Group A**: 10 threads, `GET http://localhost:8082/v1/api/lab/deadlock?side=A`
- **Thread Group B**: 10 threads, `GET http://localhost:8082/v1/api/lab/deadlock?side=B`
- Cả 2 group **bắt đầu cùng lúc** → tạo ra deadlock

---

## Bài 11: Full Table Scan (Missing Index)

### API: `GET /v1/api/lab/full-table-scan?keyword=cao+cấp`

### 🔍 Nguyên nhân gốc
Câu SQL sử dụng `LIKE '%keyword%'` trên cột `description` — cột này **không có INDEX**:

```sql
SELECT id, name, price FROM products 
WHERE description LIKE '%cao cấp%'    -- ← KHÔNG dùng được index!
ORDER BY price DESC LIMIT 20
```

Vì dùng `%` ở **đầu chuỗi**, MySQL **không thể dùng bất kỳ index nào** → phải quét **TOÀN BỘ 10 triệu records** một cách tuần tự.

Trong thực tế, lỗi này thường xảy ra khi:
- Tìm kiếm full-text bằng `LIKE '%...%'` thay vì dùng Full-Text Index
- Quên tạo index cho cột được dùng trong WHERE/JOIN/ORDER BY
- Query trên bảng nhỏ lúc dev → deploy lên production bảng có hàng triệu records
- Composite index không đúng thứ tự cột

### 🎯 Cách nhận biết
- Response time **cực cao** (có thể 10-30 giây trên 10M records)
- DB CPU tăng **rất mạnh** (phải đọc toàn bộ data từ disk)
- Response body chứa trường `scan_time_ms` cho thấy thời gian query
- Nếu `EXPLAIN` câu SQL sẽ thấy `type: ALL` (full scan)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **API Response Time** | 📈 **Cực cao** (10-30s), phụ thuộc vào tốc độ disk |
| **Database CPU** | 📈 **Tăng rất mạnh** — khác biệt rõ nhất so với các bug khác |
| **Database RAM** | 📈 Tăng (MySQL buffer pool phải load data) |
| **Database Connections** | Running tăng (connection bị giữ chờ query hoàn thành) |
| **Top 10 Slowest SQL** | Câu LIKE sẽ xuất hiện ở **top 1** |

### 🧪 Cách test bằng JMeter
- Thread Group: **3 threads**, Ramp-up: 1s, Loop: 5
- HTTP Request: `GET http://localhost:8082/v1/api/lab/full-table-scan?keyword=cao+cấp`
- ⚠️ **Dùng ít threads** vì mỗi query đã rất nặng trên 10M records

---

## Bài 12: GC Pressure (Garbage Collection Storm)

### API: `GET /v1/api/lab/gc-pressure`

### 🔍 Nguyên nhân gốc
Code tạo **3 triệu String objects** liên tiếp, mỗi 500K items lại xóa hết → JVM Garbage Collector phải chạy liên tục để dọn dẹp. Trong lúc GC chạy (**Stop-the-World pause**), tất cả thread xử lý request bị **đóng băng tạm thời**.

```java
for (int i = 0; i < 3_000_000; i++) {
    tempList.add("item_" + i + "_" + System.nanoTime()); // tạo object mới
    if (i % 500_000 == 0) tempList.clear();              // xóa → GC phải dọn
}
```

Trong thực tế, lỗi này thường xảy ra khi:
- Xử lý file lớn đọc từng dòng tạo object
- String concatenation trong vòng lặp (nên dùng StringBuilder)
- Autoboxing: `int` → `Integer` trong collection lớn
- JSON parsing tạo nhiều temporary object
- Report/export tạo hàng triệu cell objects

### 🎯 Cách nhận biết
- Response time **không ổn định** — có lúc nhanh, có lúc bị **spike đột ngột** (do GC pause)
- CPU tăng nhưng **phần lớn là GC overhead**, không phải business logic
- RAM dao động lên xuống **răng cưa** (tăng nhanh rồi drop khi GC dọn)
- Khác CPU-heavy: CPU-heavy luôn chậm đều, GC-pressure chậm **không đều** (spike)

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **App CPU Usage** | 📈 Tăng cao, nhưng phần lớn là **GC overhead** |
| **App Heap RAM Usage** | 📈📉 **Răng cưa** — tăng nhanh rồi drop đột ngột khi GC chạy |
| **API Response Time** | 📈 Có **spike không đều** (GC pause gây latency spike) |
| **API TPS** | 📉 Giảm không đều, có lúc bình thường, có lúc giảm đột ngột |

### 🧪 Cách test bằng JMeter
- Thread Group: **10 threads**, Ramp-up: 3s, Loop: 50
- HTTP Request: `GET http://localhost:8082/v1/api/lab/gc-pressure`

---

## Bài 12.1: Database CPU Heavy

### API: `GET /v1/api/lab/db-cpu-heavy`

### 🔍 Nguyên nhân gốc
Câu query sử dụng hàm `BENCHMARK()` của MySQL để lặp lại việc tính toán băm `MD5(RAND())` mười triệu lần. Hàm này được sinh ra để test performance nội bộ MySQL, và nó sẽ ngay lập tức kéo 1 DB CPU core lên 100% trong vài giây.

Trong thực tế, lỗi này thường xảy ra khi:
- Sử dụng các hàm xử lý chuỗi (String manipulation), JSON processing nặng trực tiếp trong `SELECT` hoặc `WHERE`.
- Các câu query JOIN quá phức tạp trên hàng triệu dòng dữ liệu.
- Tính toán toán học hoặc gom nhóm (`GROUP BY`) cực kỳ phức tạp trên dữ liệu lớn.

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **Database CPU Usage (%)** | 📈 Tăng vọt lên 100% (hoặc hơn tuỳ số core) |
| **Top 10 SQL Queries (DB CPU %)** | 📈 Câu lệnh `SELECT BENCHMARK...` sẽ đứng đầu với giá trị CPU cao |
| **Top 10 Slowest SQL Queries (P99 Latency)** | 📈 P99 latency của câu lệnh BENCHMARK sẽ tăng vọt (> 1.5 - 2s) |
| **App CPU Usage (%)** | ⚖️ Rất thấp, vì App chỉ đứng đợi MySQL làm việc |
| **API Response Time** | 📈 Tăng lên vài giây |

### 🧪 Cách test bằng JMeter
- Bạn dùng JMeter tạo 1 copy thread group bất kỳ và đổi tên thành "Bug 12.1 - DB CPU Heavy"
- HTTP Request: `GET http://localhost:8082/v1/api/lab/db-cpu-heavy`
- Thread Group: **10 threads**, Loop: 30

---

## Bài 12.2: Database RAM Heavy (Database Memory Bloat)

### API: `GET /v1/api/lab/db-ram-heavy`

### 🔍 Nguyên nhân gốc
API này tăng kích thước giới hạn bảng tạm (`tmp_table_size`, `max_heap_table_size`) lên 1GB, sau đó ép MySQL thực hiện subquery tạo ra dữ liệu padding rất lớn (~72KB mỗi dòng) rồi `ORDER BY RAND()`.
Điều này khiến MySQL phải nạp khối lượng dữ liệu khổng lồ đó vào RAM để phục vụ việc sắp xếp ngẫu nhiên (In-memory Sort Buffer). RAM DB sẽ tăng vọt hàng trăm MB đột ngột!

Trong thực tế, lỗi này thường xảy ra khi:
- `ORDER BY` hoặc `GROUP BY` trên số lượng lớn dữ liệu mà không có Index hỗ trợ.
- Fetch quá nhiều field rác (`SELECT *` chứa cột LONGTEXT hoặc BLOB) rồi thực hiện JOIN.
- MySQL không đủ RAM gây tràn sort buffer và OOM Killer.

### 📊 Cách xem trên Grafana
| Panel | Biểu hiện |
|-------|-----------|
| **Database RAM Usage (%)** | 📈 Tăng vọt thành bậc thang, tạo đỉnh nhọn hoặc giữ neo cao tuỳ cấu hình MySQL |
| **Top 10 SQL Queries (DB CPU %)** | 📈 Tăng nhẹ (do overhead RAM allocation và sort tầng DB) |
| **App Heap RAM Usage (%)** | ⚖️ Rất thấp, vì database gánh toàn bộ dữ liệu, chỉ trả về 1 số COUNT |
| **Database CPU Usage (%)** | 📈 Tăng nhẹ (do overhead RAM allocation) |

### 🧪 Cách test bằng JMeter
- Bạn dùng JMeter tạo 1 copy thread group bất kỳ và đổi tên thành "Bug 12.2 - DB RAM Heavy"
- HTTP Request: `GET http://localhost:8082/v1/api/lab/db-ram-heavy`
- Thread Group: **5 threads**, Loop: 10
- ⚠️ Cảnh báo: Chạy nhiều thread có thể gây sập container Database do hết RAM!

---

## Bài 13 & 14: So sánh với API Healthy

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
| 9 | db-row-lock | Row Lock Contention | Response Time + DB Connections | Response tăng tuyến tính 5s, 10s, 15s |
| 10 | deadlock | Deadlock | TPS (xuất hiện lỗi 500) | Một số request HTTP 500, không đều |
| 11 | full-table-scan | Missing Index | DB CPU + Response Time | DB CPU tăng rất mạnh, query >10s |
| 12 | gc-pressure | GC Storm | App Heap RAM (răng cưa) | RAM lên xuống răng cưa, spike không đều |
| 13 | healthy | ✅ Baseline | Tất cả | Mọi chỉ số bình thường |
| 14 | healthy-cached | ✅ Baseline + Cache | Tất cả | Response time cực thấp |

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
| db-row-lock | | | | |
| deadlock | | | | |
| full-table-scan | | | | |
| gc-pressure | | | | |

