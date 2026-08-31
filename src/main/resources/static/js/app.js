/* admin-pro 前端（Vue 3 + Element Plus，无构建，直接由 Spring Boot 静态托管） */
const { createApp } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

// ---------- API 封装 ----------
const api = axios.create({ baseURL: '' });
api.interceptors.request.use(cfg => {
  const t = localStorage.getItem('adminpro_token');
  if (t) cfg.headers.Authorization = 'Bearer ' + t;
  return cfg;
});
api.interceptors.response.use(
  resp => {
    const d = resp.data;
    if (d && d.code !== undefined && d.code !== 200) {
      if (d.code === 401) { localStorage.clear(); location.reload(); }
      return Promise.reject(new Error(d.message || '请求失败'));
    }
    return d ? d.data : resp.data;
  },
  err => {
    const d = err.response && err.response.data;
    if (d && d.code === 401) { localStorage.clear(); location.reload(); }
    return Promise.reject(new Error((d && d.message) || err.message || '网络错误'));
  }
);
const hasPerm = (perms, p) => perms.includes(p);

// ---------- 登录页 ----------
const LoginView = {
  template: `
    <div class="login-wrap">
      <el-card class="login-card">
        <div class="login-title">admin-pro 后台管理系统</div>
        <div class="login-sub">SpringBoot 3 · MyBatis-Plus · Redis · JWT</div>
        <el-form :model="form" label-width="0" @keyup.enter="submit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large"></el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">登 录</el-button>
          </el-form-item>
          <div style="text-align:center;color:#999;font-size:12px">演示账号：admin / admin123</div>
        </el-form>
      </el-card>
    </div>
  `,
  data: () => ({ form: { username: '', password: '' }, loading: false }),
  methods: {
    async submit() {
      if (!this.form.username || !this.form.password) { ElMessage.warning('请输入用户名和密码'); return; }
      this.loading = true;
      try {
        const d = await api.post('/api/auth/login', this.form);
        localStorage.setItem('adminpro_token', d.token);
        localStorage.setItem('adminpro_user', JSON.stringify(d.user));
        location.reload();
      } catch (e) { ElMessage.error(e.message); }
      finally { this.loading = false; }
    }
  }
};

// ---------- 首页 ----------
const DashboardView = {
  props: ['perms', 'user'],
  data: () => ({ health: null }),
  async created() {
    try { this.health = await api.get('/api/health'); } catch (e) { /* ignore */ }
  },
  template: `
    <div>
      <el-row :gutter="16">
        <el-col :span="8"><el-card class="stat-card"><template #header>👤 当前用户</template>
          <div style="font-size:20px;font-weight:700">{{ user.nickname || user.username }}</div>
          <div style="color:#999;margin-top:8px">用户名：{{ user.username }}</div>
          <div style="color:#999">状态：{{ user.status === 1 ? '正常' : '禁用' }}</div>
        </el-card></el-col>
        <el-col :span="8"><el-card class="stat-card"><template #header>🟢 系统状态</template>
          <div style="font-size:20px;font-weight:700" v-if="health">{{ health.status }}</div>
          <div style="color:#999;margin-top:8px" v-if="health">服务：{{ health.app }}</div>
          <div style="color:#999" v-if="health">时间：{{ health.time }}</div>
          <div v-else style="color:#999">加载中...</div>
        </el-card></el-col>
        <el-col :span="8"><el-card class="stat-card"><template #header>🔑 我的权限（{{ perms.length }}）</template>
          <el-tag v-for="p in perms" :key="p" size="small" style="margin:2px">{{ p }}</el-tag>
        </el-card></el-col>
      </el-row>
      <el-card><template #header>💡 项目说明</template>
        <p>技术栈：SpringBoot 3.2 + MyBatis-Plus + MySQL 8 + Redis + JWT + Redisson + Docker</p>
        <p>功能：JWT 登录鉴权（Redis 登录态）、RBAC 权限（用户-角色-权限）、操作日志（AOP 异步）、文件上传、Redis 缓存与防重复提交</p>
        <p>GitHub：<a href="https://github.com/javaConsumables/admin-pro" target="_blank">https://github.com/javaConsumables/admin-pro</a></p>
      </el-card>
    </div>
  `
};

