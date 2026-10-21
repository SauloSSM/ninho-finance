# Operação local do Ninho no Windows

Execute os comandos abaixo no PowerShell, na raiz deste worktree. É necessário JDK 25 (`JAVA_HOME` apontando para o JDK), Node.js compatível com o frontend e npm. O Maven Wrapper já acompanha o projeto. A primeira execução pode precisar de internet para baixar as dependências existentes; nenhuma dependência nova foi adicionada para backup.

## Iniciar e encerrar

```powershell
.\scripts\Start-Ninho.ps1
```

Verifica as portas 8080 e 5173 antes de abrir duas janelas interativas, uma para backend e outra para frontend. Se uma porta estiver indisponível, informa o conflito sem encerrar processos. O banco utilizado aparece no terminal antes da inicialização. O script compila sem `clean` e instala as dependências do frontend com `npm ci` se `node_modules` ainda não existir. Um banco inexistente é criado pelas migrations normais da aplicação.

Acesse `http://localhost:5173`. O backend atende somente em `127.0.0.1:8080`; o frontend também escuta apenas localmente. Mantenha as duas janelas abertas. Para encerrar, pressione **Ctrl+C em cada janela** e aguarde o encerramento do Java/Hikari e do Vite antes de fechar os terminais. Não desligue o computador enquanto houver gravações ou restauração em andamento.

O frontend chama `http://127.0.0.1:8080/api` por padrão, evitando resolução de `localhost` para IPv6 na API. O CORS permite a origem `http://localhost:5173`. Os scripts podem ser chamados de qualquer diretório por seu caminho absoluto; os exemplos com `.\scripts` pressupõem a raiz do projeto.

Para executar cada parte no terminal atual:

```powershell
.\scripts\Start-Ninho.ps1 -Role Backend
.\scripts\Start-Ninho.ps1 -Role Frontend
```

Esses dois comandos precisam de terminais separados. Erros de Java, Maven, npm e portas ocupadas aparecem na janela correspondente. Uma janela pode falhar mesmo que a outra tenha iniciado; confira ambas. Se a política do PowerShell impedir scripts, pode-se iniciar uma sessão temporária com `powershell.exe -NoProfile -ExecutionPolicy Bypass`, sem alterar a política permanente da máquina.

### Selecionar e conferir um banco descartável

Na raiz do projeto, para iniciar as duas janelas com um arquivo novo em `backend/target`:

```powershell
.\scripts\Start-Ninho.ps1 -DatasourceUrl "jdbc:sqlite:./target/ninho-start-$([guid]::NewGuid().ToString('N')).db"
```

O sufixo único evita reutilizar dados de um ensaio anterior. O arquivo e seus auxiliares são preservados ao encerrar; não existe exclusão automática. Para usar o nome do exemplo de diagnóstico:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlite:./target/ninho-start-check.db'
.\scripts\Start-Ninho.ps1 -ShowConfiguration
.\scripts\Start-Ninho.ps1
```

`-ShowConfiguration` somente resolve e exibe a configuração; não abre o banco, não cria diretórios, não verifica portas e não inicia processos. A saída informa origem da seleção, URL JDBC efetiva, caminho absoluto e diretório de execução do backend. Abrir as janelas não confirma prontidão: aguarde `Started NinhoApplication` no backend e a URL local do Vite no frontend. Confira a API com `Invoke-RestMethod http://127.0.0.1:8080/api/people` e abra `http://localhost:5173`.

A precedência na inicialização é:

1. `-DatabasePath` **ou** `-DatasourceUrl` explícito; informar os dois é erro.
2. `SPRING_DATASOURCE_URL`, quando não há parâmetro explícito.
3. `jdbc:sqlite:./data/ninho.db`, padrão doméstico em `backend/data/ninho.db`.

Uma URL JDBC relativa, seja do ambiente ou de `-DatasourceUrl`, é resolvida em relação a **`backend`**, diretório onde o Java executa, mesmo chamando o script de outra pasta. `-DatabasePath` continua resolvendo caminhos relativos em relação à pasta atual do PowerShell, como nos scripts de backup/restauração. A janela backend recebe a URL já absoluta; o mesmo arquivo determina o bloqueio de operação e o argumento enviado ao Java. O frontend não seleciona nem abre banco.

