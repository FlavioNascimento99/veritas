import { Container } from "@cloudflare/containers";
import { WorkerEntrypoint } from "cloudflare:workers";

interface Env {
  VERITAS_CONTAINER: DurableObjectNamespace;
  PGHOST?: string;
  PGPORT?: string;
  PGDATABASE?: string;
  PGUSER?: string;
  PGPASSWORD?: string;
  JAVA_OPTS?: string;
  ADMIN_BOOTSTRAP_EMAIL?: string;
  ADMIN_BOOTSTRAP_PASSWORD?: string;
  ADMIN_BOOTSTRAP_NAME?: string;
}

export class VeritasContainer extends Container {
  defaultPort = 8080;
  requiredPorts = [8080];
  sleepAfter = "10m";
  enableInternet = true;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    this.envVars = {
      SPRING_PROFILES_ACTIVE: "prod",
      // Cloudflare injeta PORT automaticamente; Spring lê via ${PORT:8080}
      PORT: "8080",
      PGHOST: env.PGHOST ?? "",
      PGPORT: env.PGPORT ?? "5432",
      PGDATABASE: env.PGDATABASE ?? "postgres",
      PGUSER: env.PGUSER ?? "",
      PGPASSWORD: env.PGPASSWORD ?? "",
      // Secrets de bootstrap do admin (opcionais — BootstrapAdminRunner ignora se vazios)
      ADMIN_BOOTSTRAP_EMAIL: env.ADMIN_BOOTSTRAP_EMAIL ?? "",
      ADMIN_BOOTSTRAP_PASSWORD: env.ADMIN_BOOTSTRAP_PASSWORD ?? "",
      ADMIN_BOOTSTRAP_NAME: env.ADMIN_BOOTSTRAP_NAME ?? "Administrador",
      // Limita heap da JVM para caber em standard-1/standard-2.
      // Sobrescrito se JAVA_OPTS já vier via secrets/vars.
      JAVA_OPTS: (env as Record<string, string | undefined>).JAVA_OPTS ?? "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC",
    };
  }

  onStart(): void {
    console.log("[veritas] container started");
  }

  onStop(exitCode: number, reason: string): void {
    console.log(`[veritas] container stopped exitCode=${exitCode} reason=${reason}`);
  }

  onError(error: unknown): void {
    console.error("[veritas] container error:", error);
    throw error as Error;
  }
}

export default class extends WorkerEntrypoint {
  async fetch(request: Request): Promise<Response> {
    // Falha de env aqui é a causa #1 do "container is not running":
    // sem PGHOST/PGUSER/PGPASSWORD o Spring morre no boot e o
    // container sai com exit code != 0.
    if (!this.env.PGHOST || !this.env.PGUSER || !this.env.PGPASSWORD) {
      return Response.json(
        {
          error: "missing_database_env",
          message: "PGHOST/PGUSER/PGPASSWORD não configurados no Worker. Rode: npx wrangler secret put PGHOST (etc.) e redeploy.",
        },
        { status: 500 },
      );
    }
    // NOTA: envVars do container são fixadas no construtor do Durable Object.
    // Trocar secrets (PGHOST/PGUSER/PGPASSWORD) NÃO atualiza o DO existente:
    // é preciso trocar o nome abaixo (v1 -> v2 -> ...) para forçar um DO novo
    // com o env atual, ou aguardar evicção. Histórico: "veritas" (direct db:5432),
    // "veritas-v2" (pooler us-west-2:6543).
    const id = this.env.VERITAS_CONTAINER.idFromName("veritas-v2");
    const container = this.env.VERITAS_CONTAINER.get(id);
    // fetch() inicia o container automaticamente e renova o sleepAfter.
    // Não chame start()/exec() manualmente aqui.
    return container.fetch(request);
  }
}