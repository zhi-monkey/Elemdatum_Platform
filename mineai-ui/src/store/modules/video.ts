import { defineStore } from 'pinia';

export const useVideoStore = defineStore({
  id: 'video',
  state: (): any => ({
    videoList: [],
    extractingVideoName: '', // 当前正在抽帧的视频名称
    extractingProgress: 0, // 当前抽帧进度 (0-100)
  }),
  getters: {
    getVideoList(): any {
      return this.videoList;
    },
    getExtractingVideoName(): string {
      return this.extractingVideoName;
    },
    getExtractingProgress(): number {
      return this.extractingProgress;
    },
  },
  actions: {
    addVideo(id, status, name = '') {
      this.videoList.push({ id, status, name });
      this.extractingVideoName = name;
      this.extractingProgress = 0;
    },
    setExtractingProgress(progress: number) {
      this.extractingProgress = progress;
    },
    resetVideoList() {
      this.videoList = [];
      this.extractingVideoName = '';
      this.extractingProgress = 0;
    },
  },
});
