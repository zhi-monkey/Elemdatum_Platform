<template>
  <div class="label-info-container">
    <!-- 带边框的盒子容器 -->
    <div class="bordered-box">
      <div class="sort-container">
        <a-space>
          <a-input
            v-model:value="searchText"
            placeholder="搜索标签"
            style="width: 200px"
            allow-clear
          >
            <template #prefix>
              <search-outlined />
            </template>
          </a-input>
          <span>排序方式：</span>
          <a-select v-model:value="sortType" style="width: 120px">
            <a-select-option value="id">按ID排序</a-select-option>
            <a-select-option value="name">按名称排序</a-select-option>
          </a-select>
          <a-select v-model:value="sortOrder" style="width: 120px">
            <a-select-option value="asc">升序</a-select-option>
            <a-select-option value="desc">降序</a-select-option>
          </a-select>
        </a-space>
      </div>

      <a-empty v-if="sortedLabels.length === 0" description="暂无标签" />

      <a-descriptions v-else bordered :column="1" size="middle">
        <template v-for="(label, index) in sortedLabels" :key="label.id">
          <a-descriptions-item
            :label="sortOrder === 'asc' ? `${index + 1}` : `${sortedLabels.length - index}`"
          >
            <a-row :gutter="16">
              <!--              <a-col :span="6">-->
              <!--                <div class="label-item">-->
              <!--                  <span class="label-title">ID:</span>-->
              <!--                  <span class="label-value">{{ label.id }}</span>-->
              <!--                </div>-->
              <!--              </a-col>-->
              <a-col :span="24">
                <div class="label-item">
                  <span class="label-title">标签名称:</span>
                  <span class="label-value">{{ label.name }}</span>
                </div>
              </a-col>
              <!--              <a-col :span="12">-->
              <!--                <div class="label-item">-->
              <!--                  <span class="label-title">颜色标识:</span>-->
              <!--                  <div class="color-preview">-->
              <!--                    <span-->
              <!--                      class="color-block"-->
              <!--                      :style="{-->
              <!--                        backgroundColor: label.color,-->
              <!--                        borderColor: darkenColor(label.color, 30),-->
              <!--                      }"-->
              <!--                    ></span>-->
              <!--                    <span class="color-value">{{ label.color }}</span>-->
              <!--                  </div>-->
              <!--                </div>-->
              <!--              </a-col>-->
            </a-row>
          </a-descriptions-item>
        </template>
      </a-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { getLabelByModelApplicationId } from '/@/views/mineai/application/taskManagement/api/api';
  import { computed, onMounted, ref } from 'vue';
  import {
    Descriptions as ADescriptions,
    DescriptionsItem as ADescriptionsItem,
    Empty as AEmpty,
    Space as ASpace,
    Select as ASelect,
    SelectOption as ASelectOption,
    Row as ARow,
    Col as ACol,
    Input as AInput,
  } from 'ant-design-vue';

  import { SearchOutlined } from '@ant-design/icons-vue';
  import { getDatasetVersionLabels } from '/@/views/mineai/data/dataset-details2/api';

  const labels = ref([]);
  const sortType = ref('id');
  const sortOrder = ref('asc');
  const searchText = ref('');

  const props = defineProps<{
    datasetId: number;
    versionName: string;
  }>();

  onMounted(async () => {
    labels.value = await getDatasetVersionLabels(props.datasetId, props.versionName);
  });

  // 排序后的标签
  const sortedLabels = computed(() => {
    // 先过滤
    const filteredLabels = labels.value.filter((label) => {
      if (!searchText.value) return true;

      const searchLower = searchText.value.toLowerCase();
      return label.name.toLowerCase().includes(searchLower);
    });

    // 再排序
    return filteredLabels.sort((a, b) => {
      const factor = sortOrder.value === 'asc' ? 1 : -1;

      if (sortType.value === 'id') {
        return (a.id - b.id) * factor;
      } else if (sortType.value === 'name') {
        return a.name.localeCompare(b.name, 'zh-CN') * factor;
      }

      return 0;
    });
  });

  // 颜色加深函数
  const darkenColor = (color: string, percent: number) => {
    if (!color) return '#000000';

    // 处理十六进制色值
    let r, g, b;
    if (color.startsWith('#')) {
      const hex = color.substring(1);
      r = parseInt(hex.substring(0, 2), 16);
      g = parseInt(hex.substring(2, 4), 16);
      b = parseInt(hex.substring(4, 6), 16);
    }
    // 处理 rgb 格式
    else if (color.startsWith('rgb')) {
      const rgbMatch = color.match(/\d+/g);
      if (rgbMatch && rgbMatch.length >= 3) {
        r = parseInt(rgbMatch[0]);
        g = parseInt(rgbMatch[1]);
        b = parseInt(rgbMatch[2]);
      } else {
        return '#000000';
      }
    } else {
      return '#000000';
    }

    // 将颜色加深
    r = Math.max(0, Math.floor((r * (100 - percent)) / 100));
    g = Math.max(0, Math.floor((g * (100 - percent)) / 100));
    b = Math.max(0, Math.floor((b * (100 - percent)) / 100));

    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b
      .toString(16)
      .padStart(2, '0')}`;
  };
</script>

<style scoped lang="less">
  .label-info-container {
    padding: 0 16px;
  }

  .bordered-box {
    border-radius: 4px;
    padding: 16px;
    min-height: 423px;
    max-height: 423px;
    overflow-y: scroll;
  }

  .sort-container {
    margin-bottom: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .label-item {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .label-title {
    text-align: center;
    margin-right: 8px;
    font-weight: 500;
    color: rgba(189, 189, 189, 0.85);
    white-space: nowrap;
  }

  .label-value {
    //flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .color-preview {
    display: flex;
    align-items: center;
    flex: 1;
  }

  .color-block {
    display: inline-block;
    width: 16px;
    height: 16px;
    margin-right: 8px;
    border-radius: 2px;
    border: 1px solid;
    flex-shrink: 0;
  }

  .color-value {
    font-family: monospace;
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  :deep(.ant-descriptions-item-content) {
    padding: 8px 16px !important;
  }

  :deep(.ant-descriptions-item-label) {
    padding: 8px 16px !important;
    background-color: #fafafa;
    font-weight: 500;
    width: 60px;
    text-align: center;
  }

  :deep(.custom-input) {
    background-color: transparent !important;
  }

  :deep(.custom-input .ant-input) {
    background-color: #fff;
    color: #000;
  }

  :deep(.custom-input .ant-input-prefix) {
    color: rgba(0, 0, 0, 0.45);
  }

  :deep(.custom-input input::placeholder) {
    color: rgba(0, 0, 0, 0.45);
  }
</style>
