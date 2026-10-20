# Cota Ae! — Brand Guidelines · v1.0

## Ideia da marca

**Cota Ae!** facilita juntar pessoas e dividir contribuições para experiências: churrascos, aniversários, bodas, chá de bebê, casamento e qualquer outro encontro. A marca é jovem, clara, próxima e confiável — sem parecer exclusiva de casamentos.

- **Assinatura:** Junta pra fazer acontecer.
- **Personalidade:** descontraída, humana, prática e alegre.
- **Linguagem:** português do Brasil. Objetiva, sem jargões financeiros, sem exagerar em emojis.
- **Prioridade:** tarefa de organizar e contribuir, não um catálogo de presentes.

## Fonte visual aprovada

`references/identidade-aprovada.png` é **a arte exata aprovada**. Use-a para conferir composições, formas, cores e lettering. Não confundir uma única imagem ilustrativa com uma especificação pixel-perfect de todas as telas.

### Logo

Lettering expressivo e inclinado, **Cota** em verde escuro e **Ae!** em terracota, sublinhado verde e pequena marca de energia sobre o ponto de exclamação. Aplicações em fundo creme ou verde escuro.

- Use o nome escrito **Cota Ae!** em textos, títulos e metadados.
- A margem de proteção da marca deve ser pelo menos a altura do caractere `o`.
- Em dimensões pequenas, use o ícone compacto, sem tagline.
- **Atenção:** os SVGs neste pacote são **reconstruções interpretativas**, não uma vetorização fiel do logo da arte. Antes de uso definitivo público, refinar e aprovar manualmente.
- Evite distorção, gradientes, sombra forte e alteração arbitrária das proporções da marca.

## Paleta oficial — baseada nos rótulos da arte

| Token | Hex | Aplicação |
|---|---|---|
| `primary` | `#2E5D46` | Marca, CTA, ícones, títulos |
| `secondary` | `#6BA77A` | Progresso, fundos brandizados, apoio |
| `background` | `#FFF7EF` | Fundo creme |
| `peach` | `#F4B198` | Detalhes e áreas de acolhimento |
| `terracotta` | `#D96B5B` | Destaques da marca, energia, decoração |

**Cores funcionais auxiliares (proposta):** `#16372A` para texto escuro, `#FFFFFF` para superfície, `#F3F0E9` para bordas e `#C2C9C4` para elementos neutros. Não trate cores de decoração como cores acessíveis de texto sobre branco.

Contrast rules: branco sobre `primary` funciona para botões. Terracota `#D96B5B` NÃO tem contraste AA suficiente para texto pequeno sobre branco/creme; use com texto escuro ou apenas decorativamente. Nunca utilize verde suave com texto branco pequeno sem validação de contraste.

## Tipografia

- Interface: **Poppins**, fallback `system-ui, sans-serif`.
- Pesos: 400 corpo, 500 apoio, 600 destaque, 700/800 títulos.
- O lettering do logo é um ativo gráfico separado. Não tente recriá-lo apenas digitando “Cota Ae!” em Poppins.
- Tamanhos fluidos para títulos: `clamp(1.75rem, 1.2rem + 2.5vw, 3rem)`; corpo-base `1rem`/`1.5`.
- Evite caixas-altas em parágrafos, fontes abaixo de 14px para texto de interface e blocos longos centralizados.

## Formas e estilo

- Cantos de cards: 16px; CTA pílula; insumos de formulário: 12px (valores iniciais).
- Profundidade: contornos leves/sombras discretas, jamais neomorfismo pesado.
- Ícones: lineares, traço consistente (ex.: Lucide) com 20–24px, legendas quando necessário.
- Texturas: rabiscos, pequenos traços, curvas orgânicas em uso moderado.
- Fotos: momentos reais e diversos; eventos e pessoas sem glamour artificial obrigatório.
- Não use coração, laço, aliança ou embalagem de presente como símbolo principal universal.

## Exemplos de voz

- **Título:** Junta pra fazer acontecer.
- **Ação principal:** Criar minha lista / Contribuir com uma cota.
- **Evento:** Churrasco da Firma / Bodas da Família / Nosso aniversário.
- **Disponibilidade:** 3 de 5 cotas disponíveis.
- **Tempo:** Sua reserva expira em 22 min.
- **Feedback:** Cota reservada! Agora finalize o pagamento dentro do prazo.
- **Erro de concorrência:** Essa cota acabou de ser reservada. Atualizamos a disponibilidade para você.

Veja `UI_COPY.md` para textos, estados e mensagens detalhados.
