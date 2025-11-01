package com.example.learnperformancetest.controller;

import com.example.learnperformancetest.dto.MerchantDto;
import com.example.learnperformancetest.request.MerchantRequest;
import com.example.learnperformancetest.service.MerchantService;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("v1/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    public ResponseEntity<MerchantDto> create(@RequestBody MerchantRequest request) {
       try {
           Thread.sleep(ThreadLocalRandom.current().nextInt(200, 700));
         } catch (InterruptedException e) {
              throw new RuntimeException(e);
       }

        return ResponseEntity.ok(merchantService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantDto> update(@PathVariable Long id, @RequestBody MerchantRequest request) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 700));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(merchantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 700));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        merchantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantDto> getById(@PathVariable Long id) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 700));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(merchantService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<MerchantDto>> getAll(Pageable pageable) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 700));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(merchantService.getAll(pageable));
    }
}
