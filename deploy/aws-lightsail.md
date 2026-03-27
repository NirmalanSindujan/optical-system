# AWS Lightsail deployment

## Recommended layout

- Run only `app` and `db` from `docker-compose.yml`
- Expose the Spring Boot container directly on port `8080`
- Put HTTPS in front with a Lightsail load balancer or host-level reverse proxy if needed

## First boot

1. Install Docker Engine and the Compose plugin on the instance
2. Create a 1 GB swap file before starting containers
3. Copy the project to the server
4. Create `.env` from `.env.example` and replace secrets
5. Start the stack with `docker compose up --build -d`

## Swap setup

```sh
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## Suggested environment values

```env
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

DB_HOST=db
DB_PORT=5432
DB_NAME=optical_db
DB_USERNAME=optical_user
DB_PASSWORD=replace-with-a-strong-password
DB_MAX_CONNECTIONS=20
DB_SHARED_BUFFERS=32MB
DB_EFFECTIVE_CACHE_SIZE=96MB
DB_MAINTENANCE_WORK_MEM=16MB
DB_WORK_MEM=2MB
DB_TEMP_BUFFERS=2MB
DB_WAL_BUFFERS=2MB

DB_MAX_POOL_SIZE=4
DB_MIN_IDLE=0
DB_CONNECTION_TIMEOUT=20000
DB_IDLE_TIMEOUT=300000
DB_MAX_LIFETIME=600000

TOMCAT_MAX_THREADS=8
TOMCAT_MIN_SPARE_THREADS=2
TOMCAT_ACCEPT_COUNT=20
SPRING_MAIN_LAZY_INITIALIZATION=true
SPRINGDOC_ENABLED=false

JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION=86400000
JAVA_OPTS=-Xms96m -Xmx160m -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom
```

## Runtime guidance

- Keep the app container near `320m`
- Keep Postgres near `160m`
- Leave the remaining memory for the OS, Docker, and filesystem cache
- If traffic grows, move Postgres off the same instance before increasing app concurrency
