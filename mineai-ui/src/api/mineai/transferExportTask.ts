import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

const base = 'export-tasks';
const options = { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET };

export function getTransferExportTasks(params: Recordable = {}) {
  return maHttp.get({ url: base, params, headers: {} }, options);
}
export function getTransferExportTask(id: number) {
  return maHttp.get({ url: `${base}/${id}`, headers: {} }, options);
}
export function cancelTransferExportTask(id: number) {
  return maHttp.post({ url: `${base}/${id}/cancel`, headers: {} }, options);
}
export function deleteTransferExportTasks(ids: number[]) {
  return maHttp.delete({ url: base, data: ids, headers: {} }, options);
}
export function downloadTransferExportTask(id: number) {
  return maHttp.get(
    { url: `${base}/${id}/download`, responseType: 'blob', headers: {} },
    {
      ...options,
      isReturnNativeResponse: true,
      isTransformResponse: false,
    },
  );
}
