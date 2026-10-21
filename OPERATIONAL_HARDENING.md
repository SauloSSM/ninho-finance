# Operational hardening do Alpha — 20/10/2026

## Problemas encontrados e correções

| Área | Problema | Correção |
| --- | --- | --- |
| Compras de fatura | A consulta combinava o vínculo da fatura com as datas do mês; compras de outros meses sumiam. | A tela consulta somente `creditCardInvoiceId`, sem `from`/`to`. A API já suportava essa consulta. |
| Pagamentos | O total pago não tinha histórico visível. | Histórico na conta da casa, despesa fora do cartão e fatura, com valor, data, pagador, método e conta bancária quando informada. Consulta sem corte por mês do pagamento. |
| Contas canceladas | A linha já preservava parte do tratamento financeiro, mas ainda oferecia ajustar valor. | Linha diferenciada, status Cancelada, sem dívida, pagamento ou ajuste; histórico de pagamentos preservado. Exclusão das pendentes e próximas contas. |
| Cartões inativos | A tela consultava somente cartões ativos, escondendo faturas antigas. | Consulta de todos os cartões e indicação Ativo/Inativo. Faturas, compras e pagamentos antigos continuam acessíveis. Novas faturas e compras em cartão inativo são bloqueadas também na API; limite fica oculto na interface do cartão inativo. |
| Ajustes | Valor da conta e limite abriam vazios. | Formulários preenchidos com o valor efetivo atual e o limite atual, inclusive nas Configurações. |
| Estados e termos | ACTIVE aparecia cru e cadastro inativo era apresentado como Cancelada. | Rótulos Ativo/Inativo, traduções dos demais enums existentes e proteção contra exibir enum desconhecido. |
| Erros | Validações e regras de negócio chegavam em inglês. | Tradução centralizada, identificação do campo e preservação de limites numéricos quando disponíveis. Pagamento excedente informa que o valor supera o restante. |
| Referências | Falhas resultavam em selects vazios; todos os formulários buscavam listas que não utilizavam. | Carregamento por dependência efetiva, mensagem identificando a lista com falha, nova tentativa e formulário indisponível até sucesso. Seleções obrigatórias vazias continuam impedindo envio; listas sem cadastro orientam para Configurações. |
| Confirmações | Faltavam confirmação de desativação e fechamento. | Confirmação simples nessas ações; confirmação de cancelamento de conta preservada e testada. |
| Dashboard | Próximas contas incluía pagas/canceladas e usava a ordem recebida. | Somente contas em aberto, atrasadas primeiro, depois vencimento crescente. Valor identificado como A pagar. Resumos financeiros continuam vindo do backend. |
| UX geral | Modal roubava foco ao digitar; erro de compras parecia vazio; total de fatura não informado parecia divergência; rateio inválido podia lançar exceção; renda permitia repetir ação em andamento. | Foco estável, erro de compras com nova tentativa, distinção de total não informado, feedback de valor inválido no rateio e bloqueio durante operação de renda. |
| Pagamento contextual | A seleção inicial podia ocorrer antes de carregar as opções; era possível trocar o contexto; itens quitados ainda apareciam. | Formulário montado após referências chegarem, contexto preservado, faturas e despesas sem restante removidas das opções. |

## Arquivos alterados

Produção:

