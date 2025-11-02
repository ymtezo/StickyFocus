# StickyFocus — 現状まとめと今後の作業提案（日本語）

作成日: 2025-11-02

## 目的
ロック画面解除時にユーザに「今からするタスク」を入力させ、その入力内容を画面端の半透明オーバーレイに常時表示する Android ネイティブアプリ。可能なら前面アプリ名と時刻も表示し、Super Productivity への連携も検討する。

---

## 現状（実装済み）
1. プロジェクトの最小スキャフォールドを `android/` に作成
   - Gradle 設定、AndroidManifest、主要ソース、レイアウト、README を追加
2. ロック解除検知と入力 UI
   - `UnlockMonitorService`（フォアグラウンドサービス）で `ACTION_USER_PRESENT` を監視し、解除時に `MainActivity` を起動
   - `MainActivity` はロック画面上で表示可能（setShowWhenLocked / setTurnScreenOn）でタスクを入力・保存できる
3. オーバーレイ（半透明・端に表示）
   - `OverlayService` が `WindowManager` を使って画面端に半透明の表示を追加
   - 表示内容はタスク本文と時刻。ドラッグで位置を移動可能
4. 共有機能
   - `MainActivity` に「共有」ボタンを実装（`Intent.ACTION_SEND`） — Super Productivity 等への単純共有が可能
5. 永続化（SharedPreferences → Room へ移行）
   - 最初は SharedPreferences に最新タスクを保存していたが、Room を導入して移行処理を実装
   - 追加したファイル: `TaskEntity.kt`, `TaskDao.kt`, `AppDatabase.kt`
   - `AppDatabase.getInstance()` 実行時に SharedPreferences の値があれば DB に一度だけ移行するロジックを追加
   - `MainActivity` 保存時に Room に insert する処理を追加（バックグラウンドスレッド）
   - `OverlayService` は Room から最新タスクを読み取って表示（失敗時は SharedPreferences にフォールバック）

主な作成ファイル（抜粋）:
- `android/app/src/main/java/com/example/stickyfocus/MainActivity.kt`
- `android/app/src/main/java/com/example/stickyfocus/UnlockMonitorService.kt`
- `android/app/src/main/java/com/example/stickyfocus/OverlayService.kt`
- `android/app/src/main/java/com/example/stickyfocus/BootReceiver.kt`
- `android/app/src/main/java/com/example/stickyfocus/TaskEntity.kt`
- `android/app/src/main/java/com/example/stickyfocus/TaskDao.kt`
- `android/app/src/main/java/com/example/stickyfocus/AppDatabase.kt`
- レイアウト: `activity_main.xml`, `overlay_layout.xml`
- `android/README.md`（簡易実行手順）

---

## DB 設計（Room）
テーブル: `tasks`
- `id: String` (UUID) — 主キー
- `title: String` — タスク本文
- `createdAt: Long` — 作成時刻（ms）
- `updatedAt: Long` — 更新時刻（ms）
- `source: String` — 生成元（例: "unlock_input"）
- `foregroundAppWhenCreated: String?` — 作成時の前面アプリのパッケージ名
- `isCompleted: Boolean`
- `isPinned: Boolean`
- `externalId: String?` — 外部サービスID（同期用）
- `metadata: String?` — 拡張用 JSON

主要 DAO:
- `insert(task: TaskEntity)`
- `update(task: TaskEntity)`
- `getLatestPinnedOrLatest(): TaskEntity?` — オーバーレイで使う優先タスク
- `getAll(limit, offset)`
- `deleteById(taskId)`

備考: 現在の DAO は同期メソッド（スレッドで実行）として実装。将来的には `suspend` 化して Coroutine 対応を推奨。

---

## 実行方法（短縮版）
1. Android Studio で `android` フォルダを開く
2. 実機を接続（エミュレータはオーバーレイ権限周りで扱いづらい場合あり）
3. ビルド & インストール（PowerShell 例）:

```powershell
cd android
.\gradlew assembleDebug
.\gradlew installDebug
```

4. アプリを起動 → 「オーバーレイ許可を開く」ボタンで “他のアプリの上に表示” 許可を与える
5. タスクを入力して保存するとオーバーレイが表示される
6. サービスが起動中ならロック解除で `MainActivity` が表示される

---

## 必要な権限と注意点
- `FOREGROUND_SERVICE`（フォアグラウンドサービス）
- `RECEIVE_BOOT_COMPLETED`（起動時にサービスを再開する場合）
- `SYSTEM_ALERT_WINDOW`（オーバーレイ。ユーザが設定画面で許可）
- 前面アプリ名取得には `PACKAGE_USAGE_STATS`（Usage Access）または `AccessibilityService` の権限が必要（まだ実装していない）

注意:
- オーバーレイや Accessibility 権限はプライバシー上敏感。ユーザへの説明と同意が必要。
- 一部メーカー端末の省電力設定でフォアグラウンドサービスが停止される可能性あり。
- Play ストア公開時は許可の用途説明・プライバシーポリシーが必要。

---

## 今後の作業提案（優先順）
下の番号から選んでください。選んだら実装を進め、ファイル変更を追加します。

1) 前面アプリ名の取得を実装（UsageStatsManager ベース）
   - 簡易かつ比較的実装が容易。ユーザに Usage Access の許可を要求するUIを追加。
   - オーバーレイに現在開いているアプリ名を表示する。

2) AccessibilityService を使った前面アプリ／ウィンドウタイトルの高精度取得
   - より正確だが、Accessibility 権限が敏感で審査やユーザ説明が必要。

3) DAO を Coroutine (`suspend`) ベースに書き換える
   - コード品質・保守性向上。UI 側も Coroutine/LiveData で簡潔に。

4) 履歴一覧 UI の実装（タスク一覧、削除、ピン、編集）
   - Room を活かして履歴管理を提供。Paging3 と組み合わせてもよい。

5) Super Productivity との深い連携（選択肢）
   - 単純共有（現状実装済み）より進める場合:
     - Webhook を提供するサーバ経由でデスクトップの Super Productivity と同期
     - ローカルファイル（クラウドを介した）にタスクを書き出す方式
     - Super Productivity に API があれば直接 POST する方式

6) UX 改善: オーバーレイの透明度スライダー、位置プリセット、自動非表示ルールの追加

7) テスト & Play ストア準備: 権限説明、プライバシーポリシー、端末互換性テスト

---

## 推奨（短期）
- まずは (1) UsageStats ベースの前面アプリ名取得を実装するのが効果が高く実装コストも低めです。これを実装すればオーバーレイが要求どおり「タスク + 現在開いているアプリ名 + 時刻」を表示できます。

---

必要なら、このファイルを `README_JP.md` にリネームしたり、英語版要約を追加します。どの項目から着手するか教えてください（例: `1` を選ぶ）。
