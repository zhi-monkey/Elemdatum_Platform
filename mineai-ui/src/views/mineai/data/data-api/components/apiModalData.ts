import { BasicColumn } from '/@/components/Table/src/types/table';

const prefix = window.location.host;

export function getParamColumns(): BasicColumn[] {
  return [
    {
      title: '参数',
      dataIndex: 'param',
      width: 100,
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 100,
    },
    {
      title: '说明',
      dataIndex: 'description',
      width: 200,
    },
  ];
}

export function getParamData() {
  const paramData: any = (() => {
    const arr: any = [];
    arr.push({
      param: 'id',
      type: 'long',
      description: '请求参数，数据集编号',
    });

    arr.push({
      param: 'text',
      type: 'string',
      description: '返回参数，调用结果描述',
    });

    arr.push({
      param: 'code',
      type: 'string',
      description: '返回参数，状态码',
    });

    arr.push({
      param: 'payload',
      type: 'object',
      description: '返回参数，接口调用结果',
    });

    return arr;
  })();
  return paramData;
}

export function getApiColData(param1, param2) {
  console.log('p1 in data:' + param1 + 'p2 in data' + param2);
  const apiData = apiDescriptionData[param1 - 1][param2 - 1];
  const tmpData = apiData['params'];
  const resultData: any[] = [];
  for (let i = 0; i < tmpData.length; i++) {
    resultData.push(tmpData[i]);
  }
  resultData.push({ param: 'text', type: 'string', description: '返回参数，调用结果描述' });
  resultData.push({ param: 'code', type: 'string', description: '返回参数，状态码' });
  if (apiData['return']['SUCCESS'].hasOwnProperty('payload')) {
    resultData.push({ param: 'payload', type: 'object', description: '返回参数，接口调用结果' });
  }
  return resultData;
}

//any其实是字典，一级[]表示一个接口大类，二级[]表示大分类中的具体的接口
const apiDescriptionData: any[][] = [
  [
    {
      url: prefix + '/dm/dataset/findDatasetById?id=ID',
      params: [
        {
          param: 'id',
          type: 'long',
          description: '数据集id',
        },
      ],
      return: {
        SUCCESS: {
          text: '成功',
          code: 'MS_20000',
          payload: {
            id: '数据集编号',
            name: '数据集名称',
            description: '数据集描述',
            version: '数据集版本',
          },
        },
        FAILED: {
          text: '失败',
          code: 'DM_00014',
        },
      },
    },
  ],
  [
    {
      url: prefix + '/dm/dataset/datasetList',
      params: [],
      return: {
        SUCCESS: {
          text: '成功',
          code: 'MS_20000',
          payload: [
            {
              id: '数据集编号',
              name: '数据集名称',
              description: '数据集描述',
              version: '数据集版本',
            },
          ],
        },
        FAILED: {
          text: '失败',
          code: 'DM_00014',
        },
      },
    },
    {
      url: prefix + '/dm/dataset/datasetListUser?userid=ID',
      params: [
        {
          param: 'userid',
          type: 'long',
          description: '用户id',
        },
      ],
      return: {
        SUCCESS: {
          text: '成功',
          code: 'MS_20000',
          payload: [
            {
              id: '数据集编号',
              name: '数据集名称',
              description: '数据集描述',
              version: '数据集版本',
            },
          ],
        },
        FAILED: {
          text: '失败',
          code: 'DM_00014',
        },
      },
    },
    {
      url: prefix + '/dm/dataset/datasetListByBaseDataset?baseDatasetId=ID',
      params: [
        {
          param: 'baseDatasetId',
          type: 'long',
          description: '系列中最早版本数据集的编号',
        },
      ],
      return: {
        SUCCESS: {
          text: '成功',
          code: 'MS_20000',
          payload: [
            {
              id: '数据集编号',
              name: '数据集名称',
              description: '数据集描述',
              version: '数据集版本',
            },
          ],
        },
        FAILED: {
          text: '失败',
          code: 'DM_00014',
        },
      },
    },
  ],
];
const resultInfoData: any[][] = [
  [
    '//正常返回的JSON数据包\n' +
      '{\n' +
      '    "text": "成功"\n' +
      '    "code": "MS_20000"\n' +
      '    "payload": 查询结果\n' +
      '}\n' +
      '//错误时返回JSON数据包\n' +
      '{\n' +
      '    "text": ”失败“\n' +
      '    "code": "DM_00014"\n' +
      '}',
  ],
];

export function getDescription(type1, type2) {
  return apiDescriptionData[type1 - 1][type2 - 1];
}

export function getResultInfo(type1, type2) {
  if (type2 == 1 && type1 == 1) return resultInfoData[type1 - 1][type2 - 1];
  else return resultInfoData[0][0];
}
