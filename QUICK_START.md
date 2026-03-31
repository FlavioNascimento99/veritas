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

**Para obter as credenciais:**
1. Acesse [Supabase Dashboard](https://supabase.com)
2. Selecione seu projeto
3. Vá em **Settings** → **Database** → **Connection Info**

---

## ▶️ Executar a Aplicação

### Via Maven (Recomendado)

```bash
# Opção 1: Rodar direto
./mvnw spring-boot:run

# Opção 2: Build + Execute
./mvnw clean install
java -jar target/veritas-0.0.1-SNAPSHOT.jar
```

### Via IDE (IntelliJ, VSCode, Eclipse)

1. Abra o projeto
2. Configure as variáveis de ambiente em `Run → Edit Configurations`
   - Adicione as variáveis do `.env`
3. Click em **Run** ou **Debug**

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
./mvnw spring-boot:run > app.log 2>&1 &
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

**Erro: "Cannot connect to database"**
```bash
# Verifique o .env
cat .env

# Teste a conexão Supabase
psql -h seu-projeto.supabase.co -U postgres -d postgres
```

**Erro: "Spring Boot not found"**
```bash
# Certifique-se que está no diretório correto
pwd  # deve ser /home/nascimento/veritas

# Verifique Java
java -version  # deve ser 21+
```

**Porta 8080 já em uso**
```bash
# Mude a porta no application.properties ou via CLI
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
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run

# Volta para dev
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

---

**Pronto para começar? Execute: `./mvnw spring-boot:run`**
