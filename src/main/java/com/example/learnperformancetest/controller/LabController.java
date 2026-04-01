package com.example.learnperformancetest.controller;

import com.example.learnperformancetest.entity.Merchant;
import com.example.learnperformancetest.entity.Product;
import com.example.learnperformancetest.repository.MerchantRepository;
import com.example.learnperformancetest.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance Testing Lab Controller
 * 
 * 8 APIs có lỗi performance (để học viên phát hiện qua Grafana)
 * 2 APIs hoạt động bình thường (để so sánh)
 */
@RestController
@RequestMapping("v1/api/lab")
@RequiredArgsConstructor
public class LabController {

    private static final Logger logger = LoggerFactory.getLogger(LabController.class);

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final DataSource dataSource;

    // ============================================================
    // BUG 1: Memory Leak — static list giữ data vĩnh viễn
    // ============================================================
    private static final List<byte[]> LEAKED_MEMORY = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LEAK_CHUNKS = 200; // Giới hạn an toàn ~200MB

    // ============================================================
    // BUG 3: Connection Leak — danh sách connections bị leak
    // ============================================================
    private static final List<Connection> LEAKED_CONNECTIONS = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LEAK_CONNECTIONS = 80; // Giới hạn an toàn

    // ============================================================
    // BUG 6: Thread Starvation — global lock
    // ============================================================
    private static final Object GLOBAL_LOCK = new Object();

    // ============================================================
    // BUG 7: Unbounded Cache — HashMap không giới hạn
    // ============================================================
    private static final Map<String, String> LOCAL_CACHE = new ConcurrentHashMap<>();

    // ================================================================
    //  API 1: CPU Overload
    //  Triệu chứng: App CPU tăng vọt 80-100%
    // ================================================================
    @GetMapping("/cpu-heavy")
    public ResponseEntity<?> cpuHeavy() {
        logger.info("LAB: CPU Heavy - generating heavy mathematical computation");

        // Tính pi bằng chuỗi Gregory-Leibniz với 50 triệu vòng lặp
        // Cách này ép CỰC MẠNH CPU nhưng KHÔNG DÙNG TỚI RAM (vì chỉ dùng biến nguyên thủy double)
        double pi = 0;
        double sign = 1.0;
        for (long i = 1; i <= 100_000_000L; i += 2) {
            pi += sign * (4.0 / i);
            sign = -sign;
        }

        // Hash kết quả nhiều lần để tăng CPU usage chuỗi (string manipulation)
        try {
            String hash = String.valueOf(pi);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < 5000; i++) {
                byte[] digest = md.digest(hash.getBytes());
                hash = Base64.getEncoder().encodeToString(digest);
            }
            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "hash_final", hash.substring(0, 20) + "...",
                    "pi_calculated", pi,
                    "iterations", 50_000_000L
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    //  API 2: Memory Leak
    //  Triệu chứng: Heap RAM tăng liên tục, không bao giờ giảm
    // ================================================================
    @GetMapping("/memory-leak")
    public ResponseEntity<?> memoryLeak() {
        logger.info("LAB: Memory Leak - allocating 1MB that will never be freed");

        if (LEAKED_MEMORY.size() >= MAX_LEAK_CHUNKS) {
            return ResponseEntity.ok(Map.of(
                    "status", "limit_reached",
                    "leaked_chunks", LEAKED_MEMORY.size(),
                    "leaked_mb", LEAKED_MEMORY.size(),
                    "message", "Safety limit reached at " + MAX_LEAK_CHUNKS + "MB. Restart app to reset."
            ));
        }

        // Mỗi request thêm 1MB vào bộ nhớ VĨNH VIỄN
        byte[] chunk = new byte[1024 * 1024]; // 1MB
        Arrays.fill(chunk, (byte) 0xFF);
        LEAKED_MEMORY.add(chunk);

        return ResponseEntity.ok(Map.of(
                "status", "leaked",
                "leaked_chunks", LEAKED_MEMORY.size(),
                "leaked_mb", LEAKED_MEMORY.size(),
                "message", "Added 1MB to permanent memory. Total: " + LEAKED_MEMORY.size() + "MB"
        ));
    }

    // ================================================================
    //  API 3: Database Connection Exhaustion
    //  Triệu chứng: Active connections tăng dần → timeout
    // ================================================================
    @GetMapping("/connection-leak")
    public ResponseEntity<?> connectionLeak() {
        logger.info("LAB: Connection Leak - acquiring DB connection without closing");

        if (LEAKED_CONNECTIONS.size() >= MAX_LEAK_CONNECTIONS) {
            return ResponseEntity.ok(Map.of(
                    "status", "limit_reached",
                    "leaked_connections", LEAKED_CONNECTIONS.size(),
                    "message", "Safety limit reached. Pool near exhaustion. Restart app to reset."
            ));
        }

        try {
            // Lấy connection nhưng KHÔNG BAO GIỜ đóng → connection pool cạn kiệt
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
            rs.next();
            int count = rs.getInt(1);

            // GIỮ connection trong list → KHÔNG TRẢ LẠI pool
            LEAKED_CONNECTIONS.add(conn);

            return ResponseEntity.ok(Map.of(
                    "status", "connection_leaked",
                    "product_count", count,
                    "leaked_connections", LEAKED_CONNECTIONS.size(),
                    "pool_max", 100,
                    "message", "Connection acquired but NEVER released! Total leaked: " + LEAKED_CONNECTIONS.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "pool_exhausted",
                    "error", e.getMessage(),
                    "leaked_connections", LEAKED_CONNECTIONS.size(),
                    "message", "Connection pool is EXHAUSTED! No more connections available."
            ));
        }
    }

    // ================================================================
    //  API 4: N+1 Query Problem
    //  Triệu chứng: Response time cao, hàng trăm câu SQL nhỏ
    // ================================================================
    @GetMapping("/n-plus-one")
    public ResponseEntity<?> nPlusOne() {
        logger.info("LAB: N+1 Query - fetching products then querying merchant for each");

        // 1 query lấy 500 products (đủ để thấy rõ N+1 problem)
        List<Product> products = productRepository.findAll(PageRequest.of(0, 500)).getContent();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            // +N queries: MỖI product query riêng lẻ merchant → 500 câu SQL thừa!
            Optional<Merchant> merchant = merchantRepository.findById(p.getMerchantId());
            result.add(Map.of(
                    "product_id", p.getId(),
                    "product_name", p.getName(),
                    "price", p.getPrice(),
                    "merchant_name", merchant.map(Merchant::getName).orElse("N/A")
            ));
        }

        return ResponseEntity.ok(Map.of(
                "total_products", result.size(),
                "total_queries", 1 + result.size(), // 1 (products) + N (each merchant)
                "data", result
        ));
    }

