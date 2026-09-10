import { reactive } from 'vue';

const eventBus = reactive({});

// 触发事件并传递数据
export const emit = (event: string, data: any) => {
  if (eventBus[event]) {
    eventBus[event].forEach((callback: Function) => callback(data));
  }
};

// 注册事件监听器
export const on = (event: string, callback: Function) => {
  if (!eventBus[event]) {
    eventBus[event] = []; // 确保是数组
  }
  eventBus[event].push(callback);
};