// ---------- 用户管理 ----------
const UsersView = {
  props: ['perms'],
  data: () => ({
    list: [], total: 0, pageNum: 1, pageSize: 10, username: '',
    dialog: false, form: { username: '', password: '', nickname: '', phone: '' },
    pwdDialog: false, pwdForm: { password: '' }, current: null,
    roleDialog: false, roleIds: [], roleOptions: [], currentUser: null
  }),
  async created() { await this.load(); await this.loadRoles(); },
  methods: {
    async load() {
      const d = await api.get('/api/users/page', { params: { pageNum: this.pageNum, pageSize: this.pageSize, username: this.username || undefined } });
      this.list = d.records; this.total = d.total;
    },
    async loadRoles() { this.roleOptions = await api.get('/api/roles/all'); },
    async create() {
      if (!this.form.username) { ElMessage.warning('用户名不能为空'); return; }
      try {
        await api.post('/api/users', this.form);
        ElMessage.success('创建成功'); this.dialog = false;
        this.form = { username: '', password: '', nickname: '', phone: '' };
        await this.load();
      } catch (e) { ElMessage.error(e.message); }
    },
    async toggle(row) {
      await api.put('/api/users/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 });
      ElMessage.success('已更新'); await this.load();
    },
    openPwd(row) { this.current = row; this.pwdForm = { password: '' }; this.pwdDialog = true; },
    async resetPwd() {
      if (!this.pwdForm.password || this.pwdForm.password.length < 6) { ElMessage.warning('密码至少 6 位'); return; }
      await api.put('/api/users/' + this.current.id + '/password', this.pwdForm);
      ElMessage.success('重置成功'); this.pwdDialog = false;
    },
    openRoles(row) {
      this.currentUser = row;
      this.roleDialog = true;
      this.roleIds = [];
    },
    async saveRoles() {
      await api.put('/api/users/' + this.currentUser.id + '/roles', { roleIds: this.roleIds });
      ElMessage.success('角色已分配'); this.roleDialog = false;
    }
  },
  template: `
    <el-card>
      <template #header>👤 用户管理</template>
      <div class="toolbar">
        <el-input v-model="username" placeholder="按用户名搜索" clearable style="width:220px" @keyup.enter="pageNum=1;load()"></el-input>
        <el-button type="primary" @click="pageNum=1;load()">搜索</el-button>
        <el-button type="success" v-if="hasPerm(perms,'system:user:add')" @click="dialog=true">新增用户</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="false">
        <el-table-column prop="id" label="ID" width="70"></el-table-column>
        <el-table-column prop="username" label="用户名" width="140"></el-table-column>
        <el-table-column prop="nickname" label="昵称" width="120"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="150"></el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170"></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="{row}">
            <el-button size="small" type="primary" plain @click="openRoles(row)" v-if="hasPerm(perms,'system:role:edit')">分配角色</el-button>
            <el-button size="small" @click="openPwd(row)" v-if="hasPerm(perms,'system:user:edit')">重置密码</el-button>
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" plain @click="toggle(row)" v-if="hasPerm(perms,'system:user:edit')">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          :current-page="pageNum" @current-change="n => { pageNum = n; load(); }"></el-pagination>
      </div>

      <el-dialog v-model="dialog" title="新增用户" width="420px">
        <el-form label-width="80px">
          <el-form-item label="用户名"><el-input v-model="form.username" placeholder="必填"></el-input></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" placeholder="默认 123456"></el-input></el-form-item>
          <el-form-item label="昵称"><el-input v-model="form.nickname"></el-input></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone"></el-input></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialog=false">取消</el-button>
          <el-button type="primary" @click="create">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="pwdDialog" title="重置密码" width="400px">
        <el-form label-width="80px">
          <el-form-item label="新密码"><el-input v-model="pwdForm.password" type="password" show-password></el-input></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="pwdDialog=false">取消</el-button>
          <el-button type="primary" @click="resetPwd">确认</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="roleDialog" title="分配角色" width="420px">
        <el-select v-model="roleIds" multiple placeholder="选择角色" style="width:100%">
          <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName + ' (' + r.roleCode + ')'" :value="r.id"></el-option>
        </el-select>
        <template #footer>
          <el-button @click="roleDialog=false">取消</el-button>
          <el-button type="primary" @click="saveRoles">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  `
};

// ---------- 角色管理 ----------
const RolesView = {
  props: ['perms'],
  data: () => ({
    list: [], total: 0, pageNum: 1, pageSize: 10, roleName: '',
    dialog: false, isEdit: false, form: { id: null, roleName: '', roleCode: '', remark: '' },
    menuDialog: false, currentRole: null, menuTree: [], checkedMenuIds: []
  }),
  async created() { await this.load(); },
  methods: {
    async load() {
      const d = await api.get('/api/roles/page', { params: { pageNum: this.pageNum, pageSize: this.pageSize, roleName: this.roleName || undefined } });
      this.list = d.records; this.total = d.total;
    },
    openCreate() { this.isEdit = false; this.form = { id: null, roleName: '', roleCode: '', remark: '' }; this.dialog = true; },
    openEdit(row) { this.isEdit = true; this.form = { id: row.id, roleName: row.roleName, roleCode: row.roleCode, remark: row.remark }; this.dialog = true; },
    async save() {
      if (!this.form.roleName || !this.form.roleCode) { ElMessage.warning('角色名和编码必填'); return; }
      try {
        if (this.isEdit) await api.put('/api/roles/' + this.form.id, this.form);
        else await api.post('/api/roles', this.form);
        ElMessage.success('保存成功'); this.dialog = false; await this.load();
      } catch (e) { ElMessage.error(e.message); }
    },
    async remove(row) {
      try {
        await ElMessageBox.confirm('确认删除角色「' + row.roleName + '」？', '提示', { type: 'warning' });
        await api.delete('/api/roles/' + row.id);
        ElMessage.success('已删除'); await this.load();
      } catch (e) { if (e !== 'cancel' && e.message) ElMessage.error(e.message); }
    },
    async openMenus(row) {
      this.currentRole = row;
      this.menuTree = await api.get('/api/menus/tree');
      this.checkedMenuIds = [];
      this.menuDialog = true;
      this.$nextTick(() => { if (this.$refs.menuTree) this.$refs.menuTree.setCheckedKeys([]); });
    },
    async saveMenus() {
      const tree = this.$refs.menuTree;
      const ids = [...tree.getCheckedKeys(), ...tree.getHalfCheckedKeys()];
      await api.put('/api/roles/' + this.currentRole.id + '/menus', { menuIds: ids });
      ElMessage.success('权限已更新'); this.menuDialog = false;
    }
  },
  template: `
    <el-card>
      <template #header>🎭 角色管理</template>
      <div class="toolbar">
        <el-input v-model="roleName" placeholder="按角色名搜索" clearable style="width:220px" @keyup.enter="pageNum=1;load()"></el-input>
        <el-button type="primary" @click="pageNum=1;load()">搜索</el-button>
        <el-button type="success" v-if="hasPerm(perms,'system:role:add')" @click="openCreate">新增角色</el-button>
      </div>
      <el-table :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70"></el-table-column>
        <el-table-column prop="roleName" label="角色名" width="160"></el-table-column>
        <el-table-column prop="roleCode" label="编码" width="140"></el-table-column>
        <el-table-column prop="remark" label="备注"></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" min-width="260">
          <template #default="{row}">
            <el-button size="small" type="primary" plain @click="openMenus(row)" v-if="hasPerm(perms,'system:role:edit')">分配权限</el-button>
            <el-button size="small" @click="openEdit(row)" v-if="hasPerm(perms,'system:role:edit') && row.roleCode !== 'admin'">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)" v-if="hasPerm(perms,'system:role:delete') && row.roleCode !== 'admin'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          :current-page="pageNum" @current-change="n => { pageNum = n; load(); }"></el-pagination>
      </div>

      <el-dialog v-model="dialog" :title="isEdit ? '编辑角色' : '新增角色'" width="420px">
        <el-form label-width="80px">
          <el-form-item label="角色名"><el-input v-model="form.roleName"></el-input></el-form-item>
          <el-form-item label="编码"><el-input v-model="form.roleCode" :disabled="isEdit"></el-input></el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark"></el-input></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialog=false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="menuDialog" :title="'分配权限 - ' + (currentRole ? currentRole.roleName : '')" width="460px">
        <el-tree ref="menuTree" :data="menuTree" show-checkbox node-key="id" default-expand-all
          :props="{ label: 'menuName', children: 'children' }"></el-tree>
        <template #footer>
          <el-button @click="menuDialog=false">取消</el-button>
          <el-button type="primary" @click="saveMenus">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  `
};

// ---------- 菜单/权限 ----------
const MenusView = {
  props: ['perms'],
  data: () => ({
    tree: [], dialog: false, isEdit: false,
    form: { id: null, parentId: 0, menuName: '', perms: '', menuType: 1, path: '', sort: 0 },
    parentOptions: []
  }),
  async created() { await this.load(); },
  methods: {
    async load() { this.tree = await api.get('/api/menus/tree'); },
    flatten(nodes, depth) {
      let out = [];
      (nodes || []).forEach(n => { out.push({ id: n.id, label: (depth ? '- '.repeat(depth) : '') + n.menuName }); out = out.concat(this.flatten(n.children, depth + 1)); });
      return out;
    },
    openCreate(parentId) {
      this.isEdit = false;
      this.parentOptions = this.flatten(this.tree, 0);
      this.form = { id: null, parentId: parentId || 0, menuName: '', perms: '', menuType: 1, path: '', sort: 0 };
      this.dialog = true;
    },
    openEdit(row) {
      this.isEdit = true;
      this.parentOptions = this.flatten(this.tree, 0);
      this.form = { id: row.id, parentId: row.parentId, menuName: row.menuName, perms: row.perms, menuType: row.menuType, path: row.path, sort: row.sort };
      this.dialog = true;
    },
    async save() {
      if (!this.form.menuName) { ElMessage.warning('菜单名不能为空'); return; }
      try {
        if (this.isEdit) await api.put('/api/menus/' + this.form.id, this.form);
        else await api.post('/api/menus', this.form);
        ElMessage.success('保存成功'); this.dialog = false; await this.load();
      } catch (e) { ElMessage.error(e.message); }
    },
    async remove(row) {
      try {
        await ElMessageBox.confirm('确认删除「' + row.menuName + '」？', '提示', { type: 'warning' });
        await api.delete('/api/menus/' + row.id);
        ElMessage.success('已删除'); await this.load();
      } catch (e) { if (e !== 'cancel' && e.message) ElMessage.error(e.message); }
    }
  },
  template: `
    <el-card>
      <template #header>📁 权限菜单</template>
      <div class="toolbar">
        <el-button type="success" v-if="hasPerm(perms,'system:menu:add')" @click="openCreate(0)">新增根菜单</el-button>
      </div>
      <el-table :data="tree" row-key="id" border default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="名称" width="220"></el-table-column>
        <el-table-column prop="perms" label="权限标识" width="220"></el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{row}"><el-tag size="small" :type="row.menuType === 1 ? 'primary' : 'success'">{{ row.menuType === 1 ? '菜单' : '按钮' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="path" label="路径"></el-table-column>
        <el-table-column prop="sort" label="排序" width="70"></el-table-column>
        <el-table-column label="操作" min-width="240">
          <template #default="{row}">
            <el-button size="small" type="primary" plain @click="openCreate(row.id)" v-if="hasPerm(perms,'system:menu:add')">添加子项</el-button>
            <el-button size="small" @click="openEdit(row)" v-if="hasPerm(perms,'system:menu:edit')">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)" v-if="hasPerm(perms,'system:menu:delete')">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialog" :title="isEdit ? '编辑菜单' : '新增菜单'" width="460px">
        <el-form label-width="80px">
          <el-form-item label="父级">
            <el-select v-model="form.parentId" style="width:100%">
              <el-option label="根菜单" :value="0"></el-option>
              <el-option v-for="p in parentOptions" :key="p.id" :label="p.label" :value="p.id"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="名称"><el-input v-model="form.menuName"></el-input></el-form-item>
          <el-form-item label="权限标识"><el-input v-model="form.perms" placeholder="如 system:user:list"></el-input></el-form-item>
          <el-form-item label="类型">
            <el-radio-group v-model="form.menuType"><el-radio :label="1">菜单</el-radio><el-radio :label="2">按钮</el-radio></el-radio-group>
          </el-form-item>
          <el-form-item label="路径"><el-input v-model="form.path"></el-input></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0"></el-input-number></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialog=false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  `
};

// ---------- 操作日志 ----------
const LogsView = {
  props: ['perms'],
  data: () => ({ list: [], total: 0, pageNum: 1, pageSize: 10, operation: '' }),
  async created() { await this.load(); },
  methods: {
    async load() {
      const d = await api.get('/api/logs/page', { params: { pageNum: this.pageNum, pageSize: this.pageSize, operation: this.operation || undefined } });
      this.list = d.records; this.total = d.total;
    }
  },
  template: `
    <el-card>
      <template #header>📝 操作日志</template>
      <div class="toolbar">
        <el-input v-model="operation" placeholder="按操作类型搜索" clearable style="width:220px" @keyup.enter="pageNum=1;load()"></el-input>
        <el-button type="primary" @click="pageNum=1;load()">搜索</el-button>
      </div>
      <el-table :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70"></el-table-column>
        <el-table-column prop="operation" label="操作" width="140"></el-table-column>
        <el-table-column prop="username" label="用户" width="110"></el-table-column>
        <el-table-column prop="method" label="方法" width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="params" label="参数" show-overflow-tooltip></el-table-column>
        <el-table-column prop="ip" label="IP" width="130"></el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90"></el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}"><el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170"></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          :current-page="pageNum" @current-change="n => { pageNum = n; load(); }"></el-pagination>
      </div>
    </el-card>
  `
};

// ---------- 文件管理 ----------
const FilesView = {
  props: ['perms'],
  data: () => ({ list: [], total: 0, pageNum: 1, pageSize: 10, uploading: false }),
  async created() { await this.load(); },
  methods: {
    async load() {
      const d = await api.get('/api/files/page', { params: { pageNum: this.pageNum, pageSize: this.pageSize } });
      this.list = d.records; this.total = d.total;
    },
    async customUpload(option) {
      this.uploading = true;
      const fd = new FormData();
      fd.append('file', option.file);
      try {
        await api.post('/api/files/upload', fd);
        ElMessage.success('上传成功');
        await this.load();
      } catch (e) { ElMessage.error(e.message); }
      finally { this.uploading = false; option.onSuccess(); }
    },
    async download(row) {
      try {
        const resp = await axios.get('/api/files/download/' + row.id, { responseType: 'blob', headers: { Authorization: 'Bearer ' + localStorage.getItem('adminpro_token') } });
        const url = URL.createObjectURL(resp.data);
        const a = document.createElement('a');
        a.href = url; a.download = row.originalName || 'file';
        a.click(); setTimeout(() => URL.revokeObjectURL(url), 3000);
      } catch (e) { ElMessage.error('下载失败'); }
    },
    async preview(row) {
      try {
        const resp = await axios.get('/api/files/preview/' + row.id, { responseType: 'blob', headers: { Authorization: 'Bearer ' + localStorage.getItem('adminpro_token') } });
        const url = URL.createObjectURL(resp.data);
        window.open(url, '_blank');
      } catch (e) { ElMessage.error('预览失败'); }
    },
    fmtSize(s) {
      if (s == null) return '-';
      if (s < 1024) return s + ' B';
      if (s < 1048576) return (s / 1024).toFixed(1) + ' KB';
      return (s / 1048576).toFixed(1) + ' MB';
    }
  },
  template: `
    <el-card>
      <template #header>📎 文件管理</template>
      <div class="toolbar">
        <el-upload :show-file-list="false" :http-request="customUpload" accept="*">
          <el-button type="success" :loading="uploading" v-if="hasPerm(perms,'system:file:upload')">上传文件</el-button>
        </el-upload>
      </div>
      <el-table :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70"></el-table-column>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="fileType" label="类型" width="140" show-overflow-tooltip></el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{row}">{{ fmtSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderId" label="上传人ID" width="100"></el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170"></el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{row}">
            <el-button size="small" type="primary" plain @click="download(row)">下载</el-button>
            <el-button size="small" @click="preview(row)">预览</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize"
          :current-page="pageNum" @current-change="n => { pageNum = n; load(); }"></el-pagination>
      </div>
    </el-card>
  `
};

// ---------- 根组件 ----------
const App = {
  components: { LoginView, DashboardView, UsersView, RolesView, MenusView, LogsView, FilesView },
  data: () => ({
    token: localStorage.getItem('adminpro_token'),
    user: JSON.parse(localStorage.getItem('adminpro_user') || 'null'),
    perms: [],
    currentView: 'dashboard'
  }),
  computed: {
    menus() {
      const has = p => hasPerm(this.perms, p);
      return [
        { key: 'dashboard', label: '首页', icon: '📊', show: true },
        { key: 'users', label: '用户管理', icon: '👤', show: has('system:user:list') },
        { key: 'roles', label: '角色管理', icon: '🎭', show: has('system:role:list') },
        { key: 'menus', label: '权限菜单', icon: '📁', show: has('system:menu:list') },
        { key: 'logs', label: '操作日志', icon: '📝', show: has('system:log:list') },
        { key: 'files', label: '文件管理', icon: '📎', show: has('system:file:list') }
      ].filter(m => m.show);
    }
  },
  async created() {
    if (this.token) {
      try { this.perms = await api.get('/api/auth/permissions'); }
      catch (e) { localStorage.clear(); location.reload(); }
    }
  },
  methods: {
    logout() {
      api.post('/api/auth/logout').catch(() => {}).finally(() => { localStorage.clear(); location.reload(); });
    }
  },
  template: `
    <div>
      <login-view v-if="!token" />
      <el-container v-else class="layout">
        <el-aside width="210px">
          <div class="logo">🛡️ admin-pro</div>
          <el-menu :default-active="currentView" @select="k => currentView = k">
            <el-menu-item v-for="m in menus" :key="m.key" :index="m.key">{{ m.icon }} {{ m.label }}</el-menu-item>
          </el-menu>
        </el-aside>
        <el-container>
          <el-header class="header">
            <span class="title">admin-pro 后台管理系统</span>
            <el-dropdown @command="c => c === 'logout' && logout()">
              <span class="user">👤 {{ user ? (user.nickname || user.username) : '' }} ▼</span>
              <template #dropdown>
                <el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-header>
          <el-main>
            <dashboard-view v-if="currentView === 'dashboard'" :perms="perms" :user="user" />
            <users-view v-else-if="currentView === 'users'" :perms="perms" />
            <roles-view v-else-if="currentView === 'roles'" :perms="perms" />
            <menus-view v-else-if="currentView === 'menus'" :perms="perms" />
            <logs-view v-else-if="currentView === 'logs'" :perms="perms" />
            <files-view v-else-if="currentView === 'files'" :perms="perms" />
          </el-main>
        </el-container>
      </el-container>
    </div>
  `
};

createApp(App).use(ElementPlus, { locale: ElementPlusLocaleZhCn }).mount('#app');
