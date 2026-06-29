import { Card, Tag, Typography, Divider, Space } from 'antd';
import type { WordCardDTO } from '../types/word';
const { Title, Paragraph, Text } = Typography;
export default function WordCardPreview({ data }: { data: WordCardDTO }) {
  return <Card style={{ borderRadius: 18, width: 760, background: 'linear-gradient(135deg,#ffffff,#f5f7ff)' }}>
    <Space direction="vertical" size={4} style={{ width: '100%' }}>
      <Title level={2} style={{ margin: 0 }}>{data.word} <Text type="secondary" style={{fontSize:20}}>{data.phonetic}</Text></Title>
      <Tag color="blue">{data.partOfSpeech || 'word'}</Tag>
      <Divider />
      <Title level={5}>English Definition</Title><Paragraph>{data.englishDefinition}</Paragraph>
      <Title level={5}>中文含义</Title><Paragraph>{data.chineseMeaning}</Paragraph>
      {data.usageNote && <><Title level={5}>Usage Note</Title><Paragraph>{data.usageNote}</Paragraph></>}
      <Title level={5}>Slang / Informal Expressions</Title>
      {(data.slangs || []).map((s, i) => <Paragraph key={i}><b>{s.phrase}</b> — {s.meaning}<br/><Text type="secondary">{s.example}</Text></Paragraph>)}
      <Title level={5}>Example Sentences</Title>
      {(data.examples || []).map((e, i) => <Paragraph key={i}>{e.sentence}<br/><Text type="secondary">{e.translation} {e.scene ? ` · ${e.scene}` : ''}</Text></Paragraph>)}
      <Space wrap>{(data.tags || []).map(t => <Tag key={t}>{t}</Tag>)}</Space>
    </Space>
  </Card>
}
