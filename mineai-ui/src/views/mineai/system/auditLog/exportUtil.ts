import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';
import { formatToDateTime } from '/@/utils/dateUtil';
import { getOperationTypeText } from './auditLog.data';

/**
 * 审计日志数据接口
 */
export interface AuditLogExportData {
  id: number;
  createDate: string;
  uname: string;
  ip: string;
  description: string;
  operationType: number;
}

/**
 * 导出审计日志为 Excel 文件
 * @param data 审计日志数据列表
 * @param filename 文件名（不含扩展名）
 */
export function exportAuditLogToExcel(data: AuditLogExportData[], filename: string = '审计日志') {
  // 转换数据格式，将字段映射为中文表头
  const exportData = data.map((item) => ({
    ID: item.id,
    操作时间: formatToDateTime(item.createDate),
    操作人: item.uname,
    IP地址: item.ip,
    操作描述: item.description,
    操作类型: getOperationTypeText(item.operationType),
  }));

  // 创建工作表
  const worksheet = XLSX.utils.json_to_sheet(exportData);

  // 设置列宽
  worksheet['!cols'] = [
    { wch: 10 }, // ID
    { wch: 20 }, // 操作时间
    { wch: 15 }, // 操作人
    { wch: 15 }, // IP地址
    { wch: 40 }, // 操作描述
    { wch: 12 }, // 操作类型
  ];

  // 设置操作描述列自动换行
  if (worksheet['!ref']) {
    const range = XLSX.utils.decode_range(worksheet['!ref']);
    for (let R = range.s.r + 1; R <= range.e.r; ++R) {
      const cellAddress = XLSX.utils.encode_cell({ r: R, c: 4 }); // E列（操作描述，索引从0开始）
      if (worksheet[cellAddress]) {
        if (!worksheet[cellAddress].s) {
          worksheet[cellAddress].s = {};
        }
        worksheet[cellAddress].s.alignment = {
          wrapText: true,
          vertical: 'top'
        };
      }
    }
  }

  // 创建工作簿
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, '审计日志');

  // 生成 Excel 文件
  const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' });
  const blob = new Blob([excelBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });

  // 触发下载，文件名格式：审计日志_年_月_日_时_分
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hour = String(now.getHours()).padStart(2, '0');
  const minute = String(now.getMinutes()).padStart(2, '0');
  const timestamp = `${year}_${month}_${day}_${hour}_${minute}`;
  saveAs(blob, `${filename}_${timestamp}.xlsx`);
}
