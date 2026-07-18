# Genesis Observer Camera v0.1.1

Minecraft Java Edition 1.21.1 / Fabric用の、自動巡回カメラだけを実装した最小版です。

## 操作

- `O`：自動巡回の開始・停止
- `P`：次の観測地点へ移動

開始するとHUDが消え、観測地点の周囲を旋回します。約40秒ごとに別の地点へ移動します。

## 前提

- Java 21
- Minecraft Java Edition 1.21.1
- チートを許可したシングルプレイワールド
- Apple Silicon Mac（arm64）でも使用可能

## Macで起動

ZIPを解凍後、Finderでフォルダを開きます。
フォルダをターミナルへドラッグすると、正確なパスを入力できます。

```bash
cd 「ここへ解凍したフォルダをドラッグ」
chmod +x gradlew
./gradlew runClient
```

初回はGradle、Minecraft、Fabric関連ファイルをダウンロードします。

## テスト

1. 開発用Minecraftが起動したら、Minecraft 1.21.1のシングルプレイワールドを作成
2. 「チートの許可」をオン
3. ワールド内で `O` を押す
4. 停止も `O`、次の地点は `P`

コマンド表示が邪魔なら、ワールド内で次を実行します。

```mcfunction
/gamerule sendCommandFeedback false
```

## JAR作成

```bash
./gradlew build
```

完成場所:

```text
build/libs/genesis-observer-0.1.0.jar
```

Windows側では、Minecraft 1.21.1・Fabric Loader・Fabric APIを導入し、JARを`mods`フォルダへ入れます。

## v0.1の範囲

この版は「自動巡回カメラだけ」です。

- 村の自動検索：まだなし
- NPC追跡：まだなし
- 文明システム：まだなし
- 配信向けカメラ切替：次段階

最初に`./gradlew runClient`でエラーが出た場合は、ターミナルの赤い部分を最初から最後まで共有してください。


## v0.1.1 修正内容

未読み込みチャンクで地表高度が正しく取得できず、カメラが地中を旋回する問題を修正しました。
カメラ高度は最低でもY=110になるよう安全制限を追加しています。
