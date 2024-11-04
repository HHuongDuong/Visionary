import numpy as np
import cv2
from deepface import DeepFace
from pymongo import MongoClient
import os
from dotenv import load_dotenv
from sklearn.metrics.pairwise import cosine_similarity

load_dotenv()

MONGODB_URI = os.getenv("MONGODB_URI")
DB_NAME = os.getenv("DB_NAME")
DB_COLLECTION = os.getenv("DB_COLLECTION")

def connect_mongodb():
    """Connect to the MongoDB database."""
    try:
        mongo_client = MongoClient(MONGODB_URI)
        db = mongo_client[DB_NAME]  
        collection = db[DB_COLLECTION] 
        return collection  
    except Exception as e:
        print(f"Error connecting to MongoDB: {e}")
        return None

def save_embedding_to_db(collection, name, embedding):
    """Save embedding to MongoDB."""
    try:
        collection.insert_one({
            "name": name,
            "embedding": embedding.tolist()  
        })
        print(f"Saved {name} to the database.")
    except Exception as e:
        print(f"Error saving embedding to MongoDB: {e}")

def find_existing_face(collection, embedding):
    """Find existing faces in the database and return the one with the highest similarity."""
    threshold = 0.6
    matched_faces = []

    existing_faces = collection.find({})
    
    for doc in existing_faces:
        existing_embedding = np.array(doc['embedding'])
        sim = cosine_similarity([embedding], [existing_embedding])[0][0]
        print(f"Similarity with {doc['name']}: {sim}")
        if sim >= threshold:
            matched_faces.append((doc['name'], sim)) 
    
    if matched_faces:
        highest_similarity_face = max(matched_faces, key=lambda x: x[1])  
        return highest_similarity_face  
    return None  

def detect_and_recognize_face():
    """Detect and recognize a face from a live video feed."""
    collection = connect_mongodb()  
    if collection is None:
        print("Unable to connect to MongoDB. Exiting the program.")
        return

    cap = cv2.VideoCapture(0)  
    ret, frame = cap.read() 
    if not ret:
        print("Error reading from webcam.")
        cap.release()  
        return
    
    process_frame(frame, collection)

    cap.release()  
    cv2.destroyAllWindows()  

def process_frame(frame, collection):
    """Analyze the frame and find or save embeddings."""
    try:
        representations = DeepFace.represent(frame, enforce_detection=False)
        if representations and isinstance(representations, list):
            embedding = representations[0]['embedding']
        else:
            print("No faces detected.")
            return

        analysis = DeepFace.analyze(frame, actions=['age', 'gender', 'emotion', 'race'], enforce_detection=False)
        
        if analysis and isinstance(analysis, list):
            analysis_result = analysis[0]  

            age = analysis_result['age']
            dominant_gender = analysis_result['dominant_gender']
            dominant_emotion = analysis_result['dominant_emotion']
            dominant_race = analysis_result['dominant_race']

            print(f"Age: {age}")
            print(f"Gender: {dominant_gender}")
            print(f"Emotion: {dominant_emotion}")
            print(f"Race: {dominant_race}")

            highest_similarity_face = find_existing_face(collection, np.array(embedding))
            if highest_similarity_face is None:
                current_identity = input("Enter the name of this person: ")
                save_embedding_to_db(collection, current_identity, np.array(embedding))
            else:
                name, similarity = highest_similarity_face
                print(f"Recognized as: {name} with similarity {similarity}.")
        
    except Exception as e:
        print(f"Error during analysis: {e}")

if __name__ == "__main__":
    # To start live video feed detection:
    detect_and_recognize_face()
