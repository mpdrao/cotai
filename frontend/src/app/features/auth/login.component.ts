import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/copy/ui-copy';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';
import { FieldMessages, fieldError } from './field-error';

const MESSAGES: Record<'email' | 'password', FieldMessages> = {
  email: { required: 'Informe seu e-mail.', email: 'Confira o e-mail, parece que falta alguma coisa.' },
  password: { required: 'Informe sua senha.' },
};

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink, CotaiButtonComponent, CotaiCardComponent],
  template: `
    <div class="auth">
      <header class="intro">
        <p class="cotai-eyebrow">Junta pra fazer acontecer.</p>
        <h1 class="cotai-page-title">Entrar</h1>
        <p class="cotai-lead">Acesse suas listas para editar itens, compartilhar o link e acompanhar as cotas.</p>
      </header>
      <div class="auth-content">
        <cotai-card elevation="soft">
          <form class="form" (ngSubmit)="submit(form)" #form="ngForm">
            <label class="cotai-field">
              <span class="cotai-label">E-mail</span>
              <input class="cotai-input" id="login-email" name="email" type="email" inputmode="email" autocomplete="email"
                     required email [(ngModel)]="email" #emailField="ngModel"
                     [attr.aria-invalid]="!!fieldError(emailField, messages.email)"
                     [attr.aria-describedby]="fieldError(emailField, messages.email) ? 'login-email-error' : null" />
              @if (fieldError(emailField, messages.email); as message) {
                <span class="cotai-field-error" id="login-email-error">{{ message }}</span>
              }
            </label>
            <label class="cotai-field">
              <span class="cotai-label">Senha</span>
              <input class="cotai-input" id="login-password" name="password" type="password" autocomplete="current-password"
                     required [(ngModel)]="password" #passwordField="ngModel"
                     [attr.aria-invalid]="!!fieldError(passwordField, messages.password)"
                     [attr.aria-describedby]="fieldError(passwordField, messages.password) ? 'login-password-error' : null" />
              @if (fieldError(passwordField, messages.password); as message) {
                <span class="cotai-field-error" id="login-password-error">{{ message }}</span>
              }
            </label>
            @if (error()) {
              <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
            }
            <cotai-button type="submit" [fullWidth]="true" [loading]="loading()">Entrar</cotai-button>
          </form>
        </cotai-card>
        <p class="switch">Ainda não tem conta? <a routerLink="/register" [queryParams]="{ redirect: redirect }">Criar conta</a></p>
      </div>
    </div>
  `,
  styleUrl: './auth.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly redirect = inject(ActivatedRoute).snapshot.queryParamMap.get('redirect') || '/dashboard';

  protected readonly messages = MESSAGES;
  protected readonly fieldError = fieldError;

  protected email = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected submit(form: NgForm): void {
    if (form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => this.router.navigateByUrl(this.redirect),
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
