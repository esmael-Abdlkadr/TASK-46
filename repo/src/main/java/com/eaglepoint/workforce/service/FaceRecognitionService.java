package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.BiometricTemplate;
import com.eaglepoint.workforce.enums.AsyncJobType;
import com.eaglepoint.workforce.repository.BiometricTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FaceRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(FaceRecognitionService.class);

    private final FaceRecognitionClient client;
    private final BiometricTemplateRepository templateRepository;
    private final AsyncJobService asyncJobService;
    private final ObjectMapper objectMapper;

    public FaceRecognitionService(FaceRecognitionClient client,
                                   BiometricTemplateRepository templateRepository,
                                   AsyncJobService asyncJobService,
                                   ObjectMapper objectMapper) {
        this.client = client;
        this.templateRepository = templateRepository;
        this.asyncJobService = asyncJobService;
        this.objectMapper = objectMapper;
    }

    public boolean isServiceAvailable() {
        return client.isHealthy();
    }

    @Transactional
    public BiometricTemplate enrollFace(Long userId, byte[] imageBytes) {
        FaceRecognitionClient.FaceExtractionResult result = client.extractFeatures(imageBytes);

        byte[] featureBytes;
        try {
            featureBytes = objectMapper.writeValueAsBytes(result.features());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize features", e);
        }

        Optional<BiometricTemplate> existing = templateRepository.findByUserId(userId);
        BiometricTemplate template;
        if (existing.isPresent()) {
            template = existing.get();
        } else {
            template = new BiometricTemplate();
            template.setUserId(userId);
        }
        template.setTemplateData(featureBytes);
        template.setTemplateHash(result.imageHash());

        return templateRepository.save(template);
    }

    public FaceRecognitionClient.FaceMatchResult verifyFace(Long userId, byte[] imageBytes) {
        BiometricTemplate template = templateRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No biometric template enrolled for user " + userId));

        List<Double> storedFeatures;
        try {
            storedFeatures = objectMapper.readValue(template.getTemplateData(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize stored features", e);
        }

        FaceRecognitionClient.FaceExtractionResult extraction = client.extractFeatures(imageBytes);
        return client.matchFeatures(extraction.features(), storedFeatures);
    }

    public void submitAsyncExtraction(byte[] imageBytes, Long userId,
                                       Long submittedBy, String username) {
        String inputData;
        try {
            inputData = objectMapper.writeValueAsString(java.util.Map.of(
                    "userId", userId,
                    "imageSizeBytes", imageBytes.length));
        } catch (Exception e) {
            inputData = "{}";
        }

        var job = asyncJobService.submit(AsyncJobType.FACE_FEATURE_EXTRACTION,
                "Face extraction for user " + userId, inputData, submittedBy, username);

        asyncJobService.executeAsync(job.getId(), j -> {
            enrollFace(userId, imageBytes);
            j.setResultData("Features extracted and stored for user " + userId);
        });
    }

    @Transactional(readOnly = true)
    public Optional<BiometricTemplate> getTemplate(Long userId) {
        return templateRepository.findByUserId(userId);
    }
}
