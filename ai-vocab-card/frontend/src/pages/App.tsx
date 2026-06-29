import { useRef, useState } from 'react';
import { Button, Card, Col, Form, Input, Layout, List, message, Modal, Row, Space, Typography } from 'antd';
import { BookOutlined, DownloadOutlined, KeyOutlined, LockOutlined, NumberOutlined, SaveOutlined, SearchOutlined, UserOutlined } from '@ant-design/icons';
import { toPng } from 'html-to-image';
import WordCardPreview from '../components/WordCardPreview';
import { addToWordBook, changePassword, createAnkiExportTask, exportAnkiBlob, generateWord, getDueWords, getExportTask, getWordDetail, keywordSearch, login, register, saveWord, searchWordsPage, semanticSearch, submitReview } from '../api/wordApi';
import type { WordCardDTO } from '../types/word';
import 'antd/dist/reset.css';

const empty: WordCardDTO = { word: '', englishDefinition: '', tags: [], slangs: [], examples: [] };

const getErrorMessage = (error: unknown, fallback: string) => {
  const data = (error as { response?: { data?: { message?: string } } })?.response?.data;
  if (data?.message) return data.message;
  if (error instanceof Error && error.message) return error.message;
  return fallback;
};

export default function App() {
  const [form] = Form.useForm<WordCardDTO>();
  const [authForm] = Form.useForm<{username:string;password:string;userCode?:string;confirmPassword?:string}>();
  const [pwdForm] = Form.useForm<{oldPassword:string;newPassword:string;confirmNewPassword:string}>();
  const [data, setData] = useState<WordCardDTO>(empty);
  const [list, setList] = useState<WordCardDTO[]>([]);
  const [loggedIn, setLoggedIn] = useState(!!localStorage.getItem('token'));
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [addingToBook, setAddingToBook] = useState(false);
  const [searchTotal, setSearchTotal] = useState(0);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedId, setSelectedId] = useState<number>();
  const [pwdModalOpen, setPwdModalOpen] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);

  const onAuth = async (type: 'login' | 'register') => {
    const values = await authForm.validateFields();
    if (type === 'register') {
      const res = await register(values.username, values.userCode!, values.password, values.confirmPassword!);
      localStorage.setItem('token', res.token);
      localStorage.setItem('username', res.username);
      setLoggedIn(true);
      message.success('注册成功');
    } else {
      const res = await login(values.username, values.password);
      localStorage.setItem('token', res.token);
      localStorage.setItem('username', res.username);
      setLoggedIn(true);
      message.success('登录成功');
    }
  };

  const onChangePassword = async () => {
    const values = await pwdForm.validateFields();
    await changePassword(values.oldPassword, values.newPassword, values.confirmNewPassword);
    message.success('密码修改成功');
    pwdForm.resetFields();
    setPwdModalOpen(false);
  };

  const onGenerate = async () => {
    try {
      const word = form.getFieldValue('word');
      if (!word) return message.warning('请输入英文单词');
      setGenerating(true);
      const res = await generateWord(word);
      setData(res);
      form.setFieldsValue(res);
      message.success('AI 生成完成');
    } catch (error) {
      message.error(getErrorMessage(error, 'AI 生成失败'));
    } finally {
      setGenerating(false);
    }
  };
  const sync = () => setData(form.getFieldsValue(true) as WordCardDTO);
  const saveCurrentWord = async () => {
    await form.validateFields();
    const values = form.getFieldsValue(true) as WordCardDTO;
    const res = await saveWord(values);
    const saved = await getWordDetail(res.id);
    setData(saved);
    form.setFieldsValue(saved);
    setSelectedId(saved.id);
    return saved;
  };
  const onSave = async () => {
    try {
      setSaving(true);
      await saveCurrentWord();
      message.success('保存成功');
    } catch (error) {
      message.error(getErrorMessage(error, '保存失败'));
    } finally {
      setSaving(false);
    }
  };
  const setSearchResult = (items: WordCardDTO[], total = items.length) => {
    setList(items);
    setSearchTotal(total);
  };
  const onSearch = async (keyword: string) => {
    setSearchLoading(true);
    try {
      const res = await searchWordsPage(keyword);
      setSearchResult(res.items, res.total);
    } catch (error) {
      message.error(getErrorMessage(error, '查询失败'));
    } finally {
      setSearchLoading(false);
    }
  };
  const onKeywordSearch = async (keyword: string) => {
    setSearchLoading(true);
    try {
      const items = (await keywordSearch(keyword)).map((x: any) => x.detail || x);
      setSearchResult(items);
    } catch (error) {
      message.error(getErrorMessage(error, '关键词搜索失败'));
    } finally {
      setSearchLoading(false);
    }
  };
  const onSemanticSearch = async (keyword: string) => {
    setSearchLoading(true);
    try {
      const items = (await semanticSearch(keyword, 10)).map((x: any) => x.detail || x);
      setSearchResult(items);
    } catch (error) {
      message.error(getErrorMessage(error, '语义搜索失败'));
    } finally {
      setSearchLoading(false);
    }
  };
  const onDetail = async (id?: number | string) => {
    const numericId = Number(id);
    if (!Number.isFinite(numericId) || numericId <= 0) return;
    const res = await getWordDetail(numericId);
    setData(res);
    form.setFieldsValue(res);
    setSelectedId(res.id);
  };
  const onAddToBook = async () => {
    try {
      setAddingToBook(true);
      const saved = await saveCurrentWord();
      if (!saved.id) throw new Error('词卡保存失败，无法加入词库');
      await addToWordBook(saved.id);
      message.success('已保存并加入个人词库');
    } catch (error) {
      message.error(getErrorMessage(error, '加入词库失败'));
    } finally {
      setAddingToBook(false);
    }
  };
  const onDueWords = async () => { const items = await getDueWords(); setSearchResult(items); };
  const onReview = async (result: 0 | 1 | 2) => { if (!data.id) return message.warning('请先选择词卡'); await submitReview(data.id, result); message.success('复习计划已更新'); };
  const onSyncAnki = async () => {
    const blob = await exportAnkiBlob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'anki-vocab.tsv'; a.click();
    URL.revokeObjectURL(url);
  };
  const onAsyncAnki = async () => {
    const task = await createAnkiExportTask();
    message.info(`导出任务已创建：${task.taskId}`);
    setTimeout(async () => {
      const latest = await getExportTask(task.taskId);
      if (latest.status === 'SUCCESS' && latest.fileUrl) window.open(latest.fileUrl, '_blank');
      else if (latest.status === 'FAILED') message.error(latest.errorMessage || '导出失败');
      else message.info('任务处理中，可稍后查询接口 /api/export/{taskId}');
    }, 1500);
  };
  const exportPng = async () => {
    if (!cardRef.current) return;
    const url = await toPng(cardRef.current, { pixelRatio: 2, backgroundColor: '#ffffff' });
    const a = document.createElement('a'); a.href = url; a.download = `${data.word || 'word-card'}.png`; a.click();
  };

  if (!loggedIn) return <Layout style={{ minHeight: '100vh', background: '#f5f5f5', alignItems: 'center', justifyContent: 'center', padding: 24 }}>
    <div style={{ width: '100%', maxWidth: 360 }}>
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <Typography.Title level={2} style={{ marginBottom: 8 }}>词芽</Typography.Title>
        <Typography.Text type="secondary">登录后开始生成、管理和复习英文词卡</Typography.Text>
      </div>
      <Card bordered={false} style={{ boxShadow: '0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.05)' }}>
        <Form form={authForm} layout="vertical" requiredMark={false}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
          </Form.Item>
          {authMode === 'register' && (
            <Form.Item name="userCode" rules={[
              { required: true, message: '请输入用户编号' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9]*$/, message: '只能包含英文字母和数字，且必须以字母开头' },
            ]}>
              <Input prefix={<NumberOutlined />} placeholder="用户编号（字母开头，仅字母和数字）" size="large" />
            </Form.Item>
          )}
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
          </Form.Item>
          {authMode === 'register' && (
            <Form.Item name="confirmPassword" dependencies={['password']} rules={[
              { required: true, message: '请再次输入密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) return Promise.resolve();
                  return Promise.reject(new Error('两次输入的密码不一致'));
                },
              }),
            ]}>
              <Input.Password prefix={<LockOutlined />} placeholder="确认密码" size="large" />
            </Form.Item>
          )}
          <Form.Item style={{ marginBottom: 12 }}>
            <Button type="primary" size="large" block onClick={() => onAuth(authMode)}>
              {authMode === 'login' ? '登录' : '注册'}
            </Button>
          </Form.Item>
          <Button type="link" block onClick={() => {
            authForm.resetFields();
            setAuthMode(authMode === 'login' ? 'register' : 'login');
          }}>
            {authMode === 'login' ? '没有账号？点击注册' : '已有账号？点击登录'}
          </Button>
        </Form>
      </Card>
    </div>
  </Layout>;

  return <Layout style={{ minHeight: '100vh', padding: 24 }}>
    <Space style={{ justifyContent: 'space-between', width: '100%' }}>
      <Typography.Title>词芽</Typography.Title>
      <Space>
        <Button icon={<KeyOutlined />} onClick={() => setPwdModalOpen(true)}>修改密码</Button>
        <Button onClick={() => { localStorage.clear(); setLoggedIn(false); }}>退出</Button>
      </Space>
    </Space>
    <Row gutter={24}>
      <Col span={10}>
        <Card title="生成 / 编辑词卡" bordered={false}>
          <Form form={form} layout="vertical" initialValues={empty} onValuesChange={sync}>
            <Form.Item name="id" hidden><Input /></Form.Item>
            <Form.Item label="英文单词" name="word" rules={[{ required: true }]}><Input placeholder="awesome" /></Form.Item>
            <Space wrap><Button type="primary" loading={generating} onClick={onGenerate}>AI 生成</Button><Button icon={<SaveOutlined />} loading={saving} onClick={onSave}>保存</Button><Button icon={<DownloadOutlined />} onClick={exportPng}>PNG</Button><Button icon={<BookOutlined />} loading={addingToBook} onClick={onAddToBook}>加入词库</Button></Space>
            <Form.Item label="音标" name="phonetic"><Input /></Form.Item>
            <Form.Item label="词性" name="partOfSpeech"><Input /></Form.Item>
            <Form.Item label="英文解释" name="englishDefinition" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
            <Form.Item label="中文含义" name="chineseMeaning"><Input.TextArea rows={2} /></Form.Item>
            <Form.Item label="用法说明" name="usageNote"><Input.TextArea rows={2} /></Form.Item>
            <Form.List name="slangs">{(fields, { add, remove }) => <><Typography.Title level={5}>俚语/口语</Typography.Title>{fields.map(f => <Card size="small" key={f.key} style={{marginBottom:8}}><Form.Item label="短语" name={[f.name,'phrase']}><Input /></Form.Item><Form.Item label="解释" name={[f.name,'meaning']}><Input /></Form.Item><Form.Item label="例句" name={[f.name,'example']}><Input /></Form.Item><Button danger onClick={() => remove(f.name)}>删除</Button></Card>)}<Button onClick={() => add()}>新增俚语</Button></>}</Form.List>
            <Form.List name="examples">{(fields, { add, remove }) => <><Typography.Title level={5}>英文例句</Typography.Title>{fields.map(f => <Card size="small" key={f.key} style={{marginBottom:8}}><Form.Item label="句子" name={[f.name,'sentence']}><Input /></Form.Item><Form.Item label="翻译" name={[f.name,'translation']}><Input /></Form.Item><Form.Item label="场景" name={[f.name,'scene']}><Input /></Form.Item><Button danger onClick={() => remove(f.name)}>删除</Button></Card>)}<Button onClick={() => add()}>新增例句</Button></>}</Form.List>
          </Form>
        </Card>
      </Col>
      <Col span={14}>
        <Card title="AI 生成效果" extra={<Button onClick={exportPng}>导出 PNG</Button>} bordered={false}><div ref={cardRef}><WordCardPreview data={data} /></div></Card>
        <Card title="复习 / 导出" style={{marginTop:16}} bordered={false}>
          <Space wrap>
            <Button onClick={onDueWords}>今日待复习</Button>
            <Button onClick={() => onReview(0)}>忘记</Button><Button onClick={() => onReview(1)}>模糊</Button><Button type="primary" onClick={() => onReview(2)}>记住</Button>
            <Button onClick={onSyncAnki}>同步导出 Anki</Button><Button onClick={onAsyncAnki}>异步导出到 MinIO</Button>
          </Space>
        </Card>
        <Card title="中文/英文意义查询" style={{marginTop:16}} bordered={false}>
          <Input.Search enterButton={<SearchOutlined />} loading={searchLoading} placeholder="基础查询：很棒 / daily / awesome" onSearch={onSearch} />
          <Space style={{marginTop:12}} wrap>
            <Input.Search style={{width:320}} enterButton="关键词搜索" placeholder="中文/英文关键词" onSearch={onKeywordSearch} />
            <Input.Search style={{width:320}} enterButton="语义搜索" placeholder="表示开心、很厉害、正式表达" onSearch={onSemanticSearch} />
          </Space>
          <Typography.Text type="secondary" style={{ display: 'block', marginTop: 12 }}>共 {searchTotal} 条，点击结果可在右上方查看完整词卡详情</Typography.Text>
          <List style={{marginTop:8}} loading={searchLoading} dataSource={list} renderItem={item => <List.Item onClick={() => onDetail(item.id)} style={{cursor:'pointer', background: selectedId === item.id ? '#f0f7ff' : undefined, paddingInline: 12, borderRadius: 6}}><List.Item.Meta title={item.word} description={`${item.chineseMeaning || ''} | ${item.englishDefinition || ''}`} /></List.Item>} />
        </Card>
      </Col>
    </Row>

    <Modal title="修改密码" open={pwdModalOpen} onOk={onChangePassword} onCancel={() => { pwdForm.resetFields(); setPwdModalOpen(false); }} okText="确认修改" cancelText="取消">
      <Form form={pwdForm} layout="vertical" requiredMark={false} style={{ marginTop: 16 }}>
        <Form.Item name="oldPassword" rules={[{ required: true, message: '请输入旧密码' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="旧密码" size="large" />
        </Form.Item>
        <Form.Item name="newPassword" rules={[{ required: true, min: 6, message: '新密码至少6个字符' }]}>
          <Input.Password prefix={<LockOutlined />} placeholder="新密码" size="large" />
        </Form.Item>
        <Form.Item name="confirmNewPassword" dependencies={['newPassword']} rules={[
          { required: true, message: '请再次输入新密码' },
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
              return Promise.reject(new Error('两次输入的新密码不一致'));
            },
          }),
        ]}>
          <Input.Password prefix={<LockOutlined />} placeholder="确认新密码" size="large" />
        </Form.Item>
      </Form>
    </Modal>
  </Layout>;
}
