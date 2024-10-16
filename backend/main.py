from fastapi.middleware.cors import CORSMiddleware

from fastapi import FastAPI, File, UploadFile

import os
from AI.text_recognition.pipeline import TextRecognitionPipeline

app = FastAPI()

# Init Models
ocr = TextRecognitionPipeline(mode='ocr')



origins = [
    "http://*",
    "https://*",
    "ws://*"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

@app.get("/")
async def root():
    return "Hello world"

# Nhận diện văn bản: Ví dụ, khi cần đọc một tài liệu, biển báo trên đường hoặc nhãn hiệu 
# sản phẩm trong siêu thị, Vision Mate sẽ quét văn bản và chuyển đổi thành âm thanh để người dùng có thể nghe.

@app.post("/recognize_text_from_image")
async def upload(file: UploadFile = File(...)):
    try:
        contents = file.file.read()
        if file.content_type not in ["image/png", "image/jpeg", "image/jpg"]:
            return {
                "message" : "Not the right file type"
            }   
        with open("/tmp/" + file.filename, 'wb') as f:
            f.write(contents)
        
        text = ocr.recognize_text("/tmp/" + file.filename, language="eng")
        return {
            "result" : text     
        }
    except Exception:
        return {"message": "There was an error uploading the file"}
    finally:
        os.remove("/tmp/" + file.filename)

