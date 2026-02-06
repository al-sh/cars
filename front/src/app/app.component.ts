import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * AppComponent — корневой компонент приложения.
 *
 * changeDetection: ChangeDetectionStrategy.OnPush — оптимизация производительности.
 *
 * По умолчанию Angular использует стратегию Default: при ЛЮБОМ событии
 * (клик, HTTP-ответ, setTimeout) Angular проверяет ВСЕ компоненты на изменения.
 *
 * С OnPush Angular проверяет компонент только когда:
 * 1. Изменился @Input() (по ссылке, не по содержимому)
 * 2. Произошло событие внутри компонента (клик, ввод)
 * 3. Изменился Signal, используемый в шаблоне
 *
 * Для корневого компонента это особенно важно:
 * без OnPush каждое событие запускает проверку от корня вниз по всему дереву.
 *
 * Сравнение с React: в React компонент перерисовывается только при изменении
 * state/props — это поведение ближе к OnPush. В Angular нужно указать явно.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  title = 'ui';
}
