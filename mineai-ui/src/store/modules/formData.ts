import { defineStore } from 'pinia';

export const useDataSetFormDataStore = defineStore('datasetFormDataStore', {
  state: () => ({
    name: '',
    annotateType: undefined,
    createTime: [],
  }),
  getters: {},
  actions: {
    getCurrentName(): string {
      return this.name;
    },
    setCurrentName(name: string) {
      this.name = name;
    },
    getCurrentAnnotateType(): any {
      return this.annotateType;
    },
    setCurrentAnnotateType(annotateType: any) {
      this.annotateType = annotateType;
    },
    getCurrentCreateTime(): any {
      return this.createTime;
    },
    setCurrentCreateTime(createTime: any) {
      this.createTime = createTime;
    },
  },
});