- `backend/src/main/java/br/com/saulossm/ninho/card/InvoiceService.java`
- `backend/src/main/java/br/com/saulossm/ninho/expense/ExpenseService.java`
- `frontend/src/api/api.ts`
- `frontend/src/api/client.ts`
- `frontend/src/api/types.ts`
- `frontend/src/components/forms.tsx`
- `frontend/src/components/ui.tsx`
- `frontend/src/components/PaymentHistory.tsx` (novo)
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/pages/BillsPage.tsx`
- `frontend/src/pages/CardsPage.tsx`
- `frontend/src/pages/TransactionsPage.tsx`
- `frontend/src/pages/SettingsPage.tsx`
- `frontend/src/styles.css`
- `frontend/src/utils/labels.ts`
- `frontend/src/utils/errors.ts` (novo)

Testes:

- `backend/src/test/java/br/com/saulossm/ninho/AlphaApiIntegrationTest.java`
- `frontend/src/api/client.test.ts`
- `frontend/src/components/PaymentForm.test.tsx`
- `frontend/src/components/Modal.test.tsx` (novo)
- `frontend/src/components/PaymentHistory.test.tsx` (novo)
- `frontend/src/components/References.test.tsx` (novo)
- `frontend/src/pages/BillsPage.test.tsx`
- `frontend/src/pages/DashboardPage.test.tsx`
- `frontend/src/pages/CardsPage.test.tsx` (novo)
- `frontend/src/pages/SettingsPage.test.tsx` (novo)
- `frontend/src/pages/TransactionsPage.test.tsx` (novo)

Documentação: este relatório, `OPERATIONAL_HARDENING.md`.

## Regressões e resultados

Baseline antes das alterações: **80 testes backend e 20 frontend**, todos aprovados. A primeira execução de `mvnw.cmd clean test` encontrou `target/ninho-final-check.db` bloqueado por processo preexistente; o baseline backend foi obtido com `mvnw.cmd test`.

Resultado final: **83 testes backend + 51 testes frontend = 134 testes aprovados**, sem falhas ou testes ignorados. Acréscimo de **34 testes**: 3 no backend e 31 no frontend.

Regressões cobrem compras de outro mês na fatura; fatura e pagamento em cartão inativo; rejeição de novas compras/faturas nesse cartão; histórico nos três tipos de item, inclusive pagamentos de outro mês; conta cancelada; filtros e ordenação do dashboard; confirmação; valores preenchidos; falha e nova tentativa das cinco referências solicitadas; seleção contextual de pagamento; exclusão de itens quitados; tradução de erros e foco durante digitação.

Comandos finais:

- `mvnw.cmd clean test "-Dmaven.clean.failOnError=false"`: **BUILD SUCCESS**, 83 testes. O parâmetro foi necessário para preservar o SQLite bloqueado em `target`; o processo preexistente não foi encerrado. O `clean` emitiu aviso sobre esse arquivo e a pasta que o contém. Não se trata de um clean integral.
- `npm test`: **51 aprovados**, em 15 arquivos.
- `npm run build`: **aprovado**, TypeScript e Vite.
- `git diff --check`: sem erros de whitespace.

Log backend disponível em `backend/target/operational-test.log` (artefato local, ignorado pelo Git).

## Smoke

**26 verificações HTTP aprovadas**, com instância exclusiva na porta **18081** e datasource **`jdbc:sqlite:./target/operational-hardening-smoke.db`**. O script verificou o datasource no log antes de fazer qualquer escrita HTTP.

Foram exercitados cadastros de referência, compras em meses diferentes, listagem por fatura, pagamentos dos três tipos, identificação de pagador/conta/método/data, histórico de cartão inativo, bloqueio de novas operações nesse cartão, excesso de pagamento, fechamento de fatura, cancelamento de conta e resumo consolidado. No cenário final, a API manteve R$ 60 de fatura e R$ 40 de despesa direta em aberto, totalizando R$ 100, sem reincorporar dívida da conta cancelada.

A instância criada para o smoke foi encerrada após conferir PID, porta e datasource. Script, logs e SQLite estão em `backend/target/operational-*`. Testes automatizados do backend mantiveram seus bancos temporários existentes. A verificação das cinco telas foi feita por inspeção do código e testes de componentes; não houve smoke visual em navegador.

## Fluxos que ainda merecem atenção

- Consultar faturas antigas exige selecionar o mês no seletor global; não foi criado um explorador de histórico.
- Fechar fatura mantém a semântica atual de mudança de status: o backend ainda aceita novas compras em faturas fechadas de cartões ativos. Uma regra de bloqueio exige decisão explícita; não foi introduzida nesta rodada.
- Se uma fatura foi criada sem total informado, a interface agora explica isso; continua não havendo formulário para informar esse total posteriormente.
- O histórico consulta os pagamentos disponíveis na API e filtra pelo vínculo na interface. É adequado para o Alpha doméstico; não houve mudança de paginação ou API de busca.

## Preservação de escopo e dados

**`backend/data/ninho.db` não foi aberto, lido, alterado nem usado nos testes ou smoke desta rodada.** Nenhuma migration foi criada ou modificada. Nenhum cálculo consolidado foi alterado. Não houve commit, redesign de Figma, edição/exclusão de despesas, estorno, undo, backup ou implementação das demais funcionalidades excluídas do escopo.
