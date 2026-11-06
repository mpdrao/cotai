export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

// Re-exports for clean module boundaries and backward compatibility
export * from './event-type.model';
export * from './gift-list.model';
