import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TotalComponent } from './components/total/total';
import { AddComponent } from './components/add/add';
import { ListComponent } from './components/list/list';
import { AudioComponent } from './components/audio/audio';

@Component({
  imports: [RouterOutlet, TotalComponent, AddComponent, ListComponent, AudioComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
}