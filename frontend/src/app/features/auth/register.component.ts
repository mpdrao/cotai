import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/copy/ui-copy';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';
import { FieldMessages, fieldError } from './field-error';

// Same limits as RegisterRequest in the API.
const MESSAGES: Record<'name' | 'email' | 'password', FieldMessages> = {
  name: { required: 'Informe seu nome.', minlength: 'O nome precisa ter pelo menos 2 letras.' },
  email: { required: 'Informe seu e-mail.', email: 'Confira o e-mail, parece que falta alguma coisa.' },
  password: { required: 'Crie uma senha.', minlength: 'A senha precisa ter pelo menos 8 caracteres.' },
};

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink, CotaiButtonComponent, CotaiCardComponent],
  template: `
    <div class="auth">
      <header class="intro">
        <p class="cotai-eyebrow">Junta pra fazer acontecer.</p>
        <h1 class="cotai-page-title">Criar conta</h1>
        <p class="cotai-lead">Com a conta você monta listas para qualquer evento. Quem contribui não precisa se cadastrar.</p>
      </header>
      <div class="auth-content">
        <cotai-card elevation="soft">
          <form class="form" (ngSubmit)="submit(form)" #form="ngForm">
            <label class="cotai-field">
              <span class="cotai-label">Seu nome</span>
              <input class="cotai-input" id="register-name" name="name" autocomplete="name" required minlength="2"
                     maxlength="80" [(ngModel)]="name" #nameField="ngModel"
                     [attr.aria-invalid]="!!fieldError(nameField, messages.name)"
                     [attr.aria-describedby]="fieldError(nameField, messages.name) ? 'register-name-error' : null" />
              @if (fieldError(nameField, messages.name); as message) {
                <span class="cotai-field-error" id="register-name-error">{{ message }}</span>
              }
            </label>
            <label class="cotai-field">
              <span class="cotai-label">E-mail</span>
              <input class="cotai-input" id="register-email" name="email" type="email" inputmode="email"
                     autocomplete="email" required email maxlength="254" [(ngModel)]="email" #emailField="ngModel"
                     [attr.aria-invalid]="!!fieldError(emailField, messages.email)"
                     [attr.aria-describedby]="fieldError(emailField, messages.email) ? 'register-email-error' : null" />
              @if (fieldError(emailField, messages.email); as message) {
                <span class="cotai-field-error" id="register-email-error">{{ message }}</span>
              }
            </label>
            <label class="cotai-field">
              <span class="cotai-label">Senha</span>
              <input class="cotai-input" id="register-password" name="password" type="password"
                     autocomplete="new-password" required minlength="8" maxlength="72" [(ngModel)]="password"
                     #passwordField="ngModel" [attr.aria-invalid]="!!fieldError(passwordField, messages.password)"
                     aria-describedby="register-password-hint" />
              @if (fieldError(passwordField, messages.password); as message) {
                <span class="cotai-field-error" id="register-password-hint">{{ message }}</span>
              } @else {
                <span class="cotai-hint" id="register-password-hint">Pelo menos 8 caracteres.</span>
              }
            </label>
            @if (error()) {
              <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
            }
            <cotai-button type="submit" [fullWidth]="true" [loading]="loading()">Criar conta</cotai-button>
          </form>
        </cotai-card>
        <p class="switch">Já tem conta? <a routerLink="/login" [queryParams]="{ redirect: redirect }">Entrar</a></p>
      </div>
    </div>
  `,
  styleUrl: './auth.component.scss',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly redirect = inject(ActivatedRoute).snapshot.queryParamMap.get('redirect') || '/dashboard';

  protected readonly messages = MESSAGES;
  protected readonly fieldError = fieldError;

  protected name = '';
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
    this.auth.register(this.name.trim(), this.email.trim(), this.password).subscribe({
      next: () => this.router.navigateByUrl(this.redirect),
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
