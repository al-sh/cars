import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ChatService } from '../../../core/services/chat.service';

@Component({
  selector: 'app-chat-header',
  standalone: true,
  templateUrl: './chat-header.component.html',
  styleUrl: './chat-header.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatHeaderComponent {
  private readonly chatService = inject(ChatService);

  get currentChat() {
    return this.chatService.currentChat;
  }
}
