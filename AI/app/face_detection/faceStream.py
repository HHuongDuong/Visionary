from deepface import DeepFace
import os

current_dir = os.path.dirname(os.path.abspath(__file__))
db_path = os.path.join(current_dir, 'database')

DeepFace.stream(db_path)