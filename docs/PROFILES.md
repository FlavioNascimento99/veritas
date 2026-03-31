# 🧪 PROFILES - Spring Boot Dev vs Prod

Configuração de profiles para separar ambientes (desenvolvimento vs produção).

---

## 📋 Estrutura

```
src/main/resources/
├─ application.properties      # Base (padrão)
├─ application-dev.yml         # Desenvolvimento
└─ application-prod.yml        # Produção
```

---

## 🧪 Profile: `dev` (Desenvolvimento Local)

**Quando usar:** `docker-compose up`

**Banco de Dados:**
- PostgreSQL local (em container)
- Dados podem ser perdidos a cada restart (OK!)

**Confi guração:**
- DDL Auto: `create-drop` (recria schema)
- Logs: DEBUG + SQL visível
- Inicialização: automática

**Ativar:**
```bash
export SPRING_PROFILES_ACTIVE=dev
docker-compose up -d
```

**Variáveis de Ambiente:**
```
PGHOST=postgres-local
PGPORT=5432
PGDATABASE=veritas_dev
PGUSER=dev_user
PGPASSWORD=dev_password
```

---

## 🚀 Profile: `prod` (Produção)

**Quando usar:** Kubernetes com Supabase

**Banco de Dados:**
- Supabase (PostgreSQL gerenciado)
- Dados são persistentes e reais

**Configuração:**
- DDL Auto: `validate` (NÃO altera schema!)
- Logs: INFO apenas
- Inicialização: nunca

**Ativar:**
```bash
kubectl set env deployment/veritas-app \
  SPRING_PROFILES_ACTIVE=prod -n veritas
```

**Variáveis de Ambiente:**
```
SUPABASE_DB_HOST=db.supabase.co
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=***
```

---

## 📊 Comparação

| Aspecto | Dev | Prod |
|---------|-----|------|
| Banco | Local PostgreSQL | Supabase |
| DDL | create-drop | validate |
| Logs | DEBUG | INFO |
| Dados | Podem ser perdidos | Persistentes |
| Inicialização | Automática | Nunca |
| Segurança | Baixa (dev típico) | Alta |

---

## 🔄 Fluxo

```
Local DEV
 ↓
docker-compose up (profile=dev)
 ↓
PostgreSQL local
 ↓
Testa/Desenvolve
 ↓
Merge para main
 ↓
CI/CD build image
 ↓
Deploy Kubernetes (profile=prod)
 ↓
Supabase PostgreSQL
```

---

## 🆘 Troubleshooting

**"Erro: No datasource found"**
→ Verificar `SPRING_PROFILES_ACTIVE`

**"SQL comandos não aparecem nos logs"**
→ Você está em `prod`? Mude para `dev`

**"Banco perdeu dados"**
→ Normal em `dev` (create-drop). Use migrações para evitar perda.
