export type MessageRole = 'user' | 'assistant' | 'system';

export type Message = {
  id: string;
  chat_id: string; //TODO: рефакторинг на camelCase и - а не убрать ли его вообще?
  role: MessageRole;
  content: string;
  created_at: string; //TODO: рефакторинг на camelCase
  forwardedFrom?: string; // ID чата, из которого было переслано сообщение (опционально)
};
