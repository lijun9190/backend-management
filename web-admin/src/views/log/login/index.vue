<template>
  <div>
    <div class="page-card filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="用户名"><el-input v-model="query.username" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.loginStatus" clearable placeholder="全部">
            <el-option :value="1" label="成功" />
            <el-option :value="0" label="失败" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="fetchList">查询</el-button></el-form-item>
      </el-form>
    </div>
    <div class="page-card">
      <div class="page-title" style="margin-bottom: 16px;">登录日志</div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="loginIp" label="登录 IP" min-width="140" />
        <el-table-column prop="browser" label="浏览器" min-width="120" />
        <el-table-column prop="os" label="系统" min-width="120" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }"><el-tag :type="row.loginStatus === 1 ? 'success' : 'danger'">{{ row.loginStatus === 1 ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="message" label="提示信息" min-width="160" />
        <el-table-column prop="loginTime" label="登录时间" min-width="180" />
      </el-table>
      <div style="margin-top: 16px; text-align: right;">
        <el-pagination :current-page.sync="query.current" :page-size.sync="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </div>
  </div>
</template>

<script>
import { getLoginLogPage } from '../../../api/log'

export default {
  data() {
    return {
      query: { current: 1, size: 10, username: '', loginStatus: undefined },
      tableData: [],
      total: 0
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      const res = await getLoginLogPage(this.query)
      this.tableData = res.data.records || []
      this.total = res.data.total || 0
    }
  }
}
</script>
