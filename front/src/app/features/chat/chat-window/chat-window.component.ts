import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ChatService } from '../../../core/services/chat.service';
import { ChatHeaderComponent } from '../chat-header/chat-header.component';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [ChatHeaderComponent],
  templateUrl: './chat-window.component.html',
  styleUrl: './chat-window.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatWindowComponent {
  private readonly chatService = inject(ChatService);

  get currentChat() {
    return this.chatService.currentChat;
  }

  get currentMessages() {
    return this.chatService.currentMessages;
  }
}
