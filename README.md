# BastionMCCosmetics 1.2.0 — BASTION MC

- Los diez globos usan ahora texturas reales de cabezas de colores (Minecraft-Heads), en lugar de cabezas de mobs de colores incorrectos. Todos los globos admiten `head-texture` y `head-owner` personalizados.
- El menú de globos también muestra la cabeza que realmente tendrá cada globo.
- Se incorporaron sombreros de cabezas de zombie, creeper y piglin, más una cabeza personalizada azul. El plugin también admite `helmet: PLAYER_HEAD`, `head-owner` o `head-texture` (hash de Mojang) en cualquier sombrero.
- Las auras tienen `count`, `radius` y `spiral` configurables. Aura de fuego genera una hélice de fuego con llamas adicionales.
- Se conservan los 30 IDs, la economía, los menús y permisos originales.
- La versión incluye Java 21, Paper 1.21.11 y GitHub Actions.

## Instalación

1. Guarda copia de `plugins/BastionMCCosmetics/players.yml` y `config.yml`.
2. Actualiza el repositorio con esta fuente y compila con Actions.
3. Reemplaza JAR en el servidor Lobby.
4. **Reemplaza también `plugins/BastionMCCosmetics/config.yml` por el nuevo** (el servidor no cambia automáticamente archivos preexistentes).
5. Reinicia Lobby y prueba globos, cabezas y auras.

`players.yml` NO debe borrarse; conserva gemas, compras y equipos.

Los globos y cabezas usan texturas decorativas externas (Mojang/CDN) y necesitan que los clientes las carguen. Si una textura no carga, comprueba acceso del cliente a textures.minecraft.net.
