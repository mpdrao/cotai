import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/copy/ui-copy';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';

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
          <form class="form" (ngSubmit)="submit()" #form="ngForm">
            <label class="cotai-field">
              <span class="cotai-label">Seu nome</span>
              <input class="cotai-input" id="register-name" name="name" autocomplete="name" required minlength="2"
                     maxlength="80" [(ngModel)]="name" />
            </label>
            <label class="cotai-field">
              <span class="cotai-label">E-mail</span>
              <input class="cotai-input" id="register-email" name="email" type="email" inputmode="email"
                     autocomplete="email" required maxlength="254" [(ngModel)]="email" />
            </label>
            <label class="cotai-field">
              <span class="cotai-label">Senha</span>
              <input class="cotai-input" id="register-password" name="password" type="password"
                     autocomplete="new-password" required minlength="8" maxlength="72" [(ngModel)]="password"
                     aria-describedby="register-password-hint" />
              <span class="cotai-hint" id="register-password-hint">Pelo menos 8 caracteres.</span>
            </label>
            @if (error()) {
              <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
            }
            <cotai-button type="submit" [fullWidth]="true" [loading]="loading()" [disabled]="!!form.invalid">
              Criar conta
            </cotai-button>
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

  protected name = '';
  protected email = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected submit(): void {
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
