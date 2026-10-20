import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap } from 'rxjs';
import { TokenResponse } from '../models/organizer.model';

interface Session {
  token: string;
  name: string;
  expiresAt: number;
}

const STORAGE_KEY = 'cotai.session';

/** Organizer session. The JWT lives in localStorage until it expires (app.jwt.ttl-seconds). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = signal<Session | null>(readSession());

  readonly isLoggedIn = computed(() => {
    const s = this.session();
    return !!s && s.expiresAt > Date.now();
  });
  readonly organizerName = computed(() => this.session()?.name ?? '');

  token(): string | null {
    return this.isLoggedIn() ? this.session()!.token : null;
  }

  login(email: string, password: string): Observable<void> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(
      map((t) => {
        const session = { token: t.accessToken, name: nameFromJwt(t.accessToken), expiresAt: Date.now() + t.expiresIn * 1000 };
        this.session.set(session);
        writeSession(session);
      }),
    );
  }

  register(name: string, email: string, password: string): Observable<void> {
    return this.http
      .post('/api/auth/register', { name, email, password })
      .pipe(switchMap(() => this.login(email, password)));
  }

  logout(): void {
    this.session.set(null);
    writeSession(null);
  }
}

function nameFromJwt(token: string): string {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      Array.from(atob(payload), (c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join(''),
    );
    return JSON.parse(json).name ?? '';
  } catch {
    return '';
  }
}

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

function writeSession(session: Session | null): void {
  try {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Storage blocked: the session lasts only while the page is open.
  }
}
