<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    title="数据增强"
    width="520px"
    @ok="handleSubmit"
  >
    <div class="enhance-modal-content">
      <!-- 基础信息显示 -->
      <div class="info-section">
        <div class="info-item">
          <span class="info-label">数据集名称：</span>
          <span class="info-value">{{ datasetInfo.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">当前版本：</span>
          <span class="info-value">{{ datasetInfo.currentVersionName }}</span>
        </div>
      </div>

      <!-- 增强类型选择 -->
      <div class="enhance-section">
        <!-- 增强算子选择框 -->
        <div class="enhance-selection-box">
          <div class="section-title">
            <span>增强算子选择</span>
            <span v-if="selectedTypes.length > 0" class="selected-count"
              >(已选 {{ selectedTypes.length }})</span
            >
          </div>

          <div class="enhance-types-container">
            <div
              v-for="option in enhanceOptions"
              :key="option.value"
              class="enhance-type-chip"
              :class="{ selected: isSelected(option.value) }"
              @click="toggleType(option.value)"
            >
              <span v-if="isSelected(option.value)" class="type-order">
                {{ getTypeOrder(option.value) }}
              </span>
              <span class="type-name">{{ option.label }}</span>
              <a-tooltip
                placement="right"
                :overlay-style="{ maxWidth: '450px' }"
                :overlay-class-name="'enhance-detail-tooltip'"
              >
                <template #title>
                  <div class="enhance-tooltip">
                    <div class="tooltip-header">
                      <div class="tooltip-title">{{ option.label }}</div>
                      <div class="tooltip-desc">{{ option.description }}</div>
                    </div>
                    <div class="tooltip-content">
                      <div class="image-comparison">
                        <div class="image-item">
                          <img :src="option.beforeImage" alt="增强前" class="compare-image" />
                          <div class="image-label">增强前</div>
                        </div>
                        <div class="arrow-icon">→</div>
                        <div class="image-item">
                          <img :src="option.afterImage" alt="增强后" class="compare-image" />
                          <div class="image-label">增强后</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
                <span class="detail-icon" @click.stop>i</span>
              </a-tooltip>
            </div>
          </div>

          <!-- 增强算子排序显示 -->
          <div v-if="selectedTypes.length > 0" class="enhance-order-info">
            <span class="order-title">增强算子排序：</span>
            <span v-for="(type, index) in selectedTypes" :key="type" class="order-item">
              {{ index + 1 }}. {{ getTypeName(type) }}
            </span>
          </div>

          <!-- 提示信息 -->
          <div v-if="selectedTypes.length === 0" class="empty-tip"> 请至少选择一个增强算子 </div>
        </div>
      </div>

      <!-- 参数配置区域 -->
      <div v-if="selectedParamTypes.length > 0" class="params-section">
        <div class="params-box">
          <div class="params-title">参数配置</div>
          <template v-for="t in selectedParamTypes" :key="t">
            <div v-if="t === '5'" class="param-group">
              <div class="param-group-title">锐化参数</div>
              <div class="param-item">
                <span class="param-label">锐化强度：</span>
                <a-input-number
                  v-model:value="enhanceParams.sharpen_strength"
                  :min="0.1"
                  :max="20"
                  :step="0.1"
                  :precision="2"
                  placeholder="推荐1.0~2.0"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">锐化卷积核尺寸：</span>
                <a-input-number
                  v-model:value="enhanceParams.sharpen_kernel_size"
                  :min="3"
                  :max="11"
                  :step="2"
                  :precision="0"
                  placeholder="奇数，如3、5、7"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '6'" class="param-group">
              <div class="param-group-title">亮度调整参数</div>
              <div class="param-item">
                <span class="param-label">对比度增强幅度：</span>
                <a-input-number
                  v-model:value="enhanceParams.contrast_limit"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  placeholder="建议0.1~0.5"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">亮度扰动幅度：</span>
                <a-input-number
                  v-model:value="enhanceParams.brightness_limit"
                  :min="-0.5"
                  :max="0.5"
                  :step="0.05"
                  :precision="2"
                  placeholder="正为提亮，负为调暗"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '7'" class="param-group">
              <div class="param-group-title">高斯噪声参数</div>
              <div class="param-item">
                <span class="param-label">方差范围：</span>
                <a-input-number
                  v-model:value="enhanceParams.var_limit"
                  :min="1"
                  :max="100"
                  :step="1"
                  :precision="0"
                  placeholder="建议10~50"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">均值：</span>
                <a-input-number
                  v-model:value="enhanceParams.mean"
                  :min="-10"
                  :max="10"
                  :step="1"
                  :precision="0"
                  placeholder="一般为0即可"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '8'" class="param-group">
              <div class="param-group-title">随机雾效参数</div>
              <div class="param-item">
                <span class="param-label">雾浓度系数：</span>
                <a-input-number
                  v-model:value="enhanceParams.fog_coef"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  placeholder="建议0.1~0.5"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">透明度系数：</span>
                <a-input-number
                  v-model:value="enhanceParams.alpha_coef"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  placeholder="建议0.08~0.2"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '9'" class="param-group">
              <div class="param-group-title">随机雨效参数</div>
              <div class="param-item">
                <span class="param-label">雨滴长度：</span>
                <a-input-number
                  v-model:value="enhanceParams.drop_length"
                  :min="5"
                  :max="50"
                  :step="1"
                  :precision="0"
                  placeholder="建议10~30"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">模糊值：</span>
                <a-input-number
                  v-model:value="enhanceParams.blur_value"
                  :min="1"
                  :max="10"
                  :step="1"
                  :precision="0"
                  placeholder="建议3~7"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '10'" class="param-group">
              <div class="param-group-title">随机雪效参数</div>
              <div class="param-item">
                <span class="param-label">雪花密度：</span>
                <a-input-number
                  v-model:value="enhanceParams.snow_density"
                  :min="0.001"
                  :max="0.1"
                  :step="0.001"
                  :precision="3"
                  placeholder="建议0.01~0.05"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">雪花最小尺寸：</span>
                <a-input-number
                  v-model:value="enhanceParams.snow_size_min"
                  :min="1"
                  :max="5"
                  :step="1"
                  :precision="0"
                  placeholder="建议1~2"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">雪花最大尺寸：</span>
                <a-input-number
                  v-model:value="enhanceParams.snow_size_max"
                  :min="2"
                  :max="10"
                  :step="1"
                  :precision="0"
                  placeholder="建议3~5"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">雪花透明度：</span>
                <a-input-number
                  v-model:value="enhanceParams.snow_alpha"
                  :min="0.3"
                  :max="1.0"
                  :step="0.1"
                  :precision="1"
                  placeholder="建议0.6~0.9"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '11'" class="param-group">
              <div class="param-group-title">随机阴影参数</div>
              <div class="param-item">
                <span class="param-label">阴影数量上限：</span>
                <a-input-number
                  v-model:value="enhanceParams.num_shadows_upper"
                  :min="1"
                  :max="5"
                  :step="1"
                  :precision="0"
                  placeholder="建议1~3"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '12'" class="param-group">
              <div class="param-group-title">随机阳光眩光参数</div>
              <div class="param-item">
                <span class="param-label">光源半径：</span>
                <a-input-number
                  v-model:value="enhanceParams.src_radius"
                  :min="50"
                  :max="300"
                  :step="10"
                  :precision="0"
                  placeholder="建议80~150"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">眩光强度：</span>
                <a-input-number
                  v-model:value="enhanceParams.flare_intensity"
                  :min="0.1"
                  :max="1.0"
                  :step="0.1"
                  :precision="1"
                  placeholder="建议0.4~0.8"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">光斑数量：</span>
                <a-input-number
                  v-model:value="enhanceParams.num_flare_circles"
                  :min="3"
                  :max="15"
                  :step="1"
                  :precision="0"
                  placeholder="建议6~10"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '13'" class="param-group">
              <div class="param-group-title">转为灰度参数</div>
              <div class="param-item">
                <span class="param-label">通道模式：</span>
                <select
                  v-model="enhanceParams.keep_channels"
                  style="
                    width: 200px;
                    height: 32px;
                    padding: 4px 11px;
                    border: 1px solid #434343;
                    border-radius: 4px;
                    font-size: 13px;
                    background-color: #2a2a2a;
                    color: rgba(255, 255, 255, 0.85);
                    cursor: pointer;
                    outline: none;
                    -webkit-appearance: none;
                    -moz-appearance: none;
                    appearance: none;
                  "
                >
                  <option :value="true">三通道</option>
                  <option :value="false">单通道</option>
                </select>
              </div>
            </div>
            <div v-else-if="t === '14'" class="param-group">
              <div class="param-group-title">色相饱和度亮度参数</div>
              <div class="param-item">
                <span class="param-label">色相偏移范围：</span>
                <a-input-number
                  v-model:value="enhanceParams.hsv_hue_shift_limit"
                  :min="0"
                  :max="180"
                  :step="1"
                  :precision="0"
                  placeholder="建议10~30"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">饱和度偏移范围：</span>
                <a-input-number
                  v-model:value="enhanceParams.hsv_sat_shift_limit"
                  :min="0"
                  :max="100"
                  :step="1"
                  :precision="0"
                  placeholder="建议20~40"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">亮度偏移范围：</span>
                <a-input-number
                  v-model:value="enhanceParams.hsv_val_shift_limit"
                  :min="0"
                  :max="100"
                  :step="1"
                  :precision="0"
                  placeholder="建议10~30"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '15'" class="param-group">
              <div class="param-group-title">随机亮度对比度参数</div>
              <div class="param-item">
                <span class="param-label">亮度调整范围：</span>
                <a-input-number
                  v-model:value="enhanceParams.rbc_brightness_limit"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  placeholder="建议0.1~0.3"
                  style="width: 200px"
                />
              </div>
              <div class="param-item">
                <span class="param-label">对比度调整范围：</span>
                <a-input-number
                  v-model:value="enhanceParams.rbc_contrast_limit"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  :precision="2"
                  placeholder="建议0.1~0.3"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '16'" class="param-group">
              <div class="param-group-title">高斯模糊参数</div>
              <div class="param-item">
                <span class="param-label">模糊核大小：</span>
                <a-input-number
                  v-model:value="enhanceParams.gaussian_blur_limit"
                  :min="3"
                  :max="21"
                  :step="2"
                  :precision="0"
                  placeholder="建议5~11（奇数）"
                  style="width: 200px"
                />
              </div>
            </div>
            <div v-else-if="t === '17'" class="param-group">
              <div class="param-group-title">运动模糊参数</div>
              <div class="param-item">
                <span class="param-label">模糊核大小：</span>
                <a-input-number
                  v-model:value="enhanceParams.motion_blur_limit"
                  :min="3"
                  :max="21"
                  :step="2"
                  :precision="0"
                  placeholder="建议5~11（奇数）"
                  style="width: 200px"
                />
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 处理策略选择 -->
      <div class="strategy-section-wrapper">
        <div class="strategy-section">
          <div class="strategy-title">处理策略选择</div>
          <div class="strategy-options">
            <div
              class="strategy-option"
              :class="{ selected: enhanceMode === 'serial' }"
              @click="enhanceMode = 'serial'"
            >
              串行叠加
            </div>
            <div
              class="strategy-option"
              :class="{ selected: enhanceMode === 'parallel' }"
              @click="enhanceMode = 'parallel'"
            >
              并行遍历
            </div>
          </div>
        </div>
      </div>

      <!-- 增强数量 -->
      <div class="enhance-count-wrapper">
        <div class="enhance-count-section">
          <div class="count-title">增强数量</div>
          <div class="count-input-wrapper">
            <a-input-number
              v-model:value="enhanceCount"
              :min="1"
              :max="maxEnhanceCount"
              :precision="0"
              placeholder="请输入增强数量"
              style="width: 200px"
            />
            <div class="count-tip">最大值: {{ maxEnhanceCount }}</div>
          </div>
        </div>
      </div>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { defineEmits, ref, computed, watch } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { message, InputNumber as AInputNumber, Tooltip as ATooltip } from 'ant-design-vue';

  const emits = defineEmits(['success']);

  // 增强类型选项
  const enhanceOptions = [
    {
      label: '去雾',
      value: '1',
      description: '去除图像中的雾霾效果，提升图像清晰度和对比度',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/dehaze_out.jpg',
    },
    {
      label: '增雾',
      value: '2',
      description: '调整图像对比度，能更好地识别对比度不同的图像',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/dhaze_out.jpg',
    },
    {
      label: '对比度增强',
      value: '3',
      description: '增强图像的明暗对比，使细节更加清晰',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/ace_enhanced_out.jpg',
    },
    {
      label: '直方图均衡化',
      value: '4',
      description: '在HLS色彩空间对亮度通道均衡化，自动调整明暗分布，使图像轮廓更清晰可辨',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/hist_equalize_out.jpg',
    },
    {
      label: '锐化',
      value: '5',
      description: '提取并增强图像边缘和高频细节，使轮廓更清晰、纹理更锐利，提升整体视觉清晰度',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/sharpen_out.jpg',
    },
    {
      label: '亮度调整',
      value: '6',
      description:
        '调整图像明暗对比度和整体亮度，模拟不同曝光条件和光照强度，增强模型对亮度变化的适应能力。',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/contrast_out.jpg',
    },
    {
      label: '高斯噪声',
      value: '7',
      description:
        '向图像添加随机高斯噪声，模拟低质量传感器、弱光环境或信号干扰，提高模型对噪声图像的鲁棒性和抗干扰能力',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/gauss_noise_out.jpg',
    },
    {
      label: '随机雾效',
      value: '8',
      description:
        '向图像添加随机雾效，模拟雾天、霾天等低能见度天气条件，增强模型对恶劣天气场景的适应能力',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/random_fog_out.jpg',
    },
    {
      label: '随机雨效',
      value: '9',
      description: '向图像添加随机雨滴效果，模拟雨天场景，提高模型对雨天图像的识别能力和鲁棒性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/random_rain_out.jpg',
    },
    {
      label: '随机雪效',
      value: '10',
      description: '向图像添加随机雪花效果，模拟雪天场景，增强模型对冬季天气条件下图像的处理能力',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/random_snow_out.jpg',
    },
    {
      label: '随机阴影',
      value: '11',
      description:
        '在图像中添加随机阴影区域，模拟不同光照条件下的阴影效果，提升模型对光照变化的适应性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/random_shadow_out.jpg',
    },
    {
      label: '随机阳光眩光',
      value: '12',
      description:
        '向图像添加随机阳光眩光效果，模拟强光照射场景，增强模型对过曝和眩光图像的处理能力',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/random_sunflare_out.jpg',
    },
    {
      label: '转为灰度',
      value: '13',
      description:
        '将彩色图像转换为灰度图，可选择输出单通道或三通道灰度图，用于测试模型对灰度图像的适应性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/to_gray_out.jpg',
    },
    {
      label: '色相饱和度亮度',
      value: '14',
      description:
        '调整图像的色相、饱和度和亮度，模拟不同的光照条件和相机设置，增强模型对颜色变化的鲁棒性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/hue_saturation_value_out.jpg',
    },
    {
      label: '随机亮度对比度',
      value: '15',
      description: '随机调整图像的亮度和对比度，模拟不同的曝光条件，提高模型对光照变化的适应能力',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/random_brightness_contrast_out.jpg',
    },
    {
      label: '高斯模糊',
      value: '16',
      description: '对图像应用高斯模糊，模拟失焦效果，增强模型对模糊图像的鲁棒性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/gaussian_blur_out.jpg',
    },
    {
      label: '运动模糊',
      value: '17',
      description: '对图像应用运动模糊，模拟相机或物体运动时的模糊效果，提高模型对动态场景的适应性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/motion_blur_out.jpg',
    },
    {
      label: '水平翻转',
      value: '18',
      description: '将图像沿垂直轴翻转，同时自动调整标注框位置，增加数据多样性',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/horizontal_flip_out.jpg',
    },
    {
      label: '垂直翻转',
      value: '19',
      description: '将图像沿水平轴翻转，同时自动调整标注框位置，模拟不同视角',
      beforeImage: '/resource/img/1.jpg',
      afterImage: '/resource/img/vertical_flip_out.jpg',
    },
  ];

  // 数据集信息
  const datasetInfo = ref({
    datasetId: '',
    name: '',
    currentVersionName: '',
    originFileCount: 0,
  });

  // 已选择的增强类型（按选择顺序）
  const selectedTypes = ref<string[]>([]);

  // 处理策略选择（默认串行叠加）
  const enhanceMode = ref<'serial' | 'parallel'>('serial');

  // 增强数量（默认1）
  const enhanceCount = ref<number>(1);

  // 默认参数配置（统一管理默认值）
  const DEFAULT_ENHANCE_PARAMS = {
    // 锐化参数
    sharpen_strength: 1.0,
    sharpen_kernel_size: 3,
    // 亮度调整参数
    contrast_limit: 0.2,
    brightness_limit: 0.0,
    // 高斯噪声参数
    var_limit: 30,
    mean: 0,
    // 随机雾效参数
    fog_coef: 0.3,
    alpha_coef: 0.08,
    // 随机雨效参数
    drop_length: 20,
    blur_value: 3,
    // 随机雪效参数
    snow_density: 0.01,
    snow_size_min: 1,
    snow_size_max: 3,
    snow_alpha: 0.8,
    // 随机阴影参数
    num_shadows_upper: 2,
    // 随机阳光眩光参数
    src_radius: 100,
    flare_intensity: 0.6,
    num_flare_circles: 8,
    // 转为灰度参数
    keep_channels: true,
    // 色相饱和度亮度参数
    hsv_hue_shift_limit: 20,
    hsv_sat_shift_limit: 30,
    hsv_val_shift_limit: 20,
    // 随机亮度对比度参数
    rbc_brightness_limit: 0.2,
    rbc_contrast_limit: 0.2,
    // 高斯模糊参数
    gaussian_blur_limit: 7,
    // 运动模糊参数
    motion_blur_limit: 7,
  };

  // 增强参数配置
  const enhanceParams = ref({ ...DEFAULT_ENHANCE_PARAMS });

  const selectedParamTypes = computed(() => {
    const needed = ['5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17'];
    return selectedTypes.value.filter((v) => needed.includes(v));
  });

  // 计算增强数量的最大值
  const maxEnhanceCount = computed(() => {
    const originCount = datasetInfo.value.originFileCount || 1;
    const selectedCount = selectedTypes.value.length || 1;

    if (enhanceMode.value === 'parallel') {
      // 并行遍历：最大值 = 算子数量 × 原图片数
      return selectedCount * originCount;
    } else {
      // 串行叠加：最大值 = 原图片数量
      return originCount;
    }
  });

  // 监听最大值变化，自动调整当前值
  watch(maxEnhanceCount, (newMax) => {
    if (enhanceCount.value > newMax) {
      enhanceCount.value = newMax;
    }
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    // 重置选择
    selectedTypes.value = [];
    enhanceMode.value = 'serial';
    enhanceCount.value = 1;

    // 重置参数为默认值
    enhanceParams.value = { ...DEFAULT_ENHANCE_PARAMS };

    // 获取原始文件数量
    const originFileCount = await maHttp.get(
      {
        url: `datasets/versions/${data.record.id}/originFileCount`,
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );

    setModalProps({ confirmLoading: false });

    // 设置数据集信息
    datasetInfo.value = {
      datasetId: data.record.id,
      name: data.record.name,
      currentVersionName: data.record.currentVersionName,
      originFileCount: originFileCount,
    };
  });

  // 切换选择状态
  const toggleType = (value: string) => {
    const index = selectedTypes.value.indexOf(value);
    if (index > -1) {
      // 已选择，则取消选择
      selectedTypes.value.splice(index, 1);
    } else {
      // 未选择，则添加到末尾
      selectedTypes.value.push(value);
    }
  };

  // 判断是否已选择
  const isSelected = (value: string) => {
    return selectedTypes.value.includes(value);
  };

  // 获取类型的顺序号
  const getTypeOrder = (value: string) => {
    const index = selectedTypes.value.indexOf(value);
    return index > -1 ? index + 1 : '';
  };

  // 根据value获取类型名称
  const getTypeName = (value: string) => {
    const option = enhanceOptions.find((opt) => opt.value === value);
    return option ? option.label : '';
  };

  // 提交
  async function handleSubmit() {
    // 验证至少选择一个
    if (selectedTypes.value.length === 0) {
      message.warning('请至少选择一个增强算子');
      return;
    }

    // 验证增强数量
    if (!enhanceCount.value || enhanceCount.value < 1) {
      message.warning('请输入有效的增强数量');
      return;
    }

    try {
      setModalProps({ confirmLoading: true });

      // 构建任务参数
      const taskParams = {
        enhanceMode: enhanceMode.value,
      };

      // 根据选择的算子添加对应参数
      if (selectedTypes.value?.includes('5')) {
        taskParams.sharpen_strength = enhanceParams.value.sharpen_strength;
        taskParams.sharpen_kernel_size = enhanceParams.value.sharpen_kernel_size;
      }

      if (selectedTypes.value?.includes('6')) {
        taskParams.contrast_limit = enhanceParams.value.contrast_limit;
        taskParams.brightness_limit = enhanceParams.value.brightness_limit;
      }

      if (selectedTypes.value?.includes('7')) {
        taskParams.var_limit = enhanceParams.value.var_limit;
        taskParams.mean = enhanceParams.value.mean;
      }

      if (selectedTypes.value?.includes('8')) {
        taskParams.fog_coef = enhanceParams.value.fog_coef;
        taskParams.alpha_coef = enhanceParams.value.alpha_coef;
      }

      if (selectedTypes.value?.includes('9')) {
        taskParams.drop_length = enhanceParams.value.drop_length;
        taskParams.blur_value = enhanceParams.value.blur_value;
      }

      if (selectedTypes.value?.includes('10')) {
        taskParams.snow_density = enhanceParams.value.snow_density;
        taskParams.snow_size_min = enhanceParams.value.snow_size_min;
        taskParams.snow_size_max = enhanceParams.value.snow_size_max;
        taskParams.snow_alpha = enhanceParams.value.snow_alpha;
      }

      if (selectedTypes.value?.includes('11')) {
        taskParams.num_shadows_upper = enhanceParams.value.num_shadows_upper;
      }

      if (selectedTypes.value?.includes('12')) {
        taskParams.src_radius = enhanceParams.value.src_radius;
        taskParams.flare_intensity = enhanceParams.value.flare_intensity;
        taskParams.num_flare_circles = enhanceParams.value.num_flare_circles;
      }

      if (selectedTypes.value?.includes('13')) {
        taskParams.keep_channels = enhanceParams.value.keep_channels;
      }

      if (selectedTypes.value?.includes('14')) {
        taskParams.hue_shift_limit = enhanceParams.value.hsv_hue_shift_limit;
        taskParams.sat_shift_limit = enhanceParams.value.hsv_sat_shift_limit;
        taskParams.val_shift_limit = enhanceParams.value.hsv_val_shift_limit;
      }

      if (selectedTypes.value?.includes('15')) {
        taskParams.brightness_limit = enhanceParams.value.rbc_brightness_limit;
        taskParams.contrast_limit = enhanceParams.value.rbc_contrast_limit;
      }

      if (selectedTypes.value?.includes('16')) {
        taskParams.blur_limit = enhanceParams.value.gaussian_blur_limit;
      }

      if (selectedTypes.value?.includes('17')) {
        taskParams.blur_limit = enhanceParams.value.motion_blur_limit;
      }
      console.log(datasetInfo.value.datasetId);
      console.log(selectedTypes.value.map((v: string) => Number(v)));
      console.log(enhanceCount.value);
      console.log(JSON.stringify(taskParams));
      await maHttp.post(
        {
          url: 'datasets/enhance',
          params: {
            datasetId: datasetInfo.value.datasetId,
            types: selectedTypes.value.map((v: string) => Number(v)),
            enhanceCount: enhanceCount.value,
            taskParams: JSON.stringify(taskParams),
          },
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      closeModal();
      emits('success');
      message.success('数据增强任务已提交');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style scoped lang="less">
  .enhance-modal-content {
    padding: 10px 0;
  }

  .info-section {
    margin-bottom: 20px;
    padding: 12px 14px;
    background: linear-gradient(135deg, rgba(40, 40, 50, 0.3) 0%, rgba(30, 30, 40, 0.2) 100%);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .info-item {
      display: flex;
      align-items: center;
      margin-bottom: 6px;

      &:last-child {
        margin-bottom: 0;
      }

      .info-label {
        color: #b3b3b3;
        font-size: 12px;
        min-width: 90px;
      }

      .info-value {
        color: #e0e0e0;
        font-size: 12px;
        font-weight: 500;
      }
    }
  }

  .enhance-section {
    .enhance-selection-box {
      padding: 14px 16px;
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.3) 0%, rgba(30, 30, 40, 0.2) 100%);
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.1);

      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 16px;
        font-size: 14px;
        font-weight: 600;
        color: #ffffff;

        .selected-count {
          font-size: 12px;
          color: #4096ff;
          font-weight: normal;
          background: rgba(64, 150, 255, 0.2);
          padding: 2px 6px;
          border-radius: 4px;
          border: 1px solid rgba(64, 150, 255, 0.3);
        }
      }
    }

    .enhance-types-container {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      margin-bottom: 0;
      padding: 4px;
      justify-content: flex-start;

      .enhance-type-chip {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 10px 12px;
        width: 195px;
        flex-shrink: 0;
        background: linear-gradient(135deg, rgba(60, 60, 75, 0.4) 0%, rgba(50, 50, 65, 0.3) 100%);
        border: 2px solid rgba(255, 255, 255, 0.15);
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.25s ease;
        position: relative;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);

        &:hover {
          border-color: rgba(64, 150, 255, 0.5);
          background: linear-gradient(
            135deg,
            rgba(64, 150, 255, 0.18) 0%,
            rgba(64, 150, 255, 0.12) 100%
          );
          box-shadow: 0 4px 12px rgba(64, 150, 255, 0.3);
        }

        &.selected {
          border-color: rgba(64, 150, 255, 0.8);
          border-width: 3px;
          background: linear-gradient(
            135deg,
            rgba(64, 150, 255, 0.35) 0%,
            rgba(64, 150, 255, 0.28) 100%
          );
          box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.25), 0 4px 16px rgba(64, 150, 255, 0.4);

          .type-name {
            color: #ffffff;
            font-weight: 600;
          }

          &:hover {
            border-color: rgba(64, 150, 255, 0.9);
            background: linear-gradient(
              135deg,
              rgba(64, 150, 255, 0.4) 0%,
              rgba(64, 150, 255, 0.32) 100%
            );
            box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.3), 0 4px 18px rgba(64, 150, 255, 0.5);
          }
        }

        .type-order {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
          color: white;
          font-size: 12px;
          font-weight: bold;
          flex-shrink: 0;
          box-shadow: 0 2px 6px rgba(64, 150, 255, 0.5);
        }

        .type-name {
          flex: 1;
          font-size: 13px;
          color: #e0e0e0;
          font-weight: 500;
          text-align: center;
        }

        .detail-icon {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background: rgba(64, 150, 255, 0.3);
          border: 1.5px solid rgba(64, 150, 255, 0.6);
          color: #4096ff;
          font-size: 11px;
          font-weight: bold;
          font-style: italic;
          cursor: pointer;
          flex-shrink: 0;
          transition: all 0.2s ease;

          &:hover {
            background: rgba(64, 150, 255, 0.5);
            border-color: #4096ff;
            color: #ffffff;
            box-shadow: 0 0 8px rgba(64, 150, 255, 0.6);
          }
        }
      }
    }

    .enhance-order-info {
      margin-top: 16px;
      padding: 12px 14px;
      background: linear-gradient(
        135deg,
        rgba(64, 150, 255, 0.1) 0%,
        rgba(64, 150, 255, 0.05) 100%
      );
      border-radius: 8px;
      border: 1px solid rgba(64, 150, 255, 0.2);
      line-height: 1.8;
      min-height: 44px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;

      .order-title {
        color: #4096ff;
        font-weight: 600;
        font-size: 13px;
        margin-right: 8px;
      }

      .order-item {
        display: inline-flex;
        align-items: center;
        margin-right: 8px;
        margin-bottom: 6px;
        padding: 3px 8px;
        background: rgba(64, 150, 255, 0.15);
        border-radius: 5px;
        font-size: 12px;
        color: #e0e0e0;
        border: 1px solid rgba(64, 150, 255, 0.25);
      }
    }

    .empty-tip {
      margin-top: 16px;
      padding: 12px 14px;
      text-align: center;
      color: #faad14;
      font-size: 12px;
      background: rgba(250, 173, 20, 0.1);
      border-radius: 8px;
      border: 1px solid rgba(250, 173, 20, 0.2);
      min-height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .params-section {
    margin-top: 20px;

    .params-box {
      padding: 14px 16px;
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.3) 0%, rgba(30, 30, 40, 0.2) 100%);
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.1);

      .params-title {
        font-size: 14px;
        font-weight: 600;
        color: #ffffff;
        margin-bottom: 16px;
      }

      .param-group {
        margin-bottom: 16px;
        padding: 12px 14px;
        background: linear-gradient(135deg, rgba(60, 60, 75, 0.3) 0%, rgba(50, 50, 65, 0.2) 100%);
        border-radius: 6px;
        border: 1px solid rgba(255, 255, 255, 0.08);

        &:last-child {
          margin-bottom: 0;
        }

        .param-group-title {
          font-size: 13px;
          font-weight: 600;
          color: #4096ff;
          margin-bottom: 12px;
          padding-bottom: 8px;
          border-bottom: 1px solid rgba(64, 150, 255, 0.2);
        }

        .param-item {
          display: flex;
          align-items: center;
          margin-bottom: 12px;

          &:last-child {
            margin-bottom: 0;
          }

          .param-label {
            min-width: 130px;
            font-size: 12px;
            color: #e0e0e0;
            font-weight: 500;
          }
        }
      }
    }
  }

  .strategy-section-wrapper {
    margin-top: 20px;
    padding: 14px 16px;
    background: linear-gradient(135deg, rgba(40, 40, 50, 0.3) 0%, rgba(30, 30, 40, 0.2) 100%);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .strategy-section {
      .strategy-title {
        font-size: 14px;
        font-weight: 600;
        color: #ffffff;
        margin-bottom: 12px;
      }

      .strategy-options {
        display: flex;
        gap: 12px;
        align-items: center;

        .strategy-option {
          flex: 1;
          padding: 10px 16px;
          text-align: center;
          font-size: 13px;
          font-weight: 500;
          color: #ffffff;
          background: linear-gradient(135deg, rgba(60, 60, 75, 0.4) 0%, rgba(50, 50, 65, 0.3) 100%);
          border: 2px solid rgba(255, 255, 255, 0.15);
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.3s ease;
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);

          &:hover {
            border-color: rgba(64, 150, 255, 0.5);
            background: linear-gradient(
              135deg,
              rgba(64, 150, 255, 0.2) 0%,
              rgba(64, 150, 255, 0.15) 100%
            );
            box-shadow: 0 4px 12px rgba(64, 150, 255, 0.25);
          }

          &.selected {
            color: #ffffff;
            background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
            border-color: #4096ff;
            box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.3), 0 4px 16px rgba(64, 150, 255, 0.4);

            &:hover {
              background: linear-gradient(135deg, #5aa7ff 0%, #2787ff 100%);
              box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.4), 0 4px 18px rgba(64, 150, 255, 0.5);
            }
          }
        }
      }
    }
  }

  .enhance-count-wrapper {
    margin-top: 20px;
    padding: 14px 16px;
    background: linear-gradient(135deg, rgba(40, 40, 50, 0.3) 0%, rgba(30, 30, 40, 0.2) 100%);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .enhance-count-section {
      display: flex;
      align-items: center;
      gap: 16px;

      .count-title {
        font-size: 14px;
        font-weight: 600;
        color: #ffffff;
        min-width: 80px;
      }

      .count-input-wrapper {
        display: flex;
        align-items: center;
        gap: 12px;

        .count-tip {
          font-size: 12px;
          color: #b3b3b3;
          white-space: nowrap;
        }
      }
    }
  }

  // 增强详情 Tooltip 样式
  :global(.enhance-detail-tooltip .ant-tooltip-inner) {
    padding: 0 !important;
    background: transparent !important;
    box-shadow: none !important;
  }

  :global(.enhance-detail-tooltip .ant-tooltip-arrow) {
    display: none !important;
  }

  .enhance-tooltip {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border-radius: 10px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
    overflow: hidden;
    min-width: 350px;
    max-width: 450px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .tooltip-header {
      background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
      padding: 14px 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      .tooltip-title {
        color: #ffffff;
        font-size: 15px;
        font-weight: 600;
        margin-bottom: 6px;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
      }

      .tooltip-desc {
        color: rgba(255, 255, 255, 0.9);
        font-size: 12px;
        line-height: 1.5;
      }
    }

    .tooltip-content {
      padding: 16px;

      .image-comparison {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;

        .image-item {
          flex: 1;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;

          .compare-image {
            width: 100%;
            height: 120px;
            object-fit: contain;
            border-radius: 6px;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 8px;
          }

          .image-label {
            font-size: 12px;
            color: #b3b3b3;
            font-weight: 500;
          }
        }

        .arrow-icon {
          font-size: 24px;
          color: #4096ff;
          font-weight: bold;
          flex-shrink: 0;
        }
      }
    }
  }

  // 转为灰度下拉框样式
  .gray-select {
    option {
      background-color: #2a2a2a;
      color: rgba(255, 255, 255, 0.85);
    }
    option:hover,
    option:focus,
    option:checked {
      background-color: #3a3a3a !important;
      color: rgba(255, 255, 255, 0.85) !important;
    }
  }
</style>
