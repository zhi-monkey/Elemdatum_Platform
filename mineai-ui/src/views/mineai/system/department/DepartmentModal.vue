<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="getTitle"
    @ok="handleSubmit"
    :minHeight="400"
    :minWidth="600"
  >
    <BasicForm @register="registerForm" />

    <!-- 选择成员区域 -->
    <div class="member-selection-header">
      <a class="select-member-link" @click="handleCreate">部门成员</a>
    </div>
    <div v-if="selectedMembers.length > 0" class="selected-members">
      <div class="members-tags">
        <span v-for="member in selectedMembers" :key="member.id" class="member-tag">
          {{ member.username }}
        </span>
      </div>
    </div>
    <div v-else class="no-members">
      <span class="no-members-text">暂未选择成员</span>
    </div>

    <MemberModal @register="registerMemberModal" @success="handleSuccess" />

    <!-- 自定义资源配置输入框 -->
    <div v-if="showCustomInputs" class="custom-resource-inputs">
      <Card
        size="small"
        title="自定义资源配置"
        :headStyle="{ fontSize: '14px', fontWeight: '500' }"
      >
        <div class="custom-inputs-content">
          <div class="input-item">
            <span class="input-label">内存限制</span>
            <div class="input-with-unit">
              <div class="input-number-wrapper">
                <InputNumber
                  v-model:value="customResourceData.memoryLimit"
                  :min="0"
                  placeholder="请输入"
                  @change="handleCustomResourceChange"
                  @focus="(e) => e.target.select()"
                  @click="(e) => e.target.select()"
                />
              </div>
              <span class="input-unit">GB</span>
            </div>
          </div>
          <div class="input-item">
            <span class="input-label">CPU限制</span>
            <div class="input-with-unit">
              <div class="input-number-wrapper">
                <InputNumber
                  v-model:value="customResourceData.cpuLimit"
                  :min="0"
                  placeholder="请输入"
                  @change="handleCustomResourceChange"
                  @focus="(e) => e.target.select()"
                  @click="(e) => e.target.select()"
                />
              </div>
              <span class="input-unit">核</span>
            </div>
          </div>
          <div class="input-item">
            <span class="input-label">GPU显存限制</span>
            <div class="input-with-unit">
              <div class="input-number-wrapper">
                <InputNumber
                  v-model:value="customResourceData.gpuMemoryLimit"
                  :min="0"
                  placeholder="请输入"
                  @change="handleCustomResourceChange"
                  @focus="(e) => e.target.select()"
                  @click="(e) => e.target.select()"
                />
              </div>
              <span class="input-unit">GB</span>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <!-- 资源参数显示卡片 - 仅在预设配置时显示 -->
    <div
      v-if="showResourceParams && currentResourceLevel !== 'custom'"
      class="resource-params-card"
    >
      <Card
        size="small"
        :title="getResourceCardTitle()"
        :headStyle="{ fontSize: '14px', fontWeight: '500' }"
      >
        <div class="resource-params-content">
          <div class="param-item">
            <span class="param-label">内存限制</span>
            <span class="param-value">{{ getCurrentResourceParams().memoryLimit }}GB</span>
          </div>
          <div class="param-item">
            <span class="param-label">CPU限制</span>
            <span class="param-value">{{ getCurrentResourceParams().cpuLimit }}核</span>
          </div>
          <div class="param-item">
            <span class="param-label">GPU显存限制</span>
            <span class="param-value">{{ getCurrentResourceParams().gpuMemoryLimit }}GB</span>
          </div>
        </div>
      </Card>
    </div>

    <!-- 训练规模卡片 - 仅在预设配置时显示 -->
    <div
      v-if="showTrainingScaleCard && currentResourceLevel !== 'custom'"
      class="training-scale-card"
    >
      <Card size="small" :title="'配置描述'" :headStyle="{ fontSize: '14px', fontWeight: '500' }">
        <div class="training-scale-content">
          {{ getTrainingScaleDescription() }}
        </div>
      </Card>
    </div>
  </BasicModal>
