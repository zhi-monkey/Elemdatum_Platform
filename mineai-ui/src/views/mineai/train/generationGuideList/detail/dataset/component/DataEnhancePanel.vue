<!-- DataEnhancePanel.vue -->
<template>
  <ACard
    style="padding: 20px; margin-bottom: 40px; height: 100%"
    title="数据增强"
    :tab-list="tabList"
    :active-tab-key="activeTabKey"
    @tabChange="handleTabChange"
  >
    <template #extra>
      <AButton type="primary" style="margin-right: 20px">进行数据增强</AButton>
    </template>

    <!-- 去雾内容 -->
    <div v-if="activeTabKey === 'fogRemoval'">
      <ARow :gutter="24">
        <ACol :span="12">
          <h3 style="margin-bottom: 16px; color: #1890ff">去雾增强</h3>
          <p style="line-height: 1.6; font-size: 14px">
            1. 去除图像中的雾霾，恢复图像细节。<br />
            2. 适用于低能见度图像。<br />
            3. 提升图像清晰度和对比度。
          </p>
        </ACol>
        <ACol :span="12">
          <img
            alt="fog-removal-example"
            src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
            style="max-height: 180px; width: 100%; object-fit: contain"
          />
        </ACol>
      </ARow>
    </div>

    <!-- 增雾内容 -->
    <div v-if="activeTabKey === 'fogAddition'">
      <ARow :gutter="24">
        <ACol :span="12">
          <h3 style="margin-bottom: 16px; color: #1890ff">增雾增强</h3>
          <p style="line-height: 1.6; font-size: 14px">
            1. 增加图像中的雾霾效果，模拟低能见度环境。<br />
            2. 适用于需要模拟特定气候条件的场景。<br />
            3. 控制雾霾强度，生成不同场景。
          </p>
        </ACol>
        <ACol :span="12">
          <img
            alt="fog-addition-example"
            src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
            style="max-height: 180px; width: 100%; object-fit: contain"
          />
        </ACol>
      </ARow>
    </div>

    <!-- 对比度增强内容 -->
    <div v-if="activeTabKey === 'contrastEnhancement'">
      <ARow :gutter="24">
        <ACol :span="12">
          <h3 style="margin-bottom: 16px; color: #1890ff">对比度增强</h3>
          <p style="line-height: 1.6; font-size: 14px">
            1. 增强图像的对比度，突出图像的细节。<br />
            2. 适用于图像对比度较低的场景。<br />
            3. 增强后图像色彩更加鲜明。
          </p>
        </ACol>
        <ACol :span="12">
          <img
            alt="contrast-enhancement-example"
            src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
            style="max-height: 180px; width: 100%; object-fit: contain"
          />
        </ACol>
      </ARow>
    </div>

    <!-- 直方图均衡化内容 -->
    <div v-if="activeTabKey === 'histogramEqualization'">
      <ARow :gutter="24">
        <ACol :span="12">
          <h3 style="margin-bottom: 16px; color: #1890ff">直方图均衡化</h3>
          <p style="line-height: 1.6; font-size: 14px">
            1. 通过均衡图像的亮度分布，提升图像的对比度。<br />
            2. 适用于低对比度的图像。<br />
            3. 可以更清晰地显示图像的细节。
          </p>
        </ACol>
        <ACol :span="12">
          <img
            alt="histogram-equalization-example"
            src="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
            style="max-height: 180px; width: 100%; object-fit: contain"
          />
        </ACol>
      </ARow>
    </div>
  </ACard>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { Button as AButton, Card as ACard, Col as ACol, Row as ARow } from 'ant-design-vue';

  const activeTabKey = ref<
    'fogRemoval' | 'fogAddition' | 'contrastEnhancement' | 'histogramEqualization'
  >('fogRemoval');

  const tabList = [
    { key: 'fogRemoval', tab: '去雾' },
    { key: 'fogAddition', tab: '增雾' },
    { key: 'contrastEnhancement', tab: '对比度增强' },
    { key: 'histogramEqualization', tab: '直方图均衡化' },
  ];

  const handleTabChange = (key: string) => {
    activeTabKey.value = key as typeof activeTabKey.value;
  };
</script>
