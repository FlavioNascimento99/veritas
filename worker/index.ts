import { Container } from "@cloudflare/containers";
import { WorkerEntrypoint } from "cloudflare:workers";

interface Env {
  VERITAS_CONTAINER: DurableObjectNamespace;
  PGHOST?: string;
  PGPORT?: string;
  PGDATABASE?: string;
  PGUSER?: string;
  PGPASSWORD?: string;
}

export class VeritasContainer extends Container {
  defaultPort = 8080;
  sleepAfter = "10m";
  enableInternet = true;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);

    this.envVars = {
      SPRING_PROFILES_ACTIVE: "prod",
      PGHOST: env.PGHOST ?? "",
      PGPORT: env.PGPORT ?? "5432",
      PGDATABASE: env.PGDATABASE ?? "postgres",
      PGUSER: env.PGUSER ?? "",
      PGPASSWORD: env.PGPASSWORD ?? "",
    };
  }
}

export default class extends WorkerEntrypoint {
  async fetch(request: Request): Promise<Response> {
    const id = this.env.VERITAS_CONTAINER.idFromName("veritas");
    const container = this.env.VERITAS_CONTAINER.get(id);
    return container.fetch(request);
  }
}