from typing import Union, Optional
from pathlib import Path
import logging
from dataclasses import dataclass
from typing import Literal
import numpy as np
import pytesseract
from PIL import Image

SupportedModesType = Literal["ocr", "easyocr", "trocr", "gpt4", "claude"]
SupportedLanguages = Literal["eng", "fra", "deu", "spa", "vie"]

SupportedModes = ("ocr", "easyocr", "trocr", "gpt4")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class RecognitionResult:
    text: str
    confidence: Optional[float] = None
    processing_time: Optional[float] = None

class OcrRecognition:
    def __init__(self) -> None:
        logger.info("Initializing OCR Recognition")

    def recognize_text(self,
                       image_source: Union[str, Path, Image.Image, np.ndarray],
                       language: SupportedLanguages = "vie") -> RecognitionResult:
        try:
            image = self._prepare_image(image_source)
            return self._recognize_with_ocr(image, language)
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

    @staticmethod
    def get_available_languages() -> list[SupportedLanguages]:
        return ["eng", "fra", "deu", "spa", "vie"]

def main():
    ocr = OcrRecognition()
    result = ocr.recognize_text("/home/xuananle/Documents/Learn/HMI/VisionMate/AI/image.png", language="vie")
    print(f"Recognized Text: {result.text}")
    print(f"Confidence: {result.confidence}")

if __name__ == "__main__":
    main()