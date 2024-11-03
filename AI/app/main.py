from logging import exception
from unittest import result
import cv2
from fastapi import FastAPI
from fastapi.responses import JSONResponse
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
from image_captioning.provider.gpt4.gpt4 import OpenAIProvider

start = time.time()
ocr = OcrRecognition()
currency_detection_model_path = "./currency_detection/model/best.onnx"
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
            utils.create_pdf(result, "output.pdf")
            return FileResponse("output.pdf")

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
            return JSONResponse(content={
                "total_money": total_money
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
            return JSONResponse(content={
                "description": description
            })
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")
    

@app.post("/product_recognition")
async def product_recognition(file: UploadFile = File(...)):
    try:
        with NamedTemporaryFile(delete=False) as temp:
            temp.write(file.file.read())
            temp.close()
            result = barcode_processor.process_image(temp.name)
            return JSONResponse(content=result)
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")
    

