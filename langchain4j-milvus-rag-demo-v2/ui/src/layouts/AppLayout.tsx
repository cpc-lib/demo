import {
  ApiOutlined,
  DatabaseOutlined,
  FileSearchOutlined,
  MessageOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons';
import { Button, Layout, Menu, theme } from 'antd';
import { useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

const { Header, Sider, Content } = Layout;

const items = [
  { key: '/', icon: <MessageOutlined />, label: '智能问答' },
  { key: '/knowledge', icon: <FileSearchOutlined />, label: '知识库导入' },
  { key: '/vector-stores', icon: <DatabaseOutlined />, label: '向量库配置' },
  { key: '/collections', icon: <ApiOutlined />, label: 'Milvus 集合管理' }
];

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKeys = useMemo(() => {
    const matched = items.find((item) => item.key !== '/' && location.pathname.startsWith(item.key));
    return [matched?.key || '/'];
  }, [location.pathname]);

  return (
    <Layout>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        width={250}
        style={{ background: '#0f172a' }}
      >
        <div className="sidebar-logo">{collapsed ? 'RAG' : 'RAG Agent Console'}</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={items}
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
            alignItems: 'center'
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
          />
        </Header>
        <Content style={{ padding: 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
