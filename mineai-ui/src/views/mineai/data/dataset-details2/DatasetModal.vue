<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    :title="getTitle"
    width="500px"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" class="myForm">
      <template #add="{ model, field }">
        <div class="flex flex-row justify-start">
          <Select
            style="width: 90%; margin-right: 8px"
            placeholder="请选择"
            v-model:value="model[field]"
            :options="selectOptions"
            :dropdownAlign="{
              overflow: {
                adjustY: false, // 关闭下拉框垂直位置自适应
              },
            }"
            :disabled="isUpdate"
            @dropdownVisibleChange="handleFetch"
          >
            <template #suffixIcon v-if="loading">
              <LoadingOutlined spin />
            </template>
            <template #notFoundContent v-if="loading">
              <span>
                <LoadingOutlined spin class="mr-1" />
                请等待数据加载完成...
              </span>
            </template>
          </Select>
          <Button @click="addLabel" type="primary" :disabled="isUpdate">
            <template #icon>
              <PlusOutlined />
            </template>
          </Button>
        </div>
      </template>
    </BasicForm>
  </BasicModal>
  <LabelGroupModal @register="registerLabelModal" @add-label-group="handleAfterAdd" />
</template>
<script lang="ts" setup>
  import { computed, defineEmits, onMounted, ref, unref } from 'vue';
  import { Button, Select } from 'ant-design-vue';
  import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './dataset.data';
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import LabelGroupModal from '/@/views/mineai/data/label-details/LabelGroupModal.vue';

  const isUpdate = ref(true);
  const datasetGroupId = ref<number | null>(null); // 添加数据集组ID状态
  const emits = defineEmits(['success']);

  const selectOptions = ref<{ value: string; label: string; disabled?: boolean }[]>([]);
  const loading = ref<boolean>(false);
  const handleFetch = async (open) => {
    if (open) {
      loading.value = true;
      await maHttp
        .get(
          {
            url: 'labelGroup/getList',
            params: {
              type: 0,
              dataType: 0,
              annotateType: 101,
            },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        )
        .then((v) => {
          selectOptions.value = v.map((item) => ({ value: String(item.id), label: item.name }));
        });
      loading.value = false;
    }
  };

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 90,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  const [registerLabelModal, { openModal: openLabelModal }] = useModal();

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;

    // 接收数据集组ID参数
    datasetGroupId.value = data?.datasetGroupId || null;

    if (unref(isUpdate)) {
      console.log(data.record);
      await setFieldsValue({
        ...data.record,
      });
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增数据集' : '编辑数据集'));

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      // 判断是否是编辑操作
      const isEdit = unref(isUpdate);

      // 编辑操作时不添加import字段
      if (isEdit) {
        values.import = null;
      } else {
        // 新增数据集时，如果有数据集组ID，则添加到参数中
        if (datasetGroupId.value) {
          values.datasetGroupId = datasetGroupId.value;
        }
        // 数据集组中创建数据集不需要创建数据集组
        values.isCreateDatasetGroup = false;
      }

      let apiCall;
      if (isEdit) {
        apiCall = maHttp.put(
          {
            url: `datasets/${values.id}`,
            params: values,
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      } else if (values.dataTypeTest === 5) {
        apiCall = maHttp.post(
          {
            url: 'pointcloud/datasets',
            params: {
              name: values.name,
              labelGroupId: values.labelGroupId ? Number(values.labelGroupId) : null,
              datasetGroupId: Number(values.datasetGroupId),
              remark: values.remark,
            },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      } else if (values.dataTypeTest === 6) {
        apiCall = maHttp.post(
          {
            url: 'video/datasets',
            params: {
              name: values.name,
              labelGroupId: values.labelGroupId ? Number(values.labelGroupId) : null,
              datasetGroupId: Number(values.datasetGroupId),
              remark: values.remark,
            },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      } else if (values.dataTypeTest === 7) {
        apiCall = maHttp.post(
          {
            url: 'multi/datasets',
            params: {
              name: values.name,
              labelGroupId: values.labelGroupId ? Number(values.labelGroupId) : null,
              datasetGroupId: Number(values.datasetGroupId),
              remark: values.remark,
            },
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      } else {
        apiCall = maHttp.post(
          {
            url: 'datasets',
            params: values,
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      }
      // 执行API调用
      await apiCall;
      closeModal();
      // 传递isEdit标志来区分是编辑还是新增
      emits('success', isEdit);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  function addLabel() {
    openLabelModal(true, {
      isUpdate: false,
    });
  }

  //设置标签组为新增标签组
  async function handleAfterAdd(labelGroupId) {
    await handleFetch(true);
    await setFieldsValue({ labelGroupId: String(labelGroupId) });
  }

  onMounted(async () => {
    await handleFetch(true);
  });
</script>
<style scoped>
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }
</style>
