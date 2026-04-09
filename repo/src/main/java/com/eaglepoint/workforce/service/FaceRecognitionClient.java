package com.eaglepoint.workforce.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FaceRecognitionClient {

    private static final Logger log = LoggerFactory.getLogger(FaceRecognitionClient.class);

    @Value("${app.face-recognition.base-url:http://localhost:5001}")
    private String baseUrl;

    @Value("${app.face-recognition.timeout-ms:10000}")
    private int timeoutMs;

    private final ObjectMapper objectMapper;

    public FaceRecognitionClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isHealthy() {
        try {
            HttpURLConnection conn = openConnection("/health", "GET");
            int code = conn.getResponseCode();
            if (code == 200) {
                String body = new String(conn.getInputStream().readAllBytes());
                JsonNode node = objectMapper.readTree(body);
                return "UP".equals(node.path("status").asText());
            }
            return false;
        } catch (Exception e) {
            log.warn("Face recognition service health check failed: {}", e.getMessage());
            return false;
        }
    }

    public FaceExtractionResult extractFeatures(byte[] imageBytes) {
        try {
            String boundary = "----FormBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = openConnection("/api/extract", "POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                writeMultipartFile(os, boundary, "image", "face.jpg", imageBytes);
                os.write(("--" + boundary + "--\r\n").getBytes());
            }

            int code = conn.getResponseCode();
            String body = new String(
                    (code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()).readAllBytes());

            if (code != 200) {
                throw new RuntimeException("Extract failed (HTTP " + code + "): " + body);
            }

            JsonNode node = objectMapper.readTree(body);
            List<Double> features = new ArrayList<>();
            node.path("features").forEach(f -> features.add(f.asDouble()));

            return new FaceExtractionResult(
                    features,
                    node.path("image_hash").asText(),
                    node.path("processing_time_ms").asDouble());
        } catch (Exception e) {
            throw new RuntimeException("Face feature extraction failed: " + e.getMessage(), e);
        }
    }

    public FaceMatchResult matchFeatures(List<Double> featuresA, List<Double> featuresB) {
        try {
            HttpURLConnection conn = openConnection("/api/match", "POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = objectMapper.writeValueAsString(Map.of(
                    "features_a", featuresA, "features_b", featuresB));
            conn.getOutputStream().write(json.getBytes());

            int code = conn.getResponseCode();
            String body = new String(conn.getInputStream().readAllBytes());
            JsonNode node = objectMapper.readTree(body);

            return new FaceMatchResult(
                    node.path("similarity").asDouble(),
                    node.path("threshold").asDouble(),
                    node.path("is_match").asBoolean());
        } catch (Exception e) {
            throw new RuntimeException("Face matching failed: " + e.getMessage(), e);
        }
    }

    private HttpURLConnection openConnection(String path, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + path).toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        return conn;
    }

    private void writeMultipartFile(OutputStream os, String boundary, String fieldName,
                                     String fileName, byte[] data) throws Exception {
        os.write(("--" + boundary + "\r\n").getBytes());
        os.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\""
                + fileName + "\"\r\n").getBytes());
        os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
        os.write(data);
        os.write("\r\n".getBytes());
    }

    public record FaceExtractionResult(List<Double> features, String imageHash, double processingTimeMs) {}
    public record FaceMatchResult(double similarity, double threshold, boolean isMatch) {}
}
