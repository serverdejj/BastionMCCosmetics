# BastionMCCosmetics 1.3.0

- Paginación automática para Auras, Globos y Sombreros (28 por página), con siguiente/anterior y regreso al menú principal.
- Entradas actuales: 16 auras, 16 globos y 30 sombreros. Cada una cuesta 20 gemas.
- Nuevos sombreros de texturas Minecraft-Heads: Astronauta, Cofre, Spider-Man, Batman, Discord, YouTube, TikTok, Instagram y Dinosaurio. También incluye cascos y cabezas vanilla adicionales.
- El globo se elevó 0.5 bloques respecto a su versión anterior para mejorar la unión visual con la cuerda. Los globos naranja y oscuro usan texturas existentes de repuesto, no cabezas Blaze/Enderman rotas.
- El sistema de auras no se modificó. `players.yml` se conserva, incluyendo IDs anteriores.
- Para añadir más cosméticos en YAML, agrega nuevas claves bajo `cosmetics.auras`, `cosmetics.balloons` o `cosmetics.hats`. Los `slot:` individuales antiguos se ignoran al mostrar el menú porque las posiciones son automáticas.
- Compila Java 21 para Purpur 1.21.11 usando GitHub Actions. Reemplaza `config.yml` para cargar los nuevos productos; **no borres `players.yml`**.

**Nota:** proyecto fuente, sin validación de ejecución en servidor. Verificar animación y texturas in-game.
