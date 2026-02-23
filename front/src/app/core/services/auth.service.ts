import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';

import { User } from '../models/user.model';

const USER_KEY = 'carsai_user';
const TOKEN_KEY = 'carsai_token';
const API_BASE_URL = 'http://localhost:8080/api/v1';

type AuthApiResponse = {
  user: {
    id: string;
    email: string;
    name: string;
  };
  token: string;
};

/**
 * AuthService — сервис авторизации.
 *
 * Использует HttpClient для общения с бэкендом (/api/v1/auth).
 * Хранит пользователя и токен в localStorage для восстановления сессии.
 *
 * login/register возвращают Observable<true | string>:
 * - true — успех
 * - string — сообщение об ошибке
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private userSignal = signal<User | null>(null);
  private isAuthenticatedSignal = signal(false);

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = this.isAuthenticatedSignal.asReadonly();

  constructor() {
    this.checkAuth();
  }

  /**
   * Вход в систему.
   * @returns Observable: true при успехе, строка с ошибкой при неудаче
   */
  login(email: string, password: string): Observable<true | string> {
    return this.http.post<AuthApiResponse>(`${API_BASE_URL}/auth/login`, { email, password }).pipe(
      tap(response => this.handleAuthResponse(response)),
      map(() => true as const),
      catchError(err => {
        const message = err.error?.message ?? 'Ошибка входа';
        return of(message as string);
      }),
    );
  }

  /**
   * Регистрация нового пользователя.
   * @returns Observable: true при успехе, строка с ошибкой при неудаче
   */
  register(name: string, email: string, password: string): Observable<true | string> {
    return this.http
      .post<AuthApiResponse>(`${API_BASE_URL}/auth/register`, { name, email, password })
      .pipe(
        tap(response => this.handleAuthResponse(response)),
        map(() => true as const),
        catchError(err => {
          const message = err.error?.message ?? 'Ошибка регистрации';
          return of(message as string);
        }),
      );
  }

  /**
   * Выход — очистка состояния и localStorage.
   */
  logout(): void {
    this.userSignal.set(null);
    this.isAuthenticatedSignal.set(false);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
  }

  /**
   * Восстановление сессии из localStorage при загрузке приложения.
   */
  private checkAuth(): void {
    const stored = localStorage.getItem(USER_KEY);
    if (!stored) return;

    try {
      const user: User = JSON.parse(stored);
      this.userSignal.set(user);
      this.isAuthenticatedSignal.set(true);
    } catch {
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  private handleAuthResponse(response: AuthApiResponse): void {
    const user: User = {
      id: response.user.id,
      email: response.user.email,
      name: response.user.name,
    };
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this.userSignal.set(user);
    this.isAuthenticatedSignal.set(true);
  }
}
