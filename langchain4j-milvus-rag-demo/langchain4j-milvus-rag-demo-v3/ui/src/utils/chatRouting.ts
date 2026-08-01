type AgentChatRouteInput = {
  question?: string;
  imageUrl?: string;
  imageAssetId?: number;
  imageBase64?: string;
};

const WEATHER_TERMS = [
  '天气',
  '气温',
  '温度',
  '下雨',
  '降雨',
  '降水',
  '风力',
  'weather',
  'temperature',
  'rain',
  'forecast',
  'wind'
];

export function shouldRouteToAgentChat(input: AgentChatRouteInput): boolean {
  if (hasImageInput(input)) {
    return false;
  }
  const question = input.question?.trim().toLowerCase();
  if (!question) {
    return false;
  }
  return WEATHER_TERMS.some((term) => question.includes(term));
}

function hasImageInput(input: AgentChatRouteInput): boolean {
  return !!input.imageAssetId || hasText(input.imageUrl) || hasText(input.imageBase64);
}

function hasText(value?: string): boolean {
  return !!value && !!value.trim();
}
