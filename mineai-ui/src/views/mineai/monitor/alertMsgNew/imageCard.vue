<template>
  <CardList :params="params" :api="alertApi" @get-method="getMethod" @delete="handleDel" />
</template>
<script lang="ts" setup>
  import { CardList } from '/@/views/mineai/monitor/alertMsgNew/components/CardList';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const { notification } = useMessage();
  // 请求api时附带参数
  const params = {};
  // 请求api
  const alertApi = async (params) => {
    return await getData(params);
  };
  //请求函数
  async function getData(params) {
    const v = await maHttp.get(
      {
        url: 'modelAlert/dynamicFindModelAlert',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
    v.items = v.content;
    v.total = v.totalElements;
    return v;
  }

  let reload = () => {};

  // 获取内部fetch方法;
  function getMethod(m: any) {
    reload = m;
  }

  //删除按钮事件
  function handleDel(id) {
    notification.success({ message: `成功删除${id}` });
    reload();
  }
</script>
