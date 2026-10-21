# Ninho Finance

Projeto organizado como um monorepo simples.

## Estrutura

- `backend/`: aplicação Spring Boot
- `frontend/`: aplicação React, Vite e TypeScript
- `docs/`: documentação

Para iniciar backend e frontend no Windows, criar backups e restaurar com segurança, siga [Operação local](docs/LOCAL_OPERATIONS.md).

## Backend

No Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Para executar os testes:

```powershell
cd backend
.\mvnw.cmd test
```
