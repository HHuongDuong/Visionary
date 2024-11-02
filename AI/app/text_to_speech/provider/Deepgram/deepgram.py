import json
import os
import threading
import asyncio
import queue
from websockets.sync.client import connect
import pyaudio
import app
from app.config import config


class DeepgramSpeaker:
    TIMEOUT = 0.050
    FORMAT = pyaudio.paInt16
    CHANNELS = 1
    RATE = 48000
    CHUNK = 8000

    def __init__(self, api_key=None, url=None):
        self.api_key = api_key or os.environ.get("DEEPGRAM_API_KEY")
        self.url = url or f"wss://api.deepgram.com/v1/speak?encoding=linear16&sample_rate={self.RATE}"
        self._socket = None
        self._exit = threading.Event()
        self._receiver_thread = None
        self.speaker = Speaker()

    def connect(self):
        print(f"Connecting to {self.url}")
        self._socket = connect(
            self.url, additional_headers={"Authorization": f"Token {self.api_key}"}
        )

    async def _receiver(self):
        self.speaker.start()
        try:
            while not self._exit.is_set():
                message = self._socket.recv()
                if message is None:
                    continue

                if isinstance(message, str):
                    print(message)
                elif isinstance(message, bytes):
                    self.speaker.play(message)
        except Exception as e:
            print(f"receiver: {e}")
        finally:
            self.speaker.stop()

    def start_receiver(self):
        self._receiver_thread = threading.Thread(target=asyncio.run, args=(self._receiver(),))
        self._receiver_thread.start()

    def speak(self, text):
        if not self._socket:
            raise RuntimeError("Not connected. Call connect() first.")
        print(f"Sending: {text}")
        self._socket.send(json.dumps({"type": "Speak", "text": text}))

    def flush(self):
        print("Flushing...")
        self._socket.send(json.dumps({"type": "Flush"}))

    def close(self):
        self._exit.set()
        if self._socket:
            self._socket.send(json.dumps({"type": "Close"}))
            self._socket.close()
        if self._receiver_thread:
            self._receiver_thread.join()

class Speaker:
    def __init__(
        self,
        rate=DeepgramSpeaker.RATE,
        chunk=DeepgramSpeaker.CHUNK,
        channels=DeepgramSpeaker.CHANNELS,
        output_device_index=None,
    ):
        self._exit = threading.Event()
        self._queue = queue.Queue()
        self._audio = pyaudio.PyAudio()
        self._chunk = chunk
        self._rate = rate
        self._format = DeepgramSpeaker.FORMAT
        self._channels = channels
        self._output_device_index = output_device_index
        self._stream = None
        self._thread = None

    def start(self):
        self._stream = self._audio.open(
            format=self._format,
            channels=self._channels,
            rate=self._rate,
            input=False,
            output=True,
            frames_per_buffer=self._chunk,
            output_device_index=self._output_device_index,
        )
        self._exit.clear()
        self._thread = threading.Thread(
            target=self._play, daemon=True
        )
        self._thread.start()
        self._stream.start_stream()
        return True

    def stop(self):
        self._exit.set()
        if self._stream:
            self._stream.stop_stream()
            self._stream.close()
            self._stream = None
        if self._thread:
            self._thread.join()
            self._thread = None
        self._queue = None

    def play(self, data):
        self._queue.put(data)

    def _play(self):
        while not self._exit.is_set():
            try:
                data = self._queue.get(True, DeepgramSpeaker.TIMEOUT)
                self._stream.write(data)
            except queue.Empty:
                pass
            except Exception as e:
                print(f"_play: {e}")

def main():

    speaker = DeepgramSpeaker(app.config.config.DEEPGRAM_API_KEY)
    speaker.connect()
    speaker.start_receiver()

    story = [
            "The sun had just begun to rise over the sleepy town of Millfield.",
            "Emily, a young woman in her mid-twenties, was already awake and bustling about.",
        ]

    for sentence in story:
            speaker.speak(sentence)

    speaker.flush()
    input("Press Enter to exit...")
    speaker.close()

if __name__ == "__main__":
    main()