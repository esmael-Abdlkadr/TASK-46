"""
Face Recognition Service
Local network service for face-feature extraction and matching.
Runs on the same network segment as the Spring Boot application.
"""

import hashlib
import io
import json
import os
import time
from datetime import datetime

import numpy as np
from flask import Flask, jsonify, request
from PIL import Image

app = Flask(__name__)

# Configuration
MAX_CONTENT_LENGTH = 10 * 1024 * 1024  # 10 MB
FEATURE_VECTOR_SIZE = 128
SIMILARITY_THRESHOLD = float(os.environ.get("SIMILARITY_THRESHOLD", "0.75"))


def extract_features(image_bytes: bytes) -> list[float]:
    """
    Extract a feature vector from face image bytes.
    In production, this would use a trained CNN model (e.g. FaceNet, ArcFace).
    This implementation generates a deterministic feature vector from image content
    so that the same image always produces the same features.
    """
    img = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    img = img.resize((160, 160))
    pixel_array = np.array(img, dtype=np.float32).flatten()

    # Deterministic hash-based feature extraction (placeholder for real model)
    seed = int(hashlib.sha256(pixel_array.tobytes()).hexdigest()[:8], 16)
    rng = np.random.RandomState(seed)
    features = rng.randn(FEATURE_VECTOR_SIZE).astype(np.float64)

    # L2 normalize
    norm = np.linalg.norm(features)
    if norm > 0:
        features = features / norm

    return features.tolist()


def compute_similarity(features_a: list[float], features_b: list[float]) -> float:
    """Compute cosine similarity between two feature vectors."""
    a = np.array(features_a, dtype=np.float64)
    b = np.array(features_b, dtype=np.float64)
    dot = np.dot(a, b)
    norm_a = np.linalg.norm(a)
    norm_b = np.linalg.norm(b)
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return float(dot / (norm_a * norm_b))


@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "UP",
        "service": "face-recognition",
        "timestamp": datetime.utcnow().isoformat(),
        "feature_vector_size": FEATURE_VECTOR_SIZE,
        "similarity_threshold": SIMILARITY_THRESHOLD,
    })


@app.route("/api/extract", methods=["POST"])
def extract():
    """
    Extract face features from an uploaded image.
    Expects multipart/form-data with 'image' file.
    Returns JSON with feature vector and image hash.
    """
    if "image" not in request.files:
        return jsonify({"error": "No image file provided"}), 400

    image_file = request.files["image"]
    image_bytes = image_file.read()

    if len(image_bytes) == 0:
        return jsonify({"error": "Empty image file"}), 400

    if len(image_bytes) > MAX_CONTENT_LENGTH:
        return jsonify({"error": "Image too large (max 10MB)"}), 413

    start_time = time.time()
    try:
        features = extract_features(image_bytes)
    except Exception as e:
        return jsonify({"error": f"Feature extraction failed: {str(e)}"}), 500

    image_hash = hashlib.sha256(image_bytes).hexdigest()
    elapsed_ms = round((time.time() - start_time) * 1000, 2)

    return jsonify({
        "features": features,
        "image_hash": image_hash,
        "feature_count": len(features),
        "processing_time_ms": elapsed_ms,
    })


@app.route("/api/match", methods=["POST"])
def match():
    """
    Compare two feature vectors for similarity.
    Expects JSON body with 'features_a' and 'features_b' arrays.
    Returns similarity score and match verdict.
    """
    data = request.get_json()
    if not data or "features_a" not in data or "features_b" not in data:
        return jsonify({"error": "Provide 'features_a' and 'features_b'"}), 400

    features_a = data["features_a"]
    features_b = data["features_b"]

    if len(features_a) != FEATURE_VECTOR_SIZE or len(features_b) != FEATURE_VECTOR_SIZE:
        return jsonify({
            "error": f"Feature vectors must be {FEATURE_VECTOR_SIZE} elements"
        }), 400

    similarity = compute_similarity(features_a, features_b)
    is_match = similarity >= SIMILARITY_THRESHOLD

    return jsonify({
        "similarity": round(similarity, 6),
        "threshold": SIMILARITY_THRESHOLD,
        "is_match": is_match,
    })


@app.route("/api/verify", methods=["POST"])
def verify():
    """
    One-step verification: upload image + provide stored features.
    Extracts features from the image and compares against stored features.
    Expects multipart with 'image' file and 'stored_features' JSON string.
    """
    if "image" not in request.files:
        return jsonify({"error": "No image file provided"}), 400

    stored_features_json = request.form.get("stored_features")
    if not stored_features_json:
        return jsonify({"error": "No stored_features provided"}), 400

    image_bytes = request.files["image"].read()
    if len(image_bytes) == 0:
        return jsonify({"error": "Empty image file"}), 400

    try:
        stored_features = json.loads(stored_features_json)
    except json.JSONDecodeError:
        return jsonify({"error": "Invalid stored_features JSON"}), 400

    start_time = time.time()
    try:
        live_features = extract_features(image_bytes)
    except Exception as e:
        return jsonify({"error": f"Feature extraction failed: {str(e)}"}), 500

    similarity = compute_similarity(live_features, stored_features)
    is_match = similarity >= SIMILARITY_THRESHOLD
    elapsed_ms = round((time.time() - start_time) * 1000, 2)

    return jsonify({
        "similarity": round(similarity, 6),
        "threshold": SIMILARITY_THRESHOLD,
        "is_match": is_match,
        "processing_time_ms": elapsed_ms,
    })


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    host = os.environ.get("HOST", "0.0.0.0")
    debug = os.environ.get("DEBUG", "false").lower() == "true"
    app.run(host=host, port=port, debug=debug)
