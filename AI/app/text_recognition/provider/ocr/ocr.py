import os
import mimetypes
import logging
from dataclasses import dataclass
import google.generativeai as genai

logger = logging.getLogger(__name__)

@dataclass
class RecognitionResult:
    text: str

class OcrRecognition:
    def __init__(self):
        self.generation_config = {
            "temperature": 0.5,
            "top_p": 0.95,
            "top_k": 40,
            "max_output_tokens": 8192
        }
        self.model = genai.GenerativeModel(
            model_name="gemini-1.5-flash-002", 
            generation_config=self.generation_config
        )

    def recognize_text(self, file_path: str) -> RecognitionResult:
        try:
            mime_type, _ = mimetypes.guess_type(file_path)
            logger.info(f"Detected MIME type: {mime_type}")

            if not mime_type or not mime_type.startswith("image/"):
                raise ValueError(f"Invalid MIME type: {mime_type}. Only image files are supported.")

            img_file = genai.upload_file(path=file_path, mime_type=mime_type)
            while img_file.state.name == "PROCESSING":
                img_file = genai.get_file(img_file.name)
                logger.info(f"Image processing state: {img_file.state.name}")

            if img_file.state.name == "FAILED":
                logger.error(f"Error uploading image: {img_file.state.details}")
                raise ValueError("Failed to process image.")

            prompt = (
                "Trích xuất nội dung văn bản từ ảnh tài liệu. "
                "Chỉ trả lại văn bản chính xác như xuất hiện trong tài liệu, không thêm bất kỳ thông tin nào khác."
            )

            response = self.model.generate_content(
                [img_file, prompt],
                generation_config=genai.GenerationConfig(response_mime_type="application/json"),
                request_options={"timeout": 5000}
            )

            # Xóa tệp sau khi xử lý
            img_file.delete()

            return RecognitionResult(text=response.text.strip())

        except Exception as e:
            logger.error(f"Lỗi trong quá trình nhận dạng văn bản: {str(e)}")
            raise
