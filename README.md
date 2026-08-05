# Carnet Perritos - App Android

App nativa para cargar carnets desde el celular, sin necesidad de Android Studio.
GitHub la compila sola en la nube cada vez que subís el código.

## 1. Configurar el backend (una sola vez)

En el `.env` de tu servidor de carnets (el VPS), agregá:

```
API_KEY=alguna-clave-larga-y-secreta-que-inventes
```

Reiniciá el servidor (`pm2 restart carnets` si lo corrés con pm2) para que tome el cambio.

## 2. Subir este proyecto a GitHub

Esto lo hacés desde tu VPS por Termius (ya tenés git configurado ahí).

Subí esta carpeta `carnet-android` al VPS igual que hiciste con `carnet-app`
(zip + base64, o SFTP). Después:

```bash
cd ~/carnet-android
git init
git add .
git commit -m "App de carnets"
gh repo create carnet-perritos-app --public --source=. --push
```

Si `gh` (GitHub CLI) no está instalado o no está logueado:

```bash
gh auth login
```
y seguís las instrucciones (te da un código para pegar en github.com/login/device).

Si no querés usar `gh`, la alternativa manual es crear el repo vacío en
github.com desde el navegador, y luego:

```bash
git remote add origin https://github.com/TU_USUARIO/carnet-perritos-app.git
git branch -M main
git push -u origin main
```

## 3. Descargar el APK compilado

1. Andá a `https://github.com/TU_USUARIO/carnet-perritos-app/actions` desde el navegador del celular
2. Vas a ver un workflow "Compilar APK" corriendo (tarda 3-5 minutos la primera vez)
3. Cuando termine (tilde verde), entrá a esa ejecución
4. Abajo dice "Artifacts" → descargá `carnet-perritos-apk` (te baja un .zip)
5. Descomprimilo (el navegador de Android suele hacerlo solo, o usá una app de archivos)
   y vas a tener `app-debug.apk`
6. Tocá el .apk para instalarlo. Android te va a pedir permiso para "instalar apps
   de fuentes desconocidas" la primera vez — se lo das solo para este archivo.

## 4. Usar la app

1. Abrís "Carnet Perritos"
2. En "URL del servidor" ponés la dirección de tu VPS, por ejemplo `http://IP:3000`
   (la misma que usás para entrar al panel web)
3. En "API Key" pegás la misma clave que pusiste en el `.env`
4. Guardar configuración
5. "+ Nuevo carnet" para cargar uno, o "Ver carnets" para buscar y abrir el PDF

## Actualizar la app más adelante

Cualquier cambio que quieras (agregar un campo, cambiar el diseño, etc.), avisame,
te paso el código actualizado, lo subís con:

```bash
git add .
git commit -m "cambios"
git push
```

Y GitHub te compila automáticamente el nuevo APK en unos minutos.
