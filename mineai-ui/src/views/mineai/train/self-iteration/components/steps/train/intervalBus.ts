// intervalBus.ts
import { Ref, ref } from 'vue';

interface IntervalBus {
  canSet: Ref<boolean | null>;
  setCanSet: (value: boolean) => void;
  progressInterval_T: Ref<number | null>;
  refreshInterval_T: Ref<number | null>;
  paramInterval_T: Ref<number | null>;
  epochDataInterval_T: Ref<number | null>;
  setProgressInterval_T: (value: number) => void;
  setParamInterval_T: (value: number) => void;
  setEpochDataInterval_T: (value: number) => void;
  setInitStateInterval: (value: number) => void;
  setRefreshInterval_T: (value: number) => void;
  refreshInterval_C: Ref<number | null>;
  setRefreshInterval_C: (value: number) => void;
  clearIntervals: () => void;
  initStateInterval: Ref<number | null>;
  datasetCardStatusInterval: Ref<number | null>;
  setDatasetCardStatusInterval: (value: number) => void;
  clearDatasetCardStatusInterval: (value: number) => void;
  clearInitStateInterval: () => void;
  isDatasetCardStatusIntervalExists: () => boolean;
  isInitStateIntervalExists: () => boolean;
  isProgressIntervalTExists: () => boolean;
  isRefreshIntervalTExists: () => boolean;
  isRefreshIntervalCExists: () => boolean;
  isEpochDataInterval_TExists: () => boolean;
  isDatasetCardNeedRefresh: Ref<boolean>;
}

export const intervalBus: IntervalBus = {
  canSet: ref<boolean | null>(true),
  progressInterval_T: ref<number | null>(null),
  paramInterval_T: ref<number | null>(null),
  epochDataInterval_T: ref<number | null>(null),
  refreshInterval_T: ref<number | null>(null),
  refreshInterval_C: ref<number | null>(null),
  initStateInterval: ref<number | null>(null),
  datasetCardStatusInterval: ref<number | null>(null),
  isDatasetCardNeedRefresh: ref<boolean>(false),

  setProgressInterval_T(value: number) {
    console.log('设置了setProgressInterval_T');
    this.progressInterval_T.value = value;
  },
  setParamInterval_T(value: number) {
    this.paramInterval_T.value = value;
  },
  setEpochDataInterval_T(value: number) {
    this.epochDataInterval_T.value = value;
  },
  setRefreshInterval_T(value: number) {
    console.log('设置了setRefreshInterval_T');
    this.refreshInterval_T.value = value;
  },
  setRefreshInterval_C(value: number) {
    console.log('设置了setRefreshInterval_C');
    this.refreshInterval_C.value = value;
  },
  setCanSet(value: boolean) {
    this.canSet.value = value;
  },
  setInitStateInterval(value: number) {
    if (this.canSet.value && value !== null && this.initStateInterval.value === null) {
      console.log('设置了initStateInterval');
      this.initStateInterval.value = value;
    }
  },
  clearDatasetCardStatusInterval() {
    if (this.datasetCardStatusInterval.value !== null) {
      clearInterval(this.datasetCardStatusInterval.value);
      this.datasetCardStatusInterval.value = null;
    }
  },
  clearInitStateInterval() {
    if (this.initStateInterval.value !== null) {
      clearInterval(this.initStateInterval.value);
      this.initStateInterval.value = null;
    }
  },
  setDatasetCardStatusInterval(value: number) {
    if (this.canSet.value) {
      this.datasetCardStatusInterval.value = value;
    }
  },
  isDatasetCardStatusIntervalExists() {
    return this.datasetCardStatusInterval.value !== null;
  },
  isInitStateIntervalExists() {
    return this.initStateInterval.value !== null;
  },

  isProgressIntervalTExists() {
    return this.progressInterval_T.value !== null;
  },
  isRefreshIntervalTExists() {
    return this.refreshInterval_T.value !== null;
  },
  isRefreshIntervalCExists() {
    return this.refreshInterval_C.value !== null;
  },
  isEpochDataInterval_TExists() {
    return this.epochDataInterval_T.value !== null;
  },

  clearIntervals() {
    console.log('清除intervals');
    if (this.progressInterval_T.value !== null) {
      clearInterval(this.progressInterval_T.value);
      this.progressInterval_T.value = null;
    }
    if (this.refreshInterval_T.value !== null) {
      clearInterval(this.refreshInterval_T.value);
      this.refreshInterval_T.value = null;
    }
    if (this.refreshInterval_C.value !== null) {
      clearInterval(this.refreshInterval_C.value);
      this.refreshInterval_C.value = null;
    }
    if (this.paramInterval_T.value !== null) {
      clearInterval(this.paramInterval_T.value);
      this.paramInterval_T.value = null;
    }
    if (this.epochDataInterval_T.value !== null) {
      clearInterval(this.epochDataInterval_T.value);
      this.epochDataInterval_T.value = null;
    }
    if (this.datasetCardStatusInterval.value !== null) {
      clearInterval(this.datasetCardStatusInterval.value);
      this.datasetCardStatusInterval.value = null;
    }
  },
};

// 事件总线
import mitt from 'mitt';

export const guidedTrainEventEmitter = mitt();
