"""Tests for the face recognition service."""

import io
import json
import unittest

import numpy as np
from PIL import Image

from app import app, compute_similarity, extract_features


class TestFaceRecognitionService(unittest.TestCase):

    def setUp(self):
        self.client = app.test_client()
        app.config["TESTING"] = True

    def _create_test_image(self, width=160, height=160, color=(128, 128, 128)):
        img = Image.new("RGB", (width, height), color)
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)
        return buf

    # --- Unit tests ---

    def test_extract_features_deterministic(self):
        img_bytes = self._create_test_image().read()
        f1 = extract_features(img_bytes)
        f2 = extract_features(img_bytes)
        self.assertEqual(f1, f2)

    def test_extract_features_length(self):
        img_bytes = self._create_test_image().read()
        features = extract_features(img_bytes)
        self.assertEqual(len(features), 128)

    def test_extract_features_normalized(self):
        img_bytes = self._create_test_image().read()
        features = extract_features(img_bytes)
        norm = np.linalg.norm(features)
        self.assertAlmostEqual(norm, 1.0, places=5)

    def test_different_images_different_features(self):
        img1 = self._create_test_image(color=(100, 50, 50)).read()
        img2 = self._create_test_image(color=(200, 100, 100)).read()
        f1 = extract_features(img1)
        f2 = extract_features(img2)
        self.assertNotEqual(f1, f2)

    def test_similarity_identical(self):
        features = [0.1] * 128
        sim = compute_similarity(features, features)
        self.assertAlmostEqual(sim, 1.0, places=5)

    def test_similarity_different(self):
        f1 = [1.0] + [0.0] * 127
        f2 = [0.0] + [1.0] + [0.0] * 126
        sim = compute_similarity(f1, f2)
        self.assertAlmostEqual(sim, 0.0, places=5)

    # --- API tests ---

    def test_health_endpoint(self):
        resp = self.client.get("/health")
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data["status"], "UP")
        self.assertEqual(data["feature_vector_size"], 128)

    def test_extract_endpoint(self):
        img_buf = self._create_test_image()
        resp = self.client.post("/api/extract",
                                data={"image": (img_buf, "test.png")},
                                content_type="multipart/form-data")
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertEqual(data["feature_count"], 128)
        self.assertIn("image_hash", data)
        self.assertIn("processing_time_ms", data)

    def test_extract_no_image(self):
        resp = self.client.post("/api/extract")
        self.assertEqual(resp.status_code, 400)

    def test_match_endpoint(self):
        features = [0.1] * 128
        resp = self.client.post("/api/match",
                                json={"features_a": features, "features_b": features})
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertTrue(data["is_match"])
        self.assertAlmostEqual(data["similarity"], 1.0, places=4)

    def test_match_wrong_length(self):
        resp = self.client.post("/api/match",
                                json={"features_a": [0.1] * 10, "features_b": [0.1] * 10})
        self.assertEqual(resp.status_code, 400)

    def test_verify_endpoint(self):
        img_buf = self._create_test_image()
        img_bytes = img_buf.read()
        features = extract_features(img_bytes)

        img_buf2 = self._create_test_image()
        resp = self.client.post("/api/verify",
                                data={
                                    "image": (img_buf2, "test.png"),
                                    "stored_features": json.dumps(features),
                                },
                                content_type="multipart/form-data")
        self.assertEqual(resp.status_code, 200)
        data = resp.get_json()
        self.assertTrue(data["is_match"])


if __name__ == "__main__":
    unittest.main()
