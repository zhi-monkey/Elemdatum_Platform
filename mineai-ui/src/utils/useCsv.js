import { ref } from 'vue';
//import Papa from 'papaparse';
import * as XLSX from 'xlsx';
import { message } from 'ant-design-vue';

/*export function useCSV() {
  const data = ref([]);
  const headers = ref([]);
  const loading = ref(false);
  const error = ref(null);

  const parseCSV = (file) => {
    loading.value = true;
    error.value = null;

    return new Promise((resolve, reject) => {
      Papa.parse(file, {
        header: true,
        skipEmptyLines: true,
        complete: (results) => {
          if (results.errors.length > 0) {
            const errorMsg = results.errors
              .map((err) => `${err.message} (行 ${err.row})`)
              .join('; ');
            error.value = new Error(errorMsg);
            message.error(`CSV解析错误: ${errorMsg}`);
            reject(error.value);
          } else {
            data.value = results.data;
            headers.value = results.meta.fields || [];
            message.success(`成功解析CSV文件，共 ${results.data.length} 行数据`);
            resolve(results);
          }
          loading.value = false;
        },
        error: (err) => {
          error.value = err;
          loading.value = false;
          message.error(`CSV解析失败: ${err.message}`);
          reject(err);
        },
      });
    });
  };

  // 没试过
  const loadRemoteCSV = async (url) => {
    try {
      loading.value = true;
      error.value = null;
      const response = await fetch(url);
      if (!response.ok) throw new Error(`HTTP错误: ${response.status}`);

      const csvText = await response.text();

      Papa.parse(csvText, {
        header: true,
        skipEmptyLines: true,
        complete: (results) => {
          if (results.errors.length > 0) {
            const errorMsg = results.errors
              .map((err) => `${err.message} (行 ${err.row})`)
              .join('; ');
            error.value = new Error(errorMsg);
            message.error(`CSV解析错误: ${errorMsg}`);
          } else {
            data.value = results.data;
            headers.value = results.meta.fields || [];
            message.success(`成功加载远程CSV，共 ${results.data.length} 行数据`);
          }
          loading.value = false;
        },
      });
    } catch (err) {
      error.value = err;
      loading.value = false;
      message.error(`加载远程CSV失败: ${err.message}`);
    }
  };

  const clearData = () => {
    data.value = [];
    headers.value = [];
    error.value = null;
  };

  return {
    data,
    headers,
    loading,
    error,
    parseCSV,
    loadRemoteCSV,
    clearData,
  };
}*/

export function useExcel() {
  const data = ref([]);
  const headers = ref([]);
  const loading = ref(false);
  const error = ref(null);

  const parseExcel = (file) => {
    loading.value = true;
    error.value = null;

    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target.result);
          const workbook = XLSX.read(data, { type: 'array' });

          // 获取第一个工作表
          const firstSheetName = workbook.SheetNames[0];
          const worksheet = workbook.Sheets[firstSheetName];

          // 转换为 JSON 格式
          const jsonData = XLSX.utils.sheet_to_json(worksheet);
          data.value = jsonData;
          headers.value = Object.keys(jsonData[0] || {});

          message.success(`成功解析Excel文件，共 ${jsonData.length} 行数据`);
          resolve({ data: jsonData, headers: headers.value });
        } catch (err) {
          error.value = err;
          message.error(`Excel解析失败: ${err.message}`);
          reject(err);
        } finally {
          loading.value = false;
        }
      };

      reader.onerror = (e) => {
        error.value = new Error('文件读取失败');
        loading.value = false;
        message.error('文件读取失败');
        reject(error.value);
      };

      reader.readAsArrayBuffer(file);
    });
  };

  const clearData = () => {
    data.value = [];
    headers.value = [];
    error.value = null;
  };

  return {
    data,
    headers,
    loading,
    error,
    parseExcel,
    clearData,
  };
}
