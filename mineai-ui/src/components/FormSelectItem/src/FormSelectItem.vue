<template>
  <div class="flex flex-row justify-start">
    <Select
      style="width: 100%"
      :placeholder="placeholder ? placeholder : '请选择'"
      v-model:value="selectValues"
      :mode="mode === 'multiple' ? 'multiple' : undefined"
      :options="selectOptions"
      :dropdownAlign="{
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      }"
      :disabled="disabled"
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
  </div>
</template>
<script setup lang="ts">
  import { LoadingOutlined } from '@ant-design/icons-vue';
  import { Select } from 'ant-design-vue';
  import { defineProps, ref, watch } from 'vue';

  interface itemType {
    value: string;
    label: string;
    disabled?: boolean;
  }
  const emits = defineEmits(['updateData']);
  const props = defineProps<{
    data?: string | string[];
    mode: 'single' | 'multiple';
    placeholder?: string;
    options?: itemType[];
    fetchData?: () => Promise<itemType[]>;
    disabled?: boolean;
  }>();

  const loading = ref<boolean>(false);

  const selectValues = ref<string | string[]>();
  const selectOptions = ref<itemType[]>();

  async function handleFetch() {
    if (props.fetchData) {
      loading.value = true;
      selectOptions.value = await props.fetchData();
      loading.value = false;
    }
  }

  watch(
    () => props.options,
    (newV) => {
      if (newV) {
        selectOptions.value = newV;
      }
    },
  );

  watch(
    () => props.data,
    (newV) => {
      if (newV) {
        selectValues.value = newV;
      } else {
        selectValues.value = [];
      }
    },
  );

  watch(selectValues, (newVal) => {
    emits('updateData', newVal);
  });
</script>
<style scoped lang="less"></style>
