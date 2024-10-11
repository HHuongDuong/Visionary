import os
import base64
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

client = OpenAI(api_key=OPENAI_API_KEY)

def encode_image(image_path):
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')

def frame_description(base64_image, user_prompt):
    return [
        {
            "role": "user",
            "content": [
                {"type": "text", "text": user_prompt},
                {
                    "type": "image_url",
                    "image_url": {"url": f"data:image/jpg;base64,{base64_image}"},
                },
            ],
        },
    ]

def analyze_image(base64_image, user_prompt):
    response = client.chat.completions.create(
        model="gpt-4o-mini", 
        messages=[
            {
                "role": "system",
                "content": """
                You are assisting someone who is visually impaired. Provide a brief, concise description of the image in one sentence, including key details.
                Limit your response to no more than 50 words.
                """,
            },
        ] + frame_description(base64_image, user_prompt),
        max_tokens=50,
        stream=True, 
    )

    response_text = ""
    for chunk in response:
        if chunk.choices[0].delta.content:  
            chunk_text = chunk.choices[0].delta.content
            response_text += chunk_text  
            print(chunk_text, end='', flush=True)  
    
    return response_text.strip()



def main():
    image_path = "frames/frame.jpg"
    
    user_prompt = "Can you describe this frame?"
    
    base64_image = encode_image(image_path)
    
    analysis = analyze_image(base64_image, user_prompt)

if __name__ == "__main__":
    main()
