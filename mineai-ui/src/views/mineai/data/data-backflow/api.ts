import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

/** 数据源类型枚举 */
export type DataSourceType = 'RTSP' | 'HTTP';

/** 数据源类型显示名称映射 */
export const DATA_SOURCE_TYPE_LABEL: Record<DataSourceType, string> = {
  // 现在暂时不用摄像头视频流，前端只保留数据收集服务器。
  RTSP: '摄像头视频流',
  HTTP: '数据收集服务器',
};

// ==================== RTSP 数据源 ====================

export interface RtspSourceItem {
  id: number;
  name: string;
  sourceType: DataSourceType;
  rtspUrl: string;
  username?: string;
  description?: string;
  isDelete: number;
}

export interface RtspSourceSaveDTO {
  id?: number;
  name: string;
  rtspUrl: string;
  authRequired?: boolean;
  username?: string;
  password?: string;
  description?: string;
  isDelete?: number;
}

export async function pageRtspSource(params) {
  return await maHttp
    .get(
      {
        url: 'rtsp-sources',
        params: {
          current: params.page,
          size: params.pageSize,
          name: params.name,
          rtspUrl: params.rtspUrl,
          username: params.username,
          description: params.description,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((v) => {
      v.items = v.records;
      return v;
    });
}

export async function listRtspSource(): Promise<RtspSourceItem[]> {
  return await maHttp.get(
    { url: 'rtsp-sources/all' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function createRtspSource(data: RtspSourceSaveDTO) {
  return await maHttp.post(
    { url: 'rtsp-sources', data },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function updateRtspSource(id: number, data: RtspSourceSaveDTO) {
  return await maHttp.put(
    { url: `rtsp-sources/${id}`, data },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function deleteRtspSource(id: number) {
  return await maHttp.delete(
    { url: `rtsp-sources/${id}` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function getRtspPublicKey(): Promise<string> {
  return await maHttp.get(
    { url: 'rtsp-sources/public-key' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

// ==================== HTTP 摄像头服务器 ====================

export interface HttpCameraServerItem {
  id: number;
  name: string;
  serverUrl: string;
  description?: string;
  isDelete: number;
}

export interface HttpCameraItem {
  id: number;
  httpCameraServerId: number;
  cameraId: string;
  name: string;
  streamUrl?: string;
  description?: string;
  status?: string;
  lastSeenTime?: string;
  isDelete: number;
}

export interface HttpCameraServerSaveDTO {
  id?: number;
  name: string;
  serverUrl: string;
  /** RSA 公钥加密后的 Token，编辑时为空则保持原 Token */
  authToken?: string;
  description?: string;
  isDelete?: number;
}

export interface CameraInfo {
  cameraId: string;
  name: string;
  streamUrl?: string;
  sourceId: number;
  sourceType: DataSourceType;
  description?: string;
  status?: string;
}

export interface HttpCameraSyncResult {
  addedCount: number;
  updatedCount: number;
  offlineCount: number;
  totalCount: number;
}

export async function pageHttpCameraServer(params) {
  return await maHttp
    .get(
      {
        url: 'http-camera-servers',
        params: {
          current: params.page,
          size: params.pageSize,
          name: params.name,
          serverUrl: params.serverUrl,
          description: params.description,
        },
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((v) => {
      v.items = v.records;
      return v;
    });
}

export async function listHttpCameraServer(): Promise<HttpCameraServerItem[]> {
  return await maHttp.get(
    { url: 'http-camera-servers/all' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function createHttpCameraServer(data: HttpCameraServerSaveDTO) {
  return await maHttp.post(
    { url: 'http-camera-servers', data },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function updateHttpCameraServer(id: number, data: HttpCameraServerSaveDTO) {
  return await maHttp.put(
    { url: `http-camera-servers/${id}`, data },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function deleteHttpCameraServer(id: number) {
  return await maHttp.delete(
    { url: `http-camera-servers/${id}` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function getHttpCameraServerPublicKey(): Promise<string> {
  return await maHttp.get(
    { url: 'http-camera-servers/public-key' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function listHttpCameras(serverId: number): Promise<HttpCameraItem[]> {
  return await maHttp.get(
    { url: `http-camera-servers/${serverId}/cameras` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function syncHttpCameras(serverId: number): Promise<HttpCameraSyncResult> {
  return await maHttp.post(
    { url: `http-camera-servers/${serverId}/cameras/sync` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

export async function listAllHttpCameras(): Promise<HttpCameraItem[]> {
  return await maHttp.get(
    { url: 'http-cameras/all' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

// ==================== GPU 下发地址 ====================

export interface GpuUrlTargetItem {
  id: number;
  name: string;
  ip: string;
  port: number;
  gpuUrl: string;
  platformIp: string;
  platformPort: number;
  authCode?: string;
  description?: string;
  isDelete: number;
  updateTime?: string;
}

export interface GpuUrlTargetSaveDTO {
  id?: number;
  name: string;
  ip: string;
  port: number;
  gpuUrl: string;
  platformIp: string;
  platformPort: number;
  authCode?: string;
  description?: string;
  isDelete?: number;
}

export async function pageGpuUrlTarget(params) {
  return await maHttp
    .get(
      {
        url: 'gpuUrlTarget/page',
        params: {
          page: params.page,
          pageSize: params.pageSize,
          name: params.name,
          ip: params.ip,
          platformIp: params.platformIp,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      v.items = v.content || v.records || [];
      v.total = v.totalElements ?? v.total ?? v.items.length;
      return v;
    });
}

export async function createGpuUrlTarget(data: GpuUrlTargetSaveDTO) {
  return await maHttp.post(
    { url: 'gpuUrlTarget/saveOrUpdate', data },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function updateGpuUrlTarget(data: GpuUrlTargetSaveDTO) {
  return await maHttp.post(
    { url: 'gpuUrlTarget/saveOrUpdate', data },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function deleteGpuUrlTarget(id: number) {
  return await maHttp.delete(
    { url: `gpuUrlTarget/delete/${id}` },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function cancelCaptureExecution(id: number) {
  return await maHttp.post(
    { url: `rtsp-capture-executions/${id}/cancel` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

// ==================== 统一数据源选项（两种类型合并，用于任务选择器） ====================

export interface DataSourceOption {
  /** 唯一 key：sourceType:id */
  key: string;
  label: string;
  sourceId: number;
  sourceType: DataSourceType;
}

/**
 * 加载所有可用数据源。
 * 现在暂时不用摄像头视频流，前端只保留 HTTP 数据收集服务器。
 */
export async function loadAllDataSourceOptions(): Promise<DataSourceOption[]> {
  const [httpList] = await Promise.allSettled([listHttpCameraServer()]);
  const options: DataSourceOption[] = [];

  if (httpList.status === 'fulfilled') {
    httpList.value
      .filter((item) => item.isDelete === 0)
      .forEach((item) => {
        options.push({
          key: `HTTP:${item.id}`,
          label: `[HTTP] ${item.name}`,
          sourceId: item.id,
          sourceType: 'HTTP',
        });
      });
  }

  return options;
}
