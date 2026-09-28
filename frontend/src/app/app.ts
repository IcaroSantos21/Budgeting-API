import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  links = [
    { path: '/', label: 'Dashboard' },
    { path: '/add', label: 'Adicionar' },
    { path: '/list', label: 'Transações' },
    { path: '/audio', label: 'Áudio' },
  ];

  exactMatch = { exact: true };
  activeClass = 'bg-red-600 text-white';
  inactiveClass = 'text-zinc-600 hover:bg-red-50 dark:text-zinc-300 dark:hover:bg-zinc-800';

  dark = signal(false);

  constructor() {
    const saved = localStorage.getItem('theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.setDark(saved ? saved === 'dark' : prefersDark);
  }

  toggleTheme(): void {
    this.setDark(!this.dark());
  }

  private setDark(value: boolean): void {
    this.dark.set(value);
    document.documentElement.classList.toggle('dark', value);
    localStorage.setItem('theme', value ? 'dark' : 'light');
  }
}