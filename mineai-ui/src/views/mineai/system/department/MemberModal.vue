<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="getTitle"
    @ok="handleSubmit"
    :minHeight="500"
    :minWidth="700"
    :width="'70%'"
  >
    <div class="member-selector">
      <!-- 搜索区域 -->
      <div class="search-section">
        <InputSearch
          v-model:value="searchKeyword"
          placeholder="搜索成员（用户名、邮箱、电话）"
          @search="handleSearch"
          style="width: 100%; margin-bottom: 16px"
          allowClear
        />
      </div>

      <div class="selector-layout">
        <!-- 左侧：可选成员列表（分页显示） -->
        <div class="user-list-panel">
          <Card size="small" title="可选成员" :headStyle="{ fontSize: '14px', fontWeight: '500' }">
            <div class="user-list-container">
              <div class="panel-header">
                <div class="user-count">共 {{ filteredUsers.length }} 名成员</div>
                <div class="pagination-info">
                  第 {{ currentPage }} 页 / 共 {{ totalPages }} 页
                </div>
              </div>

              <div class="user-grid">
                <div
                  v-for="user in currentPageUsers"
                  :key="user.id"
                  class="user-card"
                  :class="{ selected: selectedUserIds.includes(user.id) }"
                  @click="handleUserClick(user)"
                >
                  <Checkbox :checked="selectedUserIds.includes(user.id)" class="user-checkbox" />
                  <div class="user-avatar">
                    {{ getUserAvatar(user.username) }}
                  </div>
                  <div class="user-info">
                    <span class="username">{{ user.username }}</span>
                    <span class="contact">{{ user.email || user.phone || '暂无联系方式' }}</span>
                  </div>
                </div>

                <!-- 空白填充 -->
                <div v-for="i in emptySlots" :key="'empty-' + i" class="user-card empty-slot"></div>
              </div>

              <div v-if="filteredUsers.length === 0" class="empty-state">
                <Empty description="暂无成员数据" image-style="{ height: '60px' }" />
              </div>

              <!-- 分页控件 -->
              <div v-if="totalPages > 1" class="pagination-controls">
                <Button
                  size="small"
                  :disabled="currentPage === 1"
                  @click="goToPage(currentPage - 1)"
                >
                  上一页
                </Button>
                <div class="page-numbers">
                  <span
                    v-for="page in visiblePages"
                    :key="page"
                    class="page-number"
                    :class="{ active: page === currentPage, ellipsis: page === '...' }"
                    @click="page !== '...' && goToPage(page)"
                  >
                    {{ page }}
                  </span>
                </div>
                <Button
                  size="small"
                  :disabled="currentPage === totalPages"
                  @click="goToPage(currentPage + 1)"
                >
                  下一页
                </Button>
              </div>
            </div>
          </Card>
        </div>

        <!-- 右侧：已选成员展示 -->
        <div class="selected-panel">
          <Card size="small" title="已选成员" :headStyle="{ fontSize: '14px', fontWeight: '500' }">
            <div class="selected-container">
              <div class="selected-count">已选择 {{ selectedUsers.length }} 人</div>
              <div class="selected-tags">
                <Tag
                  v-for="user in selectedUsers"
                  :key="user.id"
                  :color="getUserTagColor(user.id)"
                  closable
                  @close="handleRemoveUser(user.id)"
                  class="user-tag"
                >
                  {{ user.username }}
                </Tag>

                <div v-if="selectedUsers.length === 0" class="empty-selected">
                  <span style="color: rgba(255, 255, 255, 0.45)">暂未选择成员</span>
                </div>
              </div>

              <div class="selected-actions">
                <Button
                  type="link"
                  size="small"
                  @click="handleClearAll"
                  :disabled="selectedUsers.length === 0"
                >
                  清空
                </Button>
                <Button
                  type="link"
                  size="small"
                  @click="handleSelectCurrentPage"
                  :disabled="currentPageUsers.length === 0"
                >
                  全选当前页
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  </BasicModal>
</template>

