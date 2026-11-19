package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.AdminDto;
import com.ibm.capstone.bookstore.domain.entities.Store;
import com.ibm.capstone.bookstore.domain.entities.StorePolicy;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.StorePolicyRepository;
import com.ibm.capstone.bookstore.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Stores and their policies. Anyone can read the active stores; only administrators change them. */
@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final StorePolicyRepository policyRepository;

    public StoreService(StoreRepository storeRepository, StorePolicyRepository policyRepository) {
        this.storeRepository = storeRepository;
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDto.StoreResponse> activeStores() {
        return storeRepository.findByActiveTrueOrderByIdAsc().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminDto.StoreResponse> allStores() {
        return storeRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminDto.StoreResponse getActive(Long id) {
        Store store = load(id);
        if (!Boolean.TRUE.equals(store.getActive())) {
            throw new ResourceNotFoundException("Store not found: " + id);
        }
        return toResponse(store);
    }

    @Transactional
    public AdminDto.StoreResponse create(AdminDto.StoreRequest request) {
        if (storeRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BadRequestException("A store named '" + request.name().trim() + "' already exists");
        }
        Store store = new Store();
        apply(store, request);
        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public AdminDto.StoreResponse update(Long id, AdminDto.StoreRequest request) {
        Store store = load(id);
        if (!store.getName().equalsIgnoreCase(request.name().trim()) && storeRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BadRequestException("A store named '" + request.name().trim() + "' already exists");
        }
        apply(store, request);
        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public AdminDto.StoreResponse setActive(Long id, boolean active) {
        Store store = load(id);
        store.setActive(active);
        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public AdminDto.StoreResponse addPolicy(Long storeId, AdminDto.PolicyRequest request) {
        Store store = load(storeId);
        StorePolicy policy = new StorePolicy();
        policy.setPolicyType(request.type());
        policy.setTitle(request.title().trim());
        policy.setBody(request.body().trim());
        store.addPolicy(policy);
        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public AdminDto.StoreResponse updatePolicy(Long storeId, Long policyId, AdminDto.PolicyRequest request) {
        Store store = load(storeId);
        StorePolicy policy = policyRepository.findByIdAndStoreId(policyId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        policy.setPolicyType(request.type());
        policy.setTitle(request.title().trim());
        policy.setBody(request.body().trim());
        policyRepository.save(policy);
        return toResponse(store);
    }

    @Transactional
    public AdminDto.StoreResponse deletePolicy(Long storeId, Long policyId) {
        Store store = load(storeId);
        StorePolicy policy = policyRepository.findByIdAndStoreId(policyId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        store.getPolicies().remove(policy);
        return toResponse(storeRepository.save(store));
    }

    private void apply(Store store, AdminDto.StoreRequest request) {
        store.setName(request.name().trim());
        store.setTagline(request.tagline() == null ? null : request.tagline().trim());
        if (request.returnWindowDays() != null) {
            store.setReturnWindowDays(request.returnWindowDays());
        }
    }

    private Store load(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Store not found: " + id));
    }

    private AdminDto.StoreResponse toResponse(Store s) {
        List<AdminDto.PolicyResponse> policies = s.getPolicies().stream()
                .map(p -> new AdminDto.PolicyResponse(p.getId(), p.getPolicyType(), p.getTitle(), p.getBody()))
                .collect(Collectors.toList());
        return new AdminDto.StoreResponse(s.getId(), s.getName(), s.getTagline(), s.getCurrency(),
                s.getReturnWindowDays(), Boolean.TRUE.equals(s.getActive()), policies);
    }
}
