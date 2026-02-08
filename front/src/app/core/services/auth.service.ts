import { Injectable, signal } from '@angular/core';

import { User } from '../models/user.model';

const STORAGE_KEY = 'carsai_user';

/**
 * AuthService — сервис авторизации (мок-реализация).
 *
 * При подключении бэкенда login/register заменятся на HTTP-запросы.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private userSignal = signal<User | null>(null);
  private isAuthenticatedSignal = signal(false);

  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = this.isAuthenticatedSignal.asReadonly();

  constructor() {
    this.checkAuth();
  }

  /**
   * Мок-логин. Принимает любой email/password, создаёт пользователя.
   * @returns true при успехе, строку с ошибкой при неудаче
   */
  login(email: string, password: string): true | string {
    if (!email || !password) {
      return 'Заполните все поля';
    }

    const user: User = {
      id: crypto.randomUUID(),
      email,
      name: email.split('@')[0],
    };

    this.setUser(user);
    return true;
  }

  /**
   * Мок-регистрация. Создаёт пользователя с указанным именем.
   * @returns true при успехе, строку с ошибкой при неудаче
   */
  register(name: string, email: string, password: string): true | string {
    if (!name || !email || !password) {
      return 'Заполните все поля';
    }

    const user: User = {
      id: crypto.randomUUID(),
      email,
      name,
    };

    this.setUser(user);
    return true;
  }

  /**
   * Выход — очистка состояния и localStorage.
   */
  logout(): void {
    this.userSignal.set(null);
    this.isAuthenticatedSignal.set(false);
    localStorage.removeItem(STORAGE_KEY);
  }

  /**
   * Восстановление сессии из localStorage при загрузке.
   */
  private checkAuth(): void {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return;

    try {
      const user: User = JSON.parse(stored);
      this.userSignal.set(user);
      this.isAuthenticatedSignal.set(true);
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    }
  }

  private setUser(user: User): void {
    this.userSignal.set(user);
    this.isAuthenticatedSignal.set(true);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  }
}
