# import cv2
# import base64
# from fastapi import FastAPI, WebSocket, WebSocketDisconnect
# from fastapi.responses import HTMLResponse

# app = FastAPI()

# @app.get("/")
# async def get():
#     html_content = """
#      <!DOCTYPE html>
#     <html>
#         <head>
#             <title>WebSocket Video Streaming</title>
#         </head>
#         <body>
#             <h1>Video Stream</h1>
#             <button id="startButton">Open Camera</button>
#             <button id="stopButton" disabled>Close Camera</button>
#             <img id="videoElement" style="width: 640px; height: 480px;" />

#             <script>
#                 var video = document.getElementById('videoElement');
#                 var startButton = document.getElementById('startButton');
#                 var stopButton = document.getElementById('stopButton');
#                 var ws = null;

#                 // Mở camera
#                 startButton.onclick = function() {
#                     if (!ws) {
#                         ws = new WebSocket("ws://localhost:8000/ws");

#                         // Xử lý khi kết nối WebSocket đã mở thành công
#                         ws.onopen = function() {
#                             console.log("WebSocket connection opened");
#                             startButton.disabled = true;  // Disable nút mở camera
#                             stopButton.disabled = false;  // Enable nút tắt camera
#                         };

#                         // Xử lý khi nhận được dữ liệu video
#                         ws.onmessage = function(event) {
#                             video.src = 'data:image/jpeg;base64,' + event.data;
#                         };

#                         ws.onclose = function() {
#                             console.log("WebSocket connection closed");
#                         };
#                     }
#                 };

#                 // Tắt camera
#                 stopButton.onclick = function() {
#                     if (ws) {
#                         ws.close();  // Đóng kết nối WebSocket
#                         ws = null;
#                         video.src = "";  // Xóa hình ảnh video
#                         startButton.disabled = false;  // Enable nút mở camera
#                         stopButton.disabled = true;  // Disable nút tắt camera
#                     }
#                 };
#             </script>
#         </body>
#     </html>
#     """
#     return HTMLResponse(content=html_content)
# @app.websocket("/ws")
# async def websocket_endpoint(websocket: WebSocket):
#     await websocket.accept()

#     cap = cv2.VideoCapture(0)  # Mở camera mỗi khi client kết nối
#     if cap is not None and cap.isOpened():
#         cap.release()  # Đảm bảo camera cũ được giải phóng trước khi khởi tạo lại
#         cap = cv2.VideoCapture(0)  # Khởi tạo camera lại từ đầu
#     try:
#         while True:
#             ret, frame = cap.read()
#             if not ret:
#                 break

#             # Encode frame thành JPEG rồi base64
#             _, buffer = cv2.imencode('.jpg', frame)
#             frame_base64 = base64.b64encode(buffer).decode('utf-8')

#             # Gửi chuỗi base64 qua WebSocket
#             await websocket.send_text(frame_base64)

#     except WebSocketDisconnect:
#         print("Client disconnected")

#     finally:
#         # Đảm bảo camera được giải phóng
#         cap.release()
#         print("Camera released")
from flask import Flask, render_template, Response
import cv2

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')

def gen():
    cap = cv2.VideoCapture(0)  # Mở webcam

    while True:
        ret, frame = cap.read()  # Đọc một khung hình từ webcam

        if not ret:
            print("Error: failed to capture image")
            break

        # Chuyển đổi khung hình thành định dạng JPEG mà không cần lưu
        ret, jpeg = cv2.imencode('.jpg', frame)

        if not ret:
            print("Error: failed to encode image")
            break

        # Gửi khung hình trực tiếp đến trình duyệt
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + jpeg.tobytes() + b'\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(gen(),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    app.run(debug=True)
