<template>
  <div>
    <div class="page-card filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" clearable placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card">
      <div class="page-header">
        <div class="page-title">角色管理</div>
        <el-button v-permission="'system:role:add'" type="primary" @click="openCreate">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="roleCode" label="角色编码" min-width="160" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!hasPermission('system:role:edit')"
              @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="220">
          <template slot-scope="{ row }">
            <el-button v-permission="'system:role:edit'" type="text" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'system:role:assign'" type="text" @click="openAssign(row)">分配权限</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; text-align: right;">
        <el-pagination
          :current-page.sync="query.current"
          :page-size.sync="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList" />
      </div>
    </div>

    <el-dialog :title="form.id ? '编辑角色' : '新增角色'" :visible.sync="dialogVisible" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="分配菜单与按钮权限" :visible.sync="assignDialogVisible" width="620px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        node-key="id"
        :check-strictly="true"
        show-checkbox
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }" />
      <span slot="footer">
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { assignRoleMenus, changeRoleStatus, getRoleMenuIds, getRolePage, saveRole, updateRole } from '../../../api/role'
import { getMenuTree } from '../../../api/menu'

export default {
  data() {
    return {
      loading: false,
      total: 0,
      tableData: [],
      query: { current: 1, size: 10, roleName: '', status: undefined },
      dialogVisible: false,
      assignDialogVisible: false,
      form: { id: null, roleName: '', roleCode: '', status: 1, remark: '' },
      rules: {
        roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
      },
      currentRoleId: null,
      menuTree: []
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    hasPermission(code) {
      return this.$store.getters.isSuperAdmin || (this.$store.getters.permissions || []).includes(code)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getRolePage(this.query)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    async loadMenuTree() {
      if (this.menuTree && this.menuTree.length > 0) {
        return true
      }
      const res = await getMenuTree()
      this.menuTree = res.data || []
      return true
    },
    resetQuery() {
      this.query = { current: 1, size: 10, roleName: '', status: undefined }
      this.fetchList()
    },
    openCreate() {
      this.form = { id: null, roleName: '', roleCode: '', status: 1, remark: '' }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = { ...row }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        if (this.form.id) {
          await updateRole(this.form.id, this.form)
          this.$message.success('角色更新成功')
        } else {
          await saveRole(this.form)
          this.$message.success('角色创建成功')
        }
        this.dialogVisible = false
        this.fetchList()
      })
    },
    async handleStatusChange(row) {
      await changeRoleStatus(row.id, { status: row.status })
      this.$message.success('状态更新成功')
    },
    async openAssign(row) {
      try {
        await this.loadMenuTree()
        this.currentRoleId = row.id
        this.assignDialogVisible = true
        await this.$nextTick()
        const res = await getRoleMenuIds(row.id)
        this.$refs.menuTreeRef.setCheckedKeys(res.data || [])
      } catch (error) {
        this.assignDialogVisible = false
      }
    },
    async submitAssign() {
      const checkedKeys = this.$refs.menuTreeRef.getCheckedKeys()
      await assignRoleMenus(this.currentRoleId, { menuIds: checkedKeys })
      this.$message.success('权限分配成功')
      this.assignDialogVisible = false
    }
  }
}
</script>