O inicializador aceita arquivos SQLite locais em disco. Outros drivers, bancos em memória, URIs `file:`, query strings, caminhos de rede e caminhos JDBC ambíguos como `C:teste.db` são recusados antes de iniciar processos, para manter o bloqueio vinculado ao arquivo correto. Um valor inválido nunca provoca retorno silencioso ao banco padrão. Um parâmetro explícito permite substituir uma variável de ambiente antiga, inclusive inválida. Para voltar ao padrão doméstico sem parâmetros, remova a variável da sessão com `Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue`.

Backup/restauração preservam sua seleção por `-DatabasePath` e não consultam `SPRING_DATASOURCE_URL`. Para operar sobre o banco descartável, passe explicitamente o caminho absoluto exibido pelo inicializador nesses comandos.

## Dados e arquivos que devem ser preservados

O banco padrão é `backend/data/ninho.db`, sempre relativo à raiz do projeto quando se usa o script. O caminho absoluto é informado na inicialização, no backup e na restauração. Para usar outro banco, forneça `-DatabasePath 'C:\caminho\teste.db'` aos scripts de inicialização, backup e restauração. Caminhos personalizados relativos são resolvidos a partir do diretório atual.

Nunca apague manualmente:

- `ninho.db` ou qualquer outro banco que você utiliza.
- Arquivos associados `-wal`, `-shm` e `-journal`: podem conter transações ainda necessárias ao banco.
- Backups `ninho-*.db` e cópias `before-restore-*.db` sem verificar sua utilidade e manter outra cópia válida.
- Arquivos `.restore-stage-*` que sobrarem após interrupção abrupta, até avaliar a recuperação.
- As migrations de `backend/src/main/resources/db/migration`.

Arquivos `.operations.lock` são marcadores de coordenação; sua existência não significa que estejam bloqueados. O Windows libera o bloqueio quando o processo encerra. Não é necessário apagar esses arquivos. Bancos locais, backups padrão, marcadores e arquivos temporários da restauração ficam fora do Git. Ao escolher uma pasta personalizada de backup, mantenha-a fora de repositórios Git.

## Criar e verificar backup

```powershell
.\scripts\Backup-Ninho.ps1
.\scripts\Test-NinhoBackup.ps1 -BackupPath '.\backups\ninho-DATA-HORA-ID.db'
```

O primeiro comando mostra o caminho completo do arquivo criado em `backups/`. O nome contém data/hora local, milissegundos e identificador único; um arquivo existente nunca é sobrescrito. Use o caminho realmente informado pelo comando no lugar do exemplo.

O backup usa a **API de backup online do SQLite**, pelo driver Java já utilizado pelo Ninho. Pode ser feito com o backend aberto, inclusive em WAL; captura transações confirmadas, sem copiar ingenuamente o arquivo aberto. Em caso de contenção prolongada, a operação falha e pode ser repetida. O resultado é independente dos arquivos auxiliares do banco de origem.

Todo backup passa por `PRAGMA integrity_check`, `PRAGMA foreign_key_check`, conferência de versões/checksums/sucesso do histórico Flyway e comparação da estrutura com as migrations atuais. Arquivos de outra versão do Ninho são recusados: utilize a versão correspondente do aplicativo para planejar sua recuperação. Não edite o histórico Flyway para contornar essa validação.

Pode-se escolher uma pasta com `-BackupDirectory 'D:\NinhoBackups'`. Os arquivos contêm dados financeiros **sem criptografia**; restrinja o acesso à pasta e mantenha uma cópia em outro dispositivo seguro. Uma cópia no mesmo disco não protege contra perda desse disco. Não há exclusão automática de backups.

## Restaurar com segurança

1. Encerre completamente backend, frontend e ferramentas que abriram o banco. Aguarde a finalização do backend.
2. Verifique o backup com `Test-NinhoBackup.ps1`.
3. Execute o comando abaixo e digite exatamente `RESTAURAR` quando solicitado.

```powershell
.\scripts\Restore-Ninho.ps1 -BackupPath '.\backups\ninho-DATA-HORA-ID.db'
```

