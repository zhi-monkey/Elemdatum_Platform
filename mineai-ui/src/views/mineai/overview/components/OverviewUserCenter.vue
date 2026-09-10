<template>
  <div class="content-container">
    <!-- 饼图容器 -->
    <div class="pie-chart-container" ref="pieChart"></div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted } from 'vue';
  import * as echarts from 'echarts';
  import { getRoleUserCountsAndTotalCount } from '../data';

  const roleUserCounts = ref([]);
  const totalUsers = ref(0);
  const pieChart = ref<HTMLDivElement | null>(null);
  let chartInstance: echarts.ECharts | null = null;

  // 获取数据的函数
  const fetchData = async () => {
    try {
      // 获取角色人数统计和总人数
      const roleStatsResponse = await getRoleUserCountsAndTotalCount();
      if (roleStatsResponse) {
        const { roleUserCounts: counts, totalUsers: total } = roleStatsResponse;

        // 角色人数统计
        roleUserCounts.value = counts;
        // 总用户数
        totalUsers.value = total || 0;

        // 饼图初始化
        if (pieChart.value) {
          if (!chartInstance) {
            chartInstance = echarts.init(pieChart.value);
          }

          // 计算总用户数（如果后端没有返回，则从角色统计中计算）
          const calculatedTotal = total || counts.reduce((sum: number, role: any) => sum + (role.userCount || 0), 0);

          // 设置圆环图的配置项
          const option = {
            title: {
              text: [
                '{title|总用户数}',
                '{value|' + calculatedTotal + '}',
              ].join('\n'),
              left: 'center',
              top: 'center',
              textStyle: {
                rich: {
                  title: {
                    fontSize: 16,
                    color: '#fff',
                    fontWeight: 'normal',
                    lineHeight: 20,
                  },
                  value: {
                    fontSize: 28,
                    color: '#fff',
                    fontWeight: 'bold',
                    lineHeight: 32,
                  },
                },
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
                radius: ['40%', '70%'], // 圆环图：内半径40%，外半径70%
                center: ['50%', '50%'],
                data: counts.map((role: any) => ({
                  value: role.userCount,
                  name: role.roleName,
                })),
                emphasis: {
                  itemStyle: {
                    shadowBlur: 30,
                    shadowOffsetX: 0,
                  },
                },
                label: {
                  show: true,
                  formatter: '{b} {d}%', // 显示角色名称和百分比
                  fontSize: '0.7rem',
                  color: '#fff',
                  lineHeight: 18,
                },
                labelLine: {
                  show: true,
                  length: 15,
                  length2: 10,
                  lineStyle: {
                    color: '#fff',
                  },
                },
              },
            ],
          };

          chartInstance.setOption(option);
        }
      }
    } catch (error) {
      console.error('获取数据失败:', error);
    }
  };

  const handleResize = () => {
    if (chartInstance) {
      chartInstance.resize();
    }
  };

  // 监听刷新事件
  const handleRefresh = () => {
    fetchData();
  };

  onMounted(() => {
    fetchData();
    window.addEventListener('resize', handleResize);
    window.addEventListener('overview-refresh-user-center', handleRefresh);
  });

  onUnmounted(() => {
    if (chartInstance) {
      chartInstance.dispose();
      chartInstance = null;
    }
    window.removeEventListener('resize', handleResize);
    window.removeEventListener('overview-refresh-user-center', handleRefresh);
  });
</script>

<style scoped lang="less">
  .content-container {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .pie-chart-container {
    width: 100%;
    height: 100%;
    min-height: 200px;
  }
</style>

