import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { App, Button, Card, Form, Input, Segmented, Space, Typography } from 'antd';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ragApi } from '../api/rag';
import { authTokenStorage, tenantContextStorage } from '../api/http';
import type { LoginRequest } from '../types';
import { getErrorMessage } from '../utils/message';

const { Title, Text } = Typography;

export default function LoginPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [form] = Form.useForm<LoginRequest>();
  const loginType = Form.useWatch('loginType', form) || 'TENANT';

  useEffect(() => {
    authTokenStorage.clear();
    tenantContextStorage.clear();
    form.setFieldsValue({
      loginType: 'TENANT',
      tenant: undefined,
      account: undefined,
      password: undefined
    });
  }, [form]);

  const submit = async (values: LoginRequest) => {
    try {
      const request: LoginRequest = {
        loginType: values.loginType || 'TENANT',
        tenant: values.loginType === 'SYSTEM' ? undefined : values.tenant,
        account: values.account,
        password: values.password
      };
      const { data } = await ragApi.login(request);
      authTokenStorage.write(data.data.accessToken);
      const user = data.data.currentUser;
      tenantContextStorage.write({
        tenantId: user.tenantId == null ? '' : String(user.tenantId),
        userId: user.userId,
        userName: user.displayName || user.userId,
        roles: (user.roles || []).join(','),
        knowledgeBaseIds: (user.authorizedKnowledgeBaseIds || []).join(','),
        permissionTags: (user.permissionTags || []).join(','),
        impersonateTenantId: '',
        impersonationReason: ''
      });
      message.success('登录成功');
      navigate(user.platformAdmin && user.tenantId == null ? '/tenant-access' : '/', { replace: true });
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <div className="login-page">
      <Card className="login-card" variant="borderless">
        <Space direction="vertical" size={18} style={{ width: '100%' }}>
          <div>
            <Title level={3} style={{ marginBottom: 4 }}>RAG 管理控制台</Title>
            <Text type="secondary">租户用户使用租户、用户名与密码；超级管理员使用用户名与密码。</Text>
          </div>
          <Form
            layout="vertical"
            form={form}
            onFinish={submit}
            initialValues={{ loginType: 'TENANT' }}
            autoComplete="off"
          >
            <div aria-hidden="true" style={{ position: 'absolute', left: -10000, width: 1, height: 1, overflow: 'hidden' }}>
              <input type="text" name="username" autoComplete="username" tabIndex={-1} />
              <input type="password" name="password" autoComplete="current-password" tabIndex={-1} />
            </div>
            <Form.Item name="loginType">
              <Segmented
                block
                options={[
                  { label: '租户登录', value: 'TENANT' },
                  { label: '系统登录', value: 'SYSTEM' }
                ]}
              />
            </Form.Item>
            {loginType === 'TENANT' && (
              <Form.Item label="租户" name="tenant" rules={[{ required: true, message: '请输入租户' }]}>
                <Input placeholder="租户编码 / 租户名称" autoComplete="off" />
              </Form.Item>
            )}
            <Form.Item label="用户名称" name="account" rules={[{ required: true, message: '请输入用户名称' }]}>
              <Input prefix={<UserOutlined />} placeholder="用户名 / 邮箱 / 用户 ID" autoComplete="new-password" />
            </Form.Item>
            <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
            </Form.Item>
            <Button block type="primary" htmlType="submit">
              登录
            </Button>
          </Form>
        </Space>
      </Card>
    </div>
  );
}
