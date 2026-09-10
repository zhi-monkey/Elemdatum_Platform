import albumentations as A
import numpy as np
import json
import cv2

def contrast_enhance(img, enhance_params):
    """
    对比度增强（Albumentations的RandomBrightnessContrast）
    enhance_params: dict, 包含
        - 'contrast_limit': float, default 0.2（±对比度增强幅度, 如0.2就是±20%）
        - 'brightness_limit': float, default 0.0（可选，调亮/调暗幅度，建议0~0.2）
    返回：0~1的float32数组
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)
        contrast_limit = enhance_params.get('contrast_limit', 0.2)
        brightness_limit = enhance_params.get('brightness_limit', 0.0)

        if img is None or not isinstance(img, np.ndarray):
            raise TypeError("[contrast_enhance ERROR] img must be a valid numpy.ndarray")
        if img.size == 0:
            raise ValueError("[contrast_enhance ERROR] img is empty.")
        if img.ndim != 3 or img.shape[2] != 3:
            raise ValueError(f"[contrast_enhance ERROR] img must be a HWC BGR/RGB image, got shape {img.shape}")

        if img.max() <= 1.0:
            img = (img * 255).astype(np.uint8)
        elif img.dtype != np.uint8:
            img = img.astype(np.uint8)

        # Albumentations假定RGB格式，需要从BGR转换
        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        
        aug = A.RandomBrightnessContrast(
            brightness_limit=brightness_limit,
            contrast_limit=contrast_limit,
            always_apply=True,
        )
        result_rgb = aug(image=img_rgb)["image"]
        
        # 转回BGR格式
        result = cv2.cvtColor(result_rgb, cv2.COLOR_RGB2BGR)
        return result.astype(np.float32) / 255.0

    except Exception as e:
        print(f"[contrast_enhance ERROR] {str(e)}")
        raise

if __name__ == '__main__':
    import cv2
    img_path = r"C:\Users\10230\Pictures\Saved Pictures\000002.jpg"
    img = cv2.imread(img_path)
    if img is None:
        print(f"[ERROR] Failed to read image: {img_path}")
        exit(1)
    enhance_params = {
        "contrast_limit": 0.5,     # 更明显的对比度增强
        "brightness_limit": 0.0
    }
    out = contrast_enhance(img, enhance_params)
    cv2.imwrite("contrast_out.jpg", (out * 255).astype(np.uint8))
    print("Done.")
