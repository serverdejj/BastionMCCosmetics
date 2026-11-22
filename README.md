# BastionMCCosmetics 1.3.1

## Novedades 1.3.1 — globos y teletransporte

- Antes de teletransportar al jugador, elimina el globo anterior y reconstruye el globo y la cuerda en el destino al siguiente tick.
- Previene el drop del objeto `LEAD` mediante `EntityUnleashEvent#setDropLeash(false)` para las entidades ancla del globo.
- Bloquea que un jugador se lleve manualmente la cuerda de un globo cosmético.
- Si se suelta la cuerda durante el seguimiento normal, la vuelve a conectar automáticamente.
- Incluye teletransportes de AjParkourBastion, `/spawn` y cualquier otro plugin que produzca `PlayerTeleportEvent`.
- No modifica el funcionamiento de auras, sombreros, precios, menús, gemas ni la estructura de `players.yml`.
- Reemplaza el JAR anterior después de compilar en GitHub Actions. **Conserva `players.yml` y tu `config.yml` actual**.

## Funciones existentes

- Paginación automática para Auras, Globos y Sombreros (28 por página), con siguiente/anterior y regreso al menú principal.
- Entradas actuales: 16 auras, 16 globos y 30 sombreros. Cada una cuesta 20 gemas.
- Nuevos sombreros de texturas Minecraft-Heads: Astronauta, Cofre, Spider-Man, Batman, Discord, YouTube, TikTok, Instagram y Dinosaurio. También incluye cascos y cabezas vanilla adicionales.
- El globo se elevó 0.5 bloques respecto a su versión anterior para mejorar la unión visual con la cuerda. Los globos naranja y oscuro usan texturas existentes de repuesto, no cabezas Blaze/Enderman rotas.
- El sistema de auras no se modificó. `players.yml` se conserva, incluyendo IDs anteriores.
- Para añadir más cosméticos en YAML, agrega nuevas claves bajo `cosmetics.auras`, `cosmetics.balloons` o `cosmetics.hats`. Los `slot:` individuales antiguos se ignoran al mostrar el menú porque las posiciones son automáticas.
- Compila Java 21 para Purpur 1.21.11 usando GitHub Actions. Reemplaza `config.yml` para cargar los nuevos productos; **no borres `players.yml`**.

**Nota:** proyecto fuente, sin validación de ejecución en servidor. Verificar animación y texturas in-game.

## v1.3.2 - corrección de compilación
- Import correcto de `org.bukkit.event.player.PlayerUnleashEntityEvent` para Paper/Purpur 1.21.11.
- Sin cambios en los cosméticos ni en los archivos de datos de los jugadores.

## v1.3.3 — gemas en el scoreboard de TAB

Instala PlaceholderAPI junto con TAB y BastionMCCosmetics. El plugin registra automáticamente el placeholder `%bastioncosmetics_gemas%` al iniciar. También acepta `%bastioncosmetics_gems%`.

En `plugins/TAB/config.yml`, dentro de las `lines:` del scoreboard agrega:

```yaml
- " &b💎 &fGemas: &b%bastioncosmetics_gemas%"
```

Ejecuta `/tab reload` después de editar el TAB. No cambies `settings.gems-source` ni borres `players.yml`: el placeholder lee el mismo saldo que `/gemas`.

La expansión requiere PlaceholderAPI instalado, pero el resto del plugin puede cargar sin él. Fuente preparada para compilar con Java 21 y Purpur/Paper 1.21.11; no probado dentro del servidor.
