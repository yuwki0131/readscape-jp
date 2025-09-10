# タスク02: ユーザードキュメント作成

## タスク概要
エンドユーザー向けAPI利用ガイド、認証方法、基本的な使用例を含む包括的なドキュメントを作成します。

## 実装内容

### 1. API利用ガイド作成
#### 基本情報
- API概要とアーキテクチャ
- エンドポイントURL一覧
- レート制限とクォータ
- サポートされるデータフォーマット

#### 環境別エンドポイント
- 開発環境: `http://localhost:8080/api`
- テスト環境: `https://api-dev.readscape.jp`
- 本番環境: `https://api.readscape.jp`

### 2. 認証ガイド作成
#### JWT認証フロー
- トークン取得方法
- トークンの利用方法
- リフレッシュトークン処理
- 認証エラーハンドリング

#### 認証例
```bash
# ログイン
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'

# 認証付きリクエスト
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/users/profile
```

### 3. 基本的な使用例作成
#### 書籍検索
- 全書籍一覧取得
- カテゴリー別検索
- キーワード検索
- ページネーション

#### ショッピング機能
- カート管理
- 商品追加/削除
- 注文作成
- 注文履歴確認

#### ユーザー管理
- 会員登録
- プロフィール更新
- パスワード変更

### 4. レスポンス例集作成
#### 成功レスポンス
- 書籍一覧レスポンス
- 書籍詳細レスポンス
- ユーザー情報レスポンス
- 注文情報レスポンス

#### エラーレスポンス
- バリデーションエラー
- 認証エラー
- 権限エラー
- システムエラー

### 5. SDKとサンプルコード
#### JavaScript/TypeScript
- axios利用例
- fetch API利用例
- エラーハンドリング

#### curl コマンド例
- 各エンドポイントのcurl例
- 認証パターン別例
- パラメータ指定例

### 6. トラブルシューティング
#### よくある問題
- 認証エラーの対処法
- レート制限対応
- CORS問題の解決
- タイムアウト対応

#### エラーコード一覧
- HTTPステータスコード
- アプリケーションエラーコード
- 対処法と推奨アクション

## 受け入れ条件
- [ ] API利用ガイドが完成している
- [ ] 認証方法の詳細ガイドが作成されている
- [ ] 基本的な使用例が網羅されている
- [ ] レスポンス例が十分に提供されている
- [ ] SDKとサンプルコードが用意されている
- [ ] トラブルシューティングガイドが完成している
- [ ] エラーコード一覧が整備されている
- [ ] 日本語での説明が適切である
- [ ] 初心者でも理解できる内容である
- [ ] コードサンプルが動作する

## 関連ファイル
- `docs/api/user-guide.md`
- `docs/api/authentication-guide.md`
- `docs/api/examples/books-api-examples.md`
- `docs/api/examples/cart-api-examples.md`
- `docs/api/examples/user-api-examples.md`
- `docs/api/troubleshooting.md`
- `docs/api/error-codes.md`
- `docs/api/sdk/javascript-examples.md`
- `docs/api/sdk/curl-examples.md`

## 技術仕様
- マークダウン形式
- 文字エンコーディング: UTF-8
- 日本語メイン、必要に応じて英語併記
- GitHub Flavored Markdown準拠
- コードブロックのシンタックスハイライト対応