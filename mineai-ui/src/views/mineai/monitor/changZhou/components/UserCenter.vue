<template>
  <div class="content-container">
    <!-- 左侧：饼图容器 -->
    <div class="pie-chart-container" ref="pieChart"></div>

    <!-- 右侧：上下排列的卡片 -->
    <div class="stats-container">
      <!-- 在线用户统计卡片 -->
      <div class="stat-card">
        <div class="stat-card-header">已启用用户</div>
        <div class="stat-card-body">{{ enabledUsers }} <span>人</span></div>
      </div>

      <!-- 总人数统计卡片 -->
      <div class="stat-card">
        <div class="stat-card-header">注册人数</div>
        <div class="stat-card-body">{{ totalUsers }} <span>人</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue';
  import { BorderBox8 } from '@kjgl77/datav-vue3';
  import * as echarts from 'echarts'; // 引入 echarts
  import { getRoleUserCountsAndTotalCount, getOnlineUserStats, getEnabledUserStats } from '../data';

  const enabledUsers = ref(0); // 在线用户数
  const totalUsers = ref(0); // 总人数
  const roleUserCounts = ref([]); // 角色用户统计

  const pieChart = ref<HTMLDivElement | null>(null); // 饼图容器的引用
  // 获取数据的函数
  const fetchData = async () => {
    try {
      // 获取在线用户数量
      const enabledResponse = await getEnabledUserStats();
      if (enabledResponse !== undefined) {
        enabledUsers.value = enabledResponse; // 直接赋值为返回的数字
      }

      // 获取角色人数统计和总人数
      const roleStatsResponse = await getRoleUserCountsAndTotalCount();
      if (roleStatsResponse) {
        const { roleUserCounts: counts, totalUsers: totalCount } = roleStatsResponse;

        // 总人数
        totalUsers.value = totalCount;

        // 角色人数统计
        roleUserCounts.value = counts;

        // 饼图初始化
        if (pieChart.value) {
          const chartInstance = echarts.init(pieChart.value); // 初始化 ECharts 实例

          // 设置饼图的配置项
          const option = {
            title: {
              text: '角色分布',
              left: 'center',
              top: 'center',
              textStyle: {
                color: '#fff',
                fontSize: 25,
              },
            },
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} ({d}%)',
            },
            series: [
              {
                name: '角色分布',
                type: 'pie',
                radius: '50%',
                data: counts.map((role) => ({
                  value: role.userCount,
                  name: role.roleName,
                })),
                emphasis: {
                  itemStyle: {
                    shadowBlur: 30,
                    shadowOffsetX: 0,
                    //shadowColor: 'rgba(0, 0, 0, 0.5)',
                  },
                },
                label: {
                  fontSize: '0.9rem', // 设置每个饼图部分的文字大小
                  color: '#fff', // 设置文字颜色
                },
              },
            ],
          };

          chartInstance.setOption(option); // 设置图表的配置项
        }
      }
    } catch (error) {
      console.error('获取数据失败:', error);
    }
  };
  onMounted(() => {
    fetchData();
  });
</script>

<style scoped lang="less">
  .title {
    left: 0.3%;
    top: 1%;
    position: relative;
    padding-left: 3vh;
    height: 3vh;
    line-height: 3vh;
    font-size: 2vh;
    font-weight: normal;
    color: rgba(35, 245, 251, 0.92);
    background-image: url('../../../../../assets/images/title-bg.png');
    background-repeat: no-repeat;
    background-size: 100% 100%;
  }

  .content-container {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    top: 10%;
    position: relative;
    height: auto;
    width: 100%;
  }

  /* 饼图容器 */
  .pie-chart-container {
    width: 40vh; /* 调整饼图容器的宽度 */
    display: flex;
    height: 33vh; /* 设置饼图容器的高度 */
    left: -1vh;
    right: 0.2vh;
    margin-top: -6vh;
    bottom: 1vh;
  }

  /* 右侧：卡片容器 */
  .stats-container {
    display: flex;
    flex-direction: column; /* 上下排列 */
    gap: 2vh; /* 调整卡片间的间距 */
    width: 15vh; /* 减少右侧容器宽度 */
    margin-top: 0vh;
  }

  .stat-card {
    //background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%); /* 渐变背景色 */
    background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
    padding: 2vh; /* 增加内边距让内容更舒适 */
    border-radius: 1vh; /* 更圆的边角 */
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2); /* 更柔和的阴影 */
    height: 8vh; /* 调整卡片高度 */
    margin-left: -10%; /* 微调左右间距 */
    margin-right: 3vh;
    color: #fff; /* 文字颜色为白色，适应深色背景 */
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
  }
  .stat-card-header {
    font-size: 1.5vh;
    font-weight: bold;
    color: #000000;
    margin-bottom: 0vh;
    margin-top: 2vh;
  }

  .stat-card-body {
    font-size: 2.5vh;
    font-weight: bold;
    color: #4caf50;
    margin-bottom: 1vh;
  }

  .stat-card-body span {
    font-size: 1.5vh;
    color: #888;
  }

  //@media (max-width: 1919px) {
  //  .pie-chart-container {
  //    width: 90%;
  //    height: auto;
  //  }
  //}
  //
  //@media (min-width: 768px) {
  //  .pie-chart-container {
  //    width: 60%;
  //    height: auto;
  //  }
  //}
</style>
