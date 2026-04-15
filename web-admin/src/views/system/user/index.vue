<template>
  <div>
    <div class="page-card filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="用户名">
          <el-input v-model="query.username" clearable placeholder="请输入用户名" />
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
        <div class="page-title">用户管理</div>
        <el-button v-permission="'system:user:add'" type="primary" @click="openCreate">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column prop="roleNames" label="角色" min-width="180">
          <template slot-scope="{ row }">{{ (row.roleNames || []).join(' / ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="!hasPermission('system:user:edit')"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template slot-scope="{ row }">
            <el-button v-permission="'system:user:edit'" type="text" @click="openEdit(row.id)">编辑</el-button>
            <el-button v-permission="'system:user:assign-role'" type="text" @click="openAssignRole(row)">分配角色</el-button>
            <el-button v-permission="'system:user:reset-password'" type="text" @click="openResetPassword(row)">重置密码</el-button>
            <el-button v-permission="'system:user:delete'" type="text" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; text-align: right;">
        <el-pagination
          :current-page.sync="query.current"
          :page-size.sync="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <el-dialog :title="form.id ? '编辑用户' : '新增用户'" :visible.sync="dialogVisible" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-input-number v-model="form.deptId" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col v-if="!form.id" :span="12">
            <el-form-item label="初始密码">
              <el-input v-model="form.password" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="分配角色" :visible.sync="roleDialogVisible" width="480px">
      <el-checkbox-group v-model="roleForm.roleIds">
        <el-checkbox v-for="item in roleOptions" :key="item.id" :label="item.id">{{ item.roleName }}</el-checkbox>
      </el-checkbox-group>
      <span slot="footer">
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRole">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog title="重置密码" :visible.sync="passwordDialogVisible" width="420px">
      <el-form :model="passwordForm" label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.password" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResetPassword">确认重置</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  assignUserRoles,
  changeUserStatus,
  getUserDetail,
  getUserPage,
  resetUserPassword,
  saveUser,
  updateUser
} from '../../../api/user'
import { removeUser } from '../../../api/user'
import { getRoleOptions } from '../../../api/role'

export default {
  data() {
    return {
      loading: false,
      total: 0,
      tableData: [],
      query: { current: 1, size: 10, username: '', status: undefined },
      dialogVisible: false,
      roleDialogVisible: false,
      passwordDialogVisible: false,
      form: {
        id: null,
        deptId: 1,
        username: '',
        nickname: '',
        realName: '',
        phone: '',
        email: '',
        password: 'Admin@123456',
        status: 1
      },
      rules: {
        deptId: [{ required: true, message: '请输入部门 ID', trigger: 'change' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
      },
      currentUserId: null,
      currentAssignRow: null,
      roleOptions: [],
      roleForm: { roleIds: [] },
      passwordForm: { id: null, password: 'Admin@123456' }
    }
  },
  created() {
    this.fetchList()
    this.loadRoleOptions()
  },
  methods: {
    hasPermission(code) {
      return this.$store.getters.isSuperAdmin || (this.$store.getters.permissions || []).includes(code)
    },
    async fetchList() {
      this.loading = true
      try {
        const res = await getUserPage(this.query)
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      } finally {
        this.loading = false
      }
    },
    async loadRoleOptions() {
      const res = await getRoleOptions()
      this.roleOptions = res.data || []
    },
    resetQuery() {
      this.query = { current: 1, size: 10, username: '', status: undefined }
      this.fetchList()
    },
    openCreate() {
      this.form = {
        id: null,
        deptId: 1,
        username: '',
        nickname: '',
        realName: '',
        phone: '',
        email: '',
        password: 'Admin@123456',
        status: 1
      }
      this.dialogVisible = true
    },
    async openEdit(id) {
      const res = await getUserDetail(id)
      this.form = { ...res.data, password: '' }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        if (this.form.id) {
          await updateUser(this.form.id, this.form)
          this.$message.success('用户更新成功')
        } else {
          await saveUser(this.form)
          this.$message.success('用户创建成功')
        }
        this.dialogVisible = false
        this.fetchList()
      })
    },
    async handleStatusChange(row) {
      await changeUserStatus(row.id, { status: row.status })
      this.$message.success('状态更新成功')
    },
    async openAssignRole(row) {
      this.currentUserId = row.id
      this.currentAssignRow = row
      const res = await getUserDetail(row.id)
      this.roleForm = { roleIds: res.data.roleIds || [] }
      this.roleDialogVisible = true
    },
    async submitAssignRole() {
      await assignUserRoles(this.currentUserId, this.roleForm)
      if (this.currentAssignRow) {
        this.currentAssignRow.roleNames = this.roleOptions
          .filter(item => this.roleForm.roleIds.includes(item.id))
          .map(item => item.roleName)
      }
      this.$message.success('角色分配成功')
      this.roleDialogVisible = false
      await this.fetchList()
    },
    openResetPassword(row) {
      this.passwordForm = { id: row.id, password: 'Admin@123456' }
      this.passwordDialogVisible = true
    },
    async submitResetPassword() {
      await resetUserPassword(this.passwordForm.id, { password: this.passwordForm.password })
      this.$message.success('密码已重置')
      this.passwordDialogVisible = false
    },
    async handleDelete(row) {
      await this.$confirm(`确认删除用户 ${row.username} 吗？`, '提示', { type: 'warning' })
      await removeUser(row.id)
      this.$message.success('用户删除成功')
      if (this.tableData.length === 1 && this.query.current > 1) {
        this.query.current -= 1
      }
      await this.fetchList()
    }
  }
}
</script>
