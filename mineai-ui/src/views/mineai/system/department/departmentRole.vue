<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px" :contentStyle="{ margin: '18px' }">
    <template #title>个人资源使用量信息</template>
    <Card class="card">
      <div class="flex flex-row justify-start space-x-5 text-base">
        <div>
          部门ID: <span style="color: #5dade2">{{ deploymentId }}</span>
        </div>
        <div>
          部门名称: <span style="color: #5dade2">{{ deploymentName }}</span>
        </div>
        <div>
          部门人数: <span style="color: #9b59b6">{{ userCount }}</span>
        </div>
        <div>
          部门内存总量: <span style="color: #f5b956">{{ memoryLimit }}GB</span>
        </div>
        <div>
          部门CPU总量: <span style="color: #f5b956">{{ cpuLimit }}核</span>
        </div>
        <div>
          部门GPU显存总量: <span style="color: #f5b956">{{ gpuMemoryLimit }}GB</span>
        </div>
      </div>
    </Card>
    <BasicTable @register="registerTable" />
  </PageWrapper>
</template>

<script setup lang="ts">
  import { PageWrapper } from '/@/components/Page';
  import { router } from '/@/router';
  import { BasicTable, useTable } from '/@/components/Table';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { memberColumns } from '/@/views/mineai/system/department/data';
  import { useRoute } from 'vue-router';
  import { ref } from 'vue';
  import { Card } from 'ant-design-vue';

  const route = useRoute();
  const departId = route.params.id;

  let deploymentId = ref(0);
  let deploymentName = ref('');
  let userCount = ref(0);
  let memoryLimit = ref(0);
  let cpuLimit = ref(0);
  const gpuMemoryLimit = ref(0);

  const [registerTable] = useTable({
    api: async (params) => {
      const v = await maHttp.get(
        {
          url: 'departments/' + departId + '/submember-info',
          headers: {},
          params,
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
      );
      console.log(v);
      // 完成前端 GET 方法后，将 Spring Page 的字段转换为 VBen 所需字段
      deploymentId.value = v.id;
      deploymentName.value = v.name;
      userCount.value = v.userCount;
      memoryLimit.value = v.memoryLimit;
      cpuLimit.value = v.cpuLimit;
      gpuMemoryLimit.value = v.gpuMemoryLimit;
      v.items = v.users;
      return v;
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
        type: 0,
      });
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序，根据id降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    columns: memberColumns,
    bordered: true,
    showIndexColumn: false,
    handleSearchInfoFn(info) {
      return info;
    },
  });

  function goBack() {
    router.go(-1);
  }
</script>
<style scoped>
  .card >>> .ant-card-body {
    padding: 18px !important;
  }
</style>