</template>

<script setup lang="ts">
  import { BasicForm, useForm } from '/@/components/Form';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { computed, ref, unref } from 'vue';
  import { formSchema } from './data';
  import {
    isDepartmentNameExists,
    saveOrUpdateDepartment,
  } from '/@/views/mineai/system/api/department';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button, Card, InputNumber } from 'ant-design-vue';
  import MemberModal from './MemberModal.vue';

  // 导入资源配置
  import {
    getResourceConfig,
    getTrainingDescription,
    isPresetConfig,
    type ResourceLevel,
  } from '/@/enums/resourceConfig';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const { createMessage } = useMessage();

  const emits = defineEmits(['register', 'success']);

  const isUpdate = ref(true);
  const departmentId = ref(null);
  const showCustomInputs = ref(false);
  const showTrainingScaleCard = ref(false);
  const showResourceParams = ref(false);
  const currentResourceLevel = ref('basic');

  const [registerMemberModal, { openModal }] = useModal();
  const selectedMembers = ref<
    Array<{ id: string | number; username: string; email?: string; phone?: string }>
  >([]);

  const handleCreate = () => {
    openModal(true, {
      isUpdate: unref(isUpdate),
      departmentId: unref(departmentId),
      selectedUserIds: selectedMembers.value.map((member) => member.id),
    });
  };

  const handleSuccess = (
    selectedUsers: Array<{ id: string | number; username: string; email?: string; phone?: string }>,
  ) => {
    selectedMembers.value = selectedUsers;
  };

  const customResourceData = ref({
    memoryLimit: 0,
    cpuLimit: 0,
    gpuMemoryLimit: 0,
  });

  // 获取训练规模描述
  const getTrainingScaleDescription = () => {
    return getTrainingDescription(currentResourceLevel.value);
  };

  // 获取资源卡片标题
  const getResourceCardTitle = () => {
    return '配置对应参数';
  };

  // 获取当前资源参数
  const getCurrentResourceParams = () => {
    return getResourceConfig(currentResourceLevel.value);
  };

  // 处理自定义资源变化
  const handleCustomResourceChange = () => {
    // 自定义配置时不显示任何额外卡片，只显示输入框
  };

  // 修改表单结构
  const modifiedFormSchema = formSchema.map((item) => {
    if (item.field === 'resourceLevel') {
      return {
        ...item,
        componentProps: {
          ...item.componentProps,
          onChange: (value) => {
            currentResourceLevel.value = value;

            // 重置所有显示状态
            showCustomInputs.value = false;
            showTrainingScaleCard.value = false;
            showResourceParams.value = false;

            if (value === 'custom') {
              // 重置自定义资源数据为0（初始值）
              customResourceData.value = {
                memoryLimit: 0,
                cpuLimit: 0,
                gpuMemoryLimit: 0,
              };
              // 仅显示自定义输入框
              showCustomInputs.value = true;
            } else if (['basic', 'medium', 'high'].includes(value)) {
              // 显示预设配置的卡片
              showTrainingScaleCard.value = true;
              showResourceParams.value = true;
            }
          },
        },
      };
    }
    return item;
  });

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 100,
    schemas: modifiedFormSchema,
    showActionButtonGroup: false,
    wrapperCol: { span: 20 },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;

    // 初始化时隐藏所有额外元素
    showCustomInputs.value = false;
    showTrainingScaleCard.value = false;
    showResourceParams.value = false;
    currentResourceLevel.value = '';

    // 重置自定义资源数据为0（初始值）
    customResourceData.value = {
      memoryLimit: 0,
      cpuLimit: 0,
      gpuMemoryLimit: 0,
    };
    // 重置选择的成员
    selectedMembers.value = [];

    if (unref(isUpdate)) {
      departmentId.value = data.record.id;

      // 使用配置文件中的工具函数判断资源等级
      let resourceLevel: ResourceLevel | 'custom' = 'custom';
      if (data.record.memoryLimit > 0) {
        resourceLevel = isPresetConfig(
          data.record.memoryLimit,
          data.record.cpuLimit,
          data.record.gpuMemoryLimit,
        );

        if (resourceLevel === 'custom') {
          customResourceData.value = {
            memoryLimit: data.record.memoryLimit,
            cpuLimit: data.record.cpuLimit,
            gpuMemoryLimit: data.record.gpuMemoryLimit,
          };
          showCustomInputs.value = true;
        }
      }

      currentResourceLevel.value = resourceLevel;

      // 只有在是基础、中等、高等配置时才显示卡片
      if (['basic', 'medium', 'high'].includes(resourceLevel)) {
        showTrainingScaleCard.value = true;
        showResourceParams.value = true;
      }

      // 直接使用返回的成员数据结构
      if (
        data.record.userIds &&
        Array.isArray(data.record.userIds) &&
        data.record.userIds.length > 0
      ) {
        try {
          setModalProps({ confirmLoading: true });

          // 调用接口获取部门成员详情
          const response = await maHttp.get(
            {
              url: `departments/${data.record.id}/department-users`,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            {
              urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN,
            },
          );

          if (response && Array.isArray(response)) {
            selectedMembers.value = response.map((user) => ({
              id: user.id,
              username: user.username,
              email: user.email,
              phone: user.phone,
            }));
          }
        } catch (error) {
          console.error('获取部门成员失败:', error);
          createMessage.error('获取部门成员信息失败');
          selectedMembers.value = [];
        } finally {
          setModalProps({ confirmLoading: false });
        }
      } else {
        // 如果没有成员，保持空数组
        selectedMembers.value = [];
      }

      await setFieldsValue({
        ...data.record,
        resourceLevel: resourceLevel,
      });
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增部门' : '编辑部门'));

  const getResourceLimits = (level: string) => {
    if (level === 'custom') {
      return {
        memoryLimit: customResourceData.value.memoryLimit,
        cpuLimit: customResourceData.value.cpuLimit,
        gpuMemoryLimit: customResourceData.value.gpuMemoryLimit,
      };
    }

    const config = getResourceConfig(level);
    return {
      memoryLimit: config.memoryLimit,
      cpuLimit: config.cpuLimit,
      gpuMemoryLimit: config.gpuMemoryLimit,
    };
  };

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });

      const values = await validate();

      // 如果是自定义配置，验证每个参数是否大于等于1
      if (values.resourceLevel === 'custom') {
        const { memoryLimit, cpuLimit, gpuMemoryLimit } = customResourceData.value;

        // 验证逻辑：检查是否为有效的正整数且大于等于1
        if (!memoryLimit || memoryLimit < 1) {
          createMessage.error('内存限制必须大于等于1GB');
          return;
        }

        if (!cpuLimit || cpuLimit < 1) {
          createMessage.error('CPU限制必须大于等于1核');
          return;
        }

        if (!gpuMemoryLimit || gpuMemoryLimit < 1) {
          createMessage.error('GPU显存限制必须大于等于1GB');
          return;
        }
      }

      if (!unref(isUpdate)) {
        const nameExists = await isDepartmentNameExists(values.name);
        if (nameExists) {
          createMessage.error('该部门名称已存在，请使用其他名称');
          return;
        }
      }

      const { memoryLimit, cpuLimit, gpuMemoryLimit } = getResourceLimits(values.resourceLevel);

      const data = {
        id: unref(isUpdate) ? values.id : undefined,
        name: values.name,
        description: values.description,
        userIds: selectedMembers.value.map((member) => member.id),
        memoryLimit,
        cpuLimit,
        gpuMemoryLimit,
      };

      console.log('提交数据:', data);

      const result = await saveOrUpdateDepartment(data);
      console.log('保存结果:', result);

      closeModal();
      createMessage.success(unref(isUpdate) ? '部门更新成功！' : '部门创建成功！');

      emits('success');
    } catch (error) {
      console.error('Error saving or updating department:', error);
      createMessage.error('操作失败，请重试');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>

<style lang="less">
  /* 注意：移除 scoped 以确保样式能够覆盖 Ant Design 组件 */
  .custom-resource-inputs {
    .input-number-wrapper {
      width: 65px;

      .ant-input-number {
        width: 65px !important;
        min-width: 65px !important;
      }

      .ant-input-number-input-wrap {
        width: 65px !important;
      }

      .ant-input-number-input {
        width: 65px !important;
        text-align: center !important;
      }

      .ant-input-number-handler-wrap {
        width: auto;
      }
    }
  }
</style>

<style scoped lang="less">
  :deep(.ant-form-item) {
    margin-bottom: 20px;
  }
  .member-selection-header {
    margin: 5px 0 12px 35px;

    .select-member-link {
      color: #1890ff;
      cursor: pointer;
      font-size: 14px;

      &:hover {
        color: #40a9ff;
      }
    }
  }

  .selected-members {
    margin-bottom: 32px;

    .members-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 8px;
      margin-left: 35px;

      .member-tag {
        display: inline-block;
        padding: 4px 8px;
        border: 1px solid;
        border-radius: 4px;
        font-size: 12px;
        line-height: 1;
        background-color: transparent;

        /* 标签颜色 - 仿照示例中的彩色标签 */
        &:nth-child(6n + 1) {
          border-color: #ff4d4f;
          color: #ff4d4f;
          background-color: rgba(255, 77, 79, 0.1);
        }
        &:nth-child(6n + 2) {
          border-color: #52c41a;
          color: #52c41a;
          background-color: rgba(82, 196, 26, 0.1);
        }
        &:nth-child(6n + 3) {
          border-color: #1890ff;
          color: #1890ff;
          background-color: rgba(24, 144, 255, 0.1);
        }
        &:nth-child(6n + 4) {
          border-color: #faad14;
          color: #faad14;
          background-color: rgba(250, 173, 20, 0.1);
        }
        &:nth-child(6n + 5) {
          border-color: #722ed1;
          color: #722ed1;
          background-color: rgba(114, 46, 209, 0.1);
        }
        &:nth-child(6n + 6) {
          border-color: #13c2c2;
          color: #13c2c2;
          background-color: rgba(19, 194, 194, 0.1);
        }
      }
    }

    .members-count {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.45);
    }
  }

  .no-members {
    margin-bottom: 32px;
    margin-left: 35px;

    .no-members-text {
      color: rgba(255, 255, 255, 0.45);
      font-size: 12px;
    }
  }

  .custom-resource-inputs {
    margin-top: 16px;
    margin-bottom: 16px;

    .custom-inputs-content {
      display: flex;
      justify-content: space-between;
      gap: 20px;

      .input-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 6px 16px;
        background: transparent;
        border-radius: 8px;
        min-width: 100px;
        flex: 1;

        .input-label {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.85);
          margin-bottom: 8px;
          font-weight: 500;
          text-align: center;
        }

        .input-with-unit {
          display: flex;
          align-items: center;
          gap: 6px;

          .input-unit {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.85);
            font-weight: 500;
            white-space: nowrap;
          }
        }
      }
    }

    @media (max-width: 768px) {
      .custom-inputs-content {
        flex-direction: column;
        gap: 16px;

        .input-item {
          width: 100%;
          min-width: auto;
        }
      }
    }
  }

  .resource-params-card {
    margin-top: 16px;
    margin-bottom: 16px;

    .resource-params-content {
      display: flex;
      justify-content: space-between;
      gap: 20px;

      .param-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 6px 16px;
        background: transparent;
        border-radius: 8px;
        min-width: 100px;

        .param-label {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.85);
          margin-bottom: 2px;
          font-weight: 500;
        }

        .param-value {
          font-size: 16px;
          font-weight: 600;
          color: rgba(255, 255, 255, 0.85);
        }
      }
    }
  }

  .training-scale-card {
    margin-top: 16px;
    margin-bottom: 20px;

    .training-scale-content {
      color: rgba(255, 255, 255, 0.85);
      font-size: 14px;
      line-height: 1.6;
      padding: 16px;
    }
  }
</style>