    // ================================================================
    //  API 5: Slow SQL Query
    //  Triệu chứng: Response time p99 rất cao, DB CPU tăng mạnh
    // ================================================================
    @GetMapping("/slow-query")
    public ResponseEntity<?> slowQuery() {
        logger.info("LAB: Slow Query - executing expensive cross join with SLEEP");

        try {
            Connection conn = dataSource.getConnection();
            try {
                Statement stmt = conn.createStatement();
                // CROSS JOIN tạo cartesian product + SLEEP mỗi dòng
                ResultSet rs = stmt.executeQuery(
                        "SELECT p1.name AS name1, p2.name AS name2, SLEEP(0.01) " +
                        "FROM products p1 CROSS JOIN products p2 LIMIT 300"
                );
                int rowCount = 0;
                while (rs.next()) rowCount++;

                return ResponseEntity.ok(Map.of(
                        "status", "completed",
                        "rows_scanned", rowCount,
                        "message", "Cross join completed. Scanned " + rowCount + " row combinations."
                ));
            } finally {
                conn.close(); // đóng connection đúng cách (chỉ lỗi slow query, không leak)
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    //  API 6: Thread Starvation (Global Lock)
    //  Triệu chứng: TPS giảm về gần 0, tất cả request xếp hàng
    // ================================================================
    @GetMapping("/thread-blocked")
    public ResponseEntity<?> threadBlocked() {
        logger.info("LAB: Thread Blocked - waiting for global lock");

        long waitStart = System.currentTimeMillis();

        synchronized (GLOBAL_LOCK) {
            long lockAcquired = System.currentTimeMillis();
            try {
                // Giữ lock 3 giây → tất cả thread khác phải xếp hàng
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return ResponseEntity.ok(Map.of(
                    "status", "processed",
                    "thread", Thread.currentThread().getName(),
                    "wait_time_ms", lockAcquired - waitStart,
                    "hold_time_ms", 3000,
                    "message", "Only ONE request can be processed at a time due to global lock"
            ));
        }
    }

    // ================================================================
    //  API 7: Unbounded Cache (OOM)
    //  Triệu chứng: RAM tăng đều mỗi lần gọi với key khác nhau
    // ================================================================
    @GetMapping("/unbounded-cache")
    public ResponseEntity<?> unboundedCache(@RequestParam(defaultValue = "default") String key) {
        logger.info("LAB: Unbounded Cache - caching key: {}", key);

        // Mỗi key khác nhau tạo 1 entry mới ~100KB, KHÔNG BAO GIỜ evict
        LOCAL_CACHE.computeIfAbsent(key, k -> {
            // Tạo value lớn ~100KB cho mỗi entry
            return "CACHED_DATA_".repeat(8000) + "_" + k + "_" + System.nanoTime();
        });

        return ResponseEntity.ok(Map.of(
                "status", "cached",
                "key", key,
                "cache_entries", LOCAL_CACHE.size(),
                "estimated_cache_mb", LOCAL_CACHE.size() * 100 / 1024, // rough estimate
                "message", "Entry cached. Total cache entries: " + LOCAL_CACHE.size() + " (NEVER evicted!)"
        ));
    }

    // ================================================================
    //  API 8: Oversized Response Payload
    //  Triệu chứng: Response time rất cao, RAM tăng đột ngột
    // ================================================================
    @GetMapping("/large-payload")
    public ResponseEntity<?> largePayload() {
        logger.info("LAB: Large Payload - building oversized response");

        // Lấy 200 products (không dùng findAll trên 10M records!)
        List<Product> products = productRepository.findAll(PageRequest.of(0, 200)).getContent();

        // Nhân bản 500 lần → ~100K records với padding → response ~50MB
        List<Map<String, Object>> hugeList = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            for (Product p : products) {
                hugeList.add(Map.of(
                        "id", p.getId() + "_copy_" + i,
                        "name", p.getName(),
                        "description", p.getDescription() != null ? p.getDescription() : "",
                        "price", p.getPrice(),
                        "padding", "X".repeat(500) // thêm dữ liệu rác mỗi dòng
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "total_records", hugeList.size(),
                "message", "Response contains " + hugeList.size() + " records with padding",
                "data", hugeList
        ));
    }

    // ================================================================
    //  API 9: Database Row Lock Contention
    //  Triệu chứng: Response time tăng, DB connections running tăng
    // ================================================================
    @GetMapping("/db-row-lock")
    public ResponseEntity<?> dbRowLock() {
        logger.info("LAB: DB Row Lock - updating same row with long transaction");

        try {
            Connection conn = dataSource.getConnection();
            try {
                conn.setAutoCommit(false);

                // Bắt đầu transaction, UPDATE row id=1 và GIỮ LOCK 5 giây
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(
                    "UPDATE account_balances SET balance = balance + 1 WHERE id = 1"
                );

                // Giữ transaction mở 5 giây → row bị LOCK
                // Các request khác cũng UPDATE row id=1 sẽ phải CHỜ
                Thread.sleep(5000);

                conn.commit();

                return ResponseEntity.ok(Map.of(
                        "status", "completed",
                        "locked_row_id", 1,
                        "lock_duration_ms", 5000,
                        "message", "Row id=1 was locked for 5 seconds during transaction"
                ));
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage(),
                    "message", "Lock wait timeout or error occurred"
            ));
        }
    }

    // ================================================================
    //  API 10: Deadlock
    //  Triệu chứng: Lỗi 500 do MySQL phát hiện deadlock, TPS giảm
    // ================================================================
    @GetMapping("/deadlock")
    public ResponseEntity<?> deadlock(@RequestParam(defaultValue = "A") String side) {
        logger.info("LAB: Deadlock - side {} starting cross-lock transaction", side);

        try {
            Connection conn = dataSource.getConnection();
            try {
                conn.setAutoCommit(false);
                Statement stmt = conn.createStatement();

                if ("A".equalsIgnoreCase(side)) {
                    // Transaction A: Lock row 1 trước, rồi cố lock row 2
                    stmt.executeUpdate("UPDATE account_balances SET balance = balance + 1 WHERE id = 1");
                    Thread.sleep(1000); // Chờ để Transaction B kịp lock row 2
                    stmt.executeUpdate("UPDATE account_balances SET balance = balance + 1 WHERE id = 2");
                } else {
                    // Transaction B: Lock row 2 trước, rồi cố lock row 1 → DEADLOCK!
                    stmt.executeUpdate("UPDATE account_balances SET balance = balance + 1 WHERE id = 2");
                    Thread.sleep(1000); // Chờ để Transaction A kịp lock row 1
                    stmt.executeUpdate("UPDATE account_balances SET balance = balance + 1 WHERE id = 1");
                }

                conn.commit();
                return ResponseEntity.ok(Map.of(
                        "status", "completed",
                        "side", side,
                        "message", "Transaction completed without deadlock (lucky timing)"
                ));
            } catch (Exception e) {
                conn.rollback();
                return ResponseEntity.internalServerError().body(Map.of(
                        "status", "deadlock_detected",
                        "side", side,
                        "error", e.getMessage(),
                        "message", "MySQL detected DEADLOCK and killed this transaction!"
                ));
            } finally {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    //  API 11: Full Table Scan (Missing Index)
    //  Triệu chứng: Response time cực cao, DB CPU tăng mạnh
    // ================================================================
    @GetMapping("/full-table-scan")
    public ResponseEntity<?> fullTableScan(@RequestParam(defaultValue = "cao cấp") String keyword) {
        logger.info("LAB: Full Table Scan - searching 10M records without index");

        try {
            Connection conn = dataSource.getConnection();
            try {
                Statement stmt = conn.createStatement();
                long start = System.currentTimeMillis();

                // LIKE '%keyword%' trên cột description KHÔNG CÓ INDEX
                // → MySQL phải quét TOÀN BỘ 10 triệu records!!!
                ResultSet rs = stmt.executeQuery(
                    "SELECT id, name, price FROM products " +
                    "WHERE description LIKE '%" + keyword + "%' " +
                    "ORDER BY price DESC LIMIT 20"
                );

                List<Map<String, Object>> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(Map.of(
                            "id", rs.getLong("id"),
                            "name", rs.getString("name"),
                            "price", rs.getBigDecimal("price")
                    ));
                }
                long elapsed = System.currentTimeMillis() - start;

                return ResponseEntity.ok(Map.of(
                        "status", "completed",
                        "keyword", keyword,
                        "results_found", results.size(),
                        "scan_time_ms", elapsed,
                        "total_records_scanned", "~10,000,000 (FULL TABLE SCAN!)",
                        "data", results,
                        "message", "Query scanned ALL records because description has NO INDEX!"
                ));
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    //  API 12: GC Pressure (Garbage Collection Storm)
    //  Triệu chứng: Response time có spike do GC pause
    // ================================================================
    @GetMapping("/gc-pressure")
    public ResponseEntity<?> gcPressure() {
        logger.info("LAB: GC Pressure - creating millions of temporary objects");

        long start = System.currentTimeMillis();

        // Tạo hàng triệu object nhỏ liên tục → GC phải chạy liên tục
        List<String> tempList = new ArrayList<>();
        for (int i = 0; i < 3_000_000; i++) {
            // Mỗi vòng lặp tạo String mới (object trên heap)
            tempList.add("item_" + i + "_" + System.nanoTime());

            // Mỗi 500K items, xóa hết để GC phải dọn
            if (i % 500_000 == 0 && i > 0) {
                tempList.clear();
            }
        }

        long elapsed = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "objects_created", 3_000_000,
                "gc_cycles_triggered", 6,
                "elapsed_ms", elapsed,
                "message", "Created 3M objects with periodic clearing to trigger GC storms"
        ));
    }

    // ================================================================
    //  API 13: Healthy API (bình thường)
    //  Dùng để so sánh với các API lỗi
    // ================================================================
    @GetMapping("/healthy")
    public ResponseEntity<?> healthy() {
        long count = productRepository.count();
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "product_count", count,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ================================================================
    //  API 14: Healthy with Redis Cache (bình thường + cache)
    //  Response time cực thấp nhờ cache
    // ================================================================
    @GetMapping("/healthy-cached")
    @Cacheable(value = "lab-health", key = "'stats'")
    public Map<String, Object> healthyCached() {
        long count = productRepository.count();
        return Map.of(
                "status", "OK",
                "product_count", count,
                "cached", true,
                "timestamp", LocalDateTime.now().toString()
        );
    }

    // ================================================================
    //  RESET: API để reset lại trạng thái leak (dùng sau khi demo)
    // ================================================================
    @PostMapping("/reset")
    public ResponseEntity<?> reset() {
        logger.warn("LAB: Resetting all leaked resources");

        // Reset memory leak
        int memoryChunks = LEAKED_MEMORY.size();
        LEAKED_MEMORY.clear();

        // Reset connection leak - đóng tất cả connections bị leak
        int connCount = LEAKED_CONNECTIONS.size();
        for (Connection conn : LEAKED_CONNECTIONS) {
            try {
                if (!conn.isClosed()) conn.close();
            } catch (Exception ignored) {}
        }
        LEAKED_CONNECTIONS.clear();

        // Reset unbounded cache
        int cacheEntries = LOCAL_CACHE.size();
        LOCAL_CACHE.clear();

        // Gợi ý GC
        System.gc();

        return ResponseEntity.ok(Map.of(
                "status", "reset_complete",
                "freed_memory_chunks", memoryChunks,
                "closed_connections", connCount,
                "cleared_cache_entries", cacheEntries,
                "message", "All leaked resources have been freed. App is clean now."
        ));
    }
}
