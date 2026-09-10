<template>
  <Dropdown placement="bottomLeft" :overlayClassName="`${prefixCls}-dropdown-overlay`">
    <span :class="[prefixCls, `${prefixCls}--${theme}`]" class="flex items-center">
      <img :class="`${prefixCls}__header`" :src="getUserInfo.avatar" />
      <span :class="`${prefixCls}__info hidden md:block`">
        <span :class="`${prefixCls}__name`" class="truncate">{{ getUserInfo.realName }}</span>
      </span>
    </span>

    <template #overlay>
      <Menu @click="handleMenuClick">
        <MenuItem
          key="userinfo"
          :text="t('layout.header.dropdownItemUserinfo')"
          icon="ion:person-outline"
        />
        <!--        <MenuItem-->
        <!--          v-if="showEngineerBrochure"-->
        <!--          key="engineerBrochure"-->
        <!--          :text="t('layout.header.dropdownItemEngineerBrochure')"-->
        <!--          icon="ion:book-outline"-->
        <!--        />-->
        <!--        <MenuItem-->
        <!--          v-if="showDeveloperBrochure"-->
        <!--          key="developerBrochure"-->
        <!--          :text="t('layout.header.dropdownItemDeveloperBrochure')"-->
        <!--          icon="ion:book-outline"-->
        <!--        />-->
        <MenuItem
          key="userBrochure"
          :text="t('layout.header.dropdownItemUserBrochure')"
          icon="ion:book-outline"
        />
        <MenuItem
          key="videoTutorial"
          :text="t('layout.header.dropdownItemVideoTutorial')"
          icon="ion:videocam-outline"
        />
        <MenuItem
          key="versionInfo"
          :text="`版本号: v${version}`"
          icon="ion:information-circle-outline"
        />
        <MenuDivider v-if="version" />
        <MenuItem
          key="logout"
          :text="t('layout.header.dropdownItemLoginOut')"
          icon="ion:power-outline"
        />
      </Menu>
    </template>
  </Dropdown>

  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="`版本信息`"
    :showOkBtn="false"
    :showCancelBtn="false"
    width="800px"
  >
    <div class="version-details">
      <a-tabs v-model:activeKey="activeKey">
        <a-tab-pane key="current" tab="当前版本">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="版本号">
              {{ currentVersion.version }}
            </a-descriptions-item>
            <a-descriptions-item label="发布日期">
              {{ currentVersion.releaseDate }}
            </a-descriptions-item>
            <a-descriptions-item label="描述">
              {{ currentVersion.description }}
            </a-descriptions-item>
            <a-descriptions-item label="功能特性">
              <div class="features-list">
                <div
                  v-for="(feature, index) in currentVersion.features"
                  :key="index"
                  class="feature-item"
                >
                  {{ feature }}
                </div>
              </div>
            </a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>
        <a-tab-pane key="history" tab="历史版本" force-render>
          <a-collapse v-model:activeKey="activeCollapseKey" ghost>
            <a-collapse-panel
              v-for="(version, index) in historyVersions"
              :key="index"
              :header="`v${version.version} - ${version.releaseDate}`"
            >
              <a-descriptions :column="1" size="small" bordered>
                <a-descriptions-item label="版本号">
                  {{ version.version }}
                </a-descriptions-item>
                <a-descriptions-item label="发布日期">
                  {{ version.releaseDate }}
                </a-descriptions-item>
                <a-descriptions-item label="描述">
                  {{ version.description }}
                </a-descriptions-item>
                <a-descriptions-item label="功能特性">
                  <div class="features-list">
                    <div
                      v-for="(feature, fIndex) in version.features"
                      :key="fIndex"
                      class="feature-item"
                    >
                      {{ feature }}
                    </div>
                  </div>
                </a-descriptions-item>
              </a-descriptions>
            </a-collapse-panel>
          </a-collapse>
        </a-tab-pane>
      </a-tabs>
    </div>
  </BasicModal>
</template>

