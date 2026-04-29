# Guía general: integración correcta de Flyway en PaperMC

Guía corta para integrar **Flyway 12.5.0** en un plugin de **PaperMC** usando MySQL/MariaDB y evitando errores comunes de classpath, migraciones y arranque.

---

## 1. Idea correcta

Flyway debe ejecutarse **al iniciar el plugin**, antes de registrar lógica que dependa de la base de datos.

No registres servicios del plugin antes de migrar la base de datos.

---

## 2. Estructura correcta

```txt
src/main/resources/
└── db/
    └── migration/
        ├── V1__create_players_table.sql
        └── V2__add_last_seen_to_players.sql
```

La ubicación recomendada para migraciones dentro del `.jar` es:

```txt
classpath:db/migration
```

En el proyecto eso equivale a:

```txt
src/main/resources/db/migration
```

---

## 3. Dependencias Gradle

```kotlin
plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val flywayVersion = "12.5.0"

// Versiones verificadas al momento de esta guía.
val mysqlConnectorVersion = "9.7.0"

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.mysql:mysql-connector-j:$mysqlConnectorVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.shadowJar {
    archiveClassifier.set("")

    // Necesario para no romper ServiceLoader de librerías como Flyway.
    mergeServiceFiles()
}

```

Regla práctica:

```txt
Paper API       -> compileOnly
Flyway/Hikari/JDBC -> implementation + shadowJar
```

---

## 6. Código base de integración

```java
package com.stephanofer.practicehub;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;

public final class PracticeHubPlugin extends JavaPlugin {

    private HikariDataSource dataSource;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            this.dataSource = createDataSource(getConfig());
            testConnection(this.dataSource);
            runMigrations(this.dataSource);

            getLogger().info("Base de datos inicializada correctamente.");
        } catch (Exception exception) {
            getLogger().severe("Error inicializando base de datos. Deshabilitando plugin.");
            exception.printStackTrace();

            closeDataSource();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Recién acá registrar comandos, listeners, services, repositories, etc.
    }

    @Override
    public void onDisable() {
        closeDataSource();
    }

    private void runMigrations(DataSource dataSource) {
        Flyway flyway = Flyway.configure(getClass().getClassLoader())
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .baselineOnMigrate(false)
            .load();

        flyway.migrate();
    }

}
```

---

## 7. Nombres correctos de migraciones

Correcto:

```txt
V1__create_players_table.sql
V2__add_last_seen_to_players.sql
V3__create_matches_table.sql
```

Incorrecto:

```txt
v1_create_players.sql
V1_create_players.sql
V1-create-players.sql
create_players.sql
```

Formato:

```txt
V + versión + __ + descripción + .sql
```

---

## 8. Ejemplo de migración

```sql
CREATE TABLE players (
    uuid CHAR(36) NOT NULL,
    name VARCHAR(16) NOT NULL,
    coins BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid)
);
```

---

## 9. Configuraciones seguras

Para plugin nuevo:

```java
.baselineOnMigrate(false)
.cleanDisabled(true)
```

Usá `baselineOnMigrate(true)` solo si ya existe una base de datos con tablas creadas manualmente y querés empezar a controlarla con Flyway.

No uses `clean` en producción.

---

## 10. Callbacks: no mezclar carpetas

Migraciones:

```java
.locations("classpath:db/migration")
```

Callbacks SQL:

```java
.callbackLocations("classpath:db/callback")
```

Estructura:

```txt
src/main/resources/db/
├── migration/
│   └── V1__create_players_table.sql
└── callback/
    └── beforeMigrate.sql
```

No metas callbacks dentro de `.locations()` como si fueran migraciones.

---

### Error raro con Shadow y Flyway

Causa probable:

```txt
No se fusionaron archivos META-INF/services.
```

Solución:

```kotlin
tasks.shadowJar {
    mergeServiceFiles()
}
```

---

Si respetás ese orden, evitás la mayoría de errores reales al integrar Flyway en plugins PaperMC modernos.
