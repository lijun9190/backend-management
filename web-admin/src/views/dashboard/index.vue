<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div class="page-title">Dashboard</div>
      <el-tag type="success">欢迎回来，{{ userInfo.nickname || userInfo.username }}</el-tag>
    </div>

    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value">{{ card.value || 0 }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="24">
        <div class="page-card quick-panel">
          <div class="page-header quick-panel__header">
            <div>
              <div class="page-title">快捷入口</div>
              <div class="quick-panel__subtitle">把常用页面放在首页，减少层层展开菜单的来回操作。</div>
            </div>
          </div>

          <el-empty v-if="quickMenus.length === 0" description="当前角色暂无可直达页面" />

          <div v-else class="quick-grid">
            <button
              v-for="item in quickMenus"
              :key="item.path"
              type="button"
              class="quick-card"
              @click="$router.push(item.path)"
            >
              <div class="quick-card__icon">
                <i :class="item.iconClass"></i>
              </div>
              <div class="quick-card__body">
                <div class="quick-card__title">{{ item.meta.title }}</div>
                <div class="quick-card__desc">{{ item.description }}</div>
              </div>
              <i class="el-icon-arrow-right quick-card__arrow"></i>
            </button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDashboardOverview } from '../../api/dashboard'
import { resolveRoutePath } from '../../utils/route'

export default {
  data() {
    return {
      cards: []
    }
  },
  computed: {
    userInfo() {
      return this.$store.getters.userInfo || {}
    },
    quickMenus() {
      return (this.$store.getters.routes || [])
        .flatMap(route => (route.children || []).map(child => {
          const meta = child.meta || {}
          const title = meta.title || '快捷入口'

          return {
            ...child,
            meta,
            path: resolveRoutePath(child, route.path),
            iconClass: meta.icon || 'el-icon-s-grid',
            description: `进入${title}，处理常用操作`
          }
        }))
        .slice(0, 6)
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      const res = await getDashboardOverview()
      this.cards = res.data.cards || []
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  --quick-border: #dbe7f3;
  --quick-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
  --quick-shadow-hover: 0 22px 40px rgba(37, 99, 235, 0.14);
  --quick-icon-start: #2563eb;
  --quick-icon-end: #38bdf8;
}

.quick-panel {
  overflow: hidden;
}

.quick-panel__header {
  margin-bottom: 20px;
}

.quick-panel__subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.quick-card {
  width: 100%;
  border: 1px solid var(--quick-border);
  border-radius: 18px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  display: flex;
  align-items: center;
  gap: 14px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease,
    background 0.2s ease;
  box-shadow: var(--quick-shadow);
}

.quick-card:hover,
.quick-card:focus {
  transform: translateY(-3px);
  border-color: #93c5fd;
  box-shadow: var(--quick-shadow-hover);
  background: linear-gradient(135deg, #ffffff 0%, #eff6ff 100%);
  outline: none;
}

.quick-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--quick-icon-start) 0%, var(--quick-icon-end) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 22px;
  box-shadow: 0 12px 20px rgba(37, 99, 235, 0.24);
}

.quick-card__body {
  flex: 1;
  min-width: 0;
}

.quick-card__title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.quick-card__desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.quick-card__arrow {
  color: #94a3b8;
  font-size: 16px;
  transition: transform 0.2s ease, color 0.2s ease;
}

.quick-card:hover .quick-card__arrow,
.quick-card:focus .quick-card__arrow {
  color: #2563eb;
  transform: translateX(2px);
}

@media (max-width: 768px) {
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .quick-card {
    padding: 16px;
  }
}
</style>
