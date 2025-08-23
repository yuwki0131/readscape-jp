# タスク01: Docker開発環境構築

## タスク概要
Docker と Docker Compose を使用した一貫性のある開発環境の構築を行います。

## 実装内容

### 1. docker-compose.yml作成
```yaml
version: '3.8'
services:
  app:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
    depends_on:
      - postgres
    volumes:
      - ./backend:/app

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: readscape
      POSTGRES_USER: readscape_user
      POSTGRES_PASSWORD: readscape_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 2. Dockerfile作成
```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY gradlew .
COPY gradle gradle/
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies
COPY src src/
CMD ["./gradlew", "bootRun"]
```

### 3. 開発用スクリプト作成
- 環境起動スクリプト
- データベース初期化スクリプト
- ログ監視スクリプト

## 受け入れ条件
- [ ] Docker環境が正常に起動する
- [ ] アプリケーションにアクセスできる
- [ ] データベースが正常に動作する
- [ ] ホットリロードが機能する
- [ ] ポート競合が発生しない
- [ ] 環境変数が正しく設定される

## 関連ファイル
- `docker-compose.yml`
- `backend/Dockerfile`
- `scripts/dev-start.sh`