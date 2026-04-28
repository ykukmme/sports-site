# GOL.GG page-game 파서 스펙 (T-1.2 selector 분석)

작성일: 2026-04-28
관련 plan: `docs/plans/2026-04-27-public-match-detail-tasks.md` (Sprint 1 / T-1.2)
픽스처: `backend/src/test/resources/golgg/page-game/73116-page-game.html` (Dplus KIA vs HANJIN BRION game 2, LCK Cup 2026 WEEK1)
보조 픽스처: `backend/src/test/resources/golgg/page-game/73115-page-game.html` (동일 시리즈 game 1)

## 추출 컨트랙트

`GolGgClient.parseGameStatsHtml(providerGameId, sourceUrl, html)` → `GolGgParsedGameStats` 채움.

| 필드 | DOM 위치 / selector | 73116 기대값 |
|---|---|---|
| durationSec | `<h1>` 안 텍스트 `MM:SS` (col-6 text-center "Game Time" 블록) — `MM*60+SS` | 1530 (25:30) |
| winnerSide | `div.blue-line-header` / `div.red-line-header`의 텍스트 끝이 ` - WIN`이면 그쪽이 승자. 두 쪽 모두 검사해 교차 검증 | BLUE |
| blueKills | `span.score-box.blue_line` 안 `img[alt='Kills']` 의 형제 텍스트 trim | 27 |
| redKills | `span.score-box.red_line` 안 `img[alt='Kills']` | 7 |
| blueDragons / redDragons | 같은 패턴, `img[alt='Dragons']` | 2 / 0 |
| blueBarons / redBarons | 같은 패턴, `img[alt='Nashor']` | 1 / 0 |
| blueBans (List<String>) | "Bans" 라벨 div 다음 `.col-10` 내부 `a > img.champion_icon_medium`의 `alt` 5개 (블루 측 컨테이너) | [Azir, Jayce, Vi, Rakan, Nautilus] |
| redBans (List<String>) | 동일, 레드 측 컨테이너 | [Qiyana, Malphite, Akali, Lucian, Kalista] |
| bluePicks (List<GolGgPickEntry>) | `table.playersInfosLine` 중 헤더가 `blue-line-header`인 첫 테이블의 `<tr>` (thead 제외). 각 행에서 `img.champion_icon` `alt` = championId, `a.link-blanc` 텍스트 = playerName, 행 인덱스 0..4 → TOP/JUNGLE/MID/ADC/SUPPORT | Rumble/Siwoo, Pantheon/Lucid, LeBlanc/ShowMaker, Sivir/Smash, Bard/Career |
| redPicks | 동일, 헤더가 `red-line-header`인 두 번째 테이블 | KSante/Casting, Nocturne/GIDEON, Orianna/Roamer, Jhin/Teddy, Elise/Namgung |

## 핵심 결정

1. **포지션 결정 = 행 순서**: GOL.GG page-game에는 명시적 포지션 라벨이 없다. `playersInfosLine` 테이블 행 순서가 LoL 표준(TOP→JUNGLE→MID→ADC→SUPPORT). T-1.2는 이 컨벤션을 신뢰 — 향후 다른 게임 픽스처로 교차 검증.
2. **픽 single source of truth = playersInfosLine 테이블**: 상단 "Picks" 요약 div는 player name이 없으므로 무시. 항상 player table을 읽고 ban만 상단 섹션에서 가져온다.
3. **championId 정제**: `<img alt='...'>`는 이미 GOL.GG가 "KSante", "LeBlanc"처럼 정규화된 형태로 내보낸다. 그래도 T-1.4 `ChampionIdNormalizer`를 한 번 통과시켜 DDragon ID로 통일 — 이중 안전망.
4. **결측 처리 (Hard Rule #4 fabrication 금지)**: 파싱 실패한 필드는 null/빈 리스트. 보간·기본값 추정 절대 금지. 텍스트가 숫자가 아니면 null.
5. **Winner 교차 검증**: blue/red 라벨이 모두 WIN이거나 모두 LOSS면 inconsistent → winnerSide=null + warning 로그.

## Selector 메모 (Jsoup 기준 의사코드)

```
Document doc = Jsoup.parse(html);

// Game time
Element gameTimeH1 = doc.selectFirst("div.col-6.text-center > h1");
Integer durationSec = parseMmSs(gameTimeH1.text());

// 양 사이드 컨테이너 — 블루는 .blue-line-header를 포함한 .col-12.col-sm-6, 레드는 .red-line-header
Elements sides = doc.select("div.col-12.col-sm-6:has(div.blue-line-header), div.col-12.col-sm-6:has(div.red-line-header)");

for each side:
  String header = side.selectFirst("div.blue-line-header, div.red-line-header").text();
  // " - WIN" / " - LOSS"
  Integer kills = parseInt(side.selectFirst("span.score-box:has(img[alt='Kills'])").ownText());
  Integer towers = parseInt(side.selectFirst("span.score-box:has(img[alt='Towers'])").ownText());
  Integer dragons = parseInt(side.selectFirst("span.score-box:has(img[alt='Dragons'])").ownText());
  Integer barons = parseInt(side.selectFirst("span.score-box:has(img[alt='Nashor'])").ownText());
  // Bans
  Element bansLabel = side.select("div.col-2:matchesOwn(^Bans$)").first();
  Element bansContainer = bansLabel.nextElementSibling(); // .col-10
  List<String> bans = bansContainer.select("a > img.champion_icon_medium").stream()
                                    .map(e -> e.attr("alt")).toList();

// Picks (player table)
Elements playerTables = doc.select("table.playersInfosLine");
// playerTables[0] = blue, playerTables[1] = red (thead의 헤더 클래스로 판별)
for each tableSide:
  rows = table.select("tbody > tr") // thead 제외
  for index, row in rows[0..4]:
    String champ = row.selectFirst("img.champion_icon").attr("alt");
    String player = row.selectFirst("a.link-blanc").text();
    String position = ROLE_ORDER[index]; // TOP, JUNGLE, MID, ADC, SUPPORT
```

## 단위 테스트 케이스 (T-1.2)

`GolGgGameStatsParserTest`:
1. **정상 BO3 게임 1개 (73116 fixture)**: 위 표의 모든 기대값 검증 — duration, winner, kills/dragons/barons, ban 5개씩, pick 5개씩 + player + position.
2. **결측 필드 (red dragons=0)**: 73116에서 red dragons=0 케이스가 이미 있어 0 정수 반환 검증.
3. **두 번째 픽스처 (73115)**: smoke 테스트 — winner/kills 정도만 확인해 selector가 한 게임에 과적합되지 않았는지 검증.
4. **깨진 HTML**: 빈 문자열 입력 시 `GolGgParsedGameStats(providerGameId, sourceUrl, null, null, null,..., List.of(), List.of(), List.of(), List.of())` 반환. 예외 던지지 않음.

## 73116 정답 시트 (테스트 assertion 참조용)

```
durationSec     = 1530
winnerSide      = BLUE
blueKills       = 27   redKills       = 7
blueTowers      = 8    redTowers      = 0   (현 record 미포함, 향후 확장 시 참고)
blueDragons     = 2    redDragons     = 0
blueBarons      = 1    redBarons      = 0
blueBans        = [Azir, Jayce, Vi, Rakan, Nautilus]
redBans         = [Qiyana, Malphite, Akali, Lucian, Kalista]
bluePicks       = [
  (Rumble,    Siwoo,     TOP),
  (Pantheon,  Lucid,     JUNGLE),
  (LeBlanc,   ShowMaker, MID),
  (Sivir,     Smash,     ADC),
  (Bard,      Career,    SUPPORT)
]
redPicks        = [
  (KSante,    Casting,   TOP),
  (Nocturne,  GIDEON,    JUNGLE),
  (Orianna,   Roamer,    MID),
  (Jhin,      Teddy,     ADC),
  (Elise,     Namgung,   SUPPORT)
]
```
