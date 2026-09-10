<template>
  <a-table
    size="small"
    row-key="id"
    :columns="columns"
    :pagination="false"
    :loading="loading"
    :data-source="data"
    :showIndexColumn="true"
  >
    <template #action="{ record }">
      <TableAction
        :actions="[
          {
            icon: 'ant-design:down-square-outlined',
            tooltip: '详情和日志',
            color: 'success',
            onClick: goTrainJobDetails.bind(null, record),
            ifShow: record.jobType !== 3,
          },
          {
            icon: 'ant-design:file-done-outlined',
            tooltip: '模型验证',
            color: 'success',
            onClick: goModalValidate.bind(null, record),
            ifShow: record.jobType !== 3 && trainModelVersionImage.length > 0,
          },
          {
            icon: 'ant-design:retweet-outlined',
            tooltip: '模型转换',
            color: 'success',
            onClick: goModelConvert.bind(null, record),
            ifShow: record.jobType !== 3 && convertModelVersionImage.length > 0,
          },
          {
            icon: 'ant-design:send-outlined',
            tooltip: '申请发布',
            color: 'success',
            //onClick: handleRelease.bind(null, record),
            onClick: goModelRelease.bind(null, record),
            ifShow: record.status === 1 && record.jobType !== 3,
          },
          {
            icon: 'ant-design:line-outlined',
            color: 'warning',
            ifShow: record.jobType === 3,
          },
        ]"
      />
    </template>
  </a-table>
  <ConvertModal @register="register" @success="handleModalConvert" />
  <ConvertHistoryModal @register="registerCH" />
  <ReleaseModel @register="register" @success="handleModalRelease" />
</template>

