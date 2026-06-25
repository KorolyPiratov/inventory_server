$ErrorActionPreference = "Stop"

$INSTALL_DIR = "C:\InventoryServer"
$PG_VERSION = "16.3-1"
$PG_INSTALLER_URL = "https://get.enterprisedb.com/postgresql/postgresql-$PG_VERSION-windows-x64.exe"
$PG_INSTALLER = "$env:TEMP\pg_installer.exe"
$PG_DATA = "C:\InventoryServer\pgdata"
$PG_PASSWORD = "inventory_pg_pass"
$DB_NAME = "inventory_db"
$DB_USER = "inventory"
$DB_PASS = "inventory_pass"
$PG_BIN = "C:\Program Files\PostgreSQL\16\bin"

function Write-Step($msg) {
    Write-Host ""
    Write-Host ">>> $msg" -ForegroundColor Cyan
}

function Check-Admin {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Host "Запустите установщик от имени администратора!" -ForegroundColor Red
        Read-Host "Нажмите Enter для выхода"
        exit 1
    }
}

function Install-PostgreSQL {
    Write-Step "Проверка PostgreSQL..."

    if (Test-Path "$PG_BIN\psql.exe") {
        Write-Host "PostgreSQL уже установлен." -ForegroundColor Green
        return
    }

    Write-Step "Скачивание PostgreSQL $PG_VERSION..."
    Write-Host "Это может занять несколько минут..."

    try {
        Invoke-WebRequest -Uri $PG_INSTALLER_URL -OutFile $PG_INSTALLER -UseBasicParsing
    } catch {
        Write-Host "Ошибка скачивания: $_" -ForegroundColor Red
        Read-Host "Нажмите Enter для выхода"
        exit 1
    }

    Write-Step "Установка PostgreSQL (тихий режим)..."
    $args = @(
        "--mode", "unattended",
        "--unattendedmodeui", "none",
        "--superpassword", $PG_PASSWORD,
        "--servicename", "postgresql-x64-16",
        "--datadir", $PG_DATA,
        "--serverport", "5432"
    )
    Start-Process -FilePath $PG_INSTALLER -ArgumentList $args -Wait -NoNewWindow

    Remove-Item $PG_INSTALLER -Force -ErrorAction SilentlyContinue
    Write-Host "PostgreSQL установлен." -ForegroundColor Green
}

function Setup-Database {
    Write-Step "Настройка базы данных..."

    $env:PGPASSWORD = $PG_PASSWORD

    # Ждём пока PostgreSQL запустится
    $retries = 0
    do {
        Start-Sleep -Seconds 2
        $retries++
        $result = & "$PG_BIN\pg_isready.exe" -U postgres 2>&1
    } while ($result -notmatch "accepting" -and $retries -lt 15)

    if ($retries -ge 15) {
        Write-Host "PostgreSQL не запустился вовремя." -ForegroundColor Red
        Read-Host "Нажмите Enter для выхода"
        exit 1
    }

    # Проверяем существует ли уже пользователь
    $userExists = & "$PG_BIN\psql.exe" -U postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" 2>&1
    if ($userExists -ne "1") {
        & "$PG_BIN\psql.exe" -U postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';"
        Write-Host "Пользователь $DB_USER создан." -ForegroundColor Green
    } else {
        Write-Host "Пользователь $DB_USER уже существует." -ForegroundColor Yellow
    }

    # Проверяем существует ли уже БД
    $dbExists = & "$PG_BIN\psql.exe" -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>&1
    if ($dbExists -ne "1") {
        & "$PG_BIN\psql.exe" -U postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
        Write-Host "База данных $DB_NAME создана." -ForegroundColor Green
    } else {
        Write-Host "База данных $DB_NAME уже существует." -ForegroundColor Yellow
    }
}

function Install-Server {
    Write-Step "Установка сервера..."

    # Создаём папку установки
    New-Item -ItemType Directory -Force -Path $INSTALL_DIR | Out-Null

    # Копируем JAR
    $scriptDir = Split-Path -Parent $MyInvocation.ScriptName
    Copy-Item "$scriptDir\inventory-server.jar" "$INSTALL_DIR\inventory-server.jar" -Force

    # Создаём application.properties
    $props = @"
spring.datasource.url=jdbc:postgresql://localhost:5432/$DB_NAME
spring.datasource.username=$DB_USER
spring.datasource.password=$DB_PASS
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
server.port=8080
"@
    $props | Out-File -FilePath "$INSTALL_DIR\application.properties" -Encoding UTF8

    # Копируем WinSW
    Copy-Item "$scriptDir\winsw.exe" "$INSTALL_DIR\InventoryServer.exe" -Force

    # Создаём winsw конфиг
    $winswXml = @"
<service>
  <id>InventoryServer</id>
  <name>Inventory Server</name>
  <description>Inventory Management Server</description>
  <executable>java</executable>
  <arguments>-jar "$INSTALL_DIR\inventory-server.jar" --spring.config.location=file:$INSTALL_DIR\application.properties</arguments>
  <logpath>$INSTALL_DIR\logs</logpath>
  <log mode="roll"/>
  <onfailure action="restart" delay="10 sec"/>
</service>
"@
    $winswXml | Out-File -FilePath "$INSTALL_DIR\InventoryServer.xml" -Encoding UTF8

    # Открываем порт в брандмауэре
    netsh advfirewall firewall add rule name="InventoryServer" dir=in action=allow protocol=TCP localport=8080 | Out-Null

    # Регистрируем и запускаем службу
    Write-Step "Регистрация службы Windows..."
    & "$INSTALL_DIR\InventoryServer.exe" install
    Start-Sleep -Seconds 2
    & "$INSTALL_DIR\InventoryServer.exe" start

    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " Сервер успешно установлен!" -ForegroundColor Green
    Write-Host " Адрес: http://localhost:8080" -ForegroundColor Green
    Write-Host " Служба: InventoryServer (автозапуск)" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Read-Host "Нажмите Enter для закрытия"
}

# Точка входа
Check-Admin
Install-PostgreSQL
Setup-Database
Install-Server