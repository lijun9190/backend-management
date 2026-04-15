<template>
  <div>
    <div class="page-card filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="模块名称"><el-input v-model="query.moduleName" clearable /></el-form-item>
        <el-form-item label="操作人"><el-input v-model="query.operatorName" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchList">查询</el-button></el-form-item>
      </el-form>
    </div>
    <div class="page-card">
      <div class="page-title" style="margin-bottom: 16px;">操作日志</div>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="moduleName" label="模块" min-width="140" />
        <el-table-column prop="operationType" label="操作类型" min-width="120" />
        <el-table-column prop="operatorName" label="操作人" min-width="120" />
        <el-table-column prop="requestMethod" label="请求方法" width="100" />
        <el-table-column prop="requestUri" label="请求地址" min-width="180" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }"><el-tag :type="row.operationStatus === 1 ? 'success' : 'danger'">{{ row.operationStatus === 1 ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="异常信息" min-width="160" />
        <el-table-column prop="operationTime" label="操作时间" min-width="180" />
      </el-table>
      <div style="margin-top: 16px; text-align: right;">
        <el-pagination :current-page.sync="query.current" :page-size.sync="query.size" :total="total" layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </div>
  </div>
</template>

<script>
import { getOperationLogPage } from '../../../api/log'

export default {
  data() {
    return {
      query: { current: 1, size: 10, moduleName: '', operatorName: '' },
      tableData: [],
      total: 0
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      const res = await getOperationLogPage(this.query)
      this.tableData = res.data.records || []
      this.total = res.data.total || 0
    }
  }
}
</script>
