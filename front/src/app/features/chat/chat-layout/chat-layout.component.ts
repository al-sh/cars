import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';
import { HeaderComponent } from '../../../shared/components/header/header.component';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [HeaderComponent],
  templateUrl: './chat-layout.component.html',
  styleUrl: './chat-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
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
