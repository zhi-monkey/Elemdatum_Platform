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
            icon: 'ant-design:interaction-outlined',
            tooltip: '转换记录',
            color: 'success',
            onClick: handleConvertHistory.bind(null, record),
            ifShow: record.jobType !== 3 && record.convertHistory > 0,
          },
          {
            icon: 'ant-design:send-outlined',
            tooltip: '模型发布',
            color: 'success',
            onClick: handleModelRelease.bind(null, record),
            ifShow:
              (record.status === 2 || record.status === 4 || record.status === 6) &&
              record.jobType !== 3,
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
  <ModelChip @register="registerModelChip" />
</template>

<script lang="ts" setup>
  import { TableAction } from '/@/components/Table';
  import { Table as ATable } from 'ant-design-vue';
  import { defineEmits, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { maHttp } from '/@/utils/http/axios';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ConvertModal from './Modal/ConvertModal.vue';
  import ConvertHistoryModal from './Modal/ConvertHistoryModal.vue';
  import { useModal } from '/@/components/Modal';
  import ModelChip from '/@/views/mineai/application/basicAlgorithm/component/ModelChip.vue';

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
  const [registerModelChip, { openModal: openModelChip }] = useModal();

  /**
   * 获取训练任务细节
   *
   * @param record
   */
  function goTrainJobDetails(record: Recordable) {
    go(`/maTrainingCenter/trainJobDetails/${record.id}_${record.jobType}`);
  }

  /**
   * 模型发布成功！！
   */
  function handleSuccess() {
    createMessage.success('发布成功！');
  }

  /**
   * 模型验证
   *
   * @param record
   */
  async function goModalValidate(record: Recordable) {
    await router.push({
      path: `/maTrainingCenter/modelValidate/${record.id}`,
      state: {
        id: record.id,
        image: props.trainModelVersionImage,
        weightPath: record.name,
        gpuNum: record.gpus,
        params: {
          MODE_WORKING_MODE: 4,
        },
      },
    });
  }

  /**
   * 发布模型
   *
   * @param record
   */
  function handleModelRelease(record: Recordable) {
    openModelChip(true, {
      modelGenerationId: props.id,
      bestWeightPath: record.weightPath,
      modelJobId: record.id,
    });
  }

  /**
   * 模型转换
   *
   * @param record
   */
  function goModelConvert(record: Recordable) {
    openModal(true, {
      modelGenerationId: props.id,
      image: props.convertModelVersionImage,
      weightPath: record.name,
      gpuNum: record.gpus,
    });
  }

  async function handleModalConvert() {
    await getSubData();
  }

  function handleConvertHistory(record: Recordable) {
    openCHModel(true, { history: convertMap.value.get(record.name) });
  }

  async function getSubData() {
    await maHttp
      .post(
        {
          url: props.url + props.id.toString(),
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: props.urlPrefix },
      )
      .then((resp) => {
        data.value = resp;
        let filterdData = [];
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
      });
  }

  onMounted(async () => {
    await getSubData();
  });
</script>
