# BastionMCCosmetics 1.0.0 — BASTION MC

Plugin independiente para Purpur/Paper 1.21.11, Java 21.

## Funciones
- `/cosmeticos` y `/cosmetics`: GUI principal → Auras, Globos, Sombreros y Mascotas (Próximamente).
- Clic izquierdo: compra y equipa. Clic derecho: desequipa la categoría.
- Si no alcanza el saldo, precio rojo y al intentar comprar cierra el menú y avisa en chat.
- OP o permiso `bastioncosmetics.free`: reclamar sin pagar; las compras se guardan por UUID.
- Gemas persistentes internas independientes de Vault, con opción `settings.gems-source: scoreboard` para integrar un objetivo *vanilla* del scoreboard. NO se enlaza automáticamente al saldo visible de otros plugins; comprueba qué lo alimenta.
- Efectos de partículas para auras, TextDisplay flotante para globos, cascos reales para sombreros.
- Menús, iconos, materiales, posiciones, costos, mensajes y HEX legacy `&#RRGGBB` modificables en `config.yml`.

## Compilar
1. Sube todos los archivos del ZIP (con sus carpetas) a un repositorio de GitHub.
2. GitHub → Actions → `Compilar BastionMCCosmetics` → Run workflow.
3. Descarga el artifact `BastionMCCosmetics-JAR` y extrae el .jar.
4. Instala el JAR en `plugins/` de tu Lobby y reinicia.

## Gemas y permisos
- `/gemas` : consultar saldo propio.
- `/gemas <jugador>`: consultar saldo de jugador conectado (admin).
- `/gemas add <jugador> <cantidad>`: dar gemas (admin).
- `/gemas remove <jugador> <cantidad>`: quitar gemas (admin).
- `/gemas set <jugador> <cantidad>`: establecer gemas (admin).
- `/cosmeticos reload` : recargar YAML (admin).
- `bastioncosmetics.use` (`default: true`).
- `bastioncosmetics.admin` (`default: op`).
- `bastioncosmetics.free` (`default: op`) — por ejemplo con LuckPerms:
  `lp user JJFOOX permission set bastioncosmetics.free true`

## Seguridad/limitaciones conocidas
- Por seguridad, un sombrero cosmético **solo puede equiparse cuando el espacio del casco está vacío**. Al quitarlo, al salir del lobby o desactivar el plugin se retira. No coloques cascos de combate en el lobby mientras llevas un sombrero.
- Los globos son representaciones de texto flotante, no modelos con cuerda real. Si quieres globos modelados, será necesario recurso gráfico/versión posterior.
- El modo `scoreboard` usa el *scoreboard principal* del servidor, no un placeholder de otra economía. Si tu sidebar `Gemas: 0` proviene de otro plugin, mantén modo `internal` hasta conocer la fuente.
- Los cosméticos solo funcionan en mundos `Void`, `void` y `spawn` por defecto (modificable).
- La funcionalidad está preparada como código fuente, pero debe compilarse y probarse en tu servidor antes de considerarla definitiva.
