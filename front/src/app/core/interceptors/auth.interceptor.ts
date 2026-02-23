import { HttpInterceptorFn } from '@angular/common/http';

const TOKEN_KEY = 'carsai_token';

/**
 * HTTP-интерцептор для добавления JWT-токена в заголовки запросов.
 *
 * Функциональный интерцептор (Angular 15+) — проще классового,
 * не требует отдельного Injectable сервиса.
 *
 * Аналог Express middleware: добавляет Authorization header к каждому запросу.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem(TOKEN_KEY);

  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(authReq);
};
