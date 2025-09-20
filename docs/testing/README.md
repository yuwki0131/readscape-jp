# テストドキュメント

このディレクトリには、Readscape-JPプロジェクトのテスト関連ドキュメントとスクリプトが整理されています。

## ディレクトリ構造

```
docs/testing/
├── README.md                    # このファイル
├── testing-strategy.md          # テスト戦略・方針
├── test-specification.md        # テスト仕様書（97のテストケース）
├── docker-compose.test.yml      # テスト環境用Docker設定
├── scripts/                     # テストスクリプト
│   ├── api-test-script.sh       # メインAPIテストスクリプト
│   ├── expand-api-test.sh       # 拡張APIテスト（97ケース対応）
│   ├── comprehensive-api-test.sh # 包括的APIテスト
│   ├── final-api-test.sh        # 最終APIテスト
│   ├── simple-api-test.sh       # シンプルAPIテスト
│   ├── working-api-test.sh      # 動作確認用テスト
│   ├── debug-test.sh           # デバッグ用テスト
│   ├── final-comprehensive-test.sh # 最終包括テスト
│   └── full-api-test.sh        # フルAPIテスト
└── results/                     # テスト結果・レポート
    ├── API_TEST_RESULTS.md      # APIテスト結果
    ├── FINAL_API_TEST_RESULTS.md # 最終APIテスト結果
    ├── TEST_FAILURE_ANALYSIS.md  # テスト失敗分析
    ├── TEST_IMPLEMENTATION_ISSUES_REPORT.md # 実装問題レポート
    └── test_results.txt         # テスト結果ログ
```

## 主要ドキュメント

### テスト戦略（testing-strategy.md）
- プロジェクト全体のテスト方針
- テストの種類と範囲
- テスト環境の設定方法

### テスト仕様書（test-specification.md）
- 97の詳細なテストケース
- 各APIエンドポイントのテスト内容
- 期待される結果と検証方法

## テストスクリプト

### 主要スクリプト
- **expand-api-test.sh**: 最も包括的なテストスクリプト（97ケース対応）
- **api-test-script.sh**: 基本的なAPIテストスクリプト
- **comprehensive-api-test.sh**: 包括的なテスト実行

### 特殊用途スクリプト
- **debug-test.sh**: デバッグ・問題調査用
- **simple-api-test.sh**: 基本動作確認用
- **working-api-test.sh**: 動作確認用

## テスト実行方法

1. **テスト環境の起動**
   ```bash
   # プロジェクトルートから
   docker-compose -f docs/testing/docker-compose.test.yml up -d
   ```

2. **APIテストの実行**
   ```bash
   # 包括的なテスト実行
   cd docs/testing/scripts
   ./expand-api-test.sh

   # 基本テスト実行
   ./api-test-script.sh
   ```

3. **テスト結果の確認**
   - テスト結果は `results/` ディレクトリに保存されます
   - 失敗したテストの詳細は各レポートファイルを参照してください

## 注意事項

- テスト実行前にアプリケーションが起動していることを確認してください
- テストスクリプトは `http://localhost:8080` をベースURLとして設定されています
- 環境に応じてスクリプト内のBASE_URLを変更してください