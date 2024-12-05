import os

from dotenv import load_dotenv

load_dotenv()


class Config(object):

    def __init__(self):
        self.MONGODB_URI = os.getenv('MONGODB_URI')
        self.DB_NAME = os.getenv('DB_NAME')
        self.DB_COLLECTION = os.getenv('DB_COLLECTION')
        self.DEEPGRAM_API_KEY = os.getenv('DEEPGRAM_API_KEY')
        self.OPENAI_API_KEY = os.getenv('OPEN_API_KEY')
        self.VOICE_RSS = os.getenv('Voice_RSS')

config = Config()
