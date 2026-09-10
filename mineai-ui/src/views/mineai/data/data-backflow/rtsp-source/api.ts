/**
 * RTSP 数据源 API - 从统一数据源 API 中重新导出，保持向后兼容
 */
export {
  type RtspSourceItem,
  type RtspSourceSaveDTO,
  pageRtspSource,
  listRtspSource,
  createRtspSource,
  updateRtspSource,
  deleteRtspSource,
  getRtspPublicKey,
} from '../api';