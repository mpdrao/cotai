import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  template: `
    <h1>Cota aí</h1>
    <p>Lista de presentes com cotas para qualquer evento.</p>
    <p>Abra o link de uma lista (<code>/l/&lt;slug&gt;</code>) ou entre em <a href="/dashboard">/dashboard</a>.</p>
  `,
})
export class HomeComponent {}
