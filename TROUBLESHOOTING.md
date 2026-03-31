# ❌ Troubleshooting - Guia de Correção de Erros

## Erro: UnknownHostException: ${PGHOST}

**Causa:** As variáveis de ambiente não foram carregadas pelo Spring Boot.

**Solução:**

### ✅ Forma Correta (Use o Script)
```bash
# Torna executável (primeira vez)
chmod +x run.sh

# Executa automaticamente com variáveis carregadas
./run.sh dev
```

### ✅ Alternativa Manual
```bash
# Bash/Zsh
set -a
source .env
set +a
./mvnw spring-boot:run

# PowerShell
$env:PGHOST = "seu-host"
$env:PGPORT = "5432"
# ... outras variáveis ...
./mvnw spring-boot:run
```

---

## Passos de Diagnóstico

### 1. Verificar .env
```bash
cat .env
```

Deve conter:
```
PGHOST=seu-projeto.supabase.co
PGPORT=5432
PGUSER=postgres
PGPASSWORD=sua-senha
PGDATABASE=postgres
SPRING_PROFILES_ACTIVE=dev
```

### 2. Verificar Variáveis Exportadas
```bash
echo "PGHOST=$PGHOST"
echo "PGUSER=$PGUSER"
echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"
```

Se aparecerem vazios, as variáveis não foram exportadas.

### 3. Testar Conexão Supabase
```bash
# Instale psql se não tiver
# Ubuntu/Debian: sudo apt-get install postgresql-client
# macOS: brew install postgresql

psql -h $PGHOST -U $PGUSER -d $PGDATABASE
```

---

## Resumo das Mudanças

- ✅ `application-prod.yml` agora usa `${PGHOST}`, `${PGUSER}`, etc. (consistente com dev)
- ✅ `run.sh` criado para carregar `.env` automaticamente
- ✅ `.env.example` atualizado com exemplos Supabase
- ✅ `QUICK_START.md` atualizado com instruções detalhadas

---

## Próximas Execuções

Sempre use:
```bash
./run.sh       # dev por padrão
# ou
./run.sh prod  # para produção
```

