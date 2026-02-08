import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ChatLayoutComponent } from './features/chat/chat-layout/chat-layout.component';

export const routes: Routes = [
  { path: '', redirectTo: 'chat', pathMatch: 'full' },
  { path: 'auth/login', component: LoginComponent },
  { path: 'auth/register', component: RegisterComponent },
  { path: 'chat', component: ChatLayoutComponent, canActivate: [authGuard] },
  { path: 'chat/:id', component: ChatLayoutComponent, canActivate: [authGuard] },
];
