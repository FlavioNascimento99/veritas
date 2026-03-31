# 🧪 Spring Boot Profiles - Veritas

## Estrutura de Profiles

Este projeto usa **Spring Boot profiles** para separar configurações entre ambientes:

```
src/main/resources/
├─ application.properties      # Base configuration (defaults)
├─ application-dev.yml         # 🧪 Development (LOCAL Docker)
└─ application-prod.yml        # 🚀 Production (SUPABASE)
```

---

## 📋 Profile: `dev` (Desenvolvimento Local)

**Quando usar:** Durante desenvolvimento local com `docker-compose up`

**Configuração:**
- 🐘 **Banco:** PostgreSQL local (em container Docker)
- 🔓 **DDL Auto:** `create-drop` (recria schema a cada restart)
- 🔊 **Logs:** DEBUG + SQL queries visíveis
- 📝 **SQL Init:** `always` (roda scripts de inicialização)

**Como ativar:**
```bash
# Opção 1: Variável de ambiente
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run

# Opção 2: Arquivo .env (docker-compose)
SPRING_PROFILES_ACTIVE=dev

# Opção 3: VM Parameter (IDE)
-Dspring.profiles.active=dev
```

**Variáveis de Ambiente Esperadas:**
```bash
PGHOST=postgres-local        # Nome do serviço Docker
PGPORT=5432
PGDATABASE=veritas_dev
PGUSER=dev_user
PGPASSWORD=dev_password
```

---

## 🚀 Profile: `prod` (Produção - Kubernetes)

**Quando usar:** Em produção no Kubernetes com Supabase

**Configuração:**
- ☁️ **Banco:** Supabase (PostgreSQL gerenciado)
- 🔒 **DDL Auto:** `validate` (NÃO altera schema em prod)
- 🔇 **Logs:** WARN + structured logging
- 📝 **SQL Init:** `never` (sem scripts automáticos)

**Como ativar:**
```bash
# Variável de ambiente
export SPRING_PROFILES_ACTIVE=prod
```

**Variáveis de Ambiente Esperadas (Kubernetes Secret):**
```bash
SUPABASE_DB_HOST=db.supabase.co
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=sua_senha_aqui
```

---

## 🔄 Como Migrar de `dev` para `prod`

### Local → Teste em Prod Config

```bash
# Simule a prod localmente (com DB de teste)
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### Validar Mudanças

✅ **DDL validado** (não recria schema)
✅ **Logs reduzidos** (apenas WARN)
✅ **Pool de conexão otimizado** (20 max connections)

---

## 🗑️ Migrations (Database)

### Desenvolvimento (create-drop)
- Útil para testes rápidos
- **⚠️ ESCOPE DADOS A CADA RESTART**

### Produção (validate)
- Schema deve existir no Supabase
- **Mudanças exigem migração manual ou Flyway/Liquibase**

Para futuro: considere adicionar **Flyway** para versionamento de schema.

---

## ⚙️ Configuração no `docker-compose.yml`

```yaml
services:
  veritas-app:
    environment:
      SPRING_PROFILES_ACTIVE: dev
      PGHOST: postgres-local
      PGPORT: 5432
      PGDATABASE: veritas_dev
      PGUSER: dev_user
      PGPASSWORD: dev_password
```

---

## 🧪 Arquivos Legados

Os arquivos antigos abaixo **podem ser deletados** (migrados para YAML):
- ~~`application-local.properties`~~
- ~~`application-prod.properties`~~

---

## 📚 Referência Rápida

| Ambiente | Profile | DDL | Logs | Banco |
|----------|---------|-----|------|-------|
| Dev | `dev` | create-drop | DEBUG | PostgreSQL local |
| Prod | `prod` | validate | WARN | Supabase |

---

## 🆘 Troubleshooting

**Erro: "No datasource found"**
→ Verifique se `SPRING_PROFILES_ACTIVE` está correto

**Erro: "Cannot connect to database"**
→ Variaveis de ambiente não foram setadas

**SQL queries não aparecem nos logs**
→ Você está em modo `prod`? Mude para `dev`
