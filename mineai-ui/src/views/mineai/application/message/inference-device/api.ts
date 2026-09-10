import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

// 业务展示名已调整为“模型接收地址”，但后端接口与字段仍沿用历史命名 InferenceDevice。
export interface InferenceDeviceItem {
  id: number;
  deviceName: string;
  inferenceIp: string;
  inferencePort: number;
  authCode?: string;
  remark?: string;
  createdBy?: string;
  createdAt?: string;
  deviceStatus?: string;
  loadedAlgorithmApp?: string;
}

export interface InferenceDeviceSaveDTO {
  id?: number;
  deviceName: string;
  inferenceIp: string;
  inferencePort: number;
  authCode?: string;
  remark?: string;
}

export async function pageInferenceDevice(params) {
  return await maHttp
    .get(
      {
        url: 'inference-devices',
        params: {
          current: params.page,
          size: params.pageSize,
          deviceName: params.deviceName,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((v) => {
      v.items = v.records;
      return v;
    });
}

export async function deleteInferenceDevice(id: number) {
  return await maHttp.delete(
    { url: `inference-devices/${id}` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function createInferenceDevice(data: InferenceDeviceSaveDTO) {
  return await maHttp.post(
    { url: 'inference-devices', data },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function updateInferenceDevice(id: number, data: InferenceDeviceSaveDTO) {
  return await maHttp.put(
    { url: `inference-devices/${id}`, data },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}
