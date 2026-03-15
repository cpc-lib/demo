import { Card, Space, Tag, Typography } from 'antd';
import type { ReactNode } from 'react';

const { Title, Paragraph } = Typography;

interface Props {
  title: string;
  description: string;
  extra?: ReactNode;
  tags?: string[];
}

export default function PageHeaderCard({ title, description, extra, tags }: Props) {
  return (
    <Card className="page-card page-section" bordered={false}>
      <Space direction="vertical" size={6} style={{ width: '100%' }}>
        <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
          <div>
            <Title level={3} style={{ margin: 0 }}>
              {title}
            </Title>
            <Paragraph type="secondary" style={{ margin: '8px 0 0' }}>
              {description}
            </Paragraph>
          </div>
          {extra}
        </Space>
        {tags?.length ? (
          <Space wrap>
            {tags.map((tag) => (
              <Tag key={tag}>{tag}</Tag>
            ))}
          </Space>
        ) : null}
      </Space>
    </Card>
  );
}
