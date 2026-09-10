import { bucketHost, bucketName, generateUuid, isValidVideo } from '/@/utils/dubhe';
import pMap from 'p-map';
import { useUploadStore } from '/@/store/modules/upload'; // const pMap = require('p-map');
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
// const pMap = require('p-map');

//const minIOPrefix = `${import.meta.env.VITE_MINIO_API}`;
async function getMinIOURL() {
  return await maHttp.post(
    {
      url: 'params/minioUrl',
      params: {
        hostIp: window.location.host,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

const minIOPrefix = await getMinIOURL();
// const fileReaderStream = require('filereader-stream')
// const path = require('path');

// 获取文件后缀名
export function getFileExtension(filename) {
  const parts = filename.split('.');
  if (parts.length === 1) {
    return '';
  }
  return '.' + parts.pop();
}

// 获取文件名
export function getBasename(filepath, extension = '') {
  // 使用正斜杠和反斜杠分割路径，然后取最后一个部分作为文件名
  const parts = filepath.split(/[\\/]/);
  const filenameWithExt = parts[parts.length - 1];

  if (extension) {
    // 如果指定了拓展名，从文件名中去掉拓展名
    if (filenameWithExt.endsWith(extension)) {
      return filenameWithExt.slice(0, -extension.length);
    }
  }

  return filenameWithExt;
}

// 是否为视频文件
const isValidVideoFile = (file) => {
  const extname = getFileExtension(file.name);
  return isValidVideo(extname);
};

// 给图片名称添加时间戳
export const hashName = (name) => {
  // 后缀名 .png
  const extname = getFileExtension(name);
  // 返回文件名称
  const basename = getBasename(name, extname);
  // 如果是视频文件，直接返回随机字符串名称（算法解析中文会有问题）
  // const isVideo = isValidVideo(extname);
  // 避免重复添加后缀 (视频也保留原名)
  const filterBaseName = basename.replace(/_ts\d+$/, '');
  return `${filterBaseName}_ap${generateUuid(10)}${extname}`;
};

// minio 上传导致 chrome crash，自定义实现上传
// **没用上minio组件库的上传，自己写的上传
export const putObject = (uploadUrl, file, options = {}) => {
  const { callback, objectName, errCallback } = options;
  // 加载进度
  let loaded = 0;
  let total = 0;
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', uploadUrl, true);
    xhr.withCredentials = false;
    xhr.onload = (e) => {
      if (xhr.readyState === 4 && xhr.status === 200) {
        resolve({
          err: null,
          data: {
            objectName,
            result: generateUuid(32),
          },
        });
      } else {
        reject(e);
      }
    };
    if (typeof errCallback === 'function') {
      xhr.onerror = () => {
        errCallback(file);
      };
    }
    xhr.send(file);
  });
};

export const newPutObject = (uploadUrl, file, objectName) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('PUT', uploadUrl, true);
    xhr.withCredentials = false;
    xhr.onload = (e) => {
      if (xhr.readyState === 4 && xhr.status === 200) {
        resolve({
          err: null,
          data: {
            objectName,
            result: generateUuid(32),
          },
        });
      } else {
        reject(e);
      }
    };
    xhr.onerror = (e) => {
      console.error(e);
    };
    xhr.send(file);
  });
};

// 默认通过 minIO 上传，上传文件方法
export const minIOUpload = async (
  { objectPath, fileList, transformFile },
  callback,
  errCallback,
) => {
  // add 进度条
  let resolved = 0;
  // 记录已上传的文件列表
  const resolveFiles = [];

  const mapper = async (d) => {
    // 生成 stream 流
    // const blob = fileReaderStream(d.raw)
    // http://localhost/undefined/upload/undefined/dataset/36/origin/1_ts9FLbzaJ088.png
    // http://210.30.97.57:30900/minio/upload/dubhe-prod
    const uploadPrefix = `${minIOPrefix}/${bucketName}`;
    // dataset/8/origin/Snipaste_2023-10-25_21-57-20_tsBcU-rglmYf.jpg
    const objectName = !objectPath || objectPath === '' ? `${d.name}` : `${objectPath}/${d.name}`;
    // console.log('objectName', objectName);
    const fileRes = await putObject(`${uploadPrefix}/${objectName}`, d.originFileObj, {
      objectName,
      callback,
      errCallback,
    });
    // minIO 上传视频 chrome crash
    // const result = await window.minioClient.putObject(`${objectPath}/${d.name}`, blob, {
    //   'Content-Type': d.raw.type
    // })
    resolved += 1;
    resolveFiles.push(fileRes);
    // 上传时的进度反馈，让进度条动起来
    if (typeof callback === 'function' && fileList.length >= 1) {
      callback(resolved, fileList, resolveFiles);
    }

    // 视频文件不做转换
    if (isValidVideoFile(d)) return fileRes;

    // 对图片文件进行转换，这一步的目的是给图片添加宽高
    if (typeof transformFile === 'function') {
      const transformed = await transformFile(fileRes, d);
      return transformed;
    }
    return fileRes;
  };

  const result = await pMap(fileList, mapper, { concurrency: 10 });
  return result;
};

export const newMinIOUpload = async (id, objectPath, fileList) => {
  const uploadStore = useUploadStore();
  const mapper = async (d) => {
    // 生成 stream 流
    const uploadPrefix = `${minIOPrefix}/${bucketName}`;
    const objectName = `${objectPath}/${d.name}`;
    const fileRes = await newPutObject(`${uploadPrefix}/${objectName}`, d.raw, objectName);
    // 视频文件不做转换,对图片文件进行转换，这一步的目的是给图片添加宽高
    const transformed = isValidVideoFile(d) ? fileRes : await transformFile(fileRes, d);
    // 保存上传进度
    await uploadStore.setUploadState(id, transformed);

    return transformed;
  };

  const result = await pMap(fileList, mapper, { concurrency: 10 });
  return result;
};

export const renameFile = (name, options = {}) => {
  name = options.hash ? hashName(name) : name;
  name = options.encode ? encodeURIComponent(name) : name;
  return name;
};

export const getFileOutputPath = (rawFiles, { objectPath }) => {
  return rawFiles.map((d) => `${bucketHost}/${bucketName}/${objectPath}/${d.name}`);
};

// 对文件进行自定义转换
export const transformFile = (result, file) => {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.addEventListener(
      'load',
      () => {
        const img = new Image();
        img.onload = () =>
          resolve({
            ...result,
            data: {
              ...result.data,
              meta: {
                width: img.width,
                height: img.height,
              },
            },
          });
        img.src = reader.result;
      },
      false,
    );

    reader.readAsDataURL(file.raw);
  });
};
