<template>
  <div>
    <Table
      size="small"
      :columns="ModelVersionColumns"
      :data-source="data"
      :loading="loading"
      :pagination="{
        pageSize: 5,
      }"
      :canResize="false"
      :rowKey="(record) => record.id"
    >
      >
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:code-outlined',
              tooltip: '新建训练',
              onClick: CreateTrainJob.bind(null, record),
              ifShow: () => {
                return record.trainable;
              },
            },
            {
              icon: 'ant-design:code-filled',
              tooltip: '新建质检',
              onClick: CreateInspectJob.bind(null, record),
              ifShow: () => {
                return record.inspectable;
              },
            },
            {
              icon: 'ant-design:sync-outlined',
              tooltip: '镜像更新',
              onClick: updateImage.bind(null, record),
              ifShow: () => {
                return record.inferable;
              },
            },
            {
              icon: 'ant-design:swap-outlined',
              tooltip: '模型转换',
              onClick: JobConvert.bind(null, record),
            },
            {
              icon: 'ant-design:database-outlined',
              tooltip: '部署结果',
              onClick: ModelDeployment.bind(null, record),
              ifShow: () => {
                return record.inferable;
              },
            },
            {
              icon: 'ant-design:cloud-server-outlined',
              tooltip: '算法部署',
              onClick: DeployMent.bind(null, record),
              ifShow: () => {
                return record.inferable;
              },
            },
            {
              icon: 'ant-design:close-square-outlined',
              tooltip: '取消部署',
              onClick: CancelDeploy.bind(null, record),
              ifShow: () => {
                return record.hasDeployment;
              },
            },
            record.canDelete === true
              ? {
                  icon: 'ant-design:delete-outlined',
                  tooltip: '删除',
                  color: 'error',
                  popConfirm: {
                    title: '是否确认删除',
                    confirm: handleDelete.bind(null, record),
                  },
                }
              : {
                  icon: 'ant-design:delete-outlined',
                  tooltip: '删除',
                  color: 'error',
                  disabled: true,
                  popConfirm: {
                    title: '是否确认删除',
                    confirm: handleDelete.bind(null, record),
                  },
                },
          ]"
        />
      </template>
    </Table>
    <DeployMentModel @register="registerModal" />
    <ModelConvert @register="registerModal1" />
    <CancelDeployModel @register="registerModal2" />
    <CreateJobModel @register="registerModal3" />
  </div>
</template>

<script lang="ts" setup>
  import { TableAction } from '/@/components/Table';
  import { ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { Table } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { router } from '/@/router';
  import { ModelVersionColumns } from './SubTable.data';
  import { useModal } from '/@/components/Modal';
  import DeployMentModel from '/@/views/mineai/monitor/model-list/DeployMentModel.vue';
  import ModelConvert from '/@/views/mineai/monitor/model-list/ModelConvert/ModelConvert.vue';
  import CancelDeployModel from '/@/views/mineai/monitor/model-list/CancelDeployModel.vue';
  import CreateJobModel from '/@/views/mineai/monitor/model-list/CreateJob/CreateJobModel.vue';

  const { createMessage } = useMessage();
  const props = defineProps<{
    modelId: Number;
  }>();

  let data = ref([]);
  let loading = ref(true);

  const [registerModal, { openModal }] = useModal();
  const [registerModal1, { openModal: openModal1 }] = useModal();
  const [registerModal2, { openModal: openModal2 }] = useModal();
  const [registerModal3, { openModal: openModal3 }] = useModal();

  function DeployMent(record: Recordable) {
    openModal(true, {
      record,
    });
  }

  function CancelDeploy(record: Recordable) {
    openModal2(true, { record: record.id });
  }

  function JobConvert(record: Recordable) {
    openModal1(true, {
      record,
    });
  }

  function CreateTrainJob(record: Recordable) {
    openModal3(true, {
      modelId: record.model.id,
      modelVersionId: record.id,
      modelName: record.model.modelName,
      modelVersionName: record.name,
      modelVersionShowName: record.showName,
      useGpu: record.useGpu,
      jobType: 1,
    });
  }

  function CreateInspectJob(record: Recordable) {
    openModal3(true, {
      modelId: record.model.id,
      modelVersionId: record.id,
      modelName: record.model.modelName,
      modelVersionName: record.name,
      modelVersionShowName: record.showName,
      useGpu: record.useGpu,
      jobType: 2,
    });
  }

  //算法部署
  function ModelDeployment(record: Recordable) {
    router.push({
      path: '/maTrainingCenter/modelDeployment',
      query: {
        modelName: record.model.modelName,
        modelVersion: record.name,
        modelVersionId: record.id,
        modelVersionShowName: record.showName,
        datasetType: record.model.datasetType,
        architecture: record.architecture,
        createTime: record.createTime,
      },
    });
    // go(`/maTrainingCenter/modelDeployment/${record.id}`);
  }

  /**
   * 更新镜像
   * */
  function updateImage(record: Recordable) {
    router.push({
      path: '/maTrainingCenter/updateImage', //页面路径
      //这里写参数，可以多个参数
      query: {
        modelId: record.model.id,
        modelName: record.model.modelName,
        modelEnglishName: record.model.modelEnglishName,
        tag: record.showName,
      },
    });
  }

  function handleDelete(record: Recordable) {
    maHttp
      .post(
        {
          url: 'modelVersion/deleteModel',
          params: record,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('删除成功！');
        //重新加载表单列表
        maHttp
          .get(
            {
              url: 'modelVersion/dynamicFindModelVersionPage',
              params: { id: props.modelId, page: 0, pageSize: 10 },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((resp) => {
            data.value = resp;
            loading.value = false;
          });
      });
  }

  //表单列表
  maHttp
    .get(
      {
        url: 'modelVersion/dynamicFindModelVersionPage',
        params: { id: props.modelId, page: 0, pageSize: 10 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((resp) => {
      data.value = resp;
      loading.value = false;
    });
</script>

<style scoped></style>
