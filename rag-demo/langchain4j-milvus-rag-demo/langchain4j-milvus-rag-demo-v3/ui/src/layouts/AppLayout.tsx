import {
  ApiOutlined,
  AuditOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  FileSearchOutlined,
  LoginOutlined,
  LogoutOutlined,
  LockOutlined,
  MessageOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PictureOutlined,
  SettingOutlined,
  TeamOutlined
} from '@ant-design/icons';
import { Button, Layout, Menu, Space, Tag, theme, Typography } from 'antd';
import { useState, type ReactNode } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { ragApi } from '../api/rag';
import { authTokenStorage, tenantContextStorage } from '../api/http';
import { mainNavigationItems } from '../utils/navigation';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const navigationIcons: Record<string, ReactNode> = {
  '/': <MessageOutlined />,
  '/knowledge': <FileSearchOutlined />,
  '/image-assets': <PictureOutlined />,
  '/query-logs': <AuditOutlined />,
  '/retrieval-evaluations': <ExperimentOutlined />,
  '/model-configs': <SettingOutlined />,
  '/agent-prompts': <FileTextOutlined />,
  '/chunks': <BranchesOutlined />,
  '/tenant-access': <TeamOutlined />,
  '/vector-stores': <DatabaseOutlined />,
  '/collections': <ApiOutlined />
};

const items = mainNavigationItems.map((item) => ({
  ...item,
  icon: navigationIcons[item.key]
}));

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const tenantContext = tenantContextStorage.read();
  const hasToken = Boolean(authTokenStorage.read());
  const roles = tenantContext.roles.split(',').map((role) => role.trim().toUpperCase()).filter(Boolean);
  const platformOnly = roles.includes('SUPER_ADMIN') && !tenantContext.tenantId && !tenantContext.impersonateTenantId;
  const visibleItems = platformOnly ? items.filter((item) => item.key === '/tenant-access') : items;

  if (!hasToken) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (platformOnly && location.pathname !== '/tenant-access' && location.pathname !== '/change-password') {
    return <Navigate to="/tenant-access" replace />;
  }

  const matched = visibleItems.find((item) => item.key !== '/' && location.pathname.startsWith(item.key));
  const selectedKeys = [matched?.key || visibleItems[0]?.key || '/'];

  return (
    <Layout>
      <Sider trigger={null} collapsible collapsed={collapsed} width={250} style={{ background: '#0f172a' }}>
        <div className="sidebar-logo">{collapsed ? 'RAG' : 'RAG Agent Console'}</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={visibleItems}
          onClick={({ key }) => navigate(key)}
          style={{ background: '#0f172a', borderInlineEnd: 0 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 20px',
            background: token.colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
          <Space size={12}>
            <Text type="secondary">{tenantContext.userId || '-'}</Text>
            <Tag color={tenantContext.impersonateTenantId ? 'warning' : 'blue'}>
              tenant {tenantContext.impersonateTenantId || tenantContext.tenantId || '-'}
            </Tag>
            <Button size="small" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
              Login
            </Button>
            <Button size="small" icon={<LockOutlined />} onClick={() => navigate('/change-password')}>
              Change Password
            </Button>
            {hasToken && (
              <Button
                size="small"
                icon={<LogoutOutlined />}
                onClick={async () => {
                  try {
                    await ragApi.logout();
                  } catch {
                    // Local token cleanup is still the effective logout action for this stateless UI.
                  }
                  authTokenStorage.clear();
                  tenantContextStorage.clear();
                  navigate('/login');
                }}
              >
                Logout
              </Button>
            )}
          </Space>
        </Header>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
