import xlsx from 'xlsx';
import type { WorkBook } from 'xlsx';
import type { JsonToSheet, AoAToSheet, ssJsonToSheet} from './typing';
const { utils, writeFile } = xlsx;

const DEF_FILE_NAME = 'excel-list.xlsx';

export function jsonToSheetXlsx<T = any>({
  data,
  header,
  filename = DEF_FILE_NAME,
  json2sheetOpts = {},
  write2excelOpts = { bookType: 'xlsx' },
}: JsonToSheet<T>) {
  const arrData = [...data];
  if (header) {
    arrData.unshift(header);
    json2sheetOpts.skipHeader = true;
  }

  const worksheet = utils.json_to_sheet(arrData, json2sheetOpts);

  /* add worksheet to workbook */
  const workbook: WorkBook = {
    SheetNames: [filename],
    Sheets: {
      [filename]: worksheet,
    },
  };
  /* output format determined by filename */
  writeFile(workbook, filename, write2excelOpts);
  /* at this point, out.xlsb will have been downloaded */
}

export function aoaToSheetXlsx<T = any>({
  data,
  header,
  filename = DEF_FILE_NAME,
  write2excelOpts = { bookType: 'xlsx' },
}: AoAToSheet<T>) {
  const arrData = [...data];
  if (header) {
    arrData.unshift(header);
  }

  const worksheet = utils.aoa_to_sheet(arrData);

  /* add worksheet to workbook */
  const workbook: WorkBook = {
    SheetNames: [filename],
    Sheets: {
      [filename]: worksheet,
    },
  };
  /* output format determined by filename */
  writeFile(workbook, filename, write2excelOpts);
  /* at this point, out.xlsb will have been downloaded */
}
//基础模块用户列表-excel表格导出
export function ssjsonToSheetXlsx<T = any>({
 data,
 header,
 map,
 filename = DEF_FILE_NAME,
 json2sheetOpts = {},
 write2excelOpts = { bookType: 'xlsx' },
}: ssJsonToSheet<T>) {
  let arrData: T[] = [];
  if (header) {
    const key = Object.keys(header);
    // @ts-ignore
    arrData = [...data].map((dd) => {
      const d = {};
      for (const k of key) {
        if (k in dd) {
          if (map) {
            if (k in map) {
              const v = map[k];
              d[k] = v[dd[k]];
            } else {
              d[k] = dd[k];
            }
          } else {
            d[k] = dd[k];
          }
        }
      }
      return d;
    });
  }
  if (header) {
    // @ts-ignore
    arrData.unshift(header);
    json2sheetOpts.skipHeader = true;
  }

  const worksheet = utils.json_to_sheet(arrData, json2sheetOpts);

  /* add worksheet to workbook */
  const workbook: WorkBook = {
    SheetNames: [filename],
    Sheets: {
      [filename]: worksheet,
    },
  };
  /* output format determined by filename */
  writeFile(workbook, filename, write2excelOpts);
  /* at this point, out.xlsb will have been downloaded */
}
