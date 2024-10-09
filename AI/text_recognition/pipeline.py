from typing import Literal, Union, Optional
from pathlib import Path
import logging
from dataclasses import dataclass

import numpy as np
import pytesseract
from PIL import Image
import torch
from transformers import TrOCRProcessor, VisionEncoderDecoderModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

SupportedModesType = Literal["ocr", "easyocr", "trocr", "openai"]
SupportedLanguages = Literal["eng", "fra", "deu", "spa", "vie"]

SupportedModes = ("ocr", "easyocr", "trocr", "openai")

# https://blog.roboflow.com/best-ocr-models-text-recognition/ 
# TODO: Implement the above
@dataclass
class RecognitionResult:
    text: str
    confidence: Optional[float] = None
    processing_time: Optional[float] = None

class TextRecognitionPipeline:
    def __init__(self, mode: SupportedModesType = "ocr", device: Optional[str] = None) -> None:
        self.mode = mode.lower()
        
        if self.mode not in SupportedModes:
            raise ValueError(f"Mode must be one of {SupportedModes}")
        
        self.device = device or ('cuda' if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        if self.mode == "trocr":
            try:
                self._setup_trocr()
            except Exception as e:
                logger.error(f"Failed to initialize TrOCR: {str(e)}")
                raise
    
    def _setup_trocr(self) -> None:
        logger.info("Initializing TrOCR model and processor...")
        self.processor = TrOCRProcessor.from_pretrained('microsoft/trocr-base-handwritten')
        self.model = VisionEncoderDecoderModel.from_pretrained('microsoft/trocr-base-handwritten')
        self.model.to(self.device)
    
    def recognize_text(self, 
                      image_source: Union[str, Path, Image.Image, np.ndarray],
                      language: SupportedLanguages = "vie") -> RecognitionResult:
        try:
            image = self._prepare_image(image_source)
            
            if self.mode == "ocr":
                return self._recognize_with_ocr(image, language)
            else:
                return self._recognize_with_trocr(image)
        except Exception as e:
            logger.error(f"Error during text recognition: {str(e)}")
            raise
    
    def _prepare_image(self, image_source: Union[str, Path, Image.Image, np.ndarray]) -> Image.Image:
        if isinstance(image_source, (str, Path)):
            return Image.open(image_source)
        elif isinstance(image_source, np.ndarray):
            return Image.fromarray(image_source)
        elif isinstance(image_source, Image.Image):
            return image_source
        else:
            raise ValueError(f"Unsupported image source type: {type(image_source)}")
    
    def _recognize_with_ocr(self, image: Image.Image, language: SupportedLanguages) -> RecognitionResult:
        try:
            data = pytesseract.image_to_data(image, lang=language, output_type=pytesseract.Output.DICT)
            text = " ".join([word for word in data['text'] if word.strip()])
            
            conf_values = [conf for conf in data['conf'] if conf != -1]
            avg_confidence = sum(conf_values) / len(conf_values) if conf_values else None
            
            return RecognitionResult(text=text, confidence=avg_confidence)
        except Exception as e:
            logger.error(f"OCR processing failed: {str(e)}")
            raise
    
    def _recognize_with_trocr(self, image: Image.Image) -> RecognitionResult:
        try:
            pixel_values = self.processor(image, return_tensors="pt").pixel_values.to(self.device)
            
            with torch.no_grad():
                generated_ids = self.model.generate(pixel_values)
            
            generated_text = self.processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
            
            return RecognitionResult(text=generated_text)
        except Exception as e:
            logger.error(f"TrOCR processing failed: {str(e)}")
            raise
    
    @staticmethod
    def get_available_languages() -> list[SupportedLanguages]:
        return ["eng", "fra", "deu", "spa", "vie"]

def main():
    ocr = TextRecognitionPipeline(mode='ocr')
    print(ocr.recognize_text("/home/xuananle/Pictures/Screenshots/Screenshot from 2024-10-09 08-36-44.png").text)

if __name__ == "__main__":
    main()
