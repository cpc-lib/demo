import {
  ClockCircleOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import { Layout, Menu, Typography } from 'antd'
import { ArticlesPage } from './features/articles/ArticlesPage'

const { Header, Sider, Content } = Layout

export default function App() {
  return (
    <Layout className="app-shell">
      <Sider width={224} className="app-sider" breakpoint="lg" collapsedWidth="0">
        <div className="brand">
          <div className="brand-mark"><ClockCircleOutlined /></div>
          <div>
            <Typography.Text className="brand-title">Article Scheduler</Typography.Text>
            <Typography.Text className="brand-subtitle">Redis · Kafka</Typography.Text>
          </div>
        </div>

        <Menu
          mode="inline"
          selectedKeys={['articles']}
          items={[
            {
              key: 'articles',
              icon: <FileTextOutlined />,
              label: '文章管理',
            },
          ]}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <Typography.Text type="secondary">可靠延时发布控制台</Typography.Text>
          <div className="environment-badge">ADMIN</div>
        </Header>
        <Content className="app-content">
          <ArticlesPage />
        </Content>
      </Layout>
    </Layout>
  )
}
