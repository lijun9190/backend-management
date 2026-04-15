<template>
  <div class="page-card">
    <div class="page-header">
      <div class="page-title">菜单管理</div>
      <el-button v-permission="'system:menu:add'" type="primary" @click="openCreate({ id: 0 })">新增菜单</el-button>
    </div>

    <el-table :data="tableData" border row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="menuName" label="菜单名称" min-width="180" />
      <el-table-column prop="menuType" label="类型" width="100" />
      <el-table-column prop="path" label="路径" min-width="160" />
      <el-table-column prop="component" label="组件" min-width="180" />
      <el-table-column prop="permissionCode" label="权限码" min-width="180" />
      <el-table-column label="显示" width="90">
        <template slot-scope="{ row }">
          <el-tag :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template slot-scope="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template slot-scope="{ row }">
          <el-button v-permission="'system:menu:add'" type="text" @click="openCreate(row)">新增下级</el-button>
          <el-button v-permission="'system:menu:edit'" type="text" @click="openEdit(row.id)">编辑</el-button>
          <el-button v-permission="'system:menu:delete'" type="text" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.id ? '编辑菜单' : '新增菜单'" :visible.sync="dialogVisible" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="上级 ID" prop="parentId"><el-input-number v-model="form.parentId" :min="0" style="width: 100%;" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="菜单名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="菜单类型" prop="menuType"><el-select v-model="form.menuType" style="width: 100%;"><el-option label="目录" value="CATALOG" /><el-option label="菜单" value="MENU" /><el-option label="按钮" value="BUTTON" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%;" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="路由路径"><el-input v-model="form.path" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="组件路径"><el-input v-model="form.component" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="路由名称"><el-input v-model="form.routeName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="图标"><el-input v-model="form.icon" placeholder="如 el-icon-user" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="权限码"><el-input v-model="form.permissionCode" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="显示"><el-switch v-model="form.visible" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getMenuDetail, getMenuTree, removeMenu, saveMenu, updateMenu } from '../../../api/menu'

export default {
  data() {
    return {
      tableData: [],
      dialogVisible: false,
      form: { id: null, parentId: 0, menuName: '', menuType: 'MENU', path: '', component: '', routeName: '', icon: 'el-icon-menu', sort: 0, permissionCode: '', visible: 1, status: 1 },
      rules: {
        parentId: [{ required: true, message: '请输入上级 ID', trigger: 'change' }],
        menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
        menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
      }
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      const res = await getMenuTree()
      this.tableData = res.data || []
    },
    openCreate(row) {
      this.form = { id: null, parentId: row.id || 0, menuName: '', menuType: 'MENU', path: '', component: '', routeName: '', icon: 'el-icon-menu', sort: 0, permissionCode: '', visible: 1, status: 1 }
      this.dialogVisible = true
    },
    async openEdit(id) {
      const res = await getMenuDetail(id)
      this.form = { ...res.data }
      this.dialogVisible = true
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        if (this.form.id) {
          await updateMenu(this.form.id, this.form)
          this.$message.success('菜单更新成功')
        } else {
          await saveMenu(this.form)
          this.$message.success('菜单创建成功')
        }
        this.dialogVisible = false
        this.fetchList()
      })
    },
    async handleDelete(id) {
      await this.$confirm('确认删除该菜单吗？', '提示', { type: 'warning' })
      await removeMenu(id)
      this.$message.success('菜单删除成功')
      this.fetchList()
    }
  }
}
</script>