<script lang="ts" setup>
  import { TableAction } from '/@/components/Table';
  import { Table as ATable } from 'ant-design-vue';
  import { defineEmits, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { maHttp } from '/@/utils/http/axios';
  import { useGo } from '/@/hooks/web/usePage';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ConvertModal from './Modal/ConvertModal.vue';
  import ConvertHistoryModal from './Modal/ConvertHistoryModal.vue';
  import { useModal } from '/@/components/Modal';
  import ReleaseModel from '/@/views/mineai/refPage/modelGeneration/Modal/ReleaseModel.vue';

  const emits = defineEmits(['success']);
  const { createMessage } = useMessage();
  const userStore = useUserStore();
  const router = useRouter();
  const go = useGo();
  const userData = userStore.getUserInfo;
  const props = defineProps<{
    id: number;
    columns: Array<Object>;
    url: string;
    urlPrefix: string;
    trainModelVersionImage: string | '';
    convertModelVersionImage: string | '';
  }>();

  const data = ref([]);
  const loading = ref(true);
  const convertMap = ref<Map<string, object[]>>(new Map());

  const [register, { openModal }] = useModal();
  const [registerCH, { openModal: openCHModel }] = useModal();

  function goTrainJobDetails(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.id}_${record.jobType}`);
  }

  function handleSuccess() {
    createMessage.success('发布已申请');
  }

  //模型验证
  async function goModalValidate(record: Recordable) {
    await router.push({
      path: `/maTrainingCenter/modelTest`,
    });
  }

  //发布模型
  /*async function handleRelease(record: Recordable) {
await maHttp
.get(
{
url: 'modelGeneration/applyGenerationRelease',
params: {
modelGenerationId: props.id,
userId: userData.id,
bestWeightPath: record.weightPath,
modelJobId: record.id,
},
headers: {
// @ts-ignore
ignoreCancelToken: true,
},
},
{ urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
)
.then(() => {
emits('success');
handleSuccess();
});
}*/

  //模型转换
  async function goModelConvert() {
    await router.push({
      path: `/maTrainingCenter/modelConvert`,
    });
  }

  //申请发布
  function goModelRelease(record: Recordable) {
    openModal(true, null);
  }

  async function handleModalConvert() {
    await getSubData();
  }

  async function handleModalRelease() {
    createMessage.success('任务发布成功');
    //await getSubData();
  }

  function handleConvertHistory(record: Recordable) {
    openCHModel(true, { history: convertMap.value.get(record.name) });
  }

  async function getSubData() {
    // 假设这是你要插入的对象
    const mockResponse = [
      {
        id: 676,
        name: 'job-cloud-merge-675-convert-2024-7-1-14-58-59',
        modelVersion: null,
        jobType: 3,
        controller: null,
        status: 1,
        modelGenerationId: 173,
        description: null,
        params: {
          HP_BATCH_SIZE: '10',
          HP_CONFIDENCE: '0.85',
          HP_EPOCHES: '5',
          HP_WEIGHT_DECAY: '0.0005',
          HP_MOMENTUN: '0.937',
          HP_LEARNING_RATE: '0.01',
          MODEL_WORKING_MODE: '1',
        },
        weightPath: 'job-cloud-merge-675-convert-2024-7-1-14-58-59',
        memory: '10Gi',
        cpus: '4',
        gpus: '2',
        logFilePath: null,
        createTime: '2024-07-01 14:55:36',
        lastJobTime: '2024-07-01 14:56:30',
        accuracy: null,
      },
      {
        id: 675,
        name: 'job-cloud-merge-675',
        modelVersion: null,
        jobType: 1,
        controller: null,
        status: 1,
        modelGenerationId: 173,
        description: null,
        params: {
          HP_BATCH_SIZE: '10',
          HP_CONFIDENCE: '0.85',
          HP_EPOCHES: '5',
          HP_WEIGHT_DECAY: '0.0005',
          HP_MOMENTUN: '0.937',
          HP_LEARNING_RATE: '0.01',
          MODEL_WORKING_MODE: '1',
        },
        weightPath: 'job-cloud-merge-675',
        memory: '10Gi',
        cpus: '4',
        gpus: '2',
        logFilePath: null,
        createTime: '2024-07-01 14:52:53',
        lastJobTime: '2024-07-01 14:54:00',
        accuracy: null,
      },
    ];

    // 直接将假数据赋值给 data.value
    data.value = mockResponse;

    let filterdData = [];

    // 保持后面的处理逻辑不变
    data.value.forEach((item) => {
      if (item.jobType === 3) {
        const end = item.name.indexOf('-convert-');
        if (end > 0) {
          const key = item.name.substring(0, end);
          if (convertMap.value.has(key)) {
            let values = convertMap.value.get(key);
            if (!values || values.length <= 0) values = [item];
            else values.push(item);
            convertMap.value.set(key, values);
          } else {
            const values = [item];
            convertMap.value.set(key, values);
          }
        }
      } else {
        filterdData.push(item);
      }
    });

    filterdData.forEach((item) => {
      if (convertMap.value.has(item.name)) {
        item.convertHistory = convertMap.value.get(item.name)
          ? convertMap.value.get(item.name)?.length
          : 0;
      } else {
        item.convertHistory = 0;
      }
    });

    data.value = filterdData;
    loading.value = false;
  }

  // async function getSubData() {
  //   await maHttp
  //     .post(
  //       {
  //         url: props.url + props.id.toString(),
  //         headers: {
  //           // @ts-ignore
  //           ignoreCancelToken: true,
  //         },
  //       },
  //       { urlPrefix: props.urlPrefix },
  //     )
  //     .then((resp) => {
  //       data.value = resp;
  //       let filterdData = [];
  //       data.value.forEach((item) => {
  //         if (item.jobType === 3) {
  //           const end = item.name.indexOf('-convert-');
  //           if (end > 0) {
  //             const key = item.name.substring(0, end);
  //             if (convertMap.value.has(key)) {
  //               let values = convertMap.value.get(key);
  //               if (!values || values.length <= 0) values = [item];
  //               else values.push(item);
  //               convertMap.value.set(key, values);
  //             } else {
  //               const values = [item];
  //               convertMap.value.set(key, values);
  //             }
  //           }
  //         } else {
  //           filterdData.push(item);
  //         }
  //       });
  //       filterdData.forEach((item) => {
  //         if (convertMap.value.has(item.name)) {
  //           item.convertHistory = convertMap.value.get(item.name)
  //             ? convertMap.value.get(item.name)?.length
  //             : 0;
  //         } else {
  //           item.convertHistory = 0;
  //         }
  //       });
  //       data.value = filterdData;
  //       loading.value = false;
  //     });
  // }

  onMounted(async () => {
    await getSubData();
  });
</script>

<style lang="less" scoped></style>
