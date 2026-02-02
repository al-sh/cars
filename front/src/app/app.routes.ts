import { Routes } from '@angular/router';
import { ChatLayoutComponent } from './features/chat/chat-layout/chat-layout.component';

export const routes: Routes = [
  { path: '', redirectTo: 'chat', pathMatch: 'full' },
  { path: 'chat', component: ChatLayoutComponent },
  { path: 'chat/:id', component: ChatLayoutComponent },
];
