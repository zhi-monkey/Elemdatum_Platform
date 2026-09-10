<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm">
      <template #deviceFirmWare="{ model, field }">
        <a-tree-select
          v-model:value="model[field]"
          style="width: 100%"
          :tree-data="treeData"
          placeholder="请选择"
        />
      </template>
    </BasicForm>
  </BasicModal>
</template>

<script setup lang="ts">
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { computed, onMounted, ref, unref, defineEmits } from 'vue';
  import { formSchema } from './model.data';
  import { TreeSelect as ATreeSelect } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useUserStore } from '/@/store/modules/user';
  import {
    addAndUpdateModelApplication,
    getModelById,
    getSceneListByIds,
  } from '/@/views/mineai/application/taskManagement/api/api';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();
  const userStore = useUserStore();
  const userData = userStore.getUserInfo;

  const emit = defineEmits(['form-submit']);

  // 定义接口
  interface DeviceFirmware {
    id: number;
    deviceName: string;
    firmwareVersion: string;
    chip?: {
      id: number;
      chipType: string;
    };
  }

  interface TreeDataItem {
    value: string;
    label: string;
    key?: string;
    title?: string;
    slots?: Record<string, string>;
    children?: TreeDataItem[];
    selectable?: boolean;
  }

  // 存储原始设备固件数据
  const deviceFirmwareList = ref<DeviceFirmware[]>([]);

  // 从API获取设备固件数据
  async function fetchDeviceFirmwareData() {
    const url = `device/findAll`;
    try {
      const devices = await maHttp.get(
        {
          url,
          headers: {
            ignoreCancelToken: 'true',
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      deviceFirmwareList.value = devices;
      return devices;
    } catch (error) {
      console.error('获取设备固件数据失败:', error);
      return [];
    }
  }

  // 将原始数据转换为树形结构
  function convertToTreeData(devices: DeviceFirmware[]): TreeDataItem[] {
    const mergedData = {};
    const noChipGroup: TreeDataItem = {
      label: '未绑定设备的芯片',
      value: 'no-chip',
      selectable: false,
      children: [],
    };

    devices.forEach((device) => {
      if (device.chip) {
        const chipType = device.chip.chipType;
        const chipId = device.chip.id.toString();

        if (!mergedData[chipId]) {
          mergedData[chipId] = {
            label: chipType,
            value: `chip-${chipId}`,
            selectable: false,
            children: [],
          };
        }

        mergedData[chipId].children!.push({
          value: device.id.toString(),
          label: `${device.deviceName} - ${device.firmwareVersion}`,
          selectable: true,
        });
      } else {
        noChipGroup.children!.push({
          value: device.id.toString(),
          label: `${device.deviceName} - ${device.firmwareVersion}`,
          selectable: true,
        });
      }
    });

    if (noChipGroup.children && noChipGroup.children.length > 0) {
      mergedData['no-chip'] = noChipGroup;
    }

    return Object.values(mergedData);
  }

  // 通过ID获取设备固件信息
  function getDeviceFirmwareById(id: string | number): DeviceFirmware | undefined {
    return deviceFirmwareList.value.find(
      (device) => device.id === (typeof id === 'string' ? parseInt(id) : id),
    );
  }

  // 初始化树形数据
  const treeData = ref<TreeDataItem[]>([]);

  const isUpdate = ref(true);
  const isSubmitting = ref(false); // 添加提交状态

  // 表单相关
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 90,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  // Modal相关
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: true });
    isUpdate.value = !!data?.isUpdate;

    if (unref(isUpdate)) {
      await setFieldsValue({
        ...data.record,
      });

      // 设置算法ID
      if (data.record.model?.id !== undefined) {
        await setFieldsValue({
          modelId: data.record.model.id,
        });
      }

      // 设置设备Id
      if (data.record.device?.id !== undefined) {
        await setFieldsValue({
          deviceFirmWare: data.record.device.id,
        });
      }

      // 处理场景数据
      if (
        data.record.applicableScene &&
        Array.isArray(data.record.applicableScene) &&
        data.record.applicableScene.length > 0
      ) {
        // 提取场景ID列表
        const sceneIds = data.record.applicableScene.map((scene) => scene.id);
        // 设置到表单
        await setFieldsValue({
          scenes: sceneIds,
        });
      }
    }

    setModalProps({ confirmLoading: false });
  });

  // 计算属性
  const getTitle = computed(() => (!unref(isUpdate) ? '新增应用任务' : '编辑应用任务'));

  onMounted(async () => {
    // 获取数据
    const devices = await fetchDeviceFirmwareData();
    // 转换为树形结构
    treeData.value = convertToTreeData(devices);
  });

  // 提交表单方法
  const handleSubmit = async () => {
    // 防止重复提交
    if (isSubmitting.value) {
      return;
    }

    try {
      isSubmitting.value = true;
      setModalProps({ confirmLoading: true });
      
      const validatedData = await validate();

      // 设备信息判断和赋值
      if (validatedData.deviceFirmWare) {
        const selectedDeviceId = validatedData.deviceFirmWare;
        const deviceInfo = getDeviceFirmwareById(selectedDeviceId);
        if (deviceInfo) {
          validatedData.device = deviceInfo;
        }
      }

      // 场景信息判断和赋值
      if (validatedData.scenes && validatedData.scenes.length > 0) {
        const sceneList = await getSceneListByIds(validatedData.scenes);
        if (sceneList) {
          validatedData.applicableScene = sceneList;
        }
      }

      // 模型信息判断和赋值
      if (validatedData.modelId) {
        const modelInfo = await getModelById(validatedData.modelId);
        if (modelInfo) {
          validatedData.model = modelInfo;
        }
      }

      // 填入发布者信息
      validatedData.publisherId = (userData as any)?.id;

      // 表单提交
      await addAndUpdateModelApplication(validatedData);
      createMessage.success(isUpdate.value ? '更新成功！' : '新增成功！');
      emit('form-submit');
      closeModal();
    } catch (error) {
      console.error('表单验证失败', error);
      const errorMessage = error instanceof Error ? error.message : '未知错误';
      createMessage.error(`操作失败: ${errorMessage}`);
    } finally {
      isSubmitting.value = false;
      setModalProps({ confirmLoading: false });
    }
  };
</script>

<style scoped lang="less"></style>