O destino padrão é o banco local existente. A restauração exige um destino existente para preservar sua versão anterior. Para confirmar explicitamente em um procedimento automatizado, existe `-ConfirmReplace`; essa opção autoriza a substituição dos dados. Confira cuidadosamente o caminho exibido.

O script recusa a operação quando a porta do backend está ocupada (8080 por padrão), quando o inicializador mantém seu bloqueio ou quando qualquer processo possui acesso incompatível ao arquivo, mesmo uma conexão ociosa. `-BackendPort` permite informar outra porta apenas se o backend correspondente estiver configurado para ela; os bloqueios de arquivo continuam obrigatórios. Não inicie backends por outros meios durante a restauração. O inicializador fornecido compartilha o bloqueio com a restauração e evita essa concorrência.

Se houver `-wal`, `-shm` ou `-journal`, a restauração é recusada. **Não apague esses arquivos.** Inicie e encerre normalmente o backend para que o SQLite faça a recuperação/checkpoint; se o erro persistir, preserve o conjunto completo e investigue antes de restaurar.

Antes de substituir, o script bloqueia o banco, impede alterações no backup escolhido, valida-o e prepara outro SQLite validado no mesmo volume do destino. A troca usa `File.Replace` do Windows, sem copiar dados por cima do banco atual. O banco anterior é preservado automaticamente em `backend/data/backups/before-restore-*.db` para o destino padrão; para outro destino, fica em `backups/` ao lado dele. Os dois caminhos são informados ao concluir. Qualquer erro de validação mantém os dados atuais.

A troca depende do suporte de substituição atômica do sistema de arquivos Windows; prefira disco local NTFS. Não há fallback para cópia destrutiva. Falta de permissão, espaço ou suporte à operação provoca erro. Uma interrupção elétrica ou falha física do disco ainda exige recuperação e uma cópia externa. Caso precise voltar ao banco anterior, utilize o mesmo procedimento de restauração, escolhendo o arquivo `before-restore` validado.

## Comprovar a recuperação sem arriscar os dados da família

```powershell
cd backend
.\mvnw.cmd test
cd ..
.\scripts\tests\Test-LocalOperations.ps1
.\scripts\tests\Test-NinhoStartup.ps1 -Integration
```

A suíte operacional usa somente um diretório novo em `%TEMP%\ninho-safety-*`, exibido ao final, e preserva os artefatos para inspeção. Ela cria SQLite com as migrations reais, grava dados, faz backup, altera e restaura, confere os dados e a cópia anterior, valida integridade/foreign keys e inicia o Spring Boot com o banco restaurado em porta temporária. Também testa backup com backend ativo e WAL com transação não confirmada, bloqueios, confirmação, backup inválido e incompatibilidade. Não usa `backend/data/ninho.db`.

Também verifica caminhos relativos fora da raiz do projeto, conflitos de porta, conexão SQLite ociosa, recusa de restauração com WAL ativo e requisições GET/preflight CORS da origem `http://localhost:5173` para a API em IPv4.

A regressão `Test-NinhoStartup.ps1` confere seleção, precedência, mensagens e repasse às duas janelas sem abrir bancos. Requer portas 8080/5173 livres para as verificações do inicializador. Com `-Integration`, inicia os servidores reais pelo script, testa ambiente e os dois parâmetros em arquivos novos com espaços no nome dentro de `backend/target/startup-check-<id>`, executa GET/POST e preflight CORS e confirma, por conexão somente leitura, `PRAGMA database_list`, migrations e um marcador escrito pela API no arquivo anunciado. Encerra apenas a árvore de processos criada pelo próprio teste e preserva bancos e logs. Nenhum teste utiliza o banco doméstico.

Para ensaiar a recuperação de um backup seu, use um **destino descartável**: inicie o backend com `Start-Ninho.ps1 -Role Backend -DatabasePath "$env:TEMP\ninho-ensaio.db"`, encerre-o, restaure nesse mesmo caminho com `-DatabasePath`, e inicie novamente nesse caminho para conferir os dados. Nunca use o banco principal para esse ensaio.
