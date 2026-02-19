## [Máster en Ingeniería Web por la Universidad Politécnica de Madrid (miw-upm)](http://miw.etsisi.upm.es)

## Back-end con Tecnologías de Código Abierto (BETCA).

> Este proyecto es un apoyo docente de la asignatura y contiene ejemplos prácticos sobre Spring

### Tecnologías necesarias

`Java` `Maven` `GitHub` `GitHub Actions` `Spring-Boot` `GitHub Packages` `Docker` `OpenAPI`

### :gear: Instalación del proyecto

1. Clonar el repositorio en tu equipo, **mediante consola**:

```sh
> cd <folder path>
> git clone https://github.com/miw-upm/goa-billing
```

2. Importar el proyecto mediante **IntelliJ IDEA**
    * **Open**, y seleccionar la carpeta del proyecto.

### :gear: Ejecución en local con IntelliJ

* Ejecutar la clase **Application** con IntelliJ

### :gear: Ejecución en local con Docker
* Crear la red, solo una vez:

```sh
> docker network create goa
```

* Ejecutar en el proyecto la siguiente secuencia de comandos de Docker:

```sh
> docker compose up --build -d
```

* Cliente Web: `http://localhost:8083/swagger-ui.html`
