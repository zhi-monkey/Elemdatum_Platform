<template>
  <div class="el-form-item__label no-float tl">
    <label>数据增强</label>
    <el-tooltip effect="dark" placement="top">
      <template #content>
        <div class="label-tooltip">
          <div class="flex tips-wrapper f12">
            <div>标注完成，或自动标注完成的数据集，可以执行数据增强</div>
          </div>
        </div>
      </template>
      <i class="el-icon-warning-outline" style="color: #2e4fde"></i>
    </el-tooltip>
  </div>
</template>
<script>
  export default {
    name: 'EnhanceTip',
  };
</script>
<style scoped lang="scss">
  .label-title {
    margin-bottom: 8px;
  }

  .tips-wrapper {
    flex-wrap: wrap;
    max-width: 200px;
    line-height: 1.5;
  }
</style>
