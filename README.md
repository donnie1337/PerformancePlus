# PerformancePlus

Plugin de controle e otimização de performance para servidores **Spigot/Bukkit**,
com configuração por mundo.

> ⚠️ **Aviso honesto**: este projeto foi escrito e revisado manualmente, mas
> **não foi compilado nem testado em servidor real**, porque o ambiente onde
> ele foi gerado não tem acesso à internet para baixar o Maven Central nem o
> repositório do Spigot. Compile localmente (veja abaixo) e me avise se
> encontrar algum erro — eu ajusto.

## Módulos incluídos

| Módulo | Classe | O que faz |
|---|---|---|
| Limite de mobs por chunk | `MobLimiter` | Cancela `CreatureSpawnEvent` acima do limite |
| Limite de spawners por chunk | `SpawnerLimiter` | Bloqueia colocação de spawner além do limite |
| Limite de entidades | `EntityLimiter` | Teto geral (todos os tipos) por chunk |
| Limite de itens dropados | `ItemLimiter` | Cancela `ItemSpawnEvent` acima do limite |
| Limite de XP | `XPLimiter` | Limita orbes de experiência por chunk |
| Limite de redstone | `RedstoneLimiter` | Contém `BlockRedstoneEvent` em excesso |
| Limite de hoppers/funis | `HopperLimiter` | Cobre hoppers de bloco e vagonetes-funil |
| Controle de pistões | `PistonController` | Limite estático + limite de ativações/s |
| Controle de observers | `ObserverController` | Limite estático de observers por chunk |
| Controle de geração de chunks | `ChunkGenerationController` | Monitoramento + alerta (ver limitações) |
| Controle de farms | `FarmController` | Pontuação heurística + sinalização |
| Monitoramento de performance | `PerformanceMonitor` | TPS/MSPT calculados manualmente |

## Como compilar

1. Instale o **Java 21+** e o **Maven**.
2. O `pom.xml` já aponta para `org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT`
   (Minecraft 26.2 "Chaos Cubed" — depois da 1.21.11 o Mojang passou a
   numerar por ano: 26.1, 26.1.1, 26.1.2, 26.2...).
3. Se o Maven não conseguir baixar essa versão do repositório do Spigot,
   gere-a você mesmo com o BuildTools oficial:
   ```bash
   wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
   java -jar BuildTools.jar --rev 26.2
   ```
   Isso instala o artefato certo no seu `~/.m2` local.
4. Rode:
   ```bash
   mvn clean package
   ```
5. O jar final fica em `target/PerformancePlus.jar`. Copie para a pasta
   `plugins/` do seu servidor.

Como `api-version` no `plugin.yml` fica fixo em `1.21` (esse campo funciona
como uma versão **mínima**, não uma versão exata), o mesmo jar deve carregar
normalmente em qualquer servidor 1.21.x até 26.x.

## Configuração por mundo

Em `config.yml`, qualquer chave dentro de `worlds.<nome-do-mundo>.limits`
sobrescreve o valor equivalente em `limits` (global) **só para aquele
mundo**. Exemplo já incluso no arquivo:

```yaml
worlds:
  world_nether:
    limits:
      mobs-per-chunk: 40
      pistons-per-chunk: 8
```

## Comandos

- `/pperf` ou `/pperf status` — TPS, MSPT, entidades e chunks carregados
- `/pperf chunk` — informações do chunk onde você está
- `/pperf farms` — lista de chunks sinalizados como possível farm
- `/pperf recount` — reconta pistões/observers via varredura completa (admin)
- `/pperf reload` — recarrega o `config.yml` (admin)

## Permissões

Veja a lista completa em `plugin.yml`. As mais importantes:
- `performanceplus.admin` — reload e recount
- `performanceplus.notify` — alertas de TPS/MSPT baixo e geração de chunks
- `performanceplus.notify.farms` — alertas de farms detectadas
- `performanceplus.bypass.*` — ignora limites de spawners/hoppers/pistões/observers

## Limitações conhecidas (leia antes de reportar "bug")

- **Pistões e observers**: o contador por chunk é incremental (soma/subtrai
  em `BlockPlaceEvent`/`BlockBreakEvent`). Blocos colocados por vias que não
  passam por esses eventos (WorldEdit, schematics, outros plugins que usam
  `Block#setType` diretamente) não são contados até você rodar
  `/pperf recount`, que faz uma varredura completa (uso manual, pode causar
  uma travada breve).
- **Geração de chunks**: o Bukkit não expõe um evento cancelável *antes* da
  geração — quando `ChunkLoadEvent` dispara, o chunk já existe. Por isso esse
  módulo é só monitoramento + alerta, não um bloqueio real. Para limitar de
  verdade, use uma ferramenta de pré-geração (ex: Chunky) ou, se migrar para
  Paper/Folia, os hooks assíncronos de geração via NMS.
- **Redstone**: a técnica usada (`setNewCurrent(oldCurrent)`) é a forma
  padrão de "engolir" uma atualização de redstone, mas em contraptions muito
  sensíveis a timing pode gerar efeitos colaterais visuais. Ajuste
  `redstone-updates-per-chunk-per-tick` com cuidado antes de usar em produção.
- **Farms**: a pontuação é uma heurística simples (pesos fixos por
  hopper/spawner/pistão/observer/mob/item). Ela **sinaliza e avisa a staff**,
  mas não pune ou destrói nada automaticamente — ajuste os pesos e o
  `activity-threshold` em `FarmController.java` e no `config.yml` conforme o
  seu servidor.

## Próximos passos sugeridos

- Adicionar persistência (SQLite/YAML) para farms sinalizadas sobreviverem a restart.
- Expor as métricas do `PerformanceMonitor` via placeholder para PlaceholderAPI.
- Adicionar testes com MockBukkit antes de publicar em produção.
