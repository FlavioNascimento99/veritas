# 📦 Kubernetes Manifests - Veritas

> Guia completo para deploy em Kubernetes (produção com Supabase)

---

## 📋 Estrutura de Arquivos

```
k8s/
├─ 01-namespace.yml          # Isolamento
├─ 02-secret.yml             # Credenciais Supabase
├─ 03-configmap.yml          # Configurações
├─ 04-deployment.yml         # Spring Boot App (3 replicas)
├─ 05-service.yml            # Exposição interna
├─ 06-ingress.yml            # HTTPS + routing
├─ 07-hpa.yml                # Auto-scaling
├─ 08-pdb.yml                # Alta disponibilidade
├─ 09-rbac.yml               # Segurança + permissões
└─ kustomization.yml         # Aplicar tudo junto
└─ KUBERNETES.md             # Este arquivo

```

---

## 🚀 Guia Rápido

### 1️⃣ Pré-requisitos

```bash
# Verificar cluster
kubectl cluster-info

# Verificar Metrics Server (para HPA)
kubectl get deployment metrics-server -n kube-system

# Verificar Ingress Controller (nginx)
kubectl get pods -n ingress-nginx
```

### 2️⃣ Configurar Secrets (CRÍTICO)

```bash
# Criar Secret com credenciais Supabase
kubectl create secret generic supabase-credentials \
  --from-literal=SUPABASE_DB_HOST=db.supabase.co \
  --from-literal=SUPABASE_DB_PORT=5432 \
  --from-literal=SUPABASE_DB_NAME=postgres \
  --from-literal=SUPABASE_DB_USER=postgres \
  --from-literal=SUPABASE_DB_PASSWORD='SUA_SENHA_AQUI' \
  --from-literal=SPRING_DATASOURCE_URL='jdbc:postgresql://db.supabase.co:5432/postgres?sslmode=require' \
  -n veritas
```

### 3️⃣ Aplicar Todos os Manifests

```bash
# Opção 1: Aplicar com Kustomize (recomendado)
kubectl apply -k k8s/

# Opção 2: Aplicar cada arquivo individualmente
kubectl apply -f k8s/01-namespace.yml
kubectl apply -f k8s/02-secret.yml
kubectl apply -f k8s/03-configmap.yml
# ... etc
```

### 4️⃣ Verificar Deploy

```bash
# Ver status dos pods
kubectl get pods -n veritas

# Ver logs da aplicação
kubectl logs -f deployment/veritas-app -n veritas

# Ver eventos
kubectl describe pod <pod-name> -n veritas
```

### 5️⃣ Exposição da API

```bash
# Obter IP do Ingress
kubectl get ingress -n veritas

# Acessar a aplicação
http://veritas.example.com
```

---

## 📊 Arquitetura Kubernetes

```
┌─────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                   │
└─────────────────┬───────────────────────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
   ┌────▼─────┐      ┌─────▼┐
   │  Ingress │      │  LB  │
   │ (nginx)  │      │      │
   └────┬─────┘      └──────┘
        │ :443 (HTTPS)
        │
   ┌────▼──────────────────┐
   │  Namespace: veritas   │
   ├───────────────────────┤
   │                       │
   │  ┌──────────────────┐ │
   │  │  Deployment     │ │
   │  │  (3 replicas)   │ │
   │  │                 │ │
   │  │ Pod 1 (Spring) ──┼──> Service (ClusterIP 80)
   │  │ Pod 2 (Spring) ──┼──> Supabase
   │  │ Pod 3 (Spring) ──┼──> Metrics Server
   │  │                 │ │
   │  └──────────────────┘ │
   │                       │
   │  HPA: 3-10 replicas   │
   │  PDB: min 1 rodando   │
   │                       │
   └───────────────────────┘
```

---

## 🔧 Comandos Principais

### Deploy

```bash
# Aplicar manifests
kubectl apply -k k8s/

# Apply com output
kubectl apply -k k8s/ --dry-run=client -o yaml

# Verificar o que será aplicado
kubectl kustomize k8s/
```

### Monitoramento

```bash
# Ver pods
kubectl get pods -n veritas

# Ver deployments
kubectl get deployments -n veritas

# Ver services
kubectl get services -n veritas

# Ver ingress
kubectl get ingress -n veritas

# Ver HPA status
kubectl get hpa -n veritas

# Ver PDB status
kubectl get pdb -n veritas

# Ver eventos
kubectl get events -n veritas --sort-by='.lastTimestamp'
```

### Logs & Debug

```bash
# Logs de um pod
kubectl logs pod/<pod-name> -n veritas

# Logs em tempo real (follow)
kubectl logs -f deployment/veritas-app -n veritas

# Logs de container anterior (crashed)
kubectl logs <pod-name> -n veritas --previous

# Shell interativo no pod
kubectl exec -it <pod-name> -n veritas -- /bin/bash

# Descrever pod
kubectl describe pod <pod-name> -n veritas

# Status do deployment
kubectl rollout status deployment/veritas-app -n veritas
```

