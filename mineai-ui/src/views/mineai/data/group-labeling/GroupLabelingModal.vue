<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm">
      <template #otherAction>
        <a-button type="link" @click="handleCustomProportion">自定义分配比例(默认均分)</a-button>
        <a-button type="link" @click="handleAddOrPreviewLabel">查看/添加标签</a-button>
      </template>
    </BasicForm>
  </BasicModal>
  <VisualProportionModal
    v-model:visible="proportionModalVisible"
    :members="teamMembers"
    :totalImages="totalImages"
    :annotatedCount="annotatedCount"
    :disabled="isProportionDisabled"
    :disabledReason="disabledReason"
    @submit="handleProportionSubmit"
  />
  <LabelManagementModal
    v-model:visible="labelModalVisible"
    :datasetId="currentDatasetId"
    :isGuided="currentDatasetIsGuided"
    @submitted="handleLabelsSubmitted"
  />
</template>
<script setup lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { computed, ref, toRaw, unref } from 'vue';
  import { formSchema } from './data';
  import {
    addNewTask,
    getDatasetImageCount,
    getTeamInfo,
    isDatasetIsGuided,
    validateDatasetPicNum,
    getFirstFileId,
    getNearestUnannotatedFileInfo,
  } from '/@/views/mineai/data/dataset-details2/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button as AButton } from 'ant-design-vue';
  import VisualProportionModal from '/@/views/mineai/data/group-labeling/VisualProportionModal.vue'
  import LabelManagementModal from '/@/views/mineai/data/group-labeling/LabelManagementModal.vue';

  const emits = defineEmits(['register', 'success']);
  const isUpdate = ref(true);
  let { createMessage } = useMessage();

  // 比例相关状态
  const proportionModalVisible = ref(false);
  const teamMembers = ref<any[]>([]);
  const totalImages = ref(0);
  const annotatedCount = ref(0);
  const customProportions = ref<Array<{ userId: number; proportion: number }>>([]);
  const customStartOffset = ref<number>(0);
  const currentTeamId = ref<number | null>(null);
  const currentDatasetId = ref<number | null>(null);

  // 标签管理相关状态
  const labelModalVisible = ref(false);
  const currentDatasetIsGuided = ref(false);

  // 计算是否禁用比例分配
  const isProportionDisabled = computed(() => {
    return (
      totalImages.value > 0 &&
      teamMembers.value.length > 0 &&
      totalImages.value < teamMembers.value.length
    );
  });

  // 禁用原因提示
  const disabledReason = computed(() => {
    if (isProportionDisabled.value) {
      return `当前数据集仅有 ${totalImages.value} 张图片，少于团队成员数量 ${teamMembers.value.length} 人，无法进行自定义分配。建议选择图片数量更多的数据集或减少团队成员。`;
    }
    return '';
  });

  const [registerForm, { resetFields, setFieldsValue, validate, getFieldsValue }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;
    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
      });
    }

    // 重置状态
    teamMembers.value = [];
    totalImages.value = 0;
    annotatedCount.value = 0;
    customProportions.value = [];
    customStartOffset.value = 0;
    currentTeamId.value = null;
    currentDatasetId.value = null;
  });

  const handleCustomProportion = async () => {
    try {
      const values = getFieldsValue();
      const teamId = values.group;
      const datasetId = values.dataset;

      if (!teamId || !datasetId) {
        createMessage.warning('请先选择团队和数据集');
        return;
      }

      try {
        // 检查是否已获取过数据
        if (teamId !== currentTeamId.value || datasetId !== currentDatasetId.value) {
          // 获取团队成员
          const teamInfo = await getTeamInfo(teamId);
          teamMembers.value = teamInfo.users || [];

          // 获取图片总数
          totalImages.value = await getDatasetImageCount(datasetId);

          // 获取已标注数量
          try {
            const firstFileId = await getFirstFileId(datasetId);
            if (firstFileId) {
              const unannotatedInfo = await getNearestUnannotatedFileInfo(datasetId, firstFileId);
              // distance表示从第一张到第一张未标注的距离，即已标注的数量
              // 如果distance为-1说明全部已标注，annotatedCount等于totalImages
              const distance = unannotatedInfo?.distance ?? 0;
              annotatedCount.value = distance === -1 ? totalImages.value : distance;
            } else {
              annotatedCount.value = 0;
            }
          } catch (e) {
            console.warn('获取已标注信息失败，默认为0:', e);
            annotatedCount.value = 0;
          }

          // 更新当前ID
          currentTeamId.value = teamId;
          currentDatasetId.value = datasetId;
        }

        // 检查图片数量是否足够
        if (totalImages.value < teamMembers.value.length) {
          createMessage.error(
            `数据集图片数量（${totalImages.value}张）少于团队成员数量（${teamMembers.value.length}人），无法进行自定义分配比例。请选择图片数量更多的数据集或减少团队成员。`,
            5,
          );
          return;
        }

        // 打开比例分配模态框
        proportionModalVisible.value = true;
      } finally {
      }
    } catch (error) {
      console.error('获取团队或数据集信息失败:', error);
      createMessage.error('获取团队或数据集信息失败');
    }
  };

  // 处理比例提交
  const handleProportionSubmit = (data: {
    proportions: Array<{ userId: number; proportion: number }>;
    startOffset: number;
  }) => {
    // 再次检查（防止异步操作导致的状态不一致）
    if (isProportionDisabled.value) {
      createMessage.error('当前图片数量不足，无法保存分配比例');
      return;
    }

    customProportions.value = data.proportions;
    customStartOffset.value = data.startOffset;
    createMessage.success('分配比例已保存');

    const summary = data.proportions
      .map((p) => {
        const member = teamMembers.value.find((m) => m.id === p.userId);
        return `${member?.nickName || '未知'}: ${p.proportion}%`;
      })
      .join('，');

    console.log('分配详情:', summary, '起始偏移:', data.startOffset);
  };

  const handleAddOrPreviewLabel = async () => {
    const values = getFieldsValue();
    const datasetId = values.dataset;
    currentDatasetId.value = datasetId;

    if (!datasetId) {
      createMessage.warning('请先选择数据集');
      return;
    }

    try {
      // 检查是否引导式数据集
      currentDatasetIsGuided.value = await isDatasetIsGuided(datasetId);
      labelModalVisible.value = true;
    } catch (e) {
      console.error(e);
      createMessage.error('获取数据集信息失败');
    }
  };

  // 处理标签提交成功
  const handleLabelsSubmitted = () => {
    // 可以在这里执行刷新标签列表等操作
    createMessage.success('标签更新成功');
  };

  const getTitle = computed(() => (!unref(isUpdate) ? '新增任务' : '编辑任务'));

  async function handleSubmit() {
    try {
      const value = await validate();

      // 提交前再次验证
      if (!(await validateDatasetPicNum(value.dataset, value.group))) {
        return createMessage.error('数据集图片数量小于已选团队数量, 无法发布任务, 请重新选择', 5);
      }

      const data = {
        name: value.name,
        datasetId: value.dataset,
        teamId: value.group,
        // 添加自定义比例参数
        proportions: toRaw(unref(customProportions.value)),
        // 添加起始偏移量
        startOffset: customStartOffset.value,
      };

      setModalProps({ confirmLoading: true });
      await addNewTask(data);
      closeModal();
      emits('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less" scoped>
  .ant-select-dropdown {
    background-color: #0a6cd5;
    overflow: auto !important;
  }
</style>
