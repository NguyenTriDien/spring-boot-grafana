# 🧪 Performance Testing Lab Guide

## Mục tiêu

Sử dụng **JMeter** (hoặc bất kỳ load testing tool nào) để tạo tải lên các API, sau đó quan sát **Grafana Dashboard** để phát hiện và phân tích lỗi performance.

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

---

## Danh sách API Lab

| # | API Endpoint | Method | Gợi ý test |
|---|-------------|--------|------------|
| 1 | `/v1/api/lab/cpu-heavy` | GET | 10-20 threads, loop 50 |
| 2 | `/v1/api/lab/memory-leak` | GET | 5 threads, loop 100 |
| 3 | `/v1/api/lab/connection-leak` | GET | 5 threads, loop 30 |
| 4 | `/v1/api/lab/n-plus-one` | GET | 10 threads, loop 20 |
| 5 | `/v1/api/lab/slow-query` | GET | 5 threads, loop 10 |
| 6 | `/v1/api/lab/thread-blocked` | GET | 20 threads, loop 5 |
| 7 | `/v1/api/lab/unbounded-cache?key=${__Random(1,100000)}` | GET | 10 threads, loop 200 |
| 8 | `/v1/api/lab/large-payload` | GET | 5 threads, loop 5 |
| 9 | `/v1/api/lab/healthy` | GET | So sánh |
| 10 | `/v1/api/lab/healthy-cached` | GET | So sánh |

---

## Bài tập

### Bài 1: Phát hiện CPU Overload
1. Dùng JMeter gọi `GET /v1/api/lab/cpu-heavy` với 20 threads
2. Quan sát panel **App CPU Usage (%)** trên Grafana
3. **Câu hỏi:** CPU tăng lên bao nhiều %? Nếu tăng số threads thì sao?

### Bài 2: Phát hiện Memory Leak
1. Gọi `GET /v1/api/lab/memory-leak` với 5 threads, loop 100 lần
2. Quan sát panel **App Heap RAM Usage (%)**
3. **Câu hỏi:** RAM có giảm xuống sau khi ngừng test không? Tại sao?

### Bài 3: Phát hiện Connection Leak
1. Gọi `GET /v1/api/lab/connection-leak` với 5 threads, loop 30 lần
2. Quan sát panel **HikariCP Connection Pool** và **Database Connections**
3. **Câu hỏi:** Active connections tăng nhưng Idle giảm — điều gì xảy ra khi hết connection?

### Bài 4: Phát hiện N+1 Query
1. Gọi `GET /v1/api/lab/n-plus-one` với 10 threads
2. Quan sát panel **API Response Time** và **Top 10 Slowest SQL**
3. **Câu hỏi:** Tại sao response time cao? Xem response body có bao nhiêu queries?

### Bài 5: Phát hiện Slow Query
1. Gọi `GET /v1/api/lab/slow-query` với 5 threads
2. Quan sát panel **API Response Time (p99)** và **Database CPU**
3. **Câu hỏi:** p99 response time là bao nhiêu? DB CPU tăng bao nhiêu?

### Bài 6: Phát hiện Thread Starvation
1. Gọi `GET /v1/api/lab/thread-blocked` với 20 threads đồng thời
2. Quan sát panel **API TPS** và **JVM Threads** (Blocked Threads)
3. **Câu hỏi:** TPS tối đa là bao nhiêu? Tại sao nhiều thread bị blocked?

### Bài 7: Phát hiện Unbounded Cache
1. Gọi `GET /v1/api/lab/unbounded-cache?key=${__Random(1,100000)}` với 10 threads, loop 200
2. Quan sát panel **App Heap RAM Usage (%)**
3. **Câu hỏi:** RAM tăng giống memory-leak không? Khác nhau ở điểm nào?

### Bài 8: Phát hiện Large Payload
1. Gọi `GET /v1/api/lab/large-payload` với 5 threads
2. Quan sát panel **API Response Time** và **App Heap RAM**
3. **Câu hỏi:** Response time là bao nhiêu? RAM có tăng đột ngột không?

### Bài 9 (So sánh): Healthy API
1. Gọi `GET /v1/api/lab/healthy` và `GET /v1/api/lab/healthy-cached`
2. So sánh TPS và Response Time với các API lỗi phía trên

---

## Reset sau khi demo

Sau khi hoàn thành lab, gọi API reset để giải phóng tài nguyên bị leak:

```bash
curl -X POST http://localhost:8082/v1/api/lab/reset
```

Hoặc restart lại app:
```bash
docker compose restart app
```

---

## Bảng tổng kết (Học viên điền sau khi làm lab)

| API | Lỗi phát hiện được | Panel Grafana nào thấy rõ nhất | Nguyên nhân gốc |
|-----|--------------------|-----------------------------|----------------|
| cpu-heavy | | | |
| memory-leak | | | |
| connection-leak | | | |
| n-plus-one | | | |
| slow-query | | | |
| thread-blocked | | | |
| unbounded-cache | | | |
| large-payload | | | |
