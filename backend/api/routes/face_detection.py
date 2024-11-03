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

from AI.app.face_detection.detectMongo import connect_mongodb, save_embedding_to_db, find_existing_face

app = FastAPI()
# Registration Endpoint
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

    # Generate embedding and recognize
    try:
        embedding = DeepFace.represent(image, enforce_detection=False)[0]['embedding']
        recognized_name = find_existing_face(np.array(embedding))

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
            pdf.cell(200, 10, txt=f"Name: {recognized_name}", ln=True, align="L")
            pdf.output(pdf_file.name)

            return {"message": "Recognition successful", "name": recognized_name, "pdf_path": pdf_file.name, "voice_path": voice_file.name}
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