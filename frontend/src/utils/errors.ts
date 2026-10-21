const messages: Record<string, string> = {
  'must not be blank': 'Preencha este campo.',
  'must not be null': 'Preencha este campo.',
  'must not be empty': 'Informe ao menos um item.',
  'must be greater than 0': 'Informe um valor maior que zero.',
  'must be greater than or equal to 0': 'Informe um valor igual ou maior que zero.',
  'payment must have exactly one target': 'Selecione uma única conta, despesa ou fatura para o pagamento.',
  'at least one amount must be provided': 'Informe o valor previsto ou o valor real da conta.',
  'Cancelled household bill cannot be paid': 'Esta conta foi cancelada e não pode receber pagamentos.',
  'cancelled bill cannot be paid': 'Esta conta foi cancelada e não pode receber pagamentos.',
  'effective amount cannot be lower than the amount already paid': 'O valor da conta não pode ser menor que o total já pago.',
  'allocation total must equal expense amount': 'A divisão entre pessoas precisa somar exatamente o valor da despesa.',
  'at least one allocation is required': 'Escolha ao menos uma pessoa responsável pela despesa.',
  'a person cannot appear more than once in allocations': 'Cada pessoa deve aparecer apenas uma vez na divisão.',
  'allocation total is too large': 'O valor total da divisão é muito alto. Revise os valores.',
  'Invoice already exists for this card and reference month': 'Já existe uma fatura para este cartão no mês selecionado.',
  'Category with the same name and type already exists': 'Já existe uma categoria com este nome e tipo.',
  'fromPerson and toPerson must be different': 'Escolha pessoas diferentes para o acerto.',
  'only expected income can be received': 'Somente uma renda prevista pode ser marcada como recebida.',
  'only expected income can be cancelled': 'Somente uma renda prevista pode ser cancelada.',
  'category must have type EXPENSE': 'Escolha uma categoria de despesa.',
  'category must have type INCOME': 'Escolha uma categoria de renda.',
  'from must not be after to': 'A data inicial deve ser anterior ou igual à data final.',
  'lastFour must contain exactly four digits': 'Informe exatamente os quatro últimos dígitos do cartão.',
};

export const fieldLabel = (field: string) => {
  const labels: Record<string, string> = {
    name: 'Nome', description: 'Descrição', institution: 'Instituição', amountCents: 'Valor',
    expectedAmountCents: 'Valor previsto', actualAmountCents: 'Valor real', creditLimitCents: 'Limite',
    reportedTotalCents: 'Total informado', initialBalanceCents: 'Saldo inicial', dueDate: 'Vencimento',
    closingDay: 'Dia de fechamento', dueDay: 'Dia de vencimento', expectedDate: 'Data prevista',
    paidAt: 'Data do pagamento', occurredAt: 'Data da despesa', receivedAt: 'Data do recebimento',
    personId: 'Pessoa', payerPersonId: 'Pagador', responsiblePersonId: 'Responsável', categoryId: 'Categoria',
    holderId: 'Titular', ownerId: 'Titular', invoicePayerId: 'Pagador da fatura', defaultResponsiblePersonId: 'Responsável padrão',
    bankAccountId: 'Conta bancária', method: 'Método', lastFour: 'Últimos quatro dígitos', type: 'Tipo', scope: 'Destino do gasto',
    referenceMonth: 'Mês', referenceYear: 'Ano', month: 'Mês', year: 'Ano', fromPersonId: 'Quem pagou', toPersonId: 'Quem recebeu',
  };
  return labels[field] ?? 'Campo informado';
};

export function friendlyError(message: string): string {
  if (messages[message]) return messages[message];
  if (message.includes('payment exceeds outstanding amount')) return 'O valor informado é maior que o valor restante.';
  const min = message.match(/^must be greater than or equal to (\d+)$/);
  if (min) return `Informe um valor igual ou maior que ${min[1]}.`;
  const max = message.match(/^must be less than or equal to (\d+)$/);
  if (max) return `Informe um valor igual ou menor que ${max[1]}.`;
  const size = message.match(/^size must be between (\d+) and (\d+)$/);
  if (size) return `Use entre ${size[1]} e ${size[2]} caracteres.`;
  if (/^must match/.test(message)) return 'Informe o valor no formato solicitado.';
  const invalid = message.match(/^(\w+) (is required|must be positive|cannot be negative|must be between 1 and (12|31))$/);
  if (invalid) return `${fieldLabel(invalid[1])}: ${invalid[2] === 'is required' ? 'preencha este campo.' : invalid[2] === 'must be positive' ? 'informe um valor maior que zero.' : invalid[2] === 'cannot be negative' ? 'informe um valor igual ou maior que zero.' : `informe um número entre 1 e ${invalid[3]}.`}`;
  const missing = message.match(/^(Person|Category|Bank account|Credit card|Invoice|Expense|Household bill|Payment).*not found/i);
  if (missing) return 'O registro selecionado não foi encontrado. Atualize os dados e selecione novamente.';
  // Preserve messages already written for the user; keep unknown technical errors out of the interface.
  return /\b(must|cannot|required|Exception|Error|SQL|constraint|failed)\b/.test(message)
    ? 'Não foi possível concluir a ação. Revise os dados informados e tente novamente.' : message;
}
