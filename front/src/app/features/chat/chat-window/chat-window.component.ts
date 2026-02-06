import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ChatService } from '../../../core/services/chat.service';
import { ChatHeaderComponent } from '../chat-header/chat-header.component';
import { MessageInputComponent } from '../message-input/message-input.component';
import { MessageListComponent } from '../message-list/message-list.component';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [ChatHeaderComponent, MessageInputComponent, MessageListComponent],
  templateUrl: './chat-window.component.html',
  styleUrl: './chat-window.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatWindowComponent {
  private readonly chatService = inject(ChatService);

  get currentChat() {
    return this.chatService.currentChat;
  }
}
