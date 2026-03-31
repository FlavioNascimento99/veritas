# ☸️ KUBERNETES - Produção

Deploy em Kubernetes com Supabase.

---

## 📁 Estrutura

```
k8s/
├─ 01-namespace.yml           # Isolamento (namespace: veritas)
├─ 02-secret.yml              # Credenciais Supabase
├─ 03-configmap.yml           # Configurações (env vars)
├─ 04-deployment.yml          # Spring Boot (3 replicas)
├─ 05-service.yml             # Exposição interna
├─ 06-ingress.yml             # HTTPS + cert-manager
├─ 07-hpa.yml                 # Auto-scaling (3-10 pods)
├─ 08-pdb.yml                 # Pod Disruption Budget
├─ 09-rbac.yml                # SecurityAccount + RBAC
├─ kustomization.yml          # Aplicar tudo junto
└─ deploy.sh                   # Script de deployment
```

---

## 🚀 Deploy Rápido

### 1. Configurar Secrets

```bash
kubectl create secret generic supabase-credentials \
  --from-literal=SUPABASE_DB_HOST=db.supabase.co \
  --from-literal=SUPABASE_DB_PORT=5432 \
  --from-literal=SUPABASE_DB_NAME=postgres \
  --from-literal=SUPABASE_DB_USER=postgres \
  --from-literal=SUPABASE_DB_PASSWORD='SUA_SENHA_AQUI' \
  -n veritas
```

### 2. Aplicar Manifests

```bash
# Opção 1: Com Kustomize (recomendado)
kubectl apply -k k8s/

# Opção 2: Com script
./k8s/deploy.sh deploy
```

### 3. Verificar

```bash
kubectl get pods -n veritas
kubectl get ingress -n veritas
kubectl logs -f deployment/veritas-app -n veritas
```

---

## 📊 Arquitetura

```
Ingress (HTTPS)
    ↓
Service (ClusterIP)
    ↓
Deployment (3 replicas)
    ├─ Pod 1 (Spring Boot)
    ├─ Pod 2 (Spring Boot)
    └─ Pod 3 (Spring Boot)
         ↓
HPA: Auto-scale 3-10 pods baseado em CPU/Memória
    ↓
Supabase PostgreSQL (db.supabase.co)
```

---

## 🔄 Comandos Principais

### Deploy

```bash
kubectl apply -k k8s/                    # Aplicar
kubectl kustomize k8s/                   # Visualizar
kubectl apply -k k8s/ --dry-run=client   # Testar sem aplicar
```

### Status

```bash
kubectl get pods -n veritas
kubectl get deployment -n veritas
kubectl get services -n veritas
kubectl get ingress -n veritas
kubectl get hpa -n veritas
```

### Logs

```bash
kubectl logs -f deployment/veritas-app -n veritas
kubectl describe pod <pod-name> -n veritas
```

### Update

```bash
# Atualizar imagem Docker
kubectl set image deployment/veritas-app \
  veritas=seu-registry/veritas:v1.0.1 \
  -n veritas

# Ver histórico
kubectl rollout history deployment/veritas-app -n veritas

# Reverter
kubectl rollout undo deployment/veritas-app -n veritas
```

### Scale

```bash
# Manual
kubectl scale deployment veritas-app --replicas=5 -n veritas

# Ver HPA
kubectl get hpa veritas-app-hpa -n veritas --watch
```

---

## 🔐 Segurança

- ✅ Non-root user
- ✅ Health checks (liveness, readiness, startup)
- ✅ Resource limits (CPU/Memory)
- ✅ RBAC (Service Account)
- ✅ Network Policy
- ✅ TLS/SSL automático
- ✅ Pod Disruption Budget

---

## 📈 Features

| Feature | O quê |
|---------|-------|
| **3 Replicas** | Alta disponibilidade |
| **Auto-scaling** | HPA de 3 a 10 pods |
| **Zero Downtime** | Rolling updates |
| **Health Checks** | Probes automáticas |
| **HTTPS** | Lei's Encrypt + cert-manager |
| **Graceful Shutdown** | 30s de grace period |

---

## 🆘 Troubleshooting

**"Pod não inicia (CrashLoopBackOff)"**
```bash
kubectl logs <pod-name> -n veritas
kubectl describe pod <pod-name> -n veritas
```

**"Readiness probe falha"**
```bash
kubectl exec <pod-name> -n veritas -- \
  curl -f http://localhost:8080/actuator/health/readiness
```

**"Ingress não funciona"**
```bash
kubectl get ingress -n veritas
kubectl describe ingress veritas-ingress -n veritas
```

**"Out of Memory"**
```bash
kubectl top pods -n veritas
kubectl set resources deployment veritas-app \
  --limits=memory=2Gi -n veritas
```

---

## 💡 Próximos Passos

1. ✅ Deploy básico (você está aqui)
2. ⬜ [ ] CI/CD GitHub Actions
3. ⬜ [ ] Monitoring (Prometheus + Grafana)
4. ⬜ [ ] Backup automatizado
5. ⬜ [ ] Sealed Secrets (encrypt)
