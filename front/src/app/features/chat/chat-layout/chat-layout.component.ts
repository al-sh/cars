import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

@Component({
  selector: 'app-chat-layout',
  imports: [],
  templateUrl: './chat-layout.component.html',
  styleUrl: './chat-layout.component.css'
})
export class ChatLayoutComponent {
  private route = inject(ActivatedRoute);

  // Получаем параметр id из роута как Signal
  readonly chatId = toSignal(
    this.route.paramMap.pipe(
      map(params => params.get('id'))
    ),
    { initialValue: null }
  );
}
