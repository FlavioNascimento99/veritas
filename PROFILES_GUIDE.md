# 🧪 Spring Boot Profiles - Veritas

## Estrutura de Profiles

Este projeto usa **Spring Boot profiles** para separar configurações entre ambientes:

```
src/main/resources/
├─ application.properties      # Base configuration (defaults)
├─ application-dev.yml         # 🧪 Development (Supabase)
└─ application-prod.yml        # 🚀 Production (Supabase)
```

---

## 📋 Profile: `dev` (Desenvolvimento Local)

**Quando usar:** Durante desenvolvimento local

**Configuração:**
- 🐘 **Banco:** Supabase PostgreSQL
- 🔓 **DDL Auto:** `create-drop` (recria schema a cada restart)
- 🔊 **Logs:** DEBUG + SQL queries visíveis
- 📝 **SQL Init:** `always` (roda scripts de inicialização)

**Como ativar:**
```bash
# Opção 1: Variável de ambiente
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run

# Opção 2: Arquivo .env
SPRING_PROFILES_ACTIVE=dev

# Opção 3: VM Parameter (IDE)
-Dspring.profiles.active=dev
```

**Variáveis de Ambiente Esperadas:**
```bash
PGHOST=seu-projeto.supabase.co
PGPORT=5432
PGDATABASE=postgres
PGUSER=postgres
PGPASSWORD=sua-senha-aqui
```

---

## 🚀 Profile: `prod` (Produção)

**Quando usar:** Em produção

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

**Variáveis de Ambiente Esperadas:**
```bash
PGHOST=seu-projeto.supabase.co
PGPORT=5432
PGDATABASE=postgres
PGUSER=postgres
PGPASSWORD=sua-senha-aqui
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
✅ **Pool de conexão otimizado**

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

## 📚 Referência Rápida

| Ambiente | Profile | DDL | Logs | Banco |
|----------|---------|-----|------|-------|
| Dev | `dev` | create-drop | DEBUG | Supabase |
| Prod | `prod` | validate | WARN | Supabase |

---

## 🆘 Troubleshooting

**Erro: "No datasource found"**
→ Verifique se `SPRING_PROFILES_ACTIVE` está correto

**Erro: "Cannot connect to database"**
→ Variáveis de ambiente não foram setadas corretamente

**SQL queries não aparecem nos logs**
→ Você está em modo `prod`? Mude para `dev`
