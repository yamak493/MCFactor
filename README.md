# MCFactor - Minecraft Two Factor Authentication Plugin

MCFactorは、Minecraftサーバーに2段階認証機能を追加するプラグインです。専用のFlutterアプリケーションと連携して、サーバーのセキュリティを強化します。

## 特徴

- **2段階認証**: プレイヤーのアカウントをスマートフォンアプリで保護
- **複数のプラットフォーム対応**: Bukkit、Spigot、Paper、Folia、BungeeCord、Velocity対応
- **2つの認証モード**:
  - **コード認証**: プレイヤーがサーバー参加時にコードを入力
  - **アプリ認証**: アプリから自動的に認証をリクエスト
- **多言語対応**: 11言語に対応（英語、日本語、中国語、スペイン語、アラビア語、フランス語、ロシア語、ポルトガル語、インドネシア語、ドイツ語、ヒンディー語）
- **セキュアな暗号化**: SHA3-256ハッシュアルゴリズムを使用
- **タイムベースド**: 3分ごとに更新されるトークン

## インストール

1. プラグインJARファイルをサーバーの`plugins`フォルダに配置
2. サーバーを起動（初回起動時に自動的にServer IDとSecret Keyが発行されます）
3. 必要に応じて`plugins/MCFactor/config.yml`を編集
4. サーバーを再起動

## 設定

`config.yml`の主な設定項目:

```yaml
# プラグインの有効/無効
enabled: true

# 言語設定 (en, ja, zh, es, ar, fr, ru, pt, id, de, hi)
language: en

# コード認証モード（サーバー参加時にコード入力）
code-authentication: true

# アプリ認証モード（アプリからREST APIで認証）
app-authentication: false

# REST API設定（アプリ認証使用時）
rest-api:
  host: 0.0.0.0
  port: 25566

# Server ID（自動生成、変更禁止）
server-id: ""

# Secret Key（自動生成、変更禁止）
secret-key: ""

# Config Version
config-version: 1
```

## コマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/2fa <appkey>` | アプリキーを使用してアカウントをリンク | なし |
| `/2fa` | アカウントのリンクを解除 | なし |
| `/2fa help` | ヘルプを表示 | なし |
| `/2fa disconnect <player>` | プレイヤーのリンクを強制解除 | `mcfactor.admin` |

## 使い方

### プレイヤー側

1. スマートフォンにMCFactorアプリをインストール
2. アプリで新しいサーバーを追加（6桁のアプリキーが生成されます）
3. Minecraftサーバーで`/2fa <アプリキー>`を実行
4. アプリに表示されるトークンをサーバーのチャットに入力
5. 登録完了！

### 管理者側

#### コード認証モードの場合
- プレイヤーはサーバー参加時にアプリに表示されるコードを入力する必要があります
- プレイヤーは一時的に動きが制限され、コード入力後に解放されます

#### アプリ認証モードの場合
- プレイヤーがサーバーに参加する前に、アプリで「認証する」ボタンをタップ
- アプリがサーバーのREST APIにリクエストを送信
- 認証が成功すると、5分間サーバーに参加可能

## セキュリティ

- すべての認証トークンはSHA3-256でハッシュ化
- タイムスタンプベースで3分ごとにトークンが更新
- REST APIはレート制限付き（10秒に1回まで）
- 各プレイヤーのApp KeyとPass Keyは暗号化されて保存

## 対応プラットフォーム

- Bukkit 1.13+
- Spigot 1.13+
- Paper 1.13+
- Folia（非同期対応）
- BungeeCord
- Velocity（予定）
- Sponge（予定）
- Fabric（予定）
- Forge（予定）

## ビルド

```bash
mvn clean package
```

ビルドされたJARファイルは`target/MCFactor-1.0-SNAPSHOT.jar`に出力されます。

## 技術仕様

- **Java**: 8+
- **ハッシュアルゴリズム**: SHA3-256
- **トークン長**: 6文字
- **トークン有効期間**: 3分
- **認証有効期間**: 5分（アプリ認証モード）

## バックエンドAPI

このプラグインは以下のバックエンドAPIエンドポイントを使用します:

- `POST /register.php` - サーバー登録
- `POST /update.php` - サーバー情報更新
- `POST /info.php` - サーバー情報取得
- `POST /temp.php` - 一時登録

## ライセンス

このプロジェクトは非公開です。

## サポート

問題が発生した場合は、GitHub Issuesまたは公式サポートチャンネルでご報告ください。

## 開発者

Enabify - https://enabify.net

---

**注意**: このプラグインを使用するには、MCFactorバックエンドサーバーとFlutterアプリケーションが必要です。
