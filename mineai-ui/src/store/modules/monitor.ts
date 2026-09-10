import { defineStore } from 'pinia';
import { store } from '/@/store';

interface MonitorState {
  ifTreeShow: boolean;
}

export const useMonitorStore = defineStore({
  id: 'monitor',
  state: (): MonitorState => ({
    ifTreeShow: true,
  }),
  getters: {
    getFullScreen(): boolean {
      return this.ifTreeShow;
    },
  },
  actions: {
    changeShow() {
      this.ifTreeShow = !this.ifTreeShow;
    },
    resetState() {
      this.ifTreeShow = false;
    },
  },
});

// Need to be used outside the setup
export function useMonitorStoreWithOut() {
  return useMonitorStore(store);
}