<script lang="ts">
  // components
  import { Dropdown, Menu, Descriptions, Tabs, Collapse } from 'ant-design-vue';

  import { defineComponent, computed, ref, onMounted } from 'vue';

  import { DOC_URL } from '/@/settings/siteSetting';

  import { useUserStore } from '/@/store/modules/user';
  import { useHeaderSetting } from '/@/hooks/setting/useHeaderSetting';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useModal } from '/@/components/Modal';

  import headerImg from '/@/assets/images/header.jpg';
  import { propTypes } from '/@/utils/propTypes';
  import { openWindow } from '/@/utils';

  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';
  import { useGo } from '/@/hooks/web/usePage';
  import { downloadByUrl } from '/@/utils/file/download';
  import yaml from 'js-yaml';
  import { BasicModal } from '/@/components/Modal';

  type MenuEvent =
    | 'logout'
    | 'doc'
    | 'lock'
    | 'userinfo'
    | 'userBrochure'
    | 'videoTutorial'
    | 'developerBrochure'
    | 'engineerBrochure'
    | 'versionInfo';

  export default defineComponent({
    name: 'UserDropdown',
    components: {
      Dropdown,
      Menu,
      MenuItem: createAsyncComponent(() => import('./DropMenuItem.vue')),
      MenuDivider: Menu.Divider,
      LockAction: createAsyncComponent(() => import('../lock/LockModal.vue')),
      BasicModal,
      [Descriptions.name]: Descriptions,
      ADescriptionsItem: Descriptions.Item,
      [Tabs.name]: Tabs,
      [Tabs.TabPane.name]: Tabs.TabPane,
      [Collapse.name]: Collapse,
      [Collapse.Panel.name]: Collapse.Panel,
    },
    props: {
      theme: propTypes.oneOf(['dark', 'light']),
    },
    setup() {
      const { prefixCls } = useDesign('header-user-dropdown');
      const { t } = useI18n();
      const { getShowDoc, getUseLockPage } = useHeaderSetting();
      const userStore = useUserStore();
      const roles = userStore.getUserInfo.roles;
      const go = useGo();

      // 版本信息相关
      const version = ref('');
      const activeKey = ref('current');
      const activeCollapseKey = ref([] as number[]);

      const currentVersion = ref({
        version: '',
        releaseDate: '',
        features: [] as string[],
        description: '',
      });

      const historyVersions = ref([] as any[]);

      const [registerModal, { openModal }] = useModal();

      const loadVersionInfo = async () => {
        try {
          const response = await fetch('/resource/version.yaml');
          const yamlText = await response.text();
          const data = yaml.load(yamlText) as any;

          // 如果是新的格式（包含versions数组）
          if (data.versions && Array.isArray(data.versions)) {
            currentVersion.value = data.versions[0];
            historyVersions.value = data.versions.slice(1);
            version.value = data.versions[0].version;
          } else {
            // 兼容旧格式
            currentVersion.value = data;
            version.value = data.version;
          }
        } catch (error) {
          console.error('Failed to load version info:', error);
          version.value = '1.0.0'; // 默认版本
        }
      };

      const showVersionInfo = () => {
        openModal(true);
        activeKey.value = 'current';
      };

      onMounted(() => {
        loadVersionInfo();
      });

      const getUserInfo = computed(() => {
        const { realName = '', avatar, desc } = userStore.getUserInfo || {};
        return { realName, avatar: avatar || headerImg, desc };
      });

      const [register, { openModal: openLockModal }] = useModal();
      function handleLock() {
        openLockModal(true);
      }

      // // 方法1：工程手册权限控制
      // const showEngineerBrochure = computed(() => {
      //   if (roles.length === 0) return false;
      //
      //   const roleName = roles[0].name;
      //   return roleName === '管理员' || roleName === '管理人员' || roleName === '工程人员';
      // });
      //
      // // 方法2：开发手册权限控制
      // const showDeveloperBrochure = computed(() => {
      //   if (roles.length === 0) return false;
      //
      //   const roleName = roles[0].name;
      //   return roleName !== '游客' && roleName !== '工程人员';
      // });

      //  login out
      function handleLoginOut() {
        userStore.confirmLoginOut();
      }

      // open doc
      function openDoc() {
        openWindow(DOC_URL);
      }
      const showUserInfo = () => {
        go('/maUser/personalinfo');
      };

      const openUserBrochure = () => {
        window.open('/userBrochure.html', '_blank');
      };

      const openVideoTutorial = () => {
        // 获取当前页面的源
        const baseUrl = window.location.origin;
        // 构建带哈希的完整URL
        const tutorialUrl = `${baseUrl}/#/video-tutorial`;
        window.open(tutorialUrl, '_blank');
      };

      const downloadEngineerBrochure = () => {
        downloadByUrl({
          url: 'zip/engineer_brochure.pdf',
          target: '_self',
        });
      };

      const downloadDeveloperBrochure = () => {
        downloadByUrl({
          url: 'zip/developer_brochure.pdf',
          target: '_self',
        });
      };

      function handleMenuClick(e: { key: MenuEvent }) {
        switch (e.key) {
          case 'logout':
            handleLoginOut();
            break;
          case 'doc':
            openDoc();
            break;
          case 'lock':
            handleLock();
            break;
          case 'userinfo':
            showUserInfo();
            break;
          case 'userBrochure':
            openUserBrochure();
            break;
          case 'videoTutorial':
            openVideoTutorial();
            break;
          case 'developerBrochure':
            downloadDeveloperBrochure();
            break;
          case 'engineerBrochure':
            downloadEngineerBrochure();
            break;
          case 'versionInfo':
            showVersionInfo();
            break;
          default:
            break;
        }
      }

      return {
        prefixCls,
        t,
        getUserInfo,
        handleMenuClick,
        getShowDoc,
        register,
        getUseLockPage,
        // 版本信息相关
        version,
        currentVersion,
        historyVersions,
        activeKey,
        activeCollapseKey,
        registerModal,
        showVersionInfo,
        // showEngineerBrochure,
        // showDeveloperBrochure,
      };
    },
  });
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-header-user-dropdown';

  .@{prefix-cls} {
    height: @header-height;
    padding: 0 0 0 10px;
    overflow: hidden;
    font-size: 12px;
    cursor: pointer;
    align-items: center;

    img {
      width: 24px;
      height: 24px;
      margin-right: 12px;
    }

    &__header {
      border-radius: 50%;
    }

    &__name {
      font-size: 14px;
    }

    &--dark {
      &:hover {
        background-color: @header-dark-bg-hover-color;
      }
    }

    &--light {
      &:hover {
        background-color: @header-light-bg-hover-color;
      }

      .@{prefix-cls}__name {
        color: @text-color-base;
      }

      .@{prefix-cls}__desc {
        color: @header-light-desc-color;
      }
    }

    &-dropdown-overlay {
      .ant-dropdown-menu-item {
        min-width: 160px;
      }
    }
  }

  .version-details {
    padding: 10px 0;

    .features-list {
      .feature-item {
        padding: 2px 0;
      }
    }
  }
</style>
