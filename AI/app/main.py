import cv2
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from fpdf import FPDF
import numpy as np
import utils
from currency_detection.yolov8.YOLOv8 import YOLOv8
from config import config
from text_recognition.provider.ocr.ocr import OcrRecognition
import sys
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import FileResponse
from tempfile import NamedTemporaryFile
from text_to_speech.provider.Deepgram.deepgram import text_to_speech_async as deepgram_text_to_speech_async
from text_to_speech.provider.Deepgram.deepgram import text_to_speech as deepgram_text_to_speech
from product_recognition.pipeline import BarcodeProcessor
from deepface import DeepFace
import time
from image_captioning.provider.gpt4.gpt4 import OpenAIProvider
import asyncio
from distance_estimate.stream_video_distance import calculate_focal_length_stream, calculate_distance_from_image
from face_detection.detectMongo import process_frame, save_embedding_to_db, connect_mongodb, calculate_focal_length
start = time.time()
ocr = OcrRecognition()
currency_detection_model_path = "./currency_detection/model/best8.onnx"
currency_detector = YOLOv8(currency_detection_model_path, conf_thres=0.2, iou_thres=0.3)
gpt4_captioning = OpenAIProvider(config.OPEN_API_KEY)
barcode_processor = BarcodeProcessor()
distance_estimation_model_path = "./distance_estimate/models/yolov8m.onnx"
print(f"All Models loaded in {time.time() - start:.2f} seconds", file=sys.stderr)


app = FastAPI()

@app.get("/")
async def read_root():
    return {"Hello": "World"}

@app.post("/document_recognition")
async def document_recognition(file: UploadFile = File(...)):
    try:
        with NamedTemporaryFile(delete=False) as temp:
            temp.write(file.file.read())
            temp.close()
            result = ocr.recognize_text(temp.name, language="eng").text
            audio_path = NamedTemporaryFile(delete=False, suffix=".mp3").name
            pdf_path = NamedTemporaryFile(delete=False, suffix=".pdf").name
            asyncio.gather(
                deepgram_text_to_speech_async(api_key= config.DEEPGRAM_API_KEY, 
                                        text = result , output_path=audio_path),
                utils.create_pdf_async(result, pdf_path)
            )
            return JSONResponse(content={
                "text": result,
                "audio_path": audio_path,
                "pdf_path": pdf_path
            })
    except Exception as e:
        print(e)
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
            audio_path = NamedTemporaryFile(delete=False, suffix=".mp3").name
            deepgram_text_to_speech(api_key= config.DEEPGRAM_API_KEY, 
                                    text = f"Total money is {total_money}" , output_path=audio_path)
            return JSONResponse(content={
                "total_money": total_money,
                "audio_path": audio_path
            })
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")
    

@app.post("/image_captioning")
async def image_captioning(file: UploadFile = File(...)):
    try:
        with NamedTemporaryFile(delete=False) as temp:
            temp.write(file.file.read())
            temp.close()
            base64_image = gpt4_captioning.encode_image(temp.name)
            if not base64_image:
                raise HTTPException(status_code=500, detail="Failed to encode image")
            description = gpt4_captioning.frame_description_stream(base64_image)
            audio_path = NamedTemporaryFile(delete=False, suffix=".mp3").name
            deepgram_text_to_speech(api_key= config.DEEPGRAM_API_KEY, 
                                    text = description , output_path=audio_path)
            return JSONResponse(content={
                "description": description,
                "audio_path": audio_path
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
            audio_path = NamedTemporaryFile(delete=False, suffix=".mp3").name
            deepgram_text_to_speech(api_key= config.DEEPGRAM_API_KEY, 
                                    text = result , output_path=audio_path)
            
            print(result)
            return JSONResponse(content= {
                "audio_path": audio_path
            })
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")

image_path = "dis.jpg"  

calculate_focal_length_stream(image_path)

@app.post("/distance_estimate")
async def calculate_distance(file: UploadFile = File(...)):
    image_data = await file.read()
    np_arr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail="Invalid image file")
    
    results = calculate_distance_from_image(image_data)

    if results is None:
        raise HTTPException(status_code=400, detail="Không thể xử lý ảnh.")

    return JSONResponse(content=results)

collection = connect_mongodb()
if collection is None:
    raise HTTPException(status_code=500, detail="Database connection failed")
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
        save_embedding_to_db(collection, name, np.array(embedding))

        # Generate success voice use deepgram
        audio_path = NamedTemporaryFile(delete=False, suffix=".mp3").name
        deepgram_text_to_speech(api_key= config.DEEPGRAM_API_KEY, 
                                    text = f"Registration successful" , output_path=audio_path)
            
        return JSONResponse(content= {
            "audio_path": audio_path
        })
        
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Failed to process registration")

# Recognition Endpoint
@app.post("/face_detection/recognize")
async def recognize(file: UploadFile = File(...)):
    image_data = await file.read()
    np_arr = np.frombuffer(image_data, np.uint8)
    image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=400, detail="Invalid image file")

    try:
        response_data = process_frame(image, collection)
        if "error" in response_data:
            raise HTTPException(status_code=500, detail=response_data['error'])
        
        if response_data:
            data = response_data[0]  
            recognized_name = data.get('Name', 'Unknown')

            audio_path = NamedTemporaryFile(delete=False, suffix=".mp3").name
            deepgram_text_to_speech(api_key= config.DEEPGRAM_API_KEY, 
                                    text = f"Hello {recognized_name}, recognition successful." , output_path=audio_path)
            print(data)

            return {
                "message": "Recognition successful",
                "name": data.get('Name', 'Unknown'),
                "age": data.get('Age'),
                "gender": data.get('Gender'),
                "emotion": data.get('Emotion'),
                "race": data.get('Race'),
                "distance": data.get('Distance'),
                "audio_path": audio_path
            }
        else:
            raise HTTPException(status_code=404, detail="Face not recognized")
    except Exception as e:
        print(f"Error in recognition endpoint: {e}")
        raise HTTPException(status_code=500, detail="Failed to process recognition")


@app.get("/download_pdf")
async def download_pdf(pdf_path: str):
    return FileResponse(pdf_path, media_type="application/pdf", filename="document.pdf")


@app.get("/download_audio")
async def download_audio(audio_path: str):
    return FileResponse(audio_path, media_type="audio/mpeg", filename="document.mp3")