import { Component } from '@angular/core';

import { AppShell } from './core/app-shell';

@Component({
  selector: 'app-root',
  imports: [AppShell],
  standalone: true,
  template: '<app-app-shell></app-app-shell>',
  styleUrls: ['./app.scss']
})
export class App {}
