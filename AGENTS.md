# Repository Guidelines

## Project Structure & Module Organization
Source lives in `Distribuidor/` (client/orchestrator) and `Receptor/` (server workers). Shared protocol classes (`Pedido`, `Resposta`, `Parceiro`, `Comunicado*`, `Teclado`) stay in those same folders because the project runs in the default Java package. IntelliJ files (`.idea/`, `Atividade PP.iml`) describe the IDE module; edit them only when changing project settings. Build output goes to `out/production/Atividade PP/`; keep it for quick runs but clean it, or add it to `.gitignore`, before opening a PR.

## Build, Test, and Development Commands
- `javac -encoding UTF-8 -d out/production/Atividade\ PP Receptor/*.java Distribuidor/*.java` compiles every class.
- `java -cp out/production/Atividade\ PP Receptor [PORTA]` starts a receptor. Omit the argument to keep `12345`.
- `java -cp out/production/Atividade\ PP Distribuidor` launches the distributor once at least one receptor is live; update the `hosts` array first.
- `java -cp out/production/Atividade\ PP Teste` runs the helper in `Distribuidor/Teste.java` to validate serialization logic without sockets.

## Coding Style & Naming Conventions
Use four-space indentation, braces on their own lines, and explicit imports. Classes remain PascalCase (`SupervisoraDeConexao`), variables/methods camelCase, and constants `public static final` with uppercase identifiers. Carry forward the `[D]`/`[R]` log prefixes and Portuguese prompts so console transcripts stay uniform. Avoid adding packages unless you update every reference to the serialized classes.

## Testing Guidelines
There is no automated suite yet. Add lightweight drivers next to the affected module (e.g., `Distribuidor/TesteContagem.java`) and launch them with the classpath shown above. Document manual checks in PRs: vector size, chosen `procurado`, receptor host list, and expected totals. Whenever networking changes, spin up at least two receptors on different hosts and confirm aggregated counts match a sequential baseline.

## Commit & Pull Request Guidelines
Git history is blank, so adopt imperative, scope-tagged messages such as `feat(distribuidor): balance workload`. PRs must summarize the change, list the commands executed (`javac`, `java Receptor`, `java Distribuidor`, test helpers), and paste relevant logs or screenshots from both distributor and receptor consoles. Flag protocol changes (new fields on `Pedido`, altered ports) prominently and explain how to migrate running servers. Exclude `out/production` and other binaries from commits.

## Configuration Tips
Keep `Distribuidor.PORTA_PADRAO`, the `hosts` array, and `Receptor.PORTA_PADRAO` in sync. On lab machines, confirm the firewall allows the chosen port or note the alternative inside the PR so collaborators can reproduce your setup.
