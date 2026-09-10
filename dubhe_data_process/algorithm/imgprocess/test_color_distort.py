import albumentations as A
import numpy as np
import json
import os
import time
import cv2

def color_distort(img, enhance_params):
    """
    色彩扰动（色相、饱和度、明度）
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)
        hue_shift = enhance_params.get('hue_shift_limit', 10)
        sat_shift = enhance_params.get('sat_shift_limit', 15)
        val_shift = enhance_params.get('val_shift_limit', 10)

        if img is None or not isinstance(img, np.ndarray):
            raise TypeError("[color_distort ERROR] img must be a valid numpy.ndarray")
        if img.size == 0:
            raise ValueError("[color_distort ERROR] img is empty.")
        if img.ndim != 3 or img.shape[2] != 3:
            raise ValueError(f"[color_distort ERROR] img must be a HWC BGR/RGB image, got shape {img.shape}")

        if img.max() <= 1.0:
            img = (img * 255).astype(np.uint8)
        elif img.dtype != np.uint8:
            img = img.astype(np.uint8)

        aug = A.HueSaturationValue(
            hue_shift_limit=hue_shift,
            sat_shift_limit=sat_shift,
            val_shift_limit=val_shift,
            always_apply=True,
        )
        result = aug(image=img)["image"]
        return result.astype(np.float32) / 255.0

    except Exception as e:
        print(f"[color_distort ERROR] {str(e)}")
        raise

if __name__ == '__main__':
    img_path = r"C:\Users\10230\Pictures\Saved Pictures\000002.jpg"
    img = cv2.imread(img_path)
    if img is None:
        print(f"[ERROR] Failed to read image: {img_path}")
        exit(1)
    enhance_params = {
        "hue_shift_limit": 10,
        "sat_shift_limit": 15,
        "val_shift_limit": 10
    }
    print(f"[INFO] Processing image: {img_path}")
    _ = color_distort(img, enhance_params)
