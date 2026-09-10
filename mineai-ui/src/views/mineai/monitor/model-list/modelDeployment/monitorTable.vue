<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../assets/icons/titles.svg" alt="标识符" /><span
      style="font-size: medium"
      >版本绑定的监控设备</span
    >
  </div>
  <BasicTable @register="registerTable" :data-source="data" :show-index-column="false" />
</template>

<script lang="ts">
  import { defineComponent, onMounted, ref, inject } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { monitorColumn } from '/@/views/mineai/monitor/model-list/modelDeployment/table.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    components: { BasicTable },
    setup() {
      const data = ref([]);
      const modelVersionId = inject('modelVersion');

      const [registerTable] = useTable({
        columns: monitorColumn,
        bordered: true,
        canResize: true,
        pagination: false,
        scroll: { y: 600 },
      });

      //获取作业详情左侧信息
      onMounted(async () => {
        await getData();
      });

      async function getData() {
        // await ma.http(请求数据);
        data.value = await maHttp.get(
          {
            url: 'modelVersion/findMonitorsInfoByModelVersionId',
            params: { modelVersionId: modelVersionId },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        );
        console.log(data);
      }

      return {
        registerTable,
        data,
      };
    },
  });
</script>
