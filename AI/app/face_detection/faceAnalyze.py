import json
from deepface import DeepFace
import os
import time
import tensorflow as tf
import logging

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
logger = tf.get_logger()
logger.setLevel(logging.ERROR)

def measure_deepface_performance() -> None:
    current_dir = os.path.dirname(os.path.abspath(__file__))
    img_path = os.path.join(current_dir, 'database/quyminh.jpg')
    
    start_load_time = time.time()
    DeepFace.analyze(
        img_path,
        actions=['age'],
        enforce_detection=False
    )
    end_load_time = time.time()
    model_load_time = end_load_time - start_load_time
    
    start_predict_time = time.time()
    objs = DeepFace.analyze(
        img_path,
        actions=['age', 'gender', 'race', 'emotion'],
        enforce_detection=False
    )
    end_predict_time = time.time()
    prediction_time = end_predict_time - start_predict_time
    
    formatted_result = json.dumps(objs, indent=4)
    
    print(f"Model Loading Time: {model_load_time:.2f} seconds")
    print(f"Prediction Time: {prediction_time:.2f} seconds")
    print("\nAnalysis Results:")
    print(formatted_result)

if __name__ == "__main__":
    measure_deepface_performance()