from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from tempfile import NamedTemporaryFile
from fpdf import FPDF
from gtts import gTTS
import cv2
import numpy as np
from deepface import DeepFace
import os
import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from AI.face_detection.detectMongo import process_frame, save_embedding_to_db, connect_mongodb, calculate_focal_length

app = FastAPI()
# Registration Endpoint
image_path = "dis.jpg"  

calculate_focal_length(image_path)

@app.post("/face_detection/register")
async def register(name: str, file: UploadFile = File(...)):
    image_data = await file.read()
    np_arr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail="Invalid image file")

    # Generate embedding
    try:
        embedding = DeepFace.represent(image, enforce_detection=False)[0]['embedding']
        save_embedding_to_db(name, np.array(embedding))

        # Generate success voice
        tts = gTTS(f"Registration successful for {name}", lang="en")
        voice_file = NamedTemporaryFile(delete=False, suffix=".mp3")
        tts.save(voice_file.name)

        return {"message": "Registration successful", "voice_path": voice_file.name}
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Failed to process registration")

# Recognition Endpoint
@app.post("/face_detetction/recognize")
async def recognize(file: UploadFile = File(...)):
    image_data = await file.read()
    np_arr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail="Invalid image file")

    collection = connect_mongodb()
    if collection is None:
        raise HTTPException(status_code=500, detail="Database connection failed")
    
    # Generate embedding and recognize
    try:
        response_data = process_frame(image, collection)
        if "error" in response_data:
            raise HTTPException(status_code=500, detail=response_data['error'])

        recognized_name = response_data['recognized_as'] 
        if recognized_name:
            # Generate success voice
            tts = gTTS(f"Hello {recognized_name}, recognition successful.", lang="en")
            voice_file = NamedTemporaryFile(delete=False, suffix=".mp3")
            tts.save(voice_file.name)

            # Generate PDF with recognition result
            pdf_file = NamedTemporaryFile(delete=False, suffix=".pdf")
            pdf = FPDF()
            pdf.add_page()
            pdf.set_font("Arial", size=12)
            pdf.cell(200, 10, txt=f"Recognition Result", ln=True, align="C")
            pdf.cell(200, 10, txt=f"Name: {response_data['recognized_as']}", ln=True, align="L")
            pdf.cell(200, 10, txt=f"Age: {response_data['age']}", ln=True, align="L")
            pdf.cell(200, 10, txt=f"Gender: {response_data['gender']}", ln=True, align="L")
            pdf.cell(200, 10, txt=f"Emotion: {response_data['emotion']}", ln=True, align="L")
            pdf.cell(200, 10, txt=f"Race: {response_data['race']}", ln=True, align="L")
            pdf.cell(200, 10, txt=f"Distance: {response_data['distance']}", ln=True, align="L")
            pdf.output(pdf_file.name)


            return {
                "message": "Recognition successful",
                "name": response_data['recognized_as'],
                "age": response_data['age'],
                "gender": response_data['gender'],
                "emotion": response_data['emotion'],
                "race": response_data['race'],
                "distance": response_data['distance'],
                "pdf_path": pdf_file.name,
                "voice_path": voice_file.name
            }
        else:
            raise HTTPException(status_code=404, detail="Face not recognized")
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Failed to process recognition")

# Endpoint to download generated files
@app.get("/face_detection/download_pdf")
async def download_pdf(pdf_path: str):
    return FileResponse(pdf_path, media_type="application/pdf", filename="recognition_result.pdf")

@app.get("/face_detection/download_audio")
async def download_audio(voice_path: str):
    return FileResponse(voice_path, media_type="audio/mpeg", filename="recognition_voice.mp3")