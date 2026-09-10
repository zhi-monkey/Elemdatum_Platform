import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

const prefix = DubheBackendUrlEnum.DUBHE_DATASET;
const base = 'import-tasks';

export interface ImportTransferTask {
  id: number;
  taskName: string;
  datasetType: 'IMAGE' | 'VIDEO' | 'POINT_CLOUD' | 'MULTI' | string;
  datasetId?: number;
  sourceType?: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | string;
  stage?: string;
  progress?: number;
  totalFiles?: number;
  successFiles?: number;
  failedFiles?: number;
  totalBytes?: number;
  transferredBytes?: number;
  retryCount?: number;
  errorMessage?: string;
  createTime?: string;
  updateTime?: string;
  events?: Array<{ eventType: string; message: string; detail?: string; createTime?: string }>;
}

export function getImportTransferTasks(params: Recordable = {}) {
  return maHttp.get({ url: base, params, headers: {} }, { urlPrefix: prefix });
}

export function getImportTransferTask(id: number) {
  return maHttp.get({ url: `${base}/${id}`, headers: {} }, { urlPrefix: prefix });
}

export function createImportTransferTask(data: Recordable) {
  return maHttp.post({ url: base, data, headers: {} }, { urlPrefix: prefix });
}

export function updateImportTransferProgress(id: number, data: Recordable) {
  return maHttp.post({ url: `${base}/${id}/progress`, data, headers: {} }, { urlPrefix: prefix });
}

export function completeImportTransferTask(id: number) {
  return maHttp.post({ url: `${base}/${id}/complete`, headers: {} }, { urlPrefix: prefix });
}

export function failImportTransferTask(id: number, message: string) {
  return maHttp.post(
    { url: `${base}/${id}/fail`, params: { message }, headers: {} },
    { urlPrefix: prefix },
  );
}

export function cancelImportTransferTask(id: number) {
  return maHttp.post({ url: `${base}/${id}/cancel`, headers: {} }, { urlPrefix: prefix });
}

export function retryImportTransferTask(id: number) {
  return maHttp.post({ url: `${base}/${id}/retry`, headers: {} }, { urlPrefix: prefix });
}

export function deleteImportTransferTasks(ids: number[]) {
  return maHttp.delete({ url: base, data: ids, headers: {} }, { urlPrefix: prefix });
}
