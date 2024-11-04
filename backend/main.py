import sys
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import FileResponse
from tempfile import NamedTemporaryFile
import uvicorn
from fpdf import FPDF
from PIL import Image
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), ".")))
from AI.app.text_recognition.provider.ocr.ocr import OcrRecognition
app = FastAPI()
# Registration Endpoint
@app.post("/face_detection/register")
async def register(name: str, file: UploadFile = File(...)):
    image_data = await file.read()
    np_arr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail="Invalid image file")


@app.get("/face_detection/download_audio")
async def download_audio(voice_path: str):
    return FileResponse(voice_path, media_type="audio/mpeg", filename="recognition_voice.mp3")