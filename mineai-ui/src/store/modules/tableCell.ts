import { defineStore } from 'pinia';

export const useTableCellStore = defineStore('tableCell', {
  state: () => ({
    title: '',
  }),
  getters: {},
  actions: {
    getCurrentTitle(): string {
      return this.title;
    },
    setCurrentTitle(title: string) {
      this.title = title;
    },
  },
});
