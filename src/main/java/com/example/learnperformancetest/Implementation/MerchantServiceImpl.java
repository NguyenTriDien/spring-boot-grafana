package com.example.learnperformancetest.Implementation;

import com.example.learnperformancetest.dto.MerchantDto;
import com.example.learnperformancetest.entity.Merchant;
import com.example.learnperformancetest.mapper.MerchantMapper;
import com.example.learnperformancetest.repository.MerchantRepository;
import com.example.learnperformancetest.request.MerchantRequest;
import com.example.learnperformancetest.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final MerchantMapper merchantMapper;

    @Override
    public MerchantDto create(MerchantRequest request) {
        if (merchantRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Merchant already exists");
        }
        Merchant merchant = merchantMapper.toEntity(request);
        return merchantMapper.toDto(merchantRepository.save(merchant));
    }

    @Override
    public MerchantDto update(Long id, MerchantRequest request) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        merchant.setName(request.getName());
        return merchantMapper.toDto(merchantRepository.save(merchant));
    }

    @Override
    public void delete(Long id) {
        merchantRepository.deleteById(id);
    }

    @Override
    public MerchantDto getById(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return merchantMapper.toDto(merchant);
    }

    @Override
    public Page<MerchantDto> getAll(Pageable pageable) {
        return merchantRepository.findAll(pageable)
                .map(merchantMapper::toDto);
    }
}
