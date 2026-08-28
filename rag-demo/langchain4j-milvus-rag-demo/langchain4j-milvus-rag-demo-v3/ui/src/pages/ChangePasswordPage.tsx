import { LockOutlined } from '@ant-design/icons';
import { App, Button, Card, Form, Input, Space } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import { getErrorMessage } from '../utils/message';

type FormValues = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export default function ChangePasswordPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();

  const submit = async (values: FormValues) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次输入的新密码不一致');
      return;
    }
    try {
      await ragApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword
      });
      message.success('密码已修改');
      navigate('/');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="修改密码"
        description="修改当前登录用户的本地账号密码。"
        tags={['Account', 'Security']}
      />
      <Card className="page-card" variant="borderless">
        <Form layout="vertical" onFinish={submit} style={{ maxWidth: 520 }}>
          <Form.Item label="当前密码" name="currentPassword" rules={[{ required: true, message: '请输入当前密码' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="current-password" />
          </Form.Item>
          <Form.Item label="新密码" name="newPassword" rules={[{ required: true, message: '请输入新密码' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
          </Form.Item>
          <Form.Item label="确认新密码" name="confirmPassword" rules={[{ required: true, message: '请再次输入新密码' }]}>
            <Input.Password prefix={<LockOutlined />} autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit">保存密码</Button>
        </Form>
      </Card>
    </Space>
  );
}
