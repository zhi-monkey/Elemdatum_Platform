<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    :title="getTitle"
    width="500px"
    @ok="handleSubmit"
    @cancel="clearLabelItem"
    ref="modalRef"
  >
    <div ref="contentRef" class="modal-content">
      <BasicForm @register="registerForm" class="myForm">
        <template #add="{ model, field }">
          <Input
            style="width: 70%; margin-right: 8px"
            v-model:value="model[field]"
            :readonly="oldLabelList.includes(field)"
          />
          <el-color-picker v-model="labelsColor[field]" :disabled="oldLabelList.includes(field)" />
          <Button
            v-if="field === labelItemList.at(-1)"
            @click="addLabelItem"
            shape="circle"
            size="small"
          >
            +
          </Button>
          <Button
            v-if="field !== 'label1' && !oldLabelList.includes(field)"
            @click="deleteLabelItem(field)"
            shape="circle"
            size="small"
          >
            -
          </Button>
        </template>
      </BasicForm>
    </div>
  </BasicModal>
</template>
<script lang="ts">
  import { computed, defineComponent, ref, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import {
    defaultColor,
    formSchema,
    labelIndex,
    labelItemList,
    labelsColor,
  } from './labelGroup.data';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Button, Input } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'DatasetModal',
    components: { Button, Input, BasicModal, BasicForm },
    emits: ['success', 'register', 'addLabelGroup'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const labelGroupId = ref<number>();
      //存储条目已有的labelList
      const oldLabelList = ref<string[]>([]);
      //存储条目已有的labels
      const oldLabels = ref<{ id: number; name: string; color: string }[]>([]);
      // 滚动到最底部
      const scrollToBottom = () => {
        const contentRef = document.querySelector('.modal-content');
        if (contentRef) {
          contentRef.scrollTop = contentRef.scrollHeight;
        }
      };

      const [
        registerForm,
        {
          resetFields,
          setFieldsValue,
          appendSchemaByField,
          removeSchemaByFiled,
          updateSchema,
          validate,
        },
      ] = useForm({
        labelWidth: 110,
        schemas: formSchema,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        isUpdate.value = data.isUpdate;
        await updateSchema([
          {
            field: 'name',
            label: '名称',
            component: 'Input',
            rules: [
              {
                required: true,
                message: '标签组名称不能为空',
              },
              {
                max: 30,
                message: '标签组名称不能超过30个字符',
              },
              {
                validator: async (_: any, value: string) => {
                  // 自定义验证规则，检查标签组名称是否包含空格
                  if (/\s/.test(value)) {
                    return Promise.reject('标签组名称不能包含空格');
                  }
                  return Promise.resolve();
                },
              },
            ],
            componentProps: { readonly: isUpdate.value },
          },
          {
            field: 'labelGroupType',
            label: '类型',
            component: 'Select',
            componentProps: {
              disabled: isUpdate.value,
              options: [{ label: '视觉', value: 0 }],
            },
            required: true,
          },
          {
            field: 'remark',
            label: '描述',
            component: 'Input',
            rules: [
              {
                max: 50,
                message: '描述不能超过50个字符',
              },
            ],
          },
        ]);
        await resetFields();
        if (unref(isUpdate)) {
          labelGroupId.value = data.labelGroupId;
          const labelGroupData = await maHttp.get(
            {
              url: `labelGroup/${data.labelGroupId}`,
              headers: {},
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
          //填充表单数据
          let formatData = labelGroupData;
          if (labelGroupData.labels) {
            oldLabels.value = labelGroupData.labels;
            for (const label of labelGroupData.labels) {
              const index = labelGroupData.labels.indexOf(label) + 1;
              const fieldName = `label${index}`;
              formatData[fieldName] = label.name;
              labelsColor[fieldName] = label.color;
              oldLabelList.value.push(fieldName);
              if (index !== 1) {
                labelIndex.value++;
                labelItemList.value.push(fieldName);
                await appendSchemaByField(
                  {
                    field: fieldName,
                    label: '标签' + labelItemList.value.length,
                    component: 'Input',
                    slot: 'add',
                    rules: [
                      {
                        required: true,
                        message: '标签名称不能为空',
                      },
                      {
                        max: 30,
                        message: '标签名称不能超过30个字符',
                      },
                      {
                        validator: async (_: any, value: string) => {
                          // 自定义验证规则，检查标签组名称是否包含空格
                          if (/\s/.test(value)) {
                            return Promise.reject('标签名称不能包含空格');
                          }
                          return Promise.resolve();
                        },
                      },
                    ],
                  },
                  '',
                );
              }
            }
          }
          delete formatData.labels;
          await setFieldsValue({
            ...formatData,
          });
        }
        setModalProps({ confirmLoading: false });
      });

      //新增item
      const addLabelItem = async () => {
        labelIndex.value++;
        const fieldName = `label${labelIndex.value}`;
        labelItemList.value.push(fieldName);
        labelsColor[fieldName] = defaultColor;
        await appendSchemaByField(
          {
            field: fieldName,
            label: '标签' + labelItemList.value.length,
            component: 'Input',
            slot: 'add',

            rules: [
              {
                required: true,
                message: '标签名称不能为空',
              },
              {
                max: 30,
                message: '标签名称不能超过30个字符',
              },
              {
                validator: async (_: any, value: string) => {
                  // 自定义验证规则，检查标签组名称是否包含空格
                  if (/\s/.test(value)) {
                    return Promise.reject('标签名称不能包含空格');
                  }
                  return Promise.resolve();
                },
              },
            ],
          },
          '',
        );
        // 滚动到底部
        scrollToBottom();
      };

      //删除item
      const deleteLabelItem = async (field: string) => {
        await removeSchemaByFiled(field);
        let updateIndex = labelItemList.value.indexOf(field);
        labelItemList.value.splice(updateIndex, 1);
        delete labelsColor[field];
        //将标签 label 数字连续
        for (const item of labelItemList.value.slice(updateIndex)) {
          const index = labelItemList.value.indexOf(item) + 1;
          await updateSchema({
            field: item,
            label: '标签' + index,
            rules: [
              {
                required: true,
                message: '标签名称不能为空',
              },
              {
                max: 30,
                message: '标签名称不能超过30个字符',
              },
              {
                validator: async (_: any, value: string) => {
                  // 自定义验证规则，检查标签组名称是否包含空格
                  if (/\s/.test(value)) {
                    return Promise.reject('标签名称不能包含空格');
                  }
                  return Promise.resolve();
                },
              },
            ],
          });
        }
      };

      //弹窗关闭时清理lableItem
      const clearLabelItem = async () => {
        for (const item of labelItemList.value.slice(1)) {
          await removeSchemaByFiled(item);
        }
        oldLabelList.value = [];
        oldLabels.value = [];
        labelIndex.value = 1;
        labelItemList.value = ['label1'];
        Object.keys(labelsColor).map((key) => {
          delete labelsColor[key];
        });
        labelsColor['label1'] = defaultColor;
      };

      const getTitle = computed(() => (!unref(isUpdate) ? '创建标签组' : '编辑标签组'));

      async function handleSubmit() {
        try {
          const values = await validate();
          const labels = Object.keys(labelsColor)
            .map((key) => {
              if (!oldLabelList.value.includes(key) && values[key]) {
                return {
                  name: values[key],
                  color: labelsColor[key],
                };
              }
            })
            .filter((value) => value);
          const labelGroupData = {
            name: values.name,
            labelGroupType: values.labelGroupType,
            remark: values.remark,
            type: values.type,
            operateType: values.operateType,
            labels: JSON.stringify([...oldLabels.value, ...labels]),
          };
          setModalProps({ confirmLoading: true });
          if (unref(isUpdate)) {
            values.import = null;
          }
          // 新增POST和修改PUT
          !unref(isUpdate)
            ? await maHttp
                .post(
                  {
                    url: 'labelGroup',
                    params: labelGroupData,
                    headers: {},
                  },
                  { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
                )
                .then((v) => {
                  emit('addLabelGroup', v);
                })
            : await maHttp.put(
                {
                  url: `labelGroup/${labelGroupId.value}`,
                  params: { id: labelGroupId.value, ...labelGroupData },
                  headers: {},
                },
                { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
              );
          closeModal();
          await clearLabelItem();
          emit('success');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      return {
        labelIndex,
        labelItemList,
        oldLabelList,
        labelsColor,
        registerModal,
        registerForm,
        getTitle,
        addLabelItem,
        deleteLabelItem,
        clearLabelItem,
        handleSubmit,
      };
    },
  });
</script>
<style scoped>
  .modal-content {
    max-height: 400px;
    overflow-y: scroll;
    padding: 10px;
  }
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }

  .myForm >>> .el-color-picker {
    margin-right: 8px;
  }
</style>
