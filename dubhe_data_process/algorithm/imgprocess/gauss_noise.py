import albumentations as A
import numpy as np
import json

def gauss_noise(img, enhance_params):
    """
    添加高斯噪声（Albumentations的GaussNoise）
    enhance_params: dict, 包含
        - 'var_limit': int or (int, int), default (10, 50)   # 噪声方差范围
        - 'mean': float, default 0                           # 噪声均值
    返回：0~1的float32数组
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)
        var_limit = enhance_params.get('var_limit', (10, 50))
        mean = enhance_params.get('mean', 0)

        if isinstance(var_limit, int):
            var_limit = (var_limit, var_limit)
        if img is None or not isinstance(img, np.ndarray):
            raise TypeError("[gauss_noise ERROR] img must be a valid numpy.ndarray")
        if img.size == 0:
            raise ValueError("[gauss_noise ERROR] img is empty.")
        if img.ndim != 3 or img.shape[2] != 3:
            raise ValueError(f"[gauss_noise ERROR] img must be a HWC BGR/RGB image, got shape {img.shape}")

        if img.max() <= 1.0:
            img = (img * 255).astype(np.uint8)
        elif img.dtype != np.uint8:
            img = img.astype(np.uint8)

        aug = A.GaussNoise(
            var_limit=var_limit,
            mean=mean,
            always_apply=True,
        )
        result = aug(image=img)["image"]

        return result.astype(np.float32) / 255.0

    except Exception as e:
        print(f"[gauss_noise ERROR] {str(e)}")
        raise

# 测试用例
if __name__ == '__main__':
    import cv2
    img_path = r"C:\Users\10230\Pictures\Saved Pictures\000002.jpg"
    img = cv2.imread(img_path)
    if img is None:
        print(f"[ERROR] Failed to read image: {img_path}")
        exit(1)
    enhance_params = {
        "var_limit": (10, 100),
        "mean": 0
    }
    out = gauss_noise(img, enhance_params)
    cv2.imwrite("gauss_noise_out.jpg", (out * 255).astype(np.uint8))
    print("Done.")
