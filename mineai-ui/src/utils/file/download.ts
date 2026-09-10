import { openWindow } from '..';
import { dataURLtoBlob, urlToBase64 } from './base64Conver';
import JSZip from 'jszip';
import { saveAs } from 'file-saver';
import { bucketHost } from '/@/utils/dubhe';
import { getFileExtension } from '/@/views/mineai/data/dataset-details2/components/UploadForm/util';
import { parseAnnotation } from '/@/views/mineai/data/dataset-details2/util';
import { getYoloLabelsFile } from '/@/views/mineai/data/dataset-details2/api';

/**
 * Download online pictures
 * @param url
 * @param filename
 * @param mime
 * @param bom
 */
export function downloadByOnlineUrl(url: string, filename: string, mime?: string, bom?: BlobPart) {
  urlToBase64(url).then((base64) => {
    downloadByBase64(base64, filename, mime, bom);
  });
}

/**
 * Download pictures based on base64
 * @param buf
 * @param filename
 * @param mime
 * @param bom
 */
export function downloadByBase64(buf: string, filename: string, mime?: string, bom?: BlobPart) {
  const base64Buf = dataURLtoBlob(buf);
  downloadByData(base64Buf, filename, mime, bom);
}

/**
 * Download according to the background interface file stream
 * @param {*} data
 * @param {*} filename
 * @param {*} mime
 * @param {*} bom
 */
export function downloadByData(data: BlobPart, filename: string, mime?: string, bom?: BlobPart) {
  const blobData = typeof bom !== 'undefined' ? [bom, data] : [data];
  const blob = new Blob(blobData, { type: mime || 'application/octet-stream' });
  const blobURL = window.URL.createObjectURL(blob);
  const tempLink = document.createElement('a');
  tempLink.style.display = 'none';
  tempLink.href = blobURL;
  tempLink.setAttribute('download', filename);
  if (typeof tempLink.download === 'undefined') {
    tempLink.setAttribute('target', '_blank');
  }
  document.body.appendChild(tempLink);
  tempLink.click();
  document.body.removeChild(tempLink);
  window.URL.revokeObjectURL(blobURL);
}

/**
 * Download file according to file address
 * @param {*} sUrl
 */
export function downloadByUrl({
  url,
  target = '_blank',
  fileName,
}: {
  url: string;
  target?: TargetContext;
  fileName?: string;
}): boolean {
  const isChrome = window.navigator.userAgent.toLowerCase().indexOf('chrome') > -1;
  const isSafari = window.navigator.userAgent.toLowerCase().indexOf('safari') > -1;

  if (/(iP)/g.test(window.navigator.userAgent)) {
    console.error('Your browser does not support download!');
    return false;
  }
  if (isChrome || isSafari) {
    const link = document.createElement('a');
    link.href = url;
    link.target = target;

    if (link.download !== undefined) {
      link.download = fileName || url.substring(url.lastIndexOf('/') + 1, url.length);
    }

    if (document.createEvent) {
      const e = document.createEvent('MouseEvents');
      e.initEvent('click', true, true);
      link.dispatchEvent(e);
      return true;
    }
  }
  if (url.indexOf('?') === -1) {
    url += '?download';
  }

  openWindow(url, { target });
  return true;
}

//下载图片
export async function downloadImage(url) {
  const response = await fetch(url);
  const data = await response.blob();
  const blobURL = window.URL.createObjectURL(data);
  const tempLink = document.createElement('a');
  tempLink.style.display = 'none';
  tempLink.href = blobURL;

  // 使用URL对象来解析URL
  const urlObject = new URL(url);
  // 从URL中获取文件名，如果没有则使用默认值
  const filename = urlObject.pathname.split('/').pop() || 'default_filename';

  tempLink.setAttribute('download', filename);
  if (typeof tempLink.download === 'undefined') {
    tempLink.setAttribute('target', '_blank');
  }
  document.body.appendChild(tempLink);
  tempLink.click();
  document.body.removeChild(tempLink);
  window.URL.revokeObjectURL(blobURL);
}

export async function downloadImagesAndAnnotationsAsZip(
  zipName,
  imageInfos,
  labelType,
  labels,
  datasetId = 0,
) {
  // 初始化一个zip打包对象
  const zip = new JSZip();

  // 创建一个名为images的新的文件目录
  // const folder = zip.folder('files');

  // 批量处理每个图片
  for (const e of imageInfos) {
    // 请求远程资源的blob
    const imageBlob = await fetch(`${bucketHost}/${e.url}`).then((response) => response.blob());
    // 文件夹添加资源，图片也支持base64类型 {base64: true}
    zip!.file(`${e.name}${getFileExtension(e.url)}`, imageBlob, { binary: true });
    if (labelType !== undefined) {
      // createML
      if (labelType === 'createML') {
        let annotations: any[] = e.annotation ? parseAnnotation(e.annotation, labels) : [];
        const blobData = [JSON.stringify(annotations)];
        const annotationBlob = new Blob(blobData, { type: 'application/octet-stream' });
        zip!.file(`${e.name}.json`, annotationBlob, { binary: true });
      }
      // VOC
      else if (labelType === 'VOC') {
        const blobData = [e.voc];
        const annotationBlob = new Blob(blobData, { type: 'application/octet-stream' });
        zip!.file(`${e.name}.xml`, annotationBlob, { binary: true });
      }
      // YOLO
      else {
        const blobData = [e.yolo];
        const annotationBlob = new Blob(blobData, { type: 'application/octet-stream' });
        zip!.file(`${e.name}.txt`, annotationBlob, { binary: true });
      }
    }
  }

  // YOLO格式添加 label txt file
  if (labelType !== undefined && labelType === 'YOLO') {
    const labelInfo = await getYoloLabelsFile(datasetId);
    const labelBlob = new Blob([labelInfo], { type: 'application/octet-stream' });
    zip!.file(`classes.txt`, labelBlob, { binary: true });
  }
  // 把打包内容异步转成blob二进制格式
  zip.generateAsync({ type: 'blob' }).then((content) => {
    // 下载压缩包
    saveAs(content, zipName);
  });
}

export async function downloadImagesAsZip(zipName, imageInfos) {
  // 初始化一个zip打包对象
  const zip = new JSZip();

  // 创建一个名为images的新的文件目录
  // const folder = zip.folder('files');

  // 批量处理每个图片
  for (const e of imageInfos) {
    // 请求远程资源的blob
    const imageBlob = await fetch(`${bucketHost}/${e.url}`).then((response) => response.blob());
    // 文件夹添加资源，图片也支持base64类型 {base64: true}
    zip!.file(`${e.name}${getFileExtension(e.url)}`, imageBlob, { binary: true });
  }
  // 把打包内容异步转成blob二进制格式
  zip.generateAsync({ type: 'blob' }).then((content) => {
    // 下载压缩包
    saveAs(content, zipName);
  });
}
