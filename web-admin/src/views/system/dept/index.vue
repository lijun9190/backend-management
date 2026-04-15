<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title">部门管理</div>
      <el-button v-permission="'system:dept:add'" type="primary" @click="openCreate({ id: 0 })">新增部门</el-button>
    </div>
    <el-table :data="tableData" border row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="deptName" label="部门名称" min-width="180" />
      <el-table-column prop="leader" label="负责人" min-width="120" />
      <el-table-column prop="phone" label="联系电话" min-width="140" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="状态" width="100">
        <template slot-scope="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template slot-scope="{ row }">
          <el-button v-permission="'system:dept:add'" type="text" @click="openCreate(row)">新增下级</el-button>
          <el-button v-permission="'system:dept:edit'" type="text" @click="openEdit(row)">编辑</el-button>
          <el-button v-permission="'system:dept:delete'" type="text" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id ? '编辑部门' : '新增部门'" :visible.sync="dialogVisible" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级 ID" prop="parentId"><el-input-number v-model="form.parentId" :min="0" style="width: 100%;" /></el-form-item>
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%;" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getDeptTree, removeDept, saveDept, updateDept } from '../../../api/dept'

export default {
  data() {
    return {
      tableData: [],
      dialogVisible: false,
      form: { id: null, parentId: 0, deptName: '', leader: '', phone: '', sort: 0, status: 1 },
      rules: {
        parentId: [{ required: true, message: '请输入上级 ID', trigger: 'change' }],
        deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      const res = await getDeptTree()
      this.tableData = res.data || []
    },
    openCreate(row) {
      this.form = { id: null, parentId: row.id || 0, deptName: '', leader: '', phone: '', sort: 0, status: 1 }
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
          await updateDept(this.form.id, this.form)
          this.$message.success('部门更新成功')
        } else {
          await saveDept(this.form)
          this.$message.success('部门创建成功')
        }
        this.dialogVisible = false
        this.fetchList()
      })
    },
    async handleDelete(id) {
      await this.$confirm('确认删除该部门吗？', '提示', { type: 'warning' })
      await removeDept(id)
      this.$message.success('部门删除成功')
      this.fetchList()
    }
  }
}
</script>
