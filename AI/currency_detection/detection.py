import cv2
import os
from yolov8 import YOLOv8
from imread_from_url import imread_from_url

current_dir = os.path.dirname(os.path.abspath(__file__))
model_path = os.path.join(current_dir, 'model/best.onnx')

yolov8_detector = YOLOv8(model_path, conf_thres=0.2, iou_thres=0.3)

img_url = "https://vnn-imgs-a1.vgcloud.vn/photo-cms-kienthuc.zadn.vn/zoom/800/Uploaded/nguyenvan/2021_07_01/5/a1_YVAT.jpg?width=0&s=RcMagANp2-BRCjG0o4uLWg"
img = imread_from_url(img_url)

yolov8_detector(img)

combined_img = yolov8_detector.draw_detections(img)
cv2.namedWindow("Detected Objects", cv2.WINDOW_NORMAL)
cv2.imshow("Detected Objects", combined_img)
cv2.waitKey(0)
cv2.destroyAllWindows()