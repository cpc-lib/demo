import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  ApiOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
  DeploymentUnitOutlined,
  EditOutlined,
  ExperimentOutlined,
  LoginOutlined,
  LogoutOutlined,
  PlayCircleOutlined,
  RadarChartOutlined,
  ReloadOutlined,
  RollbackOutlined,
  SafetyCertificateOutlined,
  WarningOutlined
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import { create } from 'zustand';
import 'antd/dist/reset.css';
import './styles.css';
import { RULE_TYPES, buildRulePayload, getRuleFormValues } from './ruleEditor.js';

const { Header, Sider, Content } = Layout;
const { Title, Text, Paragraph } = Typography;

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:18080';
const GATEWAY_BASE = import.meta.env.VITE_GATEWAY_BASE_URL || 'http://localhost:18000';
const SERVICE_ID = 'demo-order-service';

const useAuthStore = create((set) => ({
  token: localStorage.getItem('gray-token') || '',
  user: JSON.parse(localStorage.getItem('gray-user') || 'null'),
  setAuth: (payload) => {
    localStorage.setItem('gray-token', payload.token);
    localStorage.setItem('gray-user', JSON.stringify(payload));
    set({ token: payload.token, user: payload });
  },
  logout: () => {
    localStorage.removeItem('gray-token');
    localStorage.removeItem('gray-user');
    set({ token: '', user: null });
  }
}));

const request = async (path, options = {}) => {
  const token = useAuthStore.getState().token;
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-Operator': 'admin-ui',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : {};
  if (response.status === 401 || response.status === 403) {
    useAuthStore.getState().logout();
    throw new Error('登录已失效或权限不足');
  }
  if (!response.ok || payload.success === false) {
    throw new Error(payload.message || '请求失败');
  }
  return payload.data;
};

function App() {
  const { token, user, setAuth, logout } = useAuthStore();
  const [loginForm] = Form.useForm();
  const [ruleForm] = Form.useForm();
  const [editRuleForm] = Form.useForm();
  const [releaseForm] = Form.useForm();
  const [diagnosisForm] = Form.useForm();
  const [summary, setSummary] = useState(null);
  const [rules, setRules] = useState([]);
  const [releases, setReleases] = useState([]);
  const [approvals, setApprovals] = useState([]);
  const [policy, setPolicy] = useState(null);
  const [abMetrics, setAbMetrics] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [matchResult, setMatchResult] = useState(null);
  const [gatewayResult, setGatewayResult] = useState(null);
  const [editingRule, setEditingRule] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [resetPwdVisible, setResetPwdVisible] = useState(false);
  const [resetPwdForm] = Form.useForm();

  const loadAll = useCallback(async () => {
    if (!useAuthStore.getState().token) {
      return;
    }
    setLoading(true);
    try {
      const [nextSummary, nextRules, nextReleases, nextApprovals, nextPolicy, nextAbMetrics, nextAlerts] = await Promise.all([
        request('/api/dashboard/summary'),
        request(`/api/rules?serviceId=${SERVICE_ID}`),
        request(`/api/releases?serviceId=${SERVICE_ID}`),
        request('/api/approvals'),
        request(`/api/policies?serviceId=${SERVICE_ID}`),
        request(`/api/ab-metrics?serviceId=${SERVICE_ID}&experimentKey=default-exp`),
        request('/api/alerts')
      ]);
      setSummary(nextSummary);
      setRules(nextRules);
      setReleases(nextReleases);
      setApprovals(nextApprovals);
      setPolicy(nextPolicy);
      setAbMetrics(nextAbMetrics);
      setAlerts(nextAlerts);
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const login = async (values) => {
    try {
      const response = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(values)
      });
      const payload = await response.json();
      if (!response.ok || payload.success === false) {
        throw new Error(payload.message || '登录失败');
      }
      setAuth(payload.data);
      message.success('登录成功');
    } catch (err) {
      message.error(err.message);
    }
  };

  const safeAction = async (fn, successText = '操作成功') => {
    try {
      await fn();
      message.success(successText);
      await loadAll();
    } catch (err) {
      message.error(err.message);
    }
  };

  const stats = useMemo(() => [
    { title: '规则总数', value: summary?.totalRules ?? 0, prefix: <RadarChartOutlined /> },
    { title: '启用规则', value: summary?.enabledRules ?? 0, prefix: <SafetyCertificateOutlined /> },
    { title: '运行任务', value: summary?.runningTasks ?? 0, prefix: <DeploymentUnitOutlined /> },
    { title: '告警事件', value: alerts.length, prefix: <WarningOutlined /> }
  ], [summary, alerts.length]);

  const submitRule = (values) => safeAction(async () => {
    await request('/api/rules', {
      method: 'POST',
      body: JSON.stringify({ ...values, serviceId: SERVICE_ID, enabled: true, targetVersion: 'v2' })
    });
    ruleForm.resetFields();
  }, '规则已创建');

  const openRuleEditor = (rule) => {
    setEditingRule(rule);
    editRuleForm.setFieldsValue(getRuleFormValues(rule));
  };

  const closeRuleEditor = () => {
    setEditingRule(null);
    editRuleForm.resetFields();
  };

  const submitRuleUpdate = (values) => safeAction(async () => {
    await request(`/api/rules/${editingRule.id}`, {
      method: 'PUT',
      body: JSON.stringify(buildRulePayload(values, editingRule))
    });
    closeRuleEditor();
  }, '规则已更新');

  const submitRelease = (values) => safeAction(async () => {
    await request('/api/releases', {
      method: 'POST',
      body: JSON.stringify({
        ...values,
        serviceId: SERVICE_ID,
        fromVersion: 'v1',
        targetVersion: 'v2',
        currentPercent: 0,
        stagesJson: values.stagesJson || '[1,5,20,50,100]'
      })
    });
    releaseForm.resetFields();
  }, '发布任务已提交审批');

  const runDiagnosis = async (values) => {
    const payload = {
      serviceId: SERVICE_ID,
      userId: values.userId || '',
      tenantId: values.tenantId || '',
      appVersion: values.appVersion || '',
      region: values.region || '',
      headers: { 'X-Gray': values.xGray || '' },
      cookies: {}
    };
    await safeAction(async () => {
      setMatchResult(await request('/api/diagnosis/match', { method: 'POST', body: JSON.stringify(payload) }));
    }, '诊断完成');
  };

  const testGateway = async () => {
    const values = diagnosisForm.getFieldsValue();
    const response = await fetch(`${GATEWAY_BASE}/api/order/health`, {
      headers: {
        'X-User-Id': values.userId || '',
        'X-Tenant-Id': values.tenantId || '',
        'X-App-Version': values.appVersion || '',
        'X-Region': values.region || '',
        'X-Gray': values.xGray || ''
      }
    });
    setGatewayResult(await response.json());
  };

  const resetPassword = (values) => safeAction(async () => {
    await request('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify(values)
    });
    setResetPwdVisible(false);
    resetPwdForm.resetFields();
  }, `用户 ${values.username} 密码已重置`);

  if (!token) {
    return (
      <main className="loginPage">
        <Card className="loginCard">
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <div className="brandBadge">G</div>
            <Title level={3} style={{ margin: 0 }}>灰度发布管理平台</Title>
            <Paragraph type="secondary">演示账号：admin/admin123、release/release123、viewer/viewer123</Paragraph>
            <Form form={loginForm} layout="vertical" initialValues={{ username: 'admin', password: 'admin123' }} onFinish={login}>
              <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
                <Input />
              </Form.Item>
              <Form.Item name="password" label="密码" rules={[{ required: true }]}>
                <Input.Password />
              </Form.Item>
              <Button block type="primary" htmlType="submit" icon={<LoginOutlined />}>登录</Button>
            </Form>
          </Space>
        </Card>
      </main>
    );
  }

  const ruleColumns = [
    { title: '名称', dataIndex: 'ruleName' },
    { title: '类型', dataIndex: 'ruleType', render: (v) => <Tag color="blue">{v}</Tag> },
    { title: '条件', render: (_, row) => `${row.conditionKey || '-'} = ${row.conditionValue || '-'}` },
    { title: '版本', dataIndex: 'targetVersion' },
    { title: '比例', dataIndex: 'trafficPercent', render: (v) => `${v}%` },
    { title: '优先级', dataIndex: 'priority' },
    {
      title: '操作',
      render: (_, row) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openRuleEditor(row)}>编辑</Button>
        <Button danger size="small" onClick={() => safeAction(() => request(`/api/rules/${row.id}`, { method: 'DELETE' }), '规则已删除')}>删除</Button>
        </Space>
      )
    }
  ];

  const releaseColumns = [
    { title: '任务', dataIndex: 'taskName' },
    { title: '策略', dataIndex: 'strategy', render: (v) => <Tag color="purple">{v}</Tag> },
    { title: '进度', dataIndex: 'currentPercent', render: (v) => <Progress percent={v} size="small" /> },
    { title: '状态', dataIndex: 'status', render: (v) => <Tag color={statusColor(v)}>{v}</Tag> },
    { title: '健康', dataIndex: 'healthStatus' },
    {
      title: '操作',
      render: (_, row) => (
        <Space wrap>
          <Button size="small" icon={<PlayCircleOutlined />} onClick={() => safeAction(() => request(`/api/releases/${row.id}/start`, { method: 'POST' }), '任务已启动')}>开始</Button>
          <Button size="small" onClick={() => safeAction(() => request(`/api/releases/${row.id}/advance`, { method: 'POST', body: JSON.stringify({ percent: Math.min(100, row.currentPercent + 20) }) }), '已推进')}>推进</Button>
          <Button size="small" onClick={() => safeAction(() => request(`/api/releases/${row.id}/metrics`, { method: 'POST', body: JSON.stringify({ errorRate: 0.01, p99LatencyMs: 260 }) }), '健康指标已上报')}>健康</Button>
          <Button size="small" danger onClick={() => safeAction(() => request(`/api/releases/${row.id}/metrics`, { method: 'POST', body: JSON.stringify({ errorRate: 0.12, p99LatencyMs: 1800 }) }), '异常指标已上报')}>异常</Button>
          <Button size="small" icon={<CheckCircleOutlined />} onClick={() => safeAction(() => request(`/api/releases/${row.id}/complete`, { method: 'POST' }), '任务已完成')}>完成</Button>
          <Button size="small" danger icon={<RollbackOutlined />} onClick={() => safeAction(() => request(`/api/releases/${row.id}/rollback`, { method: 'POST', body: JSON.stringify({ reason: 'UI 手动回滚' }) }), '任务已回滚')}>回滚</Button>
        </Space>
      )
    }
  ];

  const approvalColumns = [
    { title: '任务ID', dataIndex: 'taskId' },
    { title: '申请人', dataIndex: 'applicant' },
    { title: '审批人', dataIndex: 'approver', render: (v) => v || '-' },
    { title: '状态', dataIndex: 'status', render: (v) => <Tag color={v === 'APPROVED' ? 'green' : v === 'REJECTED' ? 'red' : 'gold'}>{v}</Tag> },
    { title: '意见', dataIndex: 'comment' },
    {
      title: '操作',
      render: (_, row) => row.status === 'PENDING' ? (
        <Space>
          <Button size="small" type="primary" onClick={() => safeAction(() => request(`/api/approvals/${row.id}/approve`, { method: 'POST', body: JSON.stringify({ comment: '审批通过' }) }), '审批通过')}>通过</Button>
          <Button size="small" danger onClick={() => safeAction(() => request(`/api/approvals/${row.id}/reject`, { method: 'POST', body: JSON.stringify({ comment: '审批拒绝' }) }), '审批拒绝')}>拒绝</Button>
        </Space>
      ) : '-'
    }
  ];

  const abColumns = [
    { title: '实验', dataIndex: 'experimentKey' },
    { title: '版本', dataIndex: 'variant' },
    { title: '曝光', dataIndex: 'exposures' },
    { title: '转化', dataIndex: 'conversions' },
    { title: '转化率', render: (_, row) => row.exposures ? `${((row.conversions / row.exposures) * 100).toFixed(2)}%` : '0%' }
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={248} className="appSider">
        <div className="brandLine"><span className="brandBadge">G</span><div><strong>灰度发布平台</strong><small>{user?.roles?.join(', ')}</small></div></div>
        <Menu
          theme="light"
          mode="inline"
          className="sideMenu"
          defaultSelectedKeys={['dashboard']}
          items={[
            { key: 'dashboard', icon: <DashboardOutlined />, label: <a href="#dashboard">发布大盘</a> },
            { key: 'rules', icon: <RadarChartOutlined />, label: <a href="#rules">灰度规则</a> },
            { key: 'releases', icon: <DeploymentUnitOutlined />, label: <a href="#releases">发布任务</a> },
            { key: 'policy', icon: <ApiOutlined />, label: <a href="#policy">蓝绿/A-B</a> },
            { key: 'diagnosis', icon: <ExperimentOutlined />, label: <a href="#diagnosis">命中诊断</a> },
            { key: 'alerts', icon: <WarningOutlined />, label: <a href="#alerts">告警审计</a> }
          ]}
        />
      </Sider>
      <Layout className="mainLayout">
        <Header className="appHeader">
          <div><Title level={3} style={{ margin: 0 }}>灰度发布管理平台</Title><Text type="secondary">规则、发布、审批、回滚、监控一体化</Text></div>
          <Space>
            <Button icon={<ReloadOutlined />} loading={loading} onClick={loadAll}>刷新</Button>
            <Button icon={<SafetyCertificateOutlined />} onClick={() => setResetPwdVisible(true)}>重置密码</Button>
            <Button icon={<LogoutOutlined />} onClick={logout}>退出</Button>
          </Space>
        </Header>
        <Content className="appContent">
          {error && <Alert type="error" message={error} showIcon closable style={{ marginBottom: 16 }} />}
          <Row gutter={[16, 16]} id="dashboard">
            {stats.map((item) => (
              <Col xs={24} sm={12} xl={6} key={item.title}>
                <Card><Statistic title={item.title} value={item.value} prefix={item.prefix} /></Card>
              </Col>
            ))}
          </Row>

          <Card id="rules" title="灰度规则" extra={<Button onClick={() => safeAction(() => request(`/api/rules/publish?serviceId=${SERVICE_ID}`, { method: 'POST' }), '规则已发布到 Nacos')}>发布到 Nacos</Button>}>
            <Form form={ruleForm} layout="inline" onFinish={submitRule} initialValues={{ ruleType: 'USER', conditionKey: 'userId', trafficPercent: 0, priority: 10 }}>
              <Form.Item name="ruleName" rules={[{ required: true }]}><Input placeholder="规则名称" /></Form.Item>
              <Form.Item name="ruleType"><Select style={{ width: 130 }} options={RULE_TYPES.map((v) => ({ value: v, label: v }))} /></Form.Item>
              <Form.Item name="conditionKey"><Input placeholder="条件 Key" /></Form.Item>
              <Form.Item name="conditionValue"><Input placeholder="条件值" /></Form.Item>
              <Form.Item name="trafficPercent"><InputNumber min={0} max={100} placeholder="比例" /></Form.Item>
              <Form.Item name="priority"><InputNumber min={1} placeholder="优先级" /></Form.Item>
              <Form.Item><Button type="primary" htmlType="submit">新增规则</Button></Form.Item>
            </Form>
            <Table rowKey="id" columns={ruleColumns} dataSource={rules} pagination={{ pageSize: 6 }} style={{ marginTop: 16 }} />
          </Card>

          <Modal
            title={editingRule ? `编辑灰度规则：${editingRule.ruleName}` : '编辑灰度规则'}
            open={!!editingRule}
            okText="保存修改"
            cancelText="取消"
            onOk={() => editRuleForm.submit()}
            onCancel={closeRuleEditor}
            afterClose={() => editRuleForm.resetFields()}
          >
            <Form form={editRuleForm} layout="vertical" onFinish={submitRuleUpdate}>
              <Form.Item name="ruleName" label="规则名称" rules={[{ required: true, message: '请输入规则名称' }]}>
                <Input />
              </Form.Item>
              <Form.Item name="ruleType" label="类型" rules={[{ required: true, message: '请选择规则类型' }]}>
                <Select options={RULE_TYPES.map((v) => ({ value: v, label: v }))} />
              </Form.Item>
              <Form.Item name="conditionKey" label="条件 Key">
                <Input />
              </Form.Item>
              <Form.Item name="conditionValue" label="条件值">
                <Input />
              </Form.Item>
              <Row gutter={12}>
                <Col span={12}>
                  <Form.Item name="trafficPercent" label="比例" rules={[{ required: true, message: '请输入比例' }]}>
                    <InputNumber min={0} max={100} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="priority" label="优先级" rules={[{ required: true, message: '请输入优先级' }]}>
                    <InputNumber min={1} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </Modal>

          <Card id="releases" title="发布任务">
            <Form form={releaseForm} layout="inline" onFinish={submitRelease} initialValues={{ strategy: 'CANARY', owner: 'release-admin', stagesJson: '[1,5,20,50,100]' }}>
              <Form.Item name="taskName" rules={[{ required: true }]}><Input placeholder="任务名称" /></Form.Item>
              <Form.Item name="strategy"><Select style={{ width: 130 }} options={['CANARY', 'BLUE_GREEN', 'AB_TEST'].map((v) => ({ value: v, label: v }))} /></Form.Item>
              <Form.Item name="owner"><Input placeholder="负责人" /></Form.Item>
              <Form.Item name="stagesJson"><Input placeholder="阶段配置" /></Form.Item>
              <Form.Item><Button type="primary" htmlType="submit">创建任务</Button></Form.Item>
            </Form>
            <Table rowKey="id" columns={releaseColumns} dataSource={releases} pagination={{ pageSize: 5 }} style={{ marginTop: 16 }} />
          </Card>

          <Card title="审批流">
            <Table rowKey="id" columns={approvalColumns} dataSource={approvals} pagination={{ pageSize: 5 }} />
          </Card>

          <Card id="policy" title="蓝绿发布与 A/B 测试">
            <Row gutter={[16, 16]}>
              <Col xs={24} lg={8}>
                <Card type="inner" title="当前服务策略">
                  <Space direction="vertical">
                    <Text>默认版本：<Tag color="blue">{policy?.defaultVersion}</Tag></Text>
                    <Text>活跃颜色：<Tag>{policy?.activeColor}</Tag></Text>
                    <Text>A/B：<Tag color={policy?.abEnabled ? 'green' : 'default'}>{policy?.abEnabled ? '开启' : '关闭'}</Tag></Text>
                    <Space>
                      <Button onClick={() => safeAction(() => request('/api/policies/blue-green/switch', { method: 'POST', body: JSON.stringify({ serviceId: SERVICE_ID, activeColor: 'blue' }) }), '已切到 blue')}>切 Blue</Button>
                      <Button type="primary" onClick={() => safeAction(() => request('/api/policies/blue-green/switch', { method: 'POST', body: JSON.stringify({ serviceId: SERVICE_ID, activeColor: 'green' }) }), '已切到 green')}>切 Green</Button>
                    </Space>
                    <Space>
                      <Switch checked={!!policy?.abEnabled} onChange={(checked) => safeAction(() => request('/api/policies/ab', { method: 'POST', body: JSON.stringify({ serviceId: SERVICE_ID, enabled: checked, percentB: policy?.abPercentB ?? 50 }) }), checked ? 'A/B 已开启' : 'A/B 已关闭')} />
                      <InputNumber min={0} max={100} value={policy?.abPercentB ?? 50} onChange={(value) => safeAction(() => request('/api/policies/ab', { method: 'POST', body: JSON.stringify({ serviceId: SERVICE_ID, enabled: !!policy?.abEnabled, percentB: value ?? 50 }) }), 'A/B 比例已更新')} addonAfter="% B" />
                    </Space>
                  </Space>
                </Card>
              </Col>
              <Col xs={24} lg={16}>
                <Space style={{ marginBottom: 12 }}>
                  <Button onClick={() => safeAction(() => request('/api/ab-metrics/record', { method: 'POST', body: JSON.stringify({ serviceId: SERVICE_ID, experimentKey: 'default-exp', variant: 'A', converted: false }) }), 'A 曝光已记录')}>记录 A 曝光</Button>
                  <Button onClick={() => safeAction(() => request('/api/ab-metrics/record', { method: 'POST', body: JSON.stringify({ serviceId: SERVICE_ID, experimentKey: 'default-exp', variant: 'B', converted: true }) }), 'B 转化已记录')}>记录 B 转化</Button>
                </Space>
                <Table rowKey="id" columns={abColumns} dataSource={abMetrics} pagination={false} />
              </Col>
            </Row>
          </Card>

          <Card id="diagnosis" title="命中诊断">
            <Row gutter={[16, 16]}>
              <Col xs={24} lg={10}>
                <Form form={diagnosisForm} layout="vertical" onFinish={runDiagnosis} initialValues={{ userId: '1001' }}>
                  <Form.Item name="userId" label="User ID"><Input /></Form.Item>
                  <Form.Item name="tenantId" label="Tenant ID"><Input /></Form.Item>
                  <Form.Item name="appVersion" label="App Version"><Input /></Form.Item>
                  <Form.Item name="region" label="Region"><Input /></Form.Item>
                  <Form.Item name="xGray" label="X-Gray"><Input /></Form.Item>
                  <Space><Button type="primary" htmlType="submit">诊断</Button><Button onClick={testGateway}>请求网关</Button></Space>
                </Form>
              </Col>
              <Col xs={24} lg={14}>
                <pre className="resultPre">{JSON.stringify({ matchResult, gatewayResult }, null, 2)}</pre>
              </Col>
            </Row>
          </Card>

          <Card id="alerts" title="告警与审计">
            <Row gutter={[16, 16]}>
              <Col xs={24} lg={10}>
                <Table rowKey="id" dataSource={alerts} pagination={{ pageSize: 5 }} columns={[
                  { title: '级别', dataIndex: 'level', render: (v) => <Tag color={v === 'CRITICAL' ? 'red' : v === 'WARN' ? 'gold' : 'blue'}>{v}</Tag> },
                  { title: '标题', dataIndex: 'title' },
                  { title: '来源', dataIndex: 'source' }
                ]} />
              </Col>
              <Col xs={24} lg={14}>
                <Table rowKey="id" dataSource={summary?.latestAudits || []} pagination={{ pageSize: 5 }} columns={[
                  { title: '操作人', dataIndex: 'operator' },
                  { title: '动作', dataIndex: 'action' },
                  { title: '资源', render: (_, row) => `${row.resourceType}#${row.resourceId || '-'}` },
                  { title: '时间', dataIndex: 'createTime' }
                ]} />
              </Col>
            </Row>
          </Card>
          <Modal
            title="重置用户密码"
            open={resetPwdVisible}
            okText="确认重置"
            cancelText="取消"
            onOk={() => resetPwdForm.submit()}
            onCancel={() => { setResetPwdVisible(false); resetPwdForm.resetFields(); }}
            afterClose={() => resetPwdForm.resetFields()}
          >
            <Form form={resetPwdForm} layout="vertical" onFinish={resetPassword}>
              <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请选择或输入用户名' }]}>
                <Select
                  placeholder="选择用户"
                  options={[
                    { value: 'admin', label: 'admin (ADMIN)' },
                    { value: 'release', label: 'release (RELEASE_MANAGER)' },
                    { value: 'viewer', label: 'viewer (VIEWER)' }
                  ]}
                />
              </Form.Item>
              <Form.Item name="newPassword" label="新密码" rules={[{ required: true, message: '请输入新密码' }, { min: 6, message: '密码至少6位' }]}>
                <Input.Password placeholder="输入新密码" />
              </Form.Item>
            </Form>
          </Modal>
        </Content>
      </Layout>
    </Layout>
  );
}

function statusColor(status) {
  if (status === 'RUNNING') return 'green';
  if (status === 'WAITING_APPROVAL') return 'gold';
  if (status === 'ROLLED_BACK' || status === 'REJECTED') return 'red';
  if (status === 'COMPLETED') return 'blue';
  return 'default';
}

createRoot(document.getElementById('root')).render(<App />);
