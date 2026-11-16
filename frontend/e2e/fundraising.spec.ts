import { Page, expect, test } from '@playwright/test';
import { PASSWORD, Scenario, createPublishedList, payAs, registerOrganizer, reserve, waitConfirmed } from './api';

const ITEMS = [
  { name: 'Carvao', unitPriceCents: 4500 },
  { name: 'Picanha', unitPriceCents: 9000 },
  { name: 'Gelo', unitPriceCents: 1500 },
  { name: 'Refrigerante', unitPriceCents: 6000 },
  { name: 'Cerveja', unitPriceCents: 12000 },
];
const TARGET = 33000;

async function loginAs(page: Page, email: string, redirect = '/dashboard'): Promise<void> {
  await page.goto(`/login?redirect=${encodeURIComponent(redirect)}`);
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.waitForURL((url) => !url.pathname.startsWith('/login'));
}

const money = (reais: string) => new RegExp(`R\\$\\s*${reais.replace('.', '\\.')}`);

test.describe('US-02 painel de arrecadação', () => {
  let scenario: Scenario;
  const dashboardPath = () => `/lists/${scenario.listId}/arrecadacao`;

  test.beforeAll(async ({ playwright, baseURL }) => {
    const api = await playwright.request.newContext({ baseURL });
    const organizer = await registerOrganizer(api, 'painel');
    scenario = await createPublishedList(api, organizer, ITEMS);

    await payAs(api, scenario, 'Carvao', 'Bia Souza', 'Parabéns pelo encontro!', 4500);
    await payAs(api, scenario, 'Gelo', 'Dani Rocha', '=1+1', 6000);
    await payAs(api, scenario, 'Cerveja', 'Eduardo Alves', undefined, 18000);
    await reserve(api, scenario.itemIds['Picanha'], 'Caio Lima', 'Ainda vou pagar');
    await api.dispose();
  });

  test('mostra os KPIs com o dinheiro confirmado e separa o que está pendente', async ({ page }) => {
    await loginAs(page, scenario.organizer.email, dashboardPath());

    await expect(page.getByRole('heading', { level: 1, name: 'Churrasco E2E' })).toBeVisible();
    await expect(page.getByText('54% da meta confirmada')).toBeVisible();
    await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '54');

    const kpis = page.locator('dl.kpis');
    await expect(kpis.locator('div', { hasText: 'Total arrecadado' })).toContainText(money('180,00'));
    await expect(kpis.locator('div', { hasText: 'Total arrecadado' })).toContainText(money('330,00'));
    await expect(kpis.locator('div', { hasText: 'Cotas pagas' })).toContainText(/3\s*de 5/);
    const restantes = kpis.locator('div', { hasText: 'Cotas restantes' });
    await expect(restantes).toContainText('1');
    await expect(restantes).toContainText('1 aguardando pagamento');
    await expect(kpis.locator('div', { hasText: 'Contribuidores' })).toContainText('3');
  });

  test('mostra o estado de cada item', async ({ page }) => {
    await loginAs(page, scenario.organizer.email, dashboardPath());

    const row = (name: string) => page.locator('ul.items li', { hasText: name });
    await expect(row('Carvao')).toContainText('Pago');
    await expect(row('Gelo')).toContainText('Pago');
    await expect(row('Cerveja')).toContainText('Pago');
    await expect(row('Picanha')).toContainText('Aguardando pagamento');
    await expect(row('Refrigerante')).toContainText('Disponível');
  });

  test('o feed lista só pagamentos confirmados, do mais novo ao mais antigo, com a mensagem', async ({ page }) => {
    await loginAs(page, scenario.organizer.email, dashboardPath());

    const feed = page.locator('ul.feed > li');
    await expect(feed).toHaveCount(3);
    await expect(feed.nth(0)).toContainText('Eduardo Alves');
    await expect(feed.nth(1)).toContainText('Dani Rocha');
    await expect(feed.nth(2)).toContainText('Bia Souza');
    await expect(feed.nth(2).locator('blockquote')).toHaveText('Parabéns pelo encontro!');
    await expect(feed.nth(0).locator('blockquote')).toHaveCount(0);
    await expect(page.getByText('Caio Lima')).toHaveCount(0);
    await expect(page.getByText('Ainda vou pagar')).toHaveCount(0);
    await expect(page.getByRole('navigation', { name: 'Paginação das contribuições' })).toHaveCount(0);
  });

  test('filtra as contribuições por item e volta para todas', async ({ page }) => {
    await loginAs(page, scenario.organizer.email, dashboardPath());
    const feed = page.locator('ul.feed > li');
    await expect(feed).toHaveCount(3);

    await page.locator('ul.items li', { hasText: 'Gelo' }).getByRole('button').click();
    await expect(feed).toHaveCount(1);
    await expect(feed.first()).toContainText('Dani Rocha');
    await expect(page.getByRole('heading', { name: 'Contribuições de Gelo' })).toBeVisible();

    await page.locator('ul.items li', { hasText: 'Picanha' }).getByRole('button').click();
    await expect(page.getByText('Ninguém pagou este item ainda.')).toBeVisible();

    await page.locator('ul.items li', { hasText: 'Picanha' }).getByRole('button', { name: /Ver todas/ }).click();
    await expect(feed).toHaveCount(3);
  });

  test('exporta o CSV com acentos, valores em vírgula e fórmulas neutralizadas', async ({ page }) => {
    await loginAs(page, scenario.organizer.email, dashboardPath());
    await expect(page.locator('ul.feed > li')).toHaveCount(3);

    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Exportar CSV' }).click(),
    ]);
    expect(download.suggestedFilename()).toBe(`arrecadacao-${scenario.listId}.csv`);

    const stream = await download.createReadStream();
    const chunks: Buffer[] = [];
    for await (const chunk of stream) chunks.push(chunk as Buffer);
    const csv = Buffer.concat(chunks).toString('utf8');

    expect(csv.charCodeAt(0)).toBe(0xfeff);
    const lines = csv.trim().split('\r\n');
    expect(lines).toHaveLength(4);
    expect(csv).toContain('"Bia Souza";"Carvao";1;45,00;"Parabéns pelo encontro!"');
    expect(csv).toContain('"Eduardo Alves";"Cerveja";1;120,00;""');
    expect(csv).toContain(`"'=1+1"`);
    expect(csv).not.toContain('"=1+1"');
    expect(csv).not.toContain('Caio Lima');
  });

  test('não estoura a largura em tela de celular', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 740 });
    await loginAs(page, scenario.organizer.email, dashboardPath());
    await expect(page.locator('ul.feed > li')).toHaveCount(3);

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);
  });

  test('exige login e esconde o painel de outro organizador', async ({ page, playwright, baseURL }) => {
    await page.goto(dashboardPath());
    await expect(page).toHaveURL(/\/login/);

    const api = await playwright.request.newContext({ baseURL });
    const stranger = await registerOrganizer(api, 'intruso');
    await api.dispose();

    await loginAs(page, stranger.email, dashboardPath());
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByText('Bia Souza')).toHaveCount(0);
    await expect(page.locator('dl.kpis')).toHaveCount(0);
  });

  test('o link na tela da lista leva ao painel', async ({ page }) => {
    await loginAs(page, scenario.organizer.email, `/lists/${scenario.listId}`);
    await page.getByRole('link', { name: 'Ver painel de arrecadação' }).click();
    await expect(page).toHaveURL(new RegExp(`/lists/${scenario.listId}/arrecadacao$`));
    await expect(page.locator('dl.kpis')).toBeVisible();
  });
});

