import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

import { ChatService } from '../../../core/services/chat.service';
import { HeaderComponent } from '../../../shared/components/header/header.component';
import { ChatListComponent } from '../chat-list/chat-list.component';
import { ChatWindowComponent } from '../chat-window/chat-window.component';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [HeaderComponent, ChatListComponent, ChatWindowComponent],
  templateUrl: './chat-layout.component.html',
  styleUrl: './chat-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatLayoutComponent {
  private route = inject(ActivatedRoute);
  private chatService = inject(ChatService);

  readonly chatId = toSignal(
    this.route.paramMap.pipe(
      map(params => params.get('id'))
    ),
    { initialValue: null }
  );

  constructor() {
    this.chatService.loadChats();

    effect(() => {
      const id = this.chatId();
      if (id) {
        this.chatService.selectChat(id);
      } else {
        this.chatService.clearSelection();
      }
    });
  }
}
