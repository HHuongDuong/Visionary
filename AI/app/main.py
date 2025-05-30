import base64
import cv2
from fastapi import FastAPI
from fastapi.responses import JSONResponse, HTMLResponse
from fpdf import FPDF
import numpy as np
import openai
from pydantic import Json
from sympy import content
import utils
from currency_detection.yolov8.YOLOv8 import YOLOv8
from config import config
from text_recognition.provider.ocr.ocr import OcrRecognition
import sys
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import FileResponse
from tempfile import NamedTemporaryFile
from product_recognition.pipeline import BarcodeProcessor
import time
from image_captioning.provider.gemini.gemini import gen_img_description
import asyncio
from distance_estimate.stream_video_distance import calculate_focal_length_stream, calculate_distance_from_image
import json
import mimetypes
from image_captioning.provider.gpt4.gpt4 import OpenAIProvider
import os
from dotenv import load_dotenv

root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__)))
sys.path.append(root_dir)

start = time.time()
ocr = OcrRecognition()
currency_detection_model_path = "./currency_detection/model/best8.onnx"
currency_detector = YOLOv8(currency_detection_model_path, conf_thres=0.2, iou_thres=0.3)
barcode_processor = BarcodeProcessor()
distance_estimation_model_path = os.path.join(
    os.path.dirname(__file__),
    "distance_estimate", "models", "yolov8m.onnx"
)
# Ensure all model paths are absolute
print(f"Distance estimation model path: {distance_estimation_model_path}", file=sys.stderr)
print(f"All Models loaded in {time.time() - start:.2f} seconds", file=sys.stderr)

app = FastAPI()

from fastapi.middleware.cors import CORSMiddleware

# Allow frontend (port 8080) to access all backend features
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/", response_class=JSONResponse)
def root():
    # Return HTML with buttons linking to each feature
    html = '''
    <html>
    <head>
        <title>VisionMate Features</title>
        <style>
            body { background: #f3f4f6; font-family: sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; }
            h1 { color: #3b82f6; margin-bottom: 2rem; }
            .btn-group { display: flex; flex-wrap: wrap; gap: 1rem; justify-content: center; }
            a.button { background: linear-gradient(90deg, #6366f1 0%, #a78bfa 100%); color: white; padding: 1rem 2rem; border-radius: 0.75rem; text-decoration: none; font-size: 1.2rem; font-weight: 600; box-shadow: 0 2px 8px #0001; transition: background 0.2s, transform 0.2s; }
            a.button:hover { background: linear-gradient(90deg, #2563eb 0%, #7c3aed 100%); transform: translateY(-2px) scale(1.04); }
        </style>
    </head>
    <body>
        <h1>VisionMate Features</h1>
        <div class="btn-group">
            <a class="button" href="/docs#/default/document_recognition_document_recognition_post">Document Recognition</a>
            <a class="button" href="/docs#/default/currency_detection_currency_detection_post">Currency Detection</a>
            <a class="button" href="/docs#/default/image_captioning_image_captioning_post">Image Captioning</a>
            <a class="button" href="/docs#/default/product_recognition_product_recognition_post">Product Recognition</a>
            <a class="button" href="/docs#/default/calculate_distance_distance_estimate_post">Distance Estimate</a>
        </div>
        <p style="margin-top:2rem; color:#64748b;">Or use the <a href="/docs">API documentation</a>.</p>
    </body>
    </html>
    '''
    return HTMLResponse(content=html)

@app.post("/document_recognition")
async def document_recognition(file: UploadFile = File(...)):
    try:
        start = time.time()
        mime_type, _ = mimetypes.guess_type(file.filename)
        print(f"MIME Type from original filename: {mime_type}")

        if not mime_type or not mime_type.startswith("image/"):
            raise HTTPException(
                status_code=400, 
                detail=f"Invalid MIME type: {mime_type}. Only image files are allowed."
            )

        with NamedTemporaryFile(delete=False, suffix=mimetypes.guess_extension(mime_type)) as temp:
            temp.write(file.file.read())
            temp_path = temp.name

        ocr_result = ocr.recognize_text(temp_path).text
        result = ocr_result

        pdf_path = NamedTemporaryFile(delete=False, suffix=".pdf").name
        asyncio.gather(
            utils.create_pdf_async(result, pdf_path)
        )
        return JSONResponse(content={
            "text": result,
            "pdf_path": pdf_path
        })

    except Exception as e:
        print(f"Lỗi xảy ra: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")

@app.post("/currency_detection")
async def currency_detection(file: UploadFile = File(...)):
    try:
        with NamedTemporaryFile(delete=False) as temp:
            temp.write(file.file.read())
            temp.close()
            img = cv2.imread(temp.name)
            currency_detector(img)
            total_money = currency_detector.get_total_money()
            return JSONResponse(content={
                "total_money": total_money,
            })
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")

@app.post("/image_captioning")
async def image_captioning(file: UploadFile = File(...)):
    try:
        with NamedTemporaryFile(delete=False) as temp:
            temp.write(file.file.read())
            temp_path = temp.name 

        mime_type, _ = mimetypes.guess_type(file.filename)
        if not mime_type:
            raise HTTPException(status_code=400, detail="Cannot determine the mimetype of the uploaded file.")

        # Use Gemini provider
        result = gen_img_description(temp_path, mime_type)
        if not result:
            raise HTTPException(status_code=500, detail="Failed to generate image description")
        try:
            import json
            result_json = json.loads(result)
            description = result_json.get("description", "")
        except Exception:
            description = result

        if not description:
            raise HTTPException(status_code=500, detail="Failed to parse image description")

        return JSONResponse(content={
            "description": description,
        })

    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")

@app.post("/product_recognition")
async def product_recognition(file: UploadFile = File(...)):
    try:
        with NamedTemporaryFile(delete=False) as temp:
            temp.write(file.file.read())
            temp.flush()
            img = cv2.imread(temp.name)
            if img is None:
                raise HTTPException(status_code=400, detail="Invalid image file")
            result = barcode_processor.process_image(img)
            print(result)
            description = utils.format_product_information_with_openai(result)
            print(description)
            return JSONResponse(content= {
                "description": description
            })
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")

image_path = "dis.jpg"  
calculate_focal_length_stream(image_path)

@app.post("/distance_estimate")
async def calculate_distance(transcribe: str, file: UploadFile = File(...)):
    image_data = await file.read()
    base64_image = base64.b64encode(image_data).decode("utf-8")
    np_arr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail="Invalid image file")
    
    results = calculate_distance_from_image(image_data)
    print(results)
    if results is None:
        raise HTTPException(status_code=400, detail="Không thể xử lý ảnh.")
    results = utils.format_response_distance_estimate_with_openai(results, transcribe, base64_image)
    print(results)
    return JSONResponse(content={
        "description" : results
    })

@app.get("/download_pdf")
async def download_pdf(pdf_path: str):
    return FileResponse(pdf_path, media_type="application/pdf", filename="document.pdf")

@app.get("/download_audio")
async def download_audio(audio_path: str):
    return FileResponse(audio_path, media_type="audio/mpeg", filename="document.mp3")

