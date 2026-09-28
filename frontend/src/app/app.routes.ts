import { Routes } from '@angular/router';
import { TotalComponent } from './components/total/total';
import { AddComponent } from './components/add/add';
import { ListComponent } from './components/list/list';
import { AudioComponent } from './components/audio/audio';

export const routes: Routes = [
  { path: '', component: TotalComponent },
  { path: 'add', component: AddComponent },
  { path: 'list', component: ListComponent },
  { path: 'audio', component: AudioComponent },
  { path: '**', redirectTo: '' },
];