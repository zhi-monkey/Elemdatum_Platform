<template>
  <div>
    <PageWrapper :contentStyle="{ margin: '0' }">
      <template #title>数据集管理</template>
      <template #footer>
        <Tabs :active-key="activeKey" @change="handleTabChange">
          <tab-pane key="1" tab="私有数据集组">
            <PrivateDatasetGroup @back="goBack" :refresh-trigger="privateGroupRefreshTrigger" />
          </tab-pane>

          <!-- 
            以下 Tab 已被禁用，如需恢复请取消注释，取消注释之后好像也不好使
          -->
          <!--          <tab-pane key="2" tab="私有数据集">-->
          <!--            <PrivateDataset-->
          <!--              v-if="activeKey === '2'"-->
          <!--              :refresh-trigger="privateDatasetRefreshTrigger"-->
          <!--            />-->
          <!--          </tab-pane>-->

          <!-- <tab-pane key="3" tab="公共数据集">
            <PublicDataset v-if="activeKey === '3'" />
          </tab-pane> -->

          <tab-pane key="4" tab="公开数据集组">
            <PublicDatasetGroup @back="goBack" :refresh-trigger="publicGroupRefreshTrigger" />
          </tab-pane>
        </Tabs>
      </template>
    </PageWrapper>

    <!-- 弹窗组件已移至 PrivateDataset.vue 组件中 -->
  </div>
</template>

<script lang="ts">
  export default {
    name: 'DatasetDetails',
  };
</script>

<script lang="ts" setup>
  import { TabPane, Tabs } from 'ant-design-vue';
  import { ref, onMounted, nextTick } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { PageWrapper } from '/@/components/Page';
  // import PublicDataset from './publicDataset/publicDataset.vue'; // 已禁用，取消注释以恢复
  // import PrivateDataset from './PrivateDataset.vue'; // 私有数据集组件（已禁用，取消注释以恢复）
  import PrivateDatasetGroup from './PrivateDatasetGroup.vue';
  import PublicDatasetGroup from './PublicDatasetGroup.vue';

  const router = useRouter();
  const route = useRoute();

  // 先根据url拿参数，没有再赋值1
  const activeKey = ref(route.query.tab ? String(route.query.tab) : '1');

  // Tab切换时的刷新触发器
  const privateGroupRefreshTrigger = ref(0);
  // const privateDatasetRefreshTrigger = ref(0); // 私有数据集Tab刷新触发器（如需恢复请取消注释）
  const publicGroupRefreshTrigger = ref(0);

  const handleTabChange = (key) => {
    activeKey.value = key;

    // 切换到私有数据集组Tab时刷新列表
    if (key === '1') {
      privateGroupRefreshTrigger.value = Date.now();
    }

    // 切换到公开数据集组Tab时刷新列表
    if (key === '4') {
      publicGroupRefreshTrigger.value = Date.now();
    }
  };

  function goBack() {
    router.go(-1);
  }

  // 确保页面滚动可用（修复从总览页切换过来时滚动被禁用的问题）
  onMounted(() => {
    // 立即移除总览页的动态样式表（如果存在）
    if (typeof document !== 'undefined') {
      const overviewStyleSheet = document.getElementById('overview-page-styles');
      if (overviewStyleSheet) {
        overviewStyleSheet.remove();
      }
    }

    nextTick(() => {
      // 恢复 html 和 body 的滚动（覆盖总览页的 !important 样式）
      if (typeof document !== 'undefined') {
        // 再次检查并移除总览页样式表
        const overviewStyleSheet = document.getElementById('overview-page-styles');
        if (overviewStyleSheet) {
          overviewStyleSheet.remove();
        }

        // 使用 setProperty 覆盖 CSS 的 !important 规则
        document.documentElement.style.setProperty('overflow', '', 'important');
        document.body.style.setProperty('overflow', '', 'important');

        // 恢复布局容器的滚动
        const layoutSelectors = [
          '.vben-layout-content',
          '.vben-layout-content.fixed',
          '.vben-page-wrapper',
          '.vben-page-wrapper-content',
          '.app-container',
          '.wrapper',
          '.wrapper.fixed',
          '.vben-layout-main',
          '.vben-layout',
        ];

        layoutSelectors.forEach((selector) => {
          const elements = document.querySelectorAll(selector);
          elements.forEach((el) => {
            const htmlEl = el as HTMLElement;
            // 移除 overflow: hidden，恢复默认滚动行为
            htmlEl.style.setProperty('overflow', '', 'important');
            // 移除固定高度限制
            htmlEl.style.setProperty('height', '', 'important');
            htmlEl.style.setProperty('max-height', '', 'important');
          });
        });
      }
    });

    // 延迟再次检查，确保样式表被移除
    setTimeout(() => {
      if (typeof document !== 'undefined') {
        const overviewStyleSheet = document.getElementById('overview-page-styles');
        if (overviewStyleSheet) {
          overviewStyleSheet.remove();
        }
        // 再次恢复滚动
        document.documentElement.style.setProperty('overflow', '', 'important');
        document.body.style.setProperty('overflow', '', 'important');
      }
    }, 100);
  });

  // 如需恢复私有数据集Tab，请取消以下注释：
  // onUnmounted(() => {
  //   clearInterval(timer.value);
  //   clearInterval(uploadTimer.value);
  //   clearInterval(publishTimer.value);
  // });
</script>