<script setup lang="ts">
  import { BasicModal, useModal, useModalInner } from '/@/components/Modal';
  import { ref, computed, watch } from 'vue';
  import { Card, Checkbox, Tag, InputSearch, Empty, Button } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const { createMessage } = useMessage();

  const emits = defineEmits(['register', 'success']);

  interface User {
    id: string | number;
    username: string;
    email?: string;
    phone?: string;
  }

  const searchKeyword = ref('');
  const allUsers = ref<User[]>([]);
  const selectedUserIds = ref<(string | number)[]>([]);
  const departmentId = ref<string | number>('');
  const isUpdate = ref(false);

  // 分页相关
  const pageSize = 12; // 每页显示12个成员（3x4网格）
  const currentPage = ref(1);

  const tagColors = [
    'pink',
    'red',
    'orange',
    'green',
    'cyan',
    'blue',
    'purple',
    'magenta',
    'volcano',
    'gold',
    'lime',
    'geekblue',
  ];

  const getUserTagColor = (userId: string | number) => {
    const index = Number(userId) % tagColors.length;
    return tagColors[index];
  };

  // 获取用户头像（显示用户名前两个字符）
  const getUserAvatar = (username: string) => {
    return username.substring(0, 2).toUpperCase();
  };

  // 过滤后的用户列表
  const filteredUsers = computed(() => {
    if (!searchKeyword.value) {
      return allUsers.value;
    }

    const keyword = searchKeyword.value.toLowerCase();
    return allUsers.value.filter(
      (user) =>
        user.username.toLowerCase().includes(keyword) ||
        (user.email && user.email.toLowerCase().includes(keyword)) ||
        (user.phone && user.phone.includes(keyword)),
    );
  });

  // 总页数
  const totalPages = computed(() => {
    return Math.ceil(filteredUsers.value.length / pageSize);
  });

  // 当前页的用户
  const currentPageUsers = computed(() => {
    const startIndex = (currentPage.value - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    return filteredUsers.value.slice(startIndex, endIndex);
  });

  // 空白填充格子（用于网格对齐）
  const emptySlots = computed(() => {
    const remainder = currentPageUsers.value.length % 3; // 3列网格
    return remainder === 0 ? 0 : 3 - remainder;
  });

  // 可见的页码（带省略号的分页）
  const visiblePages = computed(() => {
    const pages: (number | string)[] = [];
    const current = currentPage.value;
    const total = totalPages.value;

    if (total <= 7) {
      // 总页数少于7页，显示所有页码
      for (let i = 1; i <= total; i++) {
        pages.push(i);
      }
    } else {
      if (current <= 4) {
        // 当前页在前4页
        pages.push(1, 2, 3, 4, 5, '...', total);
      } else if (current >= total - 3) {
        // 当前页在后4页
        pages.push(1, '...', total - 4, total - 3, total - 2, total - 1, total);
      } else {
        // 当前页在中间
        pages.push(1, '...', current - 1, current, current + 1, '...', total);
      }
    }

    return pages;
  });

  // 已选用户列表
  const selectedUsers = computed(() => {
    return allUsers.value.filter((user) => selectedUserIds.value.includes(user.id));
  });

  const getTitle = computed(() => '选择部门成员');

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    searchKeyword.value = '';
    selectedUserIds.value = [];
    currentPage.value = 1;

    isUpdate.value = !!data?.isUpdate;
    departmentId.value = data?.departmentId || '';

    await loadUsers();

    if (data?.selectedUserIds) {
      selectedUserIds.value = [...data.selectedUserIds];
    }
  });

  // 加载用户数据
  const loadUsers = async () => {
    try {
      setModalProps({ confirmLoading: true });

      const url =
        isUpdate.value && departmentId.value
          ? `departments/${departmentId.value}/department-users`
          : 'departments/unassigned-users';

      const response = await maHttp.get(
        {
          url,
          headers: {
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
      );

      allUsers.value = Array.isArray(response) ? response : [];
    } catch (error) {
      console.error('加载用户数据失败:', error);
      createMessage.error('加载用户数据失败');
      allUsers.value = [];
    } finally {
      setModalProps({ confirmLoading: false });
    }
  };

  // 处理搜索
  const handleSearch = () => {
    currentPage.value = 1; // 搜索时回到第一页
  };

  // 处理用户点击
  const handleUserClick = (user: User) => {
    const index = selectedUserIds.value.indexOf(user.id);
    if (index > -1) {
      selectedUserIds.value.splice(index, 1);
    } else {
      selectedUserIds.value.push(user.id);
    }
  };

  // 移除用户
  const handleRemoveUser = (userId: string | number) => {
    selectedUserIds.value = selectedUserIds.value.filter((id) => id !== userId);
  };

  // 清空选择
  const handleClearAll = () => {
    selectedUserIds.value = [];
  };

  // 全选当前页
  const handleSelectCurrentPage = () => {
    const currentPageUserIds = currentPageUsers.value.map((user) => user.id);
    const newSelectedIds = [...new Set([...selectedUserIds.value, ...currentPageUserIds])];
    selectedUserIds.value = newSelectedIds;
  };

  // 跳转到指定页
  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page;
    }
  };

  // 提交选择
  const handleSubmit = async () => {
    try {
      setModalProps({ confirmLoading: true });

      if (selectedUserIds.value.length === 0) {
        createMessage.warning('请至少选择一个成员');
        return;
      }

      // 获取完整的选中用户信息
      const selectedUsersInfo = allUsers.value.filter((user) =>
        selectedUserIds.value.includes(user.id),
      );

      // 直接关闭弹窗并通过success事件返回完整的用户信息数组
      closeModal();
      createMessage.success(`已选择 ${selectedUsersInfo.length} 名成员`);

      // 触发success事件，传递完整的用户信息数组
      emits('success', selectedUsersInfo);
    } catch (error) {
      console.error('选择成员失败:', error);
      createMessage.error('选择成员失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  };

  // 监听过滤结果变化
  watch(filteredUsers, () => {
    currentPage.value = 1; // 数据变化时回到第一页
  });
</script>

<style scoped lang="less">
  .member-selector {
    padding: 8px 0;
  }

  .selector-layout {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    min-height: 400px;
  }

  .user-list-panel,
  .selected-panel {
    display: flex;
    flex-direction: column;
  }

  .user-list-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .user-count,
    .pagination-info {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.45);
    }
  }

  .user-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-bottom: 16px;
    min-height: 55vh;
    /* 添加固定行高 */
    grid-auto-rows: 100px;
    align-items: start;

    .user-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px 8px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.3s;
      position: relative;
      /* 固定高度 */
      height: 100px;
      box-sizing: border-box;

      /* 其他样式保持不变 */
      &:hover {
        border-color: #1890ff;
        background-color: rgba(255, 255, 255, 0.02);
      }

      &.selected {
        border-color: #1890ff;
        background-color: rgba(24, 144, 255, 0.1);

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 2px;
          background: #1890ff;
        }
      }

      .user-checkbox {
        position: absolute;
        top: 4px;
        right: 4px;
      }

      .user-avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
        font-weight: 600;
        font-size: 14px;
        margin-bottom: 8px;
        flex-shrink: 0; /* 防止头像被压缩 */
      }

      .user-info {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;
        width: 100%;
        min-height: 0; /* 允许内容收缩 */

        .username {
          font-weight: 500;
          color: rgba(255, 255, 255, 0.85);
          font-size: 12px;
          line-height: 1.4;
          margin-bottom: 2px;
          /* 文本溢出处理 */
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 100%;
        }

        .contact {
          font-size: 10px;
          color: rgba(255, 255, 255, 0.45);
          line-height: 1.2;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 100%;
        }
      }

      &.empty-slot {
        visibility: hidden;
        pointer-events: none;
      }
    }
  }

  .empty-state {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 200px;
  }

  .pagination-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    .page-numbers {
      display: flex;
      gap: 4px;

      .page-number {
        padding: 4px 8px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 12px;
        color: rgba(255, 255, 255, 0.65);
        transition: all 0.3s;

        &:hover:not(.active):not(.ellipsis) {
          background-color: rgba(255, 255, 255, 0.1);
          color: rgba(255, 255, 255, 0.85);
        }

        &.active {
          background-color: #1890ff;
          color: white;
        }

        &.ellipsis {
          cursor: default;
          color: rgba(255, 255, 255, 0.3);
        }
      }
    }
  }

  .selected-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .selected-count {
    font-size: 12px;
    color: rgba(255, 255, 255, 0.45);
    margin-bottom: 8px;
  }

  .selected-tags {
    flex: 1;
    overflow-y: auto;
    max-height: 300px;
    padding: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;

    .user-tag {
      margin: 4px;
      border: none;

      :deep(.ant-tag-close-icon) {
        color: rgba(255, 255, 255, 0.65);

        &:hover {
          color: rgba(255, 255, 255, 0.85);
        }
      }
    }

    .empty-selected {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 60px;
    }
  }

  .selected-actions {
    display: flex;
    justify-content: space-between;
    padding-top: 8px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    margin-top: 8px;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .selector-layout {
      grid-template-columns: 1fr;
      gap: 16px;
    }

    .user-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .pagination-controls {
      flex-direction: column;
      gap: 8px;
    }
  }
</style>
