import base64
from openai import OpenAI
from typing import Optional
import logging


class OpenAIProvider:

    def __init__(self, api_key: str):
        self.openai = OpenAI(api_key=api_key)
        self.logger = logging.getLogger(__name__)

    @staticmethod
    def encode_image(image_path: str) -> Optional[str]:
        try:
            with open(image_path, "rb") as image_file:
                return base64.b64encode(image_file.read()).decode('utf-8')
        except Exception as e:
            logging.error(f"Error encoding image: {str(e)}")
            return None

    def frame_description(self, base64_image: str) -> Optional[str]:
        try:
            response = self.openai.chat.completions.create(
                model="gpt-4-vision-preview",  # Note: Changed from gpt-4o-mini to correct model name
                messages=[
                    {
                        "role": "system",
                        "content": """You are assisting someone who is visually impaired. 
                        Provide a brief, concise description of the image in one sentence, 
                        including key details. Limit your response to no more than 50 words."""
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "Please describe this image."
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{base64_image}"
                                }
                            }
                        ]
                    }
                ],
                max_tokens=50
            )

            # Extract the description from the response
            if response.choices and response.choices[0].message:
                return response.choices[0].message.content
            return None

        except Exception as e:
            self.logger.error(f"Error getting image description: {str(e)}")
            return None

    def frame_description_stream(self, base64_image: str) -> str:
        """
        Get streaming description for an image using GPT-4 Vision

        Args:
            base64_image (str): Base64 encoded image

        Returns:
            str: Full image description
        """
        try:
            full_response = ""
            response = self.openai.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {
                        "role": "system",
                        "content": """You are assisting someone who is visually impaired. 
                        Provide a brief, concise description of the image in one sentence, 
                        including key details. Limit your response to no more than 50 words."""
                    },
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "Please describe this image."
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{base64_image}"
                                }
                            }
                        ]
                    }
                ],
                max_tokens=50,
                stream=True
            )

            for chunk in response:
                if chunk.choices:
                    content = chunk.choices[0].delta.content
                    if content:
                        full_response += content
                        print(content, end='', flush=True)

            return full_response

        except Exception as e:
            self.logger.error(f"Error in streaming image description: {str(e)}")
            return ""


def main():
    import os
    from app.config import config

    logging.basicConfig(level=logging.INFO)

    provider = OpenAIProvider(config.OPEN_API_KEY)

    image_path = 'img.png'

    if not os.path.exists(image_path):
        logging.error(f"Image not found: {image_path}")
        return

    base64_image = provider.encode_image(image_path)
    if not base64_image:
        logging.error("Failed to encode image")
        return

    description = provider.frame_description_stream(base64_image)
    if description:
        print(f"\nFinal description: {description}")
    else:
        print("Failed to get description")


if __name__ == '__main__':
    main()