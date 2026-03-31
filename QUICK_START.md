# 🚀 Quick Start - Veritas

> Guia rápido para começar a trabalhar com o projeto

---

## ✅ Pré-requisitos

- Java 21 ou superior
- Maven 3.6+
- Git

**Verificar:**
```bash
java -version
mvn -version
```

---

## 🔧 Configurar Ambiente

### 1. Clonar ou Acessar o Projeto
```bash
cd ~/veritas
```

### 2. Configurar Variáveis de Ambiente

Crie ou edite o arquivo `.env` na raiz do projeto:

```bash
cp .env.example .env
```

Preencha com suas credenciais Supabase:

```env
# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Supabase PostgreSQL
PGHOST=seu-projeto.supabase.co
PGPORT=5432
PGUSER=postgres
PGPASSWORD=sua-senha-aqui
PGDATABASE=postgres
```

---

## ▶️ Executar a Aplicação

### Via Script Helper (Recomendado ⭐)

```bash
# Tornar executável (primeira vez)
chmod +x run.sh

# Rodar com profile dev (padrão)
./run.sh

# Ou especificar o profile
./run.sh dev      # desenvolvimento
./run.sh prod     # produção
```

**Isto automaticamente:**
1. Carrega variáveis do `.env`
2. Inicia Maven com o perfil correto
3. Conecta ao Supabase

---

### Via Maven + Exportar Variáveis (Manual)

**Bash/Zsh:**
```bash
# Carregar variáveis do .env
set -a
source .env
set +a

# Executar
./mvnw spring-boot:run
```

**PowerShell (Windows):**
```powershell
# Carregar .env manualmente ou usar:
$env:PGHOST="seu-projeto.supabase.co"
$env:PGPORT="5432"
$env:PGUSER="postgres"
$env:PGPASSWORD="sua-senha"
$env:PGDATABASE="postgres"
$env:SPRING_PROFILES_ACTIVE="dev"

./mvnw spring-boot:run
```

---

### Via Build + JAR

```bash
# Build
./mvnw clean install

# Run (com variáveis de ambiente definidas)
SPRING_PROFILES_ACTIVE=dev java -jar target/veritas-0.0.1-SNAPSHOT.jar
```

---

### Via IDE (IntelliJ, VSCode, Eclipse)

**IntelliJ IDEA:**
1. Abra o projeto
2. `Run → Edit Configurations`
3. Selecione **Spring Boot → VeritasApplication**
4. Em **Environment variables**, adicione:
   ```
   PGHOST=seu-projeto.supabase.co;PGPORT=5432;PGUSER=postgres;PGPASSWORD=sua-senha;PGDATABASE=postgres;SPRING_PROFILES_ACTIVE=dev
   ```
5. Click **Run** ou **Debug**

**VSCode:**
1. Instale a extensão "Extension Pack for Java"
2. Crie `.vscode/launch.json`:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Spring Boot",
         "request": "launch",
         "mainClass": "br.edu.ifpb.veritas.VeritasApplication",
         "projectName": "veritas",
         "env": {
           "PGHOST": "seu-projeto.supabase.co",
           "PGPORT": "5432",
           "PGUSER": "postgres",
           "PGPASSWORD": "sua-senha",
           "PGDATABASE": "postgres",
           "SPRING_PROFILES_ACTIVE": "dev"
         }
       }
     ]
   }
   ```

---

## 🌐 Acessar a Aplicação

Após iniciar:

- **URL:** http://localhost:8080
- **Banco de dados:** Supabase (remoto)
- **Perfil ativo:** `dev` (logs detalhados, DDL create-drop)

---

## 📊 Monitorar Aplicação

### Ver Logs
```bash
# Se rodando em terminal
tail -f logs/spring.log

# Se rodando em background
./run.sh dev > app.log 2>&1 &
```

### Acessar Banco de Dados

```bash
# Via PostgreSQL CLI (se instalado localmente)
psql -h seu-projeto.supabase.co -U postgres -d postgres

# Dentro do psql:
# \dt              → listar tabelas
# SELECT * FROM users;  → queries
# \q               → sair
```

---

## 🔄 Parar a Aplicação

```bash
# Ctrl+C no terminal onde está rodando
# OU (se em background)
pkill -f "veritas"
```

---

## 📚 Documentação Adicional

- [PROFILES_GUIDE.md](PROFILES_GUIDE.md) - Entender dev vs prod
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - Roadmap do projeto
- [README.md](README.md) - Funcionalidades do sistema

---

## 🆘 Troubleshooting

### Erro: "Cannot connect to database"
```bash
# Verifique o .env
cat .env

# Teste as variáveis foram exportadas
echo $PGHOST
echo $PGUSER

# Verifique a conexão manualmente
psql -h seu-projeto.supabase.co -U postgres -d postgres
```

### Erro: "UnknownHostException: ${PGHOST}"
As variáveis não foram exportadas. Use o script `./run.sh` ou exporte manualmente:
```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

### Erro: "Spring Boot not found"
```bash
# Certifique-se que está no diretório correto
pwd  # deve ser /home/nascimento/veritas

# Verifique Java
java -version  # deve ser 21+
```

### Porta 8080 já em uso
```bash
# Encontre qual processo está usando
lsof -i :8080

# Mude a porta
./mvnw spring-boot:run -Dspring-boot.run.arguments='--server.port=8081'
```

---

## 💡 Dicas Úteis

### Recompilar sem Reiniciar (Dev Tools)
Spring Boot inclui DevTools por padrão. Para ativar "Reload" automático em IntelliJ:
1. `Ctrl+F9` (Windows/Linux) → recompila
2. A aplicação recarrega automaticamente

### Executar com Diferentes Perfis
```bash
# Testar em produção localmente (sem create-drop)
./run.sh prod

# Volta para dev
./run.sh dev
```

### Ver Configuração Carregada
```bash
# Adicione isto ao application-dev.yml
logging:
  level:
    org.springframework.core.env: DEBUG
```

---

**Pronto para começar? Execute: `./run.sh`**
