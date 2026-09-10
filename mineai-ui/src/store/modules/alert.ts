import { defineStore } from 'pinia';
import { getAlerts, IAlert } from '/@/api/mineai/alertData';

export const useAlertStore = defineStore('alert', {
  state: () => ({
    data: [] as IAlert[],
    total: 0,
    page: -1,
    selectedTreeItems: [],
  }),
  getters: {},
  actions: {
    async loadAllAlerts(params) {
      params.modelNames = [];
      params.monitorNames = [];
      this.selectedTreeItems.forEach((item) => {
        // @ts-ignore
        if (item.monitorInfo.model !== '未绑定算法') {
          // 去重并且添加到查找参数中
          if (
            // @ts-ignore
            params.modelNames.filter((name) => name === item.monitorInfo.model).length === 0
          ) {
            // @ts-ignore
            params.modelNames.push(item.monitorInfo.model);
          }
          if (
            // @ts-ignore
            params.monitorNames.filter((name) => name === item.monitorInfo.monitor).length === 0
          ) {
            // @ts-ignore
            params.monitorNames.push(item.monitorInfo.monitor);
          }
        }
      });
      const ret = await getAlerts(params);
      this.page = params.page;
      this.data = ret.content;
      this.total = ret.totalElements;
    },
    setAlertItems(selectedTreeItems) {
      // 筛选按钮更新，状态更新
      this.selectedTreeItems = selectedTreeItems;
    },
  },
});
