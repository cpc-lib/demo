import { shouldRouteToAgentChat } from './chatRouting';

if (!shouldRouteToAgentChat({ question: '明天天气如何' })) {
  throw new Error('weather questions should route to agent chat so weather tools can run');
}

if (!shouldRouteToAgentChat({ question: 'Will it rain tomorrow in Shanghai?' })) {
  throw new Error('English weather questions should route to agent chat');
}

if (shouldRouteToAgentChat({ question: '明天天气如何', imageBase64: 'data:image/png;base64,abc' })) {
  throw new Error('image questions should stay on the multimodal RAG path');
}

if (shouldRouteToAgentChat({ question: '解释这个知识库里的架构图' })) {
  throw new Error('ordinary knowledge-base questions should stay on the RAG path');
}
