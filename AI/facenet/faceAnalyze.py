import json
from deepface import DeepFace
import os

current_dir = os.path.dirname(os.path.abspath(__file__))
img_path = os.path.join(current_dir, 'database/quyminh.jpg')

objs = DeepFace.analyze(
  img_path, 
  actions = ['age', 'gender', 'race', 'emotion'],
)

formatted_result = json.dumps(objs, indent=4)

print(formatted_result)