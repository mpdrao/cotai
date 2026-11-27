# COTAI — Testes de Carga e Desempenho

> [!IMPORTANT]
> **Documentação Unificada:**
> Todo o passo a passo atualizado para testes de carga e desempenho (Web Angular + API Spring Boot) e uso do **Proxy de Gravação do JMeter (HTTP(S) Test Script Recorder)** encontra-se no arquivo principal:
> 👉 **[README.md](./README.md)**

---

### Resumo do Ambiente Atual

| Componente | Detalhe |
|---|---|
| **Plano JMeter Principal** | [COTAI-Teste-Angular-Completo.jmx](./COTAI-Teste-Angular-Completo.jmx) (Web + API + Gravador Proxy) |
| **Execução Gráfica** | [abrir-jmeter.bat](./abrir-jmeter.bat) |
| **Execução Headless / Relatório HTML** | [executar-carga-cli.ps1](./executar-carga-cli.ps1) |
| **Banco de Dados** | PostgreSQL 17 via Docker (`docker compose up -d` na pasta `backend/`) |
| **Backend API** | Spring Boot na porta `8080` (`.\mvnw -DskipWeb spring-boot:run`) |
| **Frontend Web** | Angular na porta `4200` (`npm start` na pasta `frontend/`) |

> **Nota de Arquitetura:** O container Squid (`cotai-proxy`) e o antigo endpoint `/api/proxy/status` eram parte de um plano legado e foram descontinuados. O proxy do sistema é estritamente o gravador de tráfego web do JMeter (`127.0.0.1:8081`).
