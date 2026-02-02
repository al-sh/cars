export type MessageRole = 'user' | 'assistant' | 'system';

export type Message = {
  id: string;
  chat_id: string;
  role: MessageRole;
  content: string;
  created_at: string;
};
