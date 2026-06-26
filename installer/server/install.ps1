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
$JAVA_EXE = "C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot\bin\java.exe"

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

function Install-Java {
    Write-Step "Проверка Java..."

    if (Test-Path $JAVA_EXE) {
        Write-Host "Java уже установлена." -ForegroundColor Green
        return
    }

    Write-Host "Скачивание Java 21..."
    $javaUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.msi"
    $javaInstaller = "$env:TEMP\java21.msi"

    Invoke-WebRequest -Uri $javaUrl -OutFile $javaInstaller -UseBasicParsing

    Write-Host "Установка Java 21..."
    Start-Process msiexec -ArgumentList "/i `"$javaInstaller`" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome" -Wait

    Remove-Item $javaInstaller -Force -ErrorAction SilentlyContinue
    Write-Host "Java установлена." -ForegroundColor Green
}

function Install-PostgreSQL {
    Write-Step "Проверка PostgreSQL..."

    if (Test-Path "$PG_BIN\psql.exe") {
        Write-Host "PostgreSQL уже установлен." -ForegroundColor Green
        return
        $svc = Get-Service -Name "postgresql-x64-16" -ErrorAction SilentlyContinue
            if (-not $svc) {
                Write-Host "Регистрируем службу PostgreSQL..."
                & "$PG_BIN\pg_ctl.exe" register -N "postgresql-x64-16" -D "C:\InventoryServer\pgdata" -U "NT AUTHORITY\NetworkService" -w
                Start-Sleep -Seconds 2
            }
            return
    }

    Write-Host "Скачивание PostgreSQL $PG_VERSION..."
    Write-Host "Это может занять несколько минут..."

    Invoke-WebRequest -Uri $PG_INSTALLER_URL -OutFile $PG_INSTALLER -UseBasicParsing

    Write-Host "Установка PostgreSQL (тихий режим)..."
    $pgArgs = @(
        "--mode", "unattended",
        "--unattendedmodeui", "none",
        "--superpassword", $PG_PASSWORD,
        "--servicename", "postgresql-x64-16",
        "--datadir", $PG_DATA,
        "--serverport", "5432"
    )
    Start-Process -FilePath $PG_INSTALLER -ArgumentList $pgArgs -Wait -NoNewWindow

    Remove-Item $PG_INSTALLER -Force -ErrorAction SilentlyContinue
    Write-Host "PostgreSQL установлен." -ForegroundColor Green
}

function Setup-Database {
    Write-Step "Настройка базы данных..."

    chcp 65001 | Out-Null

    # Запускаем службу PostgreSQL
    $pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($pgService) {
        Write-Host "Запускаем службу: $($pgService.Name)"
        Start-Service -Name $pgService.Name -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 5
    }

    $env:PGPASSWORD = $PG_PASSWORD

    # Ждём соединения через TCP
    $retries = 0
    do {
        Start-Sleep -Seconds 3
        $retries++
        Write-Host "Проверка соединения ($retries/20)..."
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $tcp.Connect("localhost", 5432)
            $tcp.Close()
            Write-Host "PostgreSQL доступен!" -ForegroundColor Green
            break
        } catch {
            Write-Host "Ожидание..."
        }
    } while ($retries -lt 20)

    if ($retries -ge 20) {
        Write-Host "PostgreSQL не запустился." -ForegroundColor Red
        Read-Host "Нажмите Enter для выхода"
        exit 1
    }

    # Создаём пользователя
    $userExists = & "$PG_BIN\psql.exe" -U postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" 2>&1
    if ($userExists -notmatch "1") {
        & "$PG_BIN\psql.exe" -U postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';"
        Write-Host "Пользователь $DB_USER создан." -ForegroundColor Green
    } else {
        Write-Host "Пользователь $DB_USER уже существует." -ForegroundColor Yellow
    }

    # Создаём БД
    $dbExists = & "$PG_BIN\psql.exe" -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>&1
    if ($dbExists -notmatch "1") {
        & "$PG_BIN\psql.exe" -U postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
        Write-Host "База данных $DB_NAME создана." -ForegroundColor Green
    } else {
        Write-Host "База данных $DB_NAME уже существует." -ForegroundColor Yellow
    }
}

function Install-Server {
    Write-Step "Установка сервера..."

    New-Item -ItemType Directory -Force -Path $INSTALL_DIR | Out-Null
    New-Item -ItemType Directory -Force -Path "$INSTALL_DIR\logs" | Out-Null

    # Копируем JAR
    $scriptDir = Split-Path -Parent ([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName)
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

    # Создаём winsw конфиг с полным путём к Java
    $winswXml = @"
<service>
  <id>InventoryServer</id>
  <name>Inventory Server</name>
  <description>Inventory Management Server</description>
  <executable>$JAVA_EXE</executable>
  <arguments>-jar "$INSTALL_DIR\inventory-server.jar" --spring.config.location=file:$INSTALL_DIR\application.properties</arguments>
  <logpath>$INSTALL_DIR\logs</logpath>
  <log mode="roll"/>
  <onfailure action="restart" delay="10 sec"/>
</service>
"@
    $winswXml | Out-File -FilePath "$INSTALL_DIR\InventoryServer.xml" -Encoding UTF8

    # Открываем порт в брандмауэре
    netsh advfirewall firewall add rule name="InventoryServer" dir=in action=allow protocol=TCP localport=8080 | Out-Null

    # Удаляем старую службу если есть
    $existing = Get-Service -Name "InventoryServer" -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "Удаляем старую службу..."
        & "$INSTALL_DIR\InventoryServer.exe" stop 2>&1 | Out-Null
        & "$INSTALL_DIR\InventoryServer.exe" uninstall 2>&1 | Out-Null
        Start-Sleep -Seconds 2
    }

    # Регистрируем и запускаем службу
    Write-Step "Регистрация службы Windows..."
    & "$INSTALL_DIR\InventoryServer.exe" install
    Start-Sleep -Seconds 3
    & "$INSTALL_DIR\InventoryServer.exe" start

    # Проверяем что служба запустилась
    Start-Sleep -Seconds 5
    $svc = Get-Service -Name "InventoryServer" -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -eq "Running") {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host " Сервер успешно установлен!" -ForegroundColor Green
        Write-Host " Адрес: http://localhost:8080" -ForegroundColor Green
        Write-Host " Служба: InventoryServer (автозапуск)" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "Служба не запустилась. Проверьте логи:" -ForegroundColor Yellow
        Write-Host "  $INSTALL_DIR\logs\" -ForegroundColor Yellow
    }

    Write-Host ""
    Read-Host "Нажмите Enter для закрытия"
}

# Точка входа
try {
    Check-Admin
    Install-PostgreSQL
    Install-Java
    Setup-Database
    Install-Server
} catch {
    Write-Host ""
    Write-Host "ОШИБКА: $_" -ForegroundColor Red
    Write-Host $_.ScriptStackTrace -ForegroundColor DarkRed
    Read-Host "Нажмите Enter для выхода"
}