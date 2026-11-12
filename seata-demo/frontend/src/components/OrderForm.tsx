import { ReloadOutlined, SendOutlined } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Space, Switch, Tooltip } from 'antd';
import { useEffect } from 'react';
import type { OrderCreateRequest, TransactionMode } from '../types';

function generateOrderNo(mode: TransactionMode): string {
  const timestamp = Date.now().toString(36).toUpperCase();
  const random = Math.random().toString(36).slice(2, 7).toUpperCase().padEnd(5, '0');
  return `${mode}-${timestamp}-${random}`;
}

interface OrderFormProps {
  mode: TransactionMode;
  loading: boolean;
  onSubmit: (request: OrderCreateRequest) => void;
}

export function OrderForm({ mode, loading, onSubmit }: OrderFormProps) {
  const [form] = Form.useForm<OrderCreateRequest>();

  useEffect(() => {
    form.setFieldValue('orderNo', generateOrderNo(mode));
  }, [form, mode]);

  const handleFinish = (values: OrderCreateRequest) => {
    onSubmit({
      ...values,
      orderNo: values.orderNo.trim(),
      userId: values.userId.trim(),
      commodityCode: values.commodityCode.trim(),
    });
  };

  return (
    <section className="console-card order-card" aria-labelledby="order-heading">
      <div className="card-heading">
        <div>
          <span className="section-kicker">02 / 构造请求</span>
          <h2 id="order-heading">创建一笔订单</h2>
        </div>
        <span className="method-badge">POST</span>
      </div>
      <p className="endpoint">/order/{mode.toLowerCase()}/create</p>

      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        disabled={loading}
        onFinish={handleFinish}
        initialValues={{
          orderNo: generateOrderNo(mode),
          userId: 'U10001',
          commodityCode: 'C10001',
          count: 1,
          money: 99,
          mockException: false,
        }}
        className="order-form"
      >
        <Form.Item
          label="业务订单号"
          htmlFor="orderNo"
          required
          extra="全链路幂等键，每次请求请使用唯一订单号"
        >
          <Space.Compact block>
            <Form.Item
              name="orderNo"
              noStyle
              rules={[
                { required: true, whitespace: true, message: '请输入业务订单号' },
                { max: 64, message: '订单号不能超过 64 个字符' },
              ]}
            >
              <Input id="orderNo" placeholder="请输入唯一订单号" />
            </Form.Item>
            <Tooltip title="生成新订单号">
              <Button
                className="regenerate-button"
                aria-label="重新生成订单号"
                icon={<ReloadOutlined />}
                onClick={() => form.setFieldValue('orderNo', generateOrderNo(mode))}
              />
            </Tooltip>
          </Space.Compact>
        </Form.Item>

        <div className="form-row">
          <Form.Item
            name="userId"
            label="用户 ID"
            rules={[{ required: true, whitespace: true, message: '请输入用户 ID' }]}
          >
            <Input placeholder="U10001" />
          </Form.Item>
          <Form.Item
            name="commodityCode"
            label="商品编码"
            rules={[{ required: true, whitespace: true, message: '请输入商品编码' }]}
          >
            <Input placeholder="C10001" />
          </Form.Item>
        </div>

        <div className="form-row">
          <Form.Item
            name="count"
            label="购买数量"
            rules={[
              { required: true, message: '请输入购买数量' },
              {
                validator: (_, value: number | undefined) =>
                  value && Number.isInteger(value) && value > 0
                    ? Promise.resolve()
                    : Promise.reject(new Error('数量必须为正整数')),
              },
            ]}
          >
            <InputNumber min={1} precision={0} placeholder="1" />
          </Form.Item>
          <Form.Item
            name="money"
            label="订单金额（元）"
            rules={[
              { required: true, message: '请输入订单金额' },
              {
                validator: (_, value: number | undefined) => {
                  if (!value || value <= 0) return Promise.reject(new Error('金额必须大于 0'));
                  return /^\d+(\.\d{1,2})?$/.test(String(value))
                    ? Promise.resolve()
                    : Promise.reject(new Error('金额最多保留两位小数'));
                },
              },
            ]}
          >
            <InputNumber min={0.01} precision={2} prefix="¥" placeholder="99.00" />
          </Form.Item>
        </div>

        <Form.Item
          name="mockException"
          label="执行场景"
          valuePropName="checked"
          extra="开启后将在业务链路末端注入异常，用于观察回滚或补偿"
          className="scenario-field"
        >
          <Switch checkedChildren="模拟异常" unCheckedChildren="正常下单" />
        </Form.Item>

        <Button
          type="primary"
          htmlType="submit"
          size="large"
          loading={loading}
          icon={<SendOutlined />}
          aria-label={loading ? '事务执行中' : `发起 ${mode} 事务`}
          className="submit-button"
        >
          {loading ? '事务执行中' : `发起 ${mode} 事务`}
        </Button>
        <p className="submit-hint">每次操作仅发送一次请求，请勿在事务结束前重复提交</p>
      </Form>
    </section>
  );
}
