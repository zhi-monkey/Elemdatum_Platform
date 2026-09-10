import { defineStore } from 'pinia';
import {
  newMinIOUpload,
  renameFile,
} from '/@/views/mineai/data/dataset-details2/components/UploadForm/util';
import { getFileFromMinIO } from '/@/views/mineai/data/dataset-details2/util';
import { submitVideo, submitImage } from '/@/views/mineai/data/dataset-details2/api';
import { useMessage } from '/@/hooks/web/useMessage';

const { createMessage } = useMessage();

export const useUploadStore = defineStore({
  id: 'upload',
  state: (): any => ({
    // 数据集id对应文件列表
    uploadMap: new Map(),
  }),
  getters: {
    // 查询数据集是否上传中
    queryUploadStatus(): any {
      return (id) => {
        if (!this.uploadMap.has(id)) return null;
        const uploadState = this.uploadMap.get(id);
        return {
          percentage: Math.floor((uploadState.uploaded / uploadState.size) * 100),
          ...uploadState
        };
      };
    },
    // 查询上传进度
    queryUploadProgress(): any {
      return (id) => {
        const uploadState = this.uploadMap.get(id);
        return uploadState.uploaded / uploadState.size;
      };
    },
  },
  actions: {
    // 初始化上传进度
    initUploadState(id, fileList, objectPath, type, autoInsertDatabase = true) {
      this.uploadMap.set(id, { size: fileList.length, uploaded: 0, type, fileList: [] });
      const renameFileList = fileList.map((file) => ({
        ...file,
        name: renameFile(file.name, { hash: true, encode: true }),
      }));
      this.fileUpload(id, renameFileList, objectPath, autoInsertDatabase);
    },
    // 文件上传核心
    async fileUpload(id, fileList, objectPath, autoInsertDatabase = true) {
      await newMinIOUpload(id, objectPath, fileList, autoInsertDatabase);
    },
    // 更新上传状态
    async setUploadState(id, file) {
      const uploadState = this.uploadMap.get(id);
      uploadState.fileList.push(file);
      this.uploadMap.set(id, {
        ...uploadState,
        uploaded: uploadState.fileList.length,
      });
      // 全部上传完毕，存库
      if (uploadState.size === uploadState.fileList.length) {
        const files = getFileFromMinIO(uploadState.fileList);
        // 图片或视频
        try {
          uploadState.type === 0
            ? await submitImage(id, files)
            : await submitVideo(id, {
                files: files.map((file) => ({
                  url: file.url,
                  frameInterval: 1,
                })),
              });
          createMessage.success(`数据集id${id}上传完毕`);
        } catch (e) {
          console.error(e);
          createMessage.error(`数据集id${id}上传失败`);
        } finally {
          this.resetState(id);
        }
      }
    },

    // 更新上传状态（不存库）
    async setUploadStateWithoutInsertDataBase(id, file) {
      const uploadState = this.uploadMap.get(id);
      uploadState.fileList.push(file);
      this.uploadMap.set(id, {
        ...uploadState,
        uploaded: uploadState.fileList.length,
      });
    },

    // 清除上传记录
    resetState(id): void {
      this.uploadMap.delete(id);
    },
  },
});