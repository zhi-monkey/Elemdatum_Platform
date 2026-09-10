// /src/api/sys/notification.ts
import { maHttp } from '/@/utils/http/axios';
import type { ErrorMessageMode } from '/#/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export interface NotificationQuery {
  current?: number;
  size?: number;
  readStatus?: number;
  notificationType?: number;
  operationType?: string;
}

export interface NotificationVO {
  id: number;
  notificationType: number;
  operationType: string;
  message: string;
  readStatus: number;
  createTime: string; // 后端如果是 Date，请在使用处做格式化
  payload?: string;
  url?: string;
}

// 可选：通用分页返回
export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/**
 * 未读数量
 */
export function getUnreadCount(mode: ErrorMessageMode = 'none') {
  return maHttp.get<number>(
    { url: 'notifications/unread-count' },
    {
      errorMessageMode: mode,
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
}

/**
 * 分页查询（格式化）
 */
export function pageFormatted(params: NotificationQuery, mode: ErrorMessageMode = 'none') {
  return maHttp.get<PageResult<NotificationVO>>(
    {
      url: 'notifications/formatted',
      params,
    },
    {
      errorMessageMode: mode,
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
}

/**
 * 标记单条为已读
 */
export function markRead(id: number, mode: ErrorMessageMode = 'none') {
  return maHttp.post<boolean>(
    {
      url: `notifications/${id}/read`,
    },
    {
      errorMessageMode: mode,
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
}

/**
 * 标记全部为已读
 */
export function markAllRead(mode: ErrorMessageMode = 'none') {
  return maHttp.post<number>(
    {
      url: 'notifications/read-all',
    },
    {
      errorMessageMode: mode,
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
}

/**
 * 获取最新的未读消息（用于弹窗提醒）
 */
export function getLatestUnread(limit: number = 5, mode: ErrorMessageMode = 'none') {
  return maHttp.get<PageResult<NotificationVO>>(
    {
      url: 'notifications/formatted',
      params: {
        current: 1,
        size: limit,
        readStatus: 0, // 只获取未读消息
      },
    },
    {
      errorMessageMode: mode,
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
}
