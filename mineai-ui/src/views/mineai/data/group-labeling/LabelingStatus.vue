<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px" :contentStyle="{ margin: '18px' }">
    <template #title>任务标注信息</template>
    <Card class="card">
      <div class="flex flex-row justify-start space-x-5 text-base">
        <div>
          数据集ID: <span style="color: #5dade2">{{ datasetId }}</span>
        </div>
        <div>
          名称: <span style="color: #5dade2">{{ datasetName }}</span>
        </div>
        <div>
          团队人数: <span style="color: #9b59b6">{{ memberNum }}</span>
        </div>
        <div>
          每人标注数量: <span style="color: #f5b956">{{ personCount }}</span>
        </div>
        <div>
          任务总量: <span style="color: #f5b956">{{ taskImageCount }}</span>
        </div>
        <div>
          总体任务完成度:
          <span style="color: greenyellow">{{ progress + '%' }}</span>
          (已标注: <span style="color: greenyellow">{{ finishedCount }}</span
          >)
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
  import { workerColumns_l } from '/@/views/mineai/data/group-labeling/data';
  import { useRoute } from 'vue-router';
  import { ref } from 'vue';
  import { Card } from 'ant-design-vue';

  const route = useRoute();
  const taskId = route.params.id;

  let datasetId = ref(0);
  let datasetName = ref('');
  let memberNum = ref(0);
  let personCount = ref(0);
  let imageCount = ref(0);
  let finishedCount = ref(0);
  let taskImageCount = ref(0);
  let progress = ref(0);

  const [registerTable] = useTable({
    api: async () => {
      const v = await maHttp.get(
        {
          url: 'datasets/team/' + taskId + '/subtask/info',
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      // 完成前端 GET 方法后，将 Spring Page 的字段转换为 VBen 所需字段
      datasetId.value = v.datasetId;
      datasetName.value = v.datasetName;
      memberNum.value = v.memberNum;
      personCount.value = v.personCount;
      imageCount.value = v.imageCount;
      // 实际被分配的图片数量
      taskImageCount.value = v.taskImageCount;
      finishedCount.value = v.finishedCount;
      progress.value = v.progress;
      v.items = v.subtaskVOList;
      for (let i = 0; i < v.items.length; i++) {
        // 确保所有的偏移量都是数字
        v.items[i].currentOffset = Number(v.items[i].currentOffset) || 0;
        v.items[i].startOffset = Number(v.items[i].startOffset) || 0;
        v.items[i].endOffset = Number(v.items[i].endOffset) || 0;

        // 避免除以零的情况
        const range = v.items[i].endOffset - v.items[i].startOffset + 1;
        const validRange = range !== 0 ? range : 1; // 如果范围是0，则设置为1以避免除以零

        // 计算 process 字段
        v.items[i].process = `${v.items[i].currentOffset}/${range}`;

        // 计算 present 字段，并确保不会产生 NaN
        if (validRange > 0) {
          v.items[i].present = ((v.items[i].currentOffset / validRange) * 100).toFixed(2) + '%';
        } else {
          // 当范围无效时提供一个默认值或适当的信息
          v.items[i].present = 'N/A'; // 或者你可以选择其他合适的默认值
        }
      }
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
    columns: workerColumns_l,
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
