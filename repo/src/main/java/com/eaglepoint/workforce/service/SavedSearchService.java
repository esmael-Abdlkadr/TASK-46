package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.SavedSearch;
import com.eaglepoint.workforce.repository.SavedSearchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final ObjectMapper objectMapper;

    public SavedSearchService(SavedSearchRepository savedSearchRepository, ObjectMapper objectMapper) {
        this.savedSearchRepository = savedSearchRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SavedSearch save(String name, MatchSearchRequest request, Long createdBy) {
        SavedSearch ss = new SavedSearch();
        ss.setName(name);
        ss.setCreatedBy(createdBy);
        try {
            ss.setSearchCriteriaJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize search criteria", e);
        }
        return savedSearchRepository.save(ss);
    }

    @Transactional(readOnly = true)
    public List<SavedSearch> findAll() {
        return savedSearchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SavedSearch> findByCreator(Long createdBy) {
        return savedSearchRepository.findByCreatedByOrderByCreatedAtDesc(createdBy);
    }

    @Transactional(readOnly = true)
    public Optional<SavedSearch> findById(Long id) {
        return savedSearchRepository.findById(id);
    }

    public MatchSearchRequest deserializeCriteria(String json) {
        try {
            return objectMapper.readValue(json, MatchSearchRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize search criteria", e);
        }
    }

    @Transactional
    public void delete(Long id) {
        savedSearchRepository.deleteById(id);
    }
}
