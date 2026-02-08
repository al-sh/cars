export type MessageRole = 'user' | 'assistant' | 'system';

export type Message = {
  id: string;
  chatId: string;
  role: MessageRole;
  content: string;
  createdAt: string;
  forwardedFrom?: string;
};