test.describe('US-02 mensagem do convidado até o painel', () => {
  test('convidado reserva pela página pública com mensagem, paga, e ela chega ao organizador', async ({
    page,
    playwright,
    baseURL,
  }) => {
    const api = await playwright.request.newContext({ baseURL });
    const organizer = await registerOrganizer(api, 'convidado');
    const scenario = await createPublishedList(api, organizer, [{ name: 'Carvao', unitPriceCents: 4500 }]);

    await page.goto(`/l/${scenario.slug}`);
    const card = page.locator(`[data-item-id="${scenario.itemIds['Carvao']}"]`);
    await card.getByRole('button', { name: /Reservar/ }).click();

    const note = page.getByLabel('Mensagem de carinho (opcional)');
    await expect(note).toHaveAttribute('maxlength', '280');
    await page.getByLabel('Seu nome').fill('Fernanda Costa');
    await page.getByLabel('Seu e-mail').fill('fernanda@example.com');
    await note.fill('Boa festa! Com carinho, Fê');

    const [response] = await Promise.all([
      page.waitForResponse((r) => r.url().includes('/reservations') && r.request().method() === 'POST'),
      page.getByRole('button', { name: 'Ir para pagamento' }).click(),
    ]);
    expect(response.status()).toBe(201);
    const providerReference = (await response.json()).payment.providerReference;
    await page.waitForURL(/\/r\//);

    const approved = await api.post(`/mock-gateway/charges/${providerReference}/approve`);
    expect(approved.ok()).toBeTruthy();
    await waitConfirmed(api, scenario, 4500);
    await api.dispose();

    await loginAs(page, organizer.email, `/lists/${scenario.listId}/arrecadacao`);
    const feed = page.locator('ul.feed > li');
    await expect(feed).toHaveCount(1);
    await expect(feed.first()).toContainText('Fernanda Costa');
    await expect(feed.first().locator('blockquote')).toHaveText('Boa festa! Com carinho, Fê');
    await expect(page.getByText('100% da meta confirmada')).toBeVisible();
  });
});
