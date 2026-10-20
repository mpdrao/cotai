import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/copy/ui-copy';
import { CotaiButtonComponent, CotaiCardComponent } from '../../shared/cotai-ui';

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
          <form class="form" (ngSubmit)="submit()" #form="ngForm">
            <label class="cotai-field">
              <span class="cotai-label">E-mail</span>
              <input class="cotai-input" id="login-email" name="email" type="email" inputmode="email" autocomplete="email"
                     required [(ngModel)]="email" />
            </label>
            <label class="cotai-field">
              <span class="cotai-label">Senha</span>
              <input class="cotai-input" id="login-password" name="password" type="password" autocomplete="current-password"
                     required [(ngModel)]="password" />
            </label>
            @if (error()) {
              <p class="cotai-alert cotai-alert--error" role="alert">{{ error() }}</p>
            }
            <cotai-button type="submit" [fullWidth]="true" [loading]="loading()" [disabled]="!!form.invalid">Entrar</cotai-button>
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

  protected email = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected submit(): void {
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigateByUrl(this.redirect),
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
