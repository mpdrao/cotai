import { createHmac } from 'node:crypto';
import { APIRequestContext, PlaywrightWorkerArgs, expect } from '@playwright/test';

export const PASSWORD = 'senha12345';

export interface Organizer {
  email: string;
  token: string;
}

export interface Scenario {
  organizer: Organizer;
  listId: string;
  slug: string;
  itemIds: Record<string, number>;
}

export interface Reserved {
  reservationId: string;
  providerReference: string;
}

export async function registerOrganizer(api: APIRequestContext, label: string): Promise<Organizer> {
  const email = `${label}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@e2e.cotai.dev`;
  const registered = await api.post('/api/auth/register', { data: { name: `Org ${label}`, email, password: PASSWORD } });
  expect(registered.status()).toBe(201);
  const login = await api.post('/api/auth/login', { data: { email, password: PASSWORD } });
  expect(login.ok()).toBeTruthy();
  return { email, token: (await login.json()).accessToken };
}

function auth(token: string) {
  return { Authorization: `Bearer ${token}` };
}

export async function createPublishedList(
  api: APIRequestContext,
  organizer: Organizer,
  items: { name: string; unitPriceCents: number }[],
): Promise<Scenario> {
  const eventDate = new Date(Date.now() + 30 * 86_400_000).toISOString().slice(0, 10);
  const created = await api.post('/api/gift-lists', {
    headers: auth(organizer.token),
    data: {
      eventTypeCode: 'BARBECUE',
      title: 'Churrasco E2E',
      eventDate,
      items: items.map((i) => ({ name: i.name, unit: 'un', unitPriceCents: i.unitPriceCents })),
    },
  });
  expect(created.status()).toBe(201);
  const list = await created.json();
  const published = await api.post(`/api/gift-lists/${list.id}/publish`, { headers: auth(organizer.token) });
  expect(published.ok()).toBeTruthy();
  const full = await (await api.get(`/api/gift-lists/${list.id}`, { headers: auth(organizer.token) })).json();
  const itemIds: Record<string, number> = {};
  for (const item of full.items) itemIds[item.name] = item.id;
  return { organizer, listId: list.id, slug: list.slug, itemIds };
}

export async function reserve(
  api: APIRequestContext,
  itemId: number,
  guestName: string,
  message?: string,
): Promise<Reserved> {
  const response = await api.post(`/api/public/items/${itemId}/reservations`, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    data: {
      guestName,
      guestEmail: `${guestName.toLowerCase().replace(/\s+/g, '.')}@example.com`,
      quantity: 1,
      ...(message ? { message } : {}),
    },
  });
  expect(response.status()).toBe(201);
  const body = await response.json();
  return { reservationId: body.reservationId, providerReference: body.payment.providerReference };
}

export async function approve(api: APIRequestContext, providerReference: string): Promise<void> {
  const response = await api.post(`/mock-gateway/charges/${providerReference}/approve`);
  expect(response.ok()).toBeTruthy();
}

export async function confirmedCents(api: APIRequestContext, scenario: Scenario): Promise<number> {
  const response = await api.get(`/api/gift-lists/${scenario.listId}/metrics`, {
    headers: auth(scenario.organizer.token),
  });
  return (await response.json()).confirmedCents;
}

export async function waitConfirmed(api: APIRequestContext, scenario: Scenario, expectedCents: number): Promise<void> {
  await expect.poll(() => confirmedCents(api, scenario), { timeout: 15_000 }).toBe(expectedCents);
}

export async function payAs(
  api: APIRequestContext,
  scenario: Scenario,
  itemName: string,
  guestName: string,
  message: string | undefined,
  expectedTotalCents: number,
): Promise<void> {
  const r = await reserve(api, scenario.itemIds[itemName], guestName, message);
  await approve(api, r.providerReference);
  await waitConfirmed(api, scenario, expectedTotalCents);
}

export const API_URL = process.env['E2E_API_URL'] ?? 'http://localhost:8080';

/** Runs `action` with the server clock 31 minutes ahead and the expiry job executed, then puts the clock back. */
export async function withExpiredReservations(
  playwright: PlaywrightWorkerArgs['playwright'],
  action: () => Promise<void>,
): Promise<void> {
  const dev = await playwright.request.newContext({ baseURL: API_URL });
  try {
    expect((await dev.post('/dev/clock/advance', { data: { minutes: 31 } })).ok()).toBeTruthy();
    expect((await dev.post('/dev/jobs/expire-reservations')).ok()).toBeTruthy();
    await action();
  } finally {
    await dev.post('/dev/clock/reset');
    await dev.dispose();
  }
}

const WEBHOOK_SECRET = process.env['E2E_WEBHOOK_SECRET'] ?? 'change-me-webhook-secret';

/** The mock gateway refuses to approve an expired charge, so a late payment only arrives as a signed webhook. */
export async function sendPaidWebhook(api: APIRequestContext, providerReference: string, amountCents: number): Promise<void> {
  const body = JSON.stringify({ eventId: crypto.randomUUID(), type: 'payment.paid', providerReference, amountCents });
  const signature = 'sha256=' + createHmac('sha256', WEBHOOK_SECRET).update(body).digest('hex');
  const response = await api.post('/api/webhooks/payments', {
    headers: { 'Content-Type': 'application/json', 'X-Signature': signature },
    data: body,
  });
  expect(response.status()).toBe(204);
}
