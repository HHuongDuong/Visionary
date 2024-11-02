import numpy as np
import cv2
from deepface import DeepFace
from pymongo import MongoClient
import os
from dotenv import load_dotenv

load_dotenv("../../.env")


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
    except Exception as e:
        print(f"Error saving embedding to MongoDB: {e}")

def find_existing_face(collection, embedding):
    """Find existing faces in the database."""
    threshold = 0.6  
    vector_search_stage = {
        "$vectorSearch": {
            "index": "vector_index",  
            "queryVector": embedding.tolist(), 
            "path": "embedding",  
            "numCandidates": 150, 
            "limit": 4  
        }
    }
    
    try:
        results = collection.aggregate([vector_search_stage])
    except Exception as e:
        print(f"Error searching vector: {e}")
        return []

    matched_faces = []
    for doc in results:
        score = doc.get("score", 0)
        if score < threshold:
            matched_faces.append(doc['name'])
    return matched_faces

def detect_and_recognize_face(image_path=None):
    """Detect and recognize faces from an image or video feed."""
    collection = connect_mongodb()  
    if collection is None:
        print("Unable to connect to MongoDB. Exiting the program.")
        return

    current_identity = None  

    if image_path:
        # Process image file
        frame = cv2.imread(image_path)
        if frame is None:
            print(f"Error reading image: {image_path}")
            return
    else:
        # Process live video feed
        cap = cv2.VideoCapture(0)  
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            
            process_frame(frame, collection, current_identity)
            
            cv2.imshow("Camera", frame)
            
            if cv2.waitKey(1) & 0xFF == ord('q'):  
                break
            elif cv2.waitKey(1) & 0xFF == ord('c'): 
                current_identity = None  

        cap.release()
        cv2.destroyAllWindows()
        return

    # Process the image frame
    process_frame(frame, collection, current_identity)

def process_frame(frame, collection, current_identity):
    """Analyze the frame and find or save embeddings."""
    try:
        embedding = DeepFace.represent(frame, enforce_detection=False)[0]['embedding']
        analysis = DeepFace.analyze(frame, actions=['age', 'gender', 'emotion', 'race'], enforce_detection=False)

        cv2.putText(frame, 
                    f"Age: {analysis['age']} | Gender: {analysis['gender']} | Emotion: {analysis['dominant_emotion']} | Race: {analysis['dominant_race']}", 
                    (10, 30), 
                    cv2.FONT_HERSHEY_SIMPLEX, 
                    0.7, 
                    (255, 255, 255), 
                    2, 
                    cv2.LINE_AA)

        matched_faces = find_existing_face(collection, np.array(embedding))
        if not matched_faces:
            if current_identity is None:  
                current_identity = input("Enter the name of this person: ")
                save_embedding_to_db(collection, current_identity, np.array(embedding))
        else:
            print(f"Recognized as: {', '.join(matched_faces)}.")
    except Exception as e:
        print(f"Error during analysis: {e}")

if __name__ == "__main__":
    # For testing with an image, provide the image path:
    # detect_and_recognize_face(image_path=r'D:\DeepFace\database\quyminh.jpg')

    # To start live video feed detection:
    detect_and_recognize_face()
