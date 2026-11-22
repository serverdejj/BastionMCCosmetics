# BastionMCCosmetics 1.1.0 - BASTION MC

Compatible con Purpur 1.21.11 / Java 21. Proyecto fuente listo para GitHub Actions. No es un JAR compilado.

## Cambios
- **10 auras, 10 globos y 10 sombreros**, todos de **20 gemas**.
- En la GUI: para OP/permisos especiales, **DESBLOQUEADO**; para los demás sin comprar, **DESBLOQUEAR - 20 gemas** (en rojo si no alcanzan las gemas).
- Con clic izquierdo, se reclama o se paga y equipa. Con clic derecho, se desequipa. Los datos siguen en `players.yml`, que nunca debes borrar.
- Globos ahora son **cabezas con textura de jugador** (`ItemDisplay`) y correa vanilla que sale de una gallina invisible (`Chicken`) atada al jugador. El globo se mueve contigo y se elimina al desconectarte. No más letras/hologramas flotantes.
- Los `head-owner` son ejemplos MHF_ y pueden reemplazarse por nombres Minecraft; para una textura personalizada, en cada globo se puede configurar `head-texture` con un hash de textures.minecraft.net. Las cabezas de propietarios requieren que el servidor resuelva las skins.
- Mascotas mantienen el cartel **PRÓXIMAMENTE**.

## Instalación
1. Haz copia de `plugins/BastionMCCosmetics/players.yml` (guarda saldo y compras).
2. Reemplaza el repositorio de GitHub con esta versión, incluyendo `.github/workflows/build.yml`.
3. Ejecuta Actions → Compilar BastionMCCosmetics; descarga el JAR del Artifacts y úsalo en `plugins/`.
4. **Reemplaza también** `plugins/BastionMCCosmetics/config.yml` con el que trae este ZIP: `src/main/resources/config.yml`. `saveDefaultConfig()` **no sobreescribe** tu antiguo config.yml.
5. Reinicia el lobby. Si usas `/cosmeticos reload`, también se recrean los globos.

## Permisos
- OP o `bastioncosmetics.free`: desbloquea sin pagar.
- `bastioncosmetics.use`: accede al menú (default).
- `bastioncosmetics.admin`: recarga y administra gemas (OP por defecto).

## Gemas
Con `settings.gems-source: internal` se guardan en `players.yml`, independientes de cualquier marcador de otro plugin. Puedes integrar el scoreboard real luego de confirmar su origen.

## Nota técnica
El leash vanilla depende de la sincronización de entidades y puede verse brevemente tensado/soltado durante teletransportes rápidos. Se recomienda probarlo sobre el Lobby para confirmar que tu cliente muestra correctamente los heads y la correa. El código se ha inspeccionado, pero el JAR aún debe compilarse y probarse en tu servidor.
