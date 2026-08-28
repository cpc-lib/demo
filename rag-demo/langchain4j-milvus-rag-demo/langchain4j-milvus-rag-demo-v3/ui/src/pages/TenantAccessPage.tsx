import {
  DeleteOutlined,
  EditOutlined,
  KeyOutlined,
  ReloadOutlined,
  SaveOutlined,
  TeamOutlined,
  UserAddOutlined,
  UserSwitchOutlined
} from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import { tenantContextStorage, type TenantHeaderContext } from '../api/http';
import type {
  CurrentUserResponse,
  RagKnowledgeBase,
  RagKnowledgeBaseMember,
  RagTenantQuota,
  SysOperationAuditLog,
  SysRole,
  SysTenant,
  SysUser,
  TenantDeletionTaskDetail,
  SystemTenantRequest,
  SystemUserRequest
} from '../types';
import { getErrorMessage } from '../utils/message';
import { canUsePlatformTenantAdmin, getTenantAccessTabKeys } from '../utils/tenantAccess';

const { Text } = Typography;

type MemberFormValues = {
  userId: string;
  role: string;
  permissionTags?: string;
};

type ImpersonationFormValues = {
  targetTenantId: number;
  reason: string;
  ttlMinutes?: number;
};

type ResetPasswordFormValues = {
  password: string;
  mustChangePassword?: boolean;
};

type UserRolesFormValues = {
  roleCodes: string[];
};

const DEFAULT_ROLE_OPTIONS = ['TENANT_ADMIN', 'KB_OWNER', 'KB_EDITOR', 'KB_READER'];