### Update & Rollback

```bash
# Atualizar imagem Docker
kubectl set image deployment/veritas-app \
  veritas=seu-registry/veritas:v1.0.1 \
  -n veritas

# Ver histórico de updates
kubectl rollout history deployment/veritas-app -n veritas

# Reverter para versão anterior
kubectl rollout undo deployment/veritas-app \
  --to-revision=1 \
  -n veritas

# Status do rollout
kubectl rollout status deployment/veritas-app -n veritas
```

### Escalabilidade

```bash
# Ver HPA em ação
kubectl get hpa veritas-app-hpa -n veritas --watch

# Escalar manualmente
kubectl scale deployment veritas-app --replicas=5 -n veritas

# Ver métrica de CPU
kubectl top pods -n veritas
kubectl top nodes
```

### Cleanup

```bash
# Deletar namespace (remove tudo)
kubectl delete namespace veritas

# Deletar deployment específico
kubectl delete deployment veritas-app -n veritas

# Deletar recurso via kustomize
kubectl delete -k k8s/
```

---

## 🔐 Segurança

### Checklist de Segurança

- ✅ `runAsNonRoot: true` (não rodar como root)
- ✅ Liveness/Readiness/Startup probes
- ✅ Resource requests/limits
- ✅ NetworkPolicy (controle de tráfego)
- ✅ RBAC (ServiceAccount + Role)
- ✅ TLS/SSL via Ingress + cert-manager
- ✅ Secret para credenciais (não ConfigMap)
- ✅ Pod Disruption Budget

### Atualizar Secrets Seguramente

```bash
# NUNCA edite secrets diretamente em YAML

# 1. Criar novo secret
kubectl create secret generic supabase-credentials \
  --from-literal=SUPABASE_DB_HOST=novo.supabase.co \
  ... -n veritas --dry-run=client -o yaml | kubectl apply -f -

# 2. Forçar redeploy (restart pods)
kubectl rollout restart deployment/veritas-app -n veritas
```

---

## 📈 Performance & Tuning

### Aumentar Replicas Para Trafégo Alto

```yaml
# Em 04-deployment.yml:
spec:
  replicas: 5  # de 3 para 5
```

### Aumentar Limits de Recursos

```yaml
# Em 04-deployment.yml, em resources.limits:
limits:
  cpu: "2000m"      # de 1000m para 2000m
  memory: "2Gi"     # de 1Gi para 2Gi
```

### HPA Mais Agressivo

```yaml
# Em 07-hpa.yml:
- averageUtilization: 50  # de 70% para 50% (escala mais rápido)
```

---

## 🆘 Troubleshooting

### Pod não inicia (CrashLoopBackOff)

```bash
# Ver logs
kubectl logs <pod-name> -n veritas

# Ver eventos
kubectl describe pod <pod-name> -n veritas

# Comum: Secret não encontrado
# → Verificar: kubectl get secret -n veritas
```

### Aplicação não responde (ReadinessProbe falha)

```bash
# Debugar probe
kubectl describe pod <pod-name> -n veritas

# Testar probe manualmente
kubectl exec <pod-name> -n veritas -- \
  curl -f http://localhost:8080/actuator/health/readiness
```

### Ingress não funciona

```bash
# Ver Ingress status
kubectl get ingress -n veritas

# Verificar rules
kubectl describe ingress veritas-ingress -n veritas

# Testar conectividade
kubectl exec -it <pod-name> -n veritas -- \
  curl -v http://veritas-service:80
```

### Out of Memory

```bash
# Ver consumo
kubectl top pods -n veritas

# Aumentar limit
kubectl set resources deployment veritas-app \
  --limits=memory=2Gi \
  -n veritas
```

---

## 🔄 CI/CD Integration (GitOps)

### ArgoCD (Recomendado)

```bash
# Instalar ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Criar ArgoCD Application
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: veritas
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/seu-usuario/veritas
    targetRevision: main
    path: k8s/
  destination:
    server: https://kubernetes.default.svc
    namespace: veritas
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
EOF
```

---

## 📚 Próximos Passos

1. ✅ Manifests Kubernetes
2. ⬜ [ ] CI/CD Pipeline (GitHub Actions)
3. ⬜ [ ] Monitoring (Prometheus + Grafana)
4. ⬜ [ ] Backup & Disaster Recovery
5. ⬜ [ ] Multi-region deployment

---

## 🔗 Referências

- [Kubernetes Docs](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [Cert-Manager](https://cert-manager.io/)
- [Kustomize](https://kustomize.io/)
