/**
 * 标注位置记录工具
 * 用于记录用户在每个数据集中标注到的图片位置，支持：
 * 1. 切换已标/未标筛选时保持当前图片位置
 * 2. 进入标注页面时跳转到上次标注的图片
 */

interface AnnotationPositionRecord {
  datasetId: number;
  userId: string | number;
  fileId: number;
  filterType: number[]; // 筛选条件 [101] 未标注, [104] 已标注, [101, 104] 全部
  timestamp: number;
}

const STORAGE_KEY = 'annotation_position_records';
const MAX_RECORDS = 50; // 最多保存50条记录，避免localStorage过大

/**
 * 获取所有标注位置记录
 */
export function getAllPositionRecords(): AnnotationPositionRecord[] {
  try {
    const data = localStorage.getItem(STORAGE_KEY);
    if (!data) return [];
    return JSON.parse(data) as AnnotationPositionRecord[];
  } catch (e) {
    console.warn('读取标注位置记录失败:', e);
    return [];
  }
}

/**
 * 保存标注位置记录
 */
function savePositionRecords(records: AnnotationPositionRecord[]): void {
  try {
    // 只保留最近的MAX_RECORDS条记录
    const trimmedRecords = records.slice(-MAX_RECORDS);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(trimmedRecords));
  } catch (e) {
    console.warn('保存标注位置记录失败:', e);
  }
}

/**
 * 更新或创建标注位置记录
 * @param datasetId 数据集ID
 * @param userId 用户ID
 * @param fileId 当前图片ID
 * @param filterType 当前筛选条件
 */
export function updatePositionRecord(
  datasetId: number,
  userId: string | number,
  fileId: number,
  filterType: number[],
): void {
  if (!datasetId || !userId || !fileId) return;

  const records = getAllPositionRecords();

  // 查找是否已有该数据集和用户的记录
  const existingIndex = records.findIndex((r) => r.datasetId === datasetId && r.userId === userId);

  const newRecord: AnnotationPositionRecord = {
    datasetId,
    userId,
    fileId,
    filterType: [...filterType],
    timestamp: Date.now(),
  };

  if (existingIndex !== -1) {
    // 更新现有记录
    records[existingIndex] = newRecord;
  } else {
    // 添加新记录
    records.push(newRecord);
  }

  savePositionRecords(records);
}

/**
 * 获取指定数据集和用户的标注位置记录
 * @param datasetId 数据集ID
 * @param userId 用户ID
 * @returns 标注位置记录，如果不存在则返回null
 */
export function getPositionRecord(
  datasetId: number,
  userId: string | number,
): AnnotationPositionRecord | null {
  if (!datasetId || !userId) return null;

  const records = getAllPositionRecords();
  return records.find((r) => r.datasetId === datasetId && r.userId === userId) || null;
}

/**
 * 删除指定数据集和用户的标注位置记录
 * @param datasetId 数据集ID
 * @param userId 用户ID
 */
export function removePositionRecord(datasetId: number, userId: string | number): void {
  if (!datasetId || !userId) return;

  const records = getAllPositionRecords();
  const filteredRecords = records.filter(
    (r) => !(r.datasetId === datasetId && r.userId === userId),
  );
  savePositionRecords(filteredRecords);
}

/**
 * 清除所有标注位置记录
 */
export function clearAllPositionRecords(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (e) {
    console.warn('清除标注位置记录失败:', e);
  }
}

/**
 * 检查记录是否过期（默认7天）
 * @param record 标注位置记录
 * @param maxAge 最大有效期（毫秒），默认7天
 */
export function isRecordExpired(
  record: AnnotationPositionRecord,
  maxAge: number = 7 * 24 * 60 * 60 * 1000,
): boolean {
  return Date.now() - record.timestamp > maxAge;
}
