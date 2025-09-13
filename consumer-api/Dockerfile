FROM openjdk:21-jdk-slim

WORKDIR /app

# 必要なパッケージのインストール
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 非ルートユーザーの作成
RUN groupadd -r appgroup && useradd -r -g appgroup -m appuser

# Gradle Wrapperファイルのコピー
COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .

# 実行権限の付与
RUN chmod +x ./gradlew

# 依存関係の事前ダウンロード（キャッシュ効率化）
RUN ./gradlew dependencies --no-daemon || return 0

# ソースコードのコピー
COPY src src/

# アプリケーションのビルド
RUN ./gradlew build --no-daemon -x test

# Gradle cache directory作成と権限設定
RUN mkdir -p /home/appuser/.gradle && chown -R appuser:appgroup /home/appuser/.gradle
RUN chown -R appuser:appgroup /app

# ヘルスチェック用のエンドポイント設定
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# ポートの公開
EXPOSE 8080

# 非ルートユーザーに切り替え
USER appuser

# アプリケーションの起動
CMD ["./gradlew", "bootRun", "--no-daemon"]