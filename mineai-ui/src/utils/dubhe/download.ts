import streamSaver from 'streamsaver';
import { bucketName, minioBaseUrl } from './minIO';
import ZIP from './zip';
import pMap from 'p-map';

const toArray = require('stream-to-array');

//streamSaver.mitm = 'https://static.zhejianglab.com/mitm.html';

// 默认名字解析
const defaultName = (file) => file.name;

// 下载单一文件
export const downloadFileAsStream = (url, fileName) => {
  const fileStream = streamSaver.createWriteStream(fileName || url.split('/').pop());
  fetch(url).then((res) => {
    const readableStream = res.body;

    // @ts-ignore
    if (window.WritableStream && readableStream.pipeTo) {
      // @ts-ignore
      return readableStream.pipeTo(fileStream);
    }
    // 兼容 WritableStream
    const writer = fileStream.getWriter();
    // @ts-ignore
    const reader = res.body.getReader();
    const pump = () =>
      reader
        .read()
        .then((result) => (result.done ? writer.close() : writer.write(result.value).then(pump)));
    return pump();
  });
};

// 下载 zip 包
// eslint-disable-next-line
export const downloadFilesAsZip = (files, zipName = 'demo.zip', options: any = {}) => {
  const fileName = options.fileName || defaultName;
  const { concurrency = 5 } = options;
  const fileStream = streamSaver.createWriteStream(zipName);
  // @ts-ignore
  const readableZipStream = new ZIP({
    async pull(ctrl) {
      const mapper = async (file) => {
        return fetch(file.url).then(({ body }) => {
          ctrl.enqueue({
            name: typeof fileName === 'function' ? fileName(file) : fileName,
            stream: () => body,
          });
        });
      };

      await pMap(files, mapper, { concurrency });
      ctrl.close();
    },
  });

  // more optimized
  if (window.WritableStream && readableZipStream.pipeTo) {
    // eslint-disable-next-line
    return readableZipStream.pipeTo(fileStream).then(() => console.log('done writing'));
  }

  // less optimized
  const writer = fileStream.getWriter();
  const reader = readableZipStream.getReader();
  const pump = () =>
    reader.read().then((res) => (res.done ? writer.close() : writer.write(res.value).then(pump)));

  pump();
};

// 基于minIO objectPath 自动解析目录
export const downloadZipFromObjectPath = async (
  minioClient,
  objectPath,
  zipName = 'demo.zip',
  options: any = {},
) => {
  // minio listObjects
  const result = await minioClient.listObjects(bucketName, objectPath, true);
  // stream to Array
  let objects = await toArray(result);
  if (typeof options.filter === 'function') {
    objects = options.filter(objects);
  }
  const files = objects.map((d) => ({
    url: `${minioBaseUrl}/${d.name}`,
    name: d.name,
  }));
  if (options.flat) {
    let path = objectPath;
    if (path.charAt(path.length - 1) !== '/') {
      path += '/';
    }
    options.fileName = (file) => file.name.replace(path, '');
  }
  if (files.length) {
    downloadFilesAsZip(files, zipName, options);
  }
};

// 获取minio指定目录下的文件Url列表
export const getFileUrlList = async (minioClient, objectPath, filterType: string[]) => {
  // minio listObjects
  const result = await minioClient.listObjects(bucketName, objectPath, true);
  console.log(bucketName);
  // stream to Array
  const objects = await toArray(result);
  console.log('objects', objects);
  const files = objects
    .filter((d) => {
      const ext = d.name.split('.').pop().toLowerCase();
      return filterType.includes(ext);
    })
    .map((d) => `${minioBaseUrl}/${d.name}`);

  console.log('files:' + files);
  console.log('minioBaseUrl', minioBaseUrl);
  return files;
};