export default function TenantAccessPage() {
  const { message } = App.useApp();
  const [contextForm] = Form.useForm<TenantHeaderContext>();
  const [memberForm] = Form.useForm<MemberFormValues>();
  const [quotaForm] = Form.useForm<RagTenantQuota>();
  const [impersonationForm] = Form.useForm<ImpersonationFormValues>();
  const [deletionForm] = Form.useForm<{ tenantId: number; reason?: string; executionMode?: 'DRY_RUN' | 'EXECUTE' }>();
  const [tenantForm] = Form.useForm<SystemTenantRequest>();
  const [userForm] = Form.useForm<SystemUserRequest>();
  const [resetPasswordForm] = Form.useForm<ResetPasswordFormValues>();
  const [userRolesForm] = Form.useForm<UserRolesFormValues>();

  const [currentUser, setCurrentUser] = useState<CurrentUserResponse>();
  const [visibleTenants, setVisibleTenants] = useState<SysTenant[]>([]);
  const [systemTenants, setSystemTenants] = useState<SysTenant[]>([]);
  const [systemUsers, setSystemUsers] = useState<SysUser[]>([]);
  const [systemRoles, setSystemRoles] = useState<SysRole[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<RagKnowledgeBase[]>([]);
  const [members, setMembers] = useState<RagKnowledgeBaseMember[]>([]);
  const [auditLogs, setAuditLogs] = useState<SysOperationAuditLog[]>([]);
  const [quota, setQuota] = useState<RagTenantQuota>();
  const [deletionTask, setDeletionTask] = useState<TenantDeletionTaskDetail>();

  const [selectedTenantId, setSelectedTenantId] = useState<number>();
  const [selectedKnowledgeBaseId, setSelectedKnowledgeBaseId] = useState<number>();
  const [editingTenant, setEditingTenant] = useState<SysTenant>();
  const [editingUser, setEditingUser] = useState<SysUser>();
  const [resetUser, setResetUser] = useState<SysUser>();
  const [roleUser, setRoleUser] = useState<SysUser>();
  const [tenantDrawerOpen, setTenantDrawerOpen] = useState(false);
  const [userDrawerOpen, setUserDrawerOpen] = useState(false);
  const [resetDrawerOpen, setResetDrawerOpen] = useState(false);
  const [rolesDrawerOpen, setRolesDrawerOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [memberLoading, setMemberLoading] = useState(false);
  const [adminLoading, setAdminLoading] = useState(false);

  const activeTenantId = useMemo(() => {
    if (currentUser?.tenantId != null) {
      return currentUser.tenantId;
    }
    if (selectedTenantId != null) {
      return selectedTenantId;
    }
    const storedTenantId = tenantContextStorage.read().tenantId;
    return storedTenantId ? Number(storedTenantId) : undefined;
  }, [currentUser, selectedTenantId]);
  const platformTenantAdmin = canUsePlatformTenantAdmin(currentUser);
  const tenantAccessTabKeys = getTenantAccessTabKeys(currentUser);
  const roleOptions = useMemo(() => {
    const codes = new Set([...DEFAULT_ROLE_OPTIONS, ...systemRoles.map((role) => role.roleCode).filter(Boolean)]);
    return Array.from(codes).map((roleCode) => ({ label: roleCode, value: roleCode }));
  }, [systemRoles]);

  const loadIdentity = async () => {
    setLoading(true);
    try {
      const [meResult, tenantResult, kbResult] = await Promise.all([
        ragApi.getCurrentUser(),
        ragApi.listCurrentTenants(),
        ragApi.listCurrentKnowledgeBases()
      ]);
      const me = meResult.data.data;
      const kbs = kbResult.data.data || [];
      setCurrentUser(me);
      setVisibleTenants(tenantResult.data.data || []);
      setKnowledgeBases(kbs);
      setSelectedKnowledgeBaseId((current) => current || kbs[0]?.id);
      if (me?.tenantId != null) {
        deletionForm.setFieldsValue({ tenantId: me.tenantId });
      }
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const loadSystemTenants = async () => {
    if (!platformTenantAdmin) {
      setSystemTenants(visibleTenants);
      setSelectedTenantId((current) => current ?? currentUser?.tenantId ?? visibleTenants[0]?.id);
      return;
    }
    setAdminLoading(true);
    try {
      const { data } = await ragApi.listSystemTenants({ limit: 500 });
      const tenants = data.data || [];
      setSystemTenants(tenants);
      setSelectedTenantId((current) => current ?? tenants[0]?.id);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  const loadSystemUsers = async (tenantId = selectedTenantId) => {
    setAdminLoading(true);
    try {
      const { data } = platformTenantAdmin
        ? await ragApi.listSystemUsers({ tenantId, limit: 500 })
        : await ragApi.listTenantUsers({ limit: 500 });
      setSystemUsers(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  const loadSystemRoles = async (tenantId = selectedTenantId) => {
    try {
      const { data } = platformTenantAdmin
        ? await ragApi.listSystemRoles({ tenantId })
        : await ragApi.listTenantRoles();
      setSystemRoles(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const loadMembers = async (knowledgeBaseId = selectedKnowledgeBaseId) => {
    if (!knowledgeBaseId) {
      setMembers([]);
      return;
    }
    setMemberLoading(true);
    try {
      const { data } = await ragApi.listKnowledgeBaseMembers(knowledgeBaseId);
      setMembers(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setMemberLoading(false);
    }
  };

  const loadQuota = async () => {
    if (activeTenantId == null) {
      return;
    }
    setAdminLoading(true);
    try {
      const { data } = await ragApi.getTenantQuota(activeTenantId);
      setQuota(data.data);
      quotaForm.setFieldsValue(data.data);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  const loadAuditLogs = async () => {
    if (activeTenantId == null) {
      return;
    }
    setAdminLoading(true);
    try {
      const { data } = await ragApi.listAuditLogs({ targetTenantId: activeTenantId, limit: 100 });
      setAuditLogs(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  useEffect(() => {
    contextForm.setFieldsValue(tenantContextStorage.read());
    loadIdentity();
  }, []);

  useEffect(() => {
    if (!currentUser) {
      return;
    }
    if (platformTenantAdmin) {
      loadSystemTenants();
      return;
    }
    setSystemTenants(visibleTenants);
    setSelectedTenantId(currentUser.tenantId ?? visibleTenants[0]?.id);
    setSystemUsers([]);
    setSystemRoles([]);
  }, [currentUser, platformTenantAdmin, visibleTenants]);

  useEffect(() => {
    loadMembers(selectedKnowledgeBaseId);
  }, [selectedKnowledgeBaseId]);

  useEffect(() => {
    if (selectedTenantId !== undefined || !platformTenantAdmin) {
      loadSystemUsers(selectedTenantId);
      loadSystemRoles(selectedTenantId);
    }
  }, [selectedTenantId, platformTenantAdmin]);

  const saveContext = async (values: TenantHeaderContext) => {
    tenantContextStorage.write(values);
    message.success('请求上下文已保存');
    await loadIdentity();
  };

  const openTenantDrawer = (tenant?: SysTenant) => {
    setEditingTenant(tenant);
    tenantForm.setFieldsValue(tenant || { status: 1 });
    setTenantDrawerOpen(true);
  };

  const saveTenant = async (values: SystemTenantRequest) => {
    try {
      if (editingTenant?.id) {
        await ragApi.updateSystemTenant(editingTenant.id, values);
      } else {
        await ragApi.createSystemTenant(values);
      }
      message.success('租户已保存');
      setTenantDrawerOpen(false);
      await loadSystemTenants();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const toggleTenant = async (tenant: SysTenant) => {
    try {
      if (tenant.status === 1) {
        await ragApi.disableSystemTenant(tenant.id);
      } else {
        await ragApi.enableSystemTenant(tenant.id);
      }
      message.success('租户状态已更新');
      await loadSystemTenants();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const openUserDrawer = (user?: SysUser) => {
    setEditingUser(user);
    userForm.setFieldsValue(user ? {
      tenantId: user.tenantId,
      externalUserId: user.externalUserId,
      username: user.username,
      displayName: user.displayName,
      email: user.email,
      status: user.status,
      mustChangePassword: user.mustChangePassword === 1
    } : { tenantId: selectedTenantId, status: 1, mustChangePassword: true });
    setUserDrawerOpen(true);
  };

  const saveUser = async (values: SystemUserRequest) => {
    try {
      if (editingUser?.id) {
        if (platformTenantAdmin) {
          await ragApi.updateSystemUser(editingUser.id, values);
        } else {
          await ragApi.updateTenantUser(editingUser.id, values);
        }
      } else {
        if (platformTenantAdmin) {
          await ragApi.createSystemUser(values);
        } else {
          await ragApi.createTenantUser(values);
        }
      }
      message.success('用户已保存');
      setUserDrawerOpen(false);
      await loadSystemUsers();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const toggleUser = async (user: SysUser) => {
    try {
      if (user.status === 1) {
        if (platformTenantAdmin) {
          await ragApi.disableSystemUser(user.id);
        } else {
          await ragApi.disableTenantUser(user.id);
        }
      } else {
        if (platformTenantAdmin) {
          await ragApi.enableSystemUser(user.id);
        } else {
          await ragApi.enableTenantUser(user.id);
        }
      }
      message.success('用户状态已更新');
      await loadSystemUsers();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const openResetDrawer = (user: SysUser) => {
    setResetUser(user);
    resetPasswordForm.setFieldsValue({ mustChangePassword: true });
    setResetDrawerOpen(true);
  };

  const resetPassword = async (values: ResetPasswordFormValues) => {
    if (!resetUser?.id) return;
    try {
      if (platformTenantAdmin) {
        await ragApi.resetSystemUserPassword(resetUser.id, values);
      } else {
        await ragApi.resetTenantUserPassword(resetUser.id, values);
      }
      message.success('密码已重置');
      setResetDrawerOpen(false);
      await loadSystemUsers();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const openRolesDrawer = async (user: SysUser) => {
    setRoleUser(user);
    setRolesDrawerOpen(true);
    try {
      const { data } = platformTenantAdmin
        ? await ragApi.listSystemUserRoles(user.id)
        : await ragApi.listTenantUserRoles(user.id);
      userRolesForm.setFieldsValue({ roleCodes: (data.data || []).map((role) => role.roleCode) });
    } catch (error) {
      userRolesForm.setFieldsValue({ roleCodes: [] });
      message.error(getErrorMessage(error));
    }
  };

  const saveUserRoles = async (values: UserRolesFormValues) => {
    if (!roleUser?.id) return;
    try {
      if (platformTenantAdmin) {
        await ragApi.updateSystemUserRoles(roleUser.id, { roleCodes: values.roleCodes || [] });
      } else {
        await ragApi.updateTenantUserRoles(roleUser.id, { roleCodes: values.roleCodes || [] });
      }
      message.success('角色已保存');
      setRolesDrawerOpen(false);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const saveMember = async (values: MemberFormValues) => {
    if (!selectedKnowledgeBaseId) return;
    await ragApi.upsertKnowledgeBaseMember(selectedKnowledgeBaseId, {
      userId: values.userId,
      role: values.role,
      permissionTags: splitCsv(values.permissionTags)
    });
    memberForm.resetFields();
    message.success('知识库成员已保存');
    await loadMembers(selectedKnowledgeBaseId);
  };

  const removeMember = async (userId: string) => {
    if (!selectedKnowledgeBaseId) return;
    await ragApi.removeKnowledgeBaseMember(selectedKnowledgeBaseId, userId);
    message.success('知识库成员已移除');
    await loadMembers(selectedKnowledgeBaseId);
  };

  const saveQuota = async (values: RagTenantQuota) => {
    if (activeTenantId == null) {
      return;
    }
    const { data } = await ragApi.updateTenantQuota(activeTenantId, { ...values, tenantId: activeTenantId });
    setQuota(data.data);
    quotaForm.setFieldsValue(data.data);
    message.success('租户额度已保存');
  };

  const startImpersonation = async (values: ImpersonationFormValues) => {
    await ragApi.startImpersonation(values);
    tenantContextStorage.write({
      impersonateTenantId: String(values.targetTenantId),
      impersonationReason: values.reason
    });
    message.success('已开始模拟租户上下文');
    await loadIdentity();
  };

  const revokeImpersonation = async () => {
    await ragApi.revokeCurrentImpersonation();
    tenantContextStorage.write({ impersonateTenantId: '', impersonationReason: '' });
    message.success('已取消模拟租户上下文');
    await loadIdentity();
  };

  const createDeletionTask = async (values: { tenantId: number; reason?: string; executionMode?: 'DRY_RUN' | 'EXECUTE' }) => {
    setAdminLoading(true);
    try {
      const { data } = await ragApi.createTenantDeletionTask(values.tenantId, { reason: values.reason, executionMode: values.executionMode });
      if (data.data?.id) {
        const detail = await ragApi.getTenantDeletionTask(data.data.id);
        setDeletionTask(detail.data.data);
      }
      message.success('租户删除任务已创建');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  const runDeletionTask = async (executionMode: 'DRY_RUN' | 'EXECUTE') => {
    if (!deletionTask?.task?.id) return;
    setAdminLoading(true);
    try {
      const { data } = await ragApi.runTenantDeletionTask(deletionTask.task.id, executionMode);
      setDeletionTask(data.data);
      message.success(`删除任务 ${executionMode} 已完成`);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  const cancelDeletionTask = async () => {
    if (!deletionTask?.task?.id) return;
    setAdminLoading(true);
    try {
      const { data } = await ragApi.cancelTenantDeletionTask(deletionTask.task.id);
      setDeletionTask(data.data);
      message.success('删除任务已取消');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAdminLoading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="系统管理"
        description={platformTenantAdmin ? '平台超级管理员可管理租户、用户、角色、模拟租户、配额与审计。' : '租户管理员可管理当前租户内的知识库成员。'}
        tags={['Tenant', 'User', 'RBAC', 'Audit']}
      />

      <Tabs
        items={[
          {
            key: 'tenants',
            label: '租户管理',
            children: (
              <Card
                className="page-card"
                variant="borderless"
                title="租户列表"
                extra={
                  <Space>
                    <Button icon={<ReloadOutlined />} onClick={loadSystemTenants} loading={adminLoading}>刷新</Button>
                    <Button type="primary" icon={<TeamOutlined />} onClick={() => openTenantDrawer()}>新增租户</Button>
                  </Space>
                }
              >
                <Table
                  rowKey="id"
                  loading={adminLoading}
                  dataSource={systemTenants}
                  pagination={{ pageSize: 10 }}
                  columns={[
                    { title: 'ID', dataIndex: 'id', width: 80 },
                    { title: '租户编码', dataIndex: 'tenantCode' },
                    { title: '租户名称', dataIndex: 'tenantName' },
                    { title: '外部 ID', dataIndex: 'externalId' },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 100,
                      render: (status?: number) => <StatusTag enabled={status === 1} />
                    },
                    { title: '创建时间', dataIndex: 'createdAt', width: 190 },
                    {
                      title: '操作',
                      width: 180,
                      render: (_, record: SysTenant) => (
                        <Space>
                          <Button size="small" icon={<EditOutlined />} onClick={() => openTenantDrawer(record)} />
                          <Popconfirm title={record.status === 1 ? '停用该租户？' : '启用该租户？'} onConfirm={() => toggleTenant(record)}>
                            <Button size="small" danger={record.status === 1}>
                              {record.status === 1 ? '停用' : '启用'}
                            </Button>
                          </Popconfirm>
                        </Space>
                      )
                    }
                  ]}
                />
              </Card>
            )
          },
          {
            key: 'users',
            label: '用户管理',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card
                  className="page-card"
                  variant="borderless"
                  title="用户列表"
                  extra={
                    <Space>
                      <Select
                        style={{ width: 220 }}
                        placeholder="选择租户"
                        value={selectedTenantId}
                        disabled={!platformTenantAdmin}
                        options={systemTenants.map((tenant) => ({
                          label: `${tenant.tenantName} (#${tenant.id})`,
                          value: tenant.id
                        }))}
                        onChange={setSelectedTenantId}
                      />
                      <Button icon={<ReloadOutlined />} onClick={() => loadSystemUsers()} loading={adminLoading}>刷新</Button>
                      <Button type="primary" icon={<UserAddOutlined />} onClick={() => openUserDrawer()}>新增用户</Button>
                    </Space>
                  }
                >
                  <Table
                    rowKey="id"
                    loading={adminLoading}
                    dataSource={systemUsers}
                    pagination={{ pageSize: 10 }}
                    columns={[
                      { title: 'ID', dataIndex: 'id', width: 80 },
                      { title: '租户', dataIndex: 'tenantId', width: 90 },
                      { title: '用户 ID', dataIndex: 'externalUserId' },
                      { title: '用户名', dataIndex: 'username' },
                      { title: '显示名', dataIndex: 'displayName' },
                      { title: '邮箱', dataIndex: 'email' },
                      {
                        title: '状态',
                        dataIndex: 'status',
                        width: 100,
                        render: (status?: number) => <StatusTag enabled={status === 1} />
                      },
                      {
                        title: '需改密',
                        dataIndex: 'mustChangePassword',
                        width: 100,
                        render: (value?: number) => <Tag color={value === 1 ? 'warning' : 'default'}>{value === 1 ? '是' : '否'}</Tag>
                      },
                      {
                        title: '操作',
                        width: 260,
                        render: (_, record: SysUser) => (
                          <Space wrap>
                            <Button size="small" icon={<EditOutlined />} onClick={() => openUserDrawer(record)} />
                            <Button size="small" icon={<KeyOutlined />} onClick={() => openResetDrawer(record)}>重置密码</Button>
                            <Button size="small" onClick={() => openRolesDrawer(record)}>角色</Button>
                            <Popconfirm title={record.status === 1 ? '停用该用户？' : '启用该用户？'} onConfirm={() => toggleUser(record)}>
                              <Button size="small" danger={record.status === 1}>
                                {record.status === 1 ? '停用' : '启用'}
                              </Button>
                            </Popconfirm>
                          </Space>
                        )
                      }
                    ]}
                  />
                </Card>
                <Card className="page-card" variant="borderless" title="当前租户角色">
                  <Table
                    rowKey={(record) => `${record.tenantId}-${record.roleCode}`}
                    size="small"
                    dataSource={systemRoles}
                    pagination={false}
                    columns={[
                      { title: '租户', dataIndex: 'tenantId', width: 90 },
                      { title: '角色编码', dataIndex: 'roleCode' },
                      { title: '角色名称', dataIndex: 'roleName' },
                      { title: '作用域', dataIndex: 'roleScope', width: 140 }
                    ]}
                  />
                </Card>
              </Space>
            )
          },
          {
            key: 'knowledge-members',
            label: '知识库成员',
            children: knowledgeMemberTab()
          },
          {
            key: 'ops',
            label: '平台操作',
            children: platformOpsTab()
          }
        ].filter((item) => tenantAccessTabKeys.includes(item.key as import('../utils/tenantAccess').TenantAccessTabKey))}
      />

      <Drawer
        title={editingTenant ? '编辑租户' : '新增租户'}
        open={tenantDrawerOpen}
        onClose={() => setTenantDrawerOpen(false)}
        width={480}
        destroyOnClose
      >
        <Form layout="vertical" form={tenantForm} onFinish={saveTenant}>
          <Form.Item label="租户编码" name="tenantCode" rules={[{ required: true, message: '请输入租户编码' }]}>
            <Input disabled={Boolean(editingTenant)} />
          </Form.Item>
          <Form.Item label="租户名称" name="tenantName" rules={[{ required: true, message: '请输入租户名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="外部 ID" name="externalId">
            <Input />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
          <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>保存</Button>
        </Form>
      </Drawer>

      <Drawer
        title={editingUser ? '编辑用户' : '新增用户'}
        open={userDrawerOpen}
        onClose={() => setUserDrawerOpen(false)}
        width={520}
        destroyOnClose
      >
        <Form layout="vertical" form={userForm} onFinish={saveUser}>
          <Form.Item label="租户" name="tenantId" rules={[{ required: true, message: '请选择租户' }]}>
            <Select
              disabled={Boolean(editingUser) || !platformTenantAdmin}
              options={systemTenants.map((tenant) => ({ label: `${tenant.tenantName} (#${tenant.id})`, value: tenant.id }))}
            />
          </Form.Item>
          <Form.Item label="用户 ID" name="externalUserId" rules={[{ required: !editingUser, message: '请输入用户 ID' }]}>
            <Input disabled={Boolean(editingUser)} />
          </Form.Item>
          <Form.Item label="用户名" name="username">
            <Input />
          </Form.Item>
          <Form.Item label="显示名" name="displayName">
            <Input />
          </Form.Item>
          <Form.Item label="邮箱" name="email">
            <Input />
          </Form.Item>
          {!editingUser && (
            <Form.Item label="初始密码" name="password" rules={[{ required: true, message: '请输入初始密码' }]}>
              <Input.Password autoComplete="new-password" />
            </Form.Item>
          )}
          <Form.Item label="首次登录修改密码" name="mustChangePassword" initialValue>
            <Select options={[{ label: '是', value: true }, { label: '否', value: false }]} />
          </Form.Item>
          <Form.Item label="状态" name="status" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
          <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>保存</Button>
        </Form>
      </Drawer>

      <Drawer
        title={`重置密码 ${resetUser?.externalUserId || ''}`}
        open={resetDrawerOpen}
        onClose={() => setResetDrawerOpen(false)}
        width={420}
        destroyOnClose
      >
        <Form layout="vertical" form={resetPasswordForm} onFinish={resetPassword}>
          <Form.Item label="新密码" name="password" rules={[{ required: true, message: '请输入新密码' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item label="下次登录必须修改" name="mustChangePassword" initialValue>
            <Select options={[{ label: '是', value: true }, { label: '否', value: false }]} />
          </Form.Item>
          <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>保存</Button>
        </Form>
      </Drawer>

      <Drawer
        title={`分配角色 ${roleUser?.externalUserId || ''}`}
        open={rolesDrawerOpen}
        onClose={() => setRolesDrawerOpen(false)}
        width={460}
        destroyOnClose
      >
        <Form layout="vertical" form={userRolesForm} onFinish={saveUserRoles}>
          <Form.Item label="角色" name="roleCodes">
            <Select mode="tags" options={roleOptions} tokenSeparators={[',']} />
          </Form.Item>
          <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>保存</Button>
        </Form>
      </Drawer>
    </Space>
  );

  function knowledgeMemberTab() {
    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card className="page-card" variant="borderless" title="可见知识库" loading={loading}>
            <Table
              rowKey="id"
              size="small"
              dataSource={knowledgeBases}
              pagination={false}
              columns={[
                { title: 'ID', dataIndex: 'id', width: 80 },
                { title: '编码', dataIndex: 'kbCode' },
                { title: '名称', dataIndex: 'name' },
                {
                  title: '操作',
                  width: 110,
                  render: (_, record: RagKnowledgeBase) => (
                    <Button type="link" size="small" onClick={() => setSelectedKnowledgeBaseId(record.id)}>
                      成员
                    </Button>
                  )
                }
              ]}
            />
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card className="page-card" variant="borderless" title="知识库成员" extra={<TeamOutlined />}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Select
                style={{ width: '100%' }}
                placeholder="选择知识库"
                value={selectedKnowledgeBaseId}
                options={knowledgeBases.map((kb) => ({ label: `${kb.name} (#${kb.id})`, value: kb.id }))}
                onChange={setSelectedKnowledgeBaseId}
              />
              <Form layout="inline" form={memberForm} onFinish={saveMember}>
                <Form.Item name="userId" rules={[{ required: true }]}>
                  <Input placeholder="user id" />
                </Form.Item>
                <Form.Item name="role" rules={[{ required: true }]} initialValue="READER">
                  <Select
                    style={{ width: 120 }}
                    options={['READER', 'EDITOR', 'ADMIN', 'OWNER'].map((role) => ({ label: role, value: role }))}
                  />
                </Form.Item>
                <Form.Item name="permissionTags">
                  <Input placeholder="tags" />
                </Form.Item>
                <Button htmlType="submit" type="primary">保存</Button>
              </Form>
              <Table
                rowKey="id"
                size="small"
                loading={memberLoading}
                dataSource={members}
                pagination={false}
                columns={[
                  { title: '用户', dataIndex: 'userId' },
                  { title: '角色', dataIndex: 'memberRole', width: 120 },
                  { title: '标签', dataIndex: 'permissionTags' },
                  {
                    title: '操作',
                    width: 90,
                    render: (_, record: RagKnowledgeBaseMember) => (
                      <Popconfirm title="移除该成员？" onConfirm={() => removeMember(record.userId)}>
                        <Button type="text" danger icon={<DeleteOutlined />} />
                      </Popconfirm>
                    )
                  }
                ]}
              />
            </Space>
          </Card>
        </Col>
      </Row>
    );
  }

  function platformOpsTab() {
    return (
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} xl={10}>
            <Card className="page-card" variant="borderless" title="当前上下文" extra={<Button icon={<ReloadOutlined />} onClick={loadIdentity} />}>
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="Tenant">{currentUser?.tenantId ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="Operator Tenant">{currentUser?.operatorTenantId ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="User">{currentUser?.displayName || currentUser?.userId || '-'}</Descriptions.Item>
                <Descriptions.Item label="Roles">
                  <Space wrap>{(currentUser?.roles || []).map((role) => <Tag key={role}>{role}</Tag>)}</Space>
                </Descriptions.Item>
                <Descriptions.Item label="Impersonating">
                  <Tag color={currentUser?.impersonating ? 'warning' : 'default'}>{currentUser?.impersonating ? 'YES' : 'NO'}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Request">{currentUser?.requestId ?? '-'}</Descriptions.Item>
              </Descriptions>
            </Card>
          </Col>
          <Col xs={24} xl={14}>
            <Card className="page-card" variant="borderless" title="开发调试 Header">
              <Form layout="vertical" form={contextForm} onFinish={saveContext}>
                <Row gutter={12}>
                  <Col xs={24} md={8}>
                    <Form.Item label="Tenant ID" name="tenantId" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item label="User ID" name="userId" rules={[{ required: true }]}>
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={8}>
                    <Form.Item label="User Name" name="userName">
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item label="Roles" name="roles">
                      <Input placeholder="TENANT_ADMIN,KB_OWNER" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item label="Permission Tags" name="permissionTags">
                      <Input placeholder="internal,finance" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item label="Authorized KB IDs" name="knowledgeBaseIds">
                      <Input placeholder="1,2,3" />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item label="Impersonate Tenant ID" name="impersonateTenantId">
                      <Input />
                    </Form.Item>
                  </Col>
                  <Col span={24}>
                    <Form.Item label="Impersonation Reason" name="impersonationReason">
                      <Input />
                    </Form.Item>
                  </Col>
                </Row>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>保存上下文</Button>
              </Form>
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} xl={12}>
            <Card className="page-card" variant="borderless" title="租户额度">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Space>
                  <Button onClick={loadQuota} loading={adminLoading}>加载额度</Button>
                  <Text type="secondary">Tenant {activeTenantId}</Text>
                </Space>
                <Form layout="vertical" form={quotaForm} onFinish={saveQuota} initialValues={quota}>
                  <Row gutter={12}>
                    {[
                      ['maxDocuments', 'Max Documents'],
                      ['maxStorageBytes', 'Max Storage Bytes'],
                      ['maxFileBytes', 'Max File Bytes'],
                      ['dailyOcrLimit', 'Daily OCR'],
                      ['dailyEmbeddingTokens', 'Daily Embedding Tokens'],
                      ['maxConcurrentIngestionTasks', 'Concurrent Ingestion'],
                      ['dailyQueryLimit', 'Daily Queries'],
                      ['monthlyBudgetCents', 'Monthly Budget Cents']
                    ].map(([name, label]) => (
                      <Col xs={24} md={12} key={name}>
                        <Form.Item label={label} name={name}>
                          <InputNumber min={0} style={{ width: '100%' }} />
                        </Form.Item>
                      </Col>
                    ))}
                  </Row>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>保存额度</Button>
                </Form>
              </Space>
            </Card>
          </Col>
          <Col xs={24} xl={12}>
            <Card className="page-card" variant="borderless" title="模拟租户与删除任务" extra={<UserSwitchOutlined />}>
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Table
                  rowKey="id"
                  size="small"
                  dataSource={visibleTenants}
                  pagination={false}
                  columns={[
                    { title: 'ID', dataIndex: 'id', width: 80 },
                    { title: '编码', dataIndex: 'tenantCode' },
                    { title: '名称', dataIndex: 'tenantName' }
                  ]}
                />
                <Form layout="inline" form={impersonationForm} onFinish={startImpersonation}>
                  <Form.Item name="targetTenantId" rules={[{ required: true }]}>
                    <InputNumber min={0} placeholder="target tenant" />
                  </Form.Item>
                  <Form.Item name="reason" rules={[{ required: true }]}>
                    <Input placeholder="reason" />
                  </Form.Item>
                  <Form.Item name="ttlMinutes" initialValue={30}>
                    <InputNumber min={1} max={240} />
                  </Form.Item>
                  <Button htmlType="submit" type="primary">开始</Button>
                  <Button danger onClick={revokeImpersonation}>取消</Button>
                </Form>
                <Form layout="inline" form={deletionForm} onFinish={createDeletionTask}>
                  <Form.Item name="tenantId" rules={[{ required: true }]}>
                    <InputNumber min={0} placeholder="tenant" />
                  </Form.Item>
                  <Form.Item name="reason">
                    <Input placeholder="deletion reason" />
                  </Form.Item>
                  <Form.Item name="executionMode" initialValue="DRY_RUN">
                    <Select
                      style={{ width: 120 }}
                      options={[{ label: 'Dry Run', value: 'DRY_RUN' }, { label: 'Execute', value: 'EXECUTE' }]}
                    />
                  </Form.Item>
                  <Button danger htmlType="submit" loading={adminLoading}>创建删除任务</Button>
                </Form>
                {deletionTask && (
                  <Space wrap>
                    <Tag color="warning">
                      {deletionTask.task.taskNo} {deletionTask.task.taskStatus} stages:{deletionTask.stages?.length || 0}
                    </Tag>
                    <Button size="small" onClick={() => runDeletionTask('DRY_RUN')} loading={adminLoading}>Dry Run</Button>
                    <Button size="small" danger onClick={() => runDeletionTask('EXECUTE')} loading={adminLoading}>Execute</Button>
                    <Button size="small" onClick={cancelDeletionTask} loading={adminLoading}>Cancel</Button>
                  </Space>
                )}
              </Space>
            </Card>
          </Col>
        </Row>

        <Card
          className="page-card"
          variant="borderless"
          title="审计日志"
          extra={<Button icon={<ReloadOutlined />} onClick={loadAuditLogs} loading={adminLoading}>加载</Button>}
        >
          <Table
            rowKey="id"
            size="small"
            dataSource={auditLogs}
            pagination={{ pageSize: 10 }}
            columns={[
              { title: '时间', dataIndex: 'createdAt', width: 190 },
              { title: '操作人', dataIndex: 'operatorUserId', width: 160 },
              { title: '目标租户', dataIndex: 'targetTenantId', width: 130 },
              { title: '操作', dataIndex: 'operation', width: 220 },
              { title: '资源', dataIndex: 'resourceType', width: 160 },
              { title: '结果', dataIndex: 'result', width: 110 },
              { title: '详情', dataIndex: 'detailJson' }
            ]}
          />
        </Card>
      </Space>
    );
  }
}

function StatusTag({ enabled }: { enabled: boolean }) {
  return <Tag color={enabled ? 'green' : 'default'}>{enabled ? '启用' : '停用'}</Tag>;
}

function splitCsv(value?: string) {
  if (!value) return [];
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}
