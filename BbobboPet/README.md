# 뽀뽀 키우기 (BBOBBO PET)

기획서 v1.0 을 구현한 Android 프로젝트입니다.
Kotlin + Jetpack Compose + Room 으로 작성했고, **외부 이미지 에셋 없이 실행됩니다.**
캐릭터는 `ui/components/PetCharacter.kt` 에서 Compose Canvas 로 직접 그립니다.

## 실행 방법

1. Android Studio (Koala 이상) 에서 이 폴더를 엽니다. Gradle wrapper 가 포함돼 있어 별도 설치는 필요 없습니다.
2. `local.properties` 에 Android SDK 경로가 없으면 Android Studio 가 자동으로 만들어 줍니다.
3. Run ▶ 으로 실기기/에뮬레이터에 설치합니다. minSdk 26 / targetSdk 35.

터미널에서 바로 빌드하려면:

```bash
./gradlew assembleDebug      # APK 생성
./gradlew testDebugUnitTest  # 단위 테스트 (32개)
./gradlew lintDebug          # 정적 분석
```

## 구현 범위

| 기획서 | 구현 |
|---|---|
| §1-1 스탯 | 5종 자연 감소, 오프라인 12시간 상한 정산, 시간 되돌리기 방어, 수면 중 감소율 50% |
| §1-2 레벨 | 경험치·레벨업·칭호, 레벨업 보너스 코인 |
| §1-3 재화 | 코인·하트, 음수 방지 |
| §1-4 에너지 | 시간당 회복, 부족 팝업, 하트 10개로 즉시 완충 |
| §1-5 미션 | 일일 3종 랜덤(오전 5시 갱신), 진행도 추적, 보상 수령 |
| §1-6 알림 | WorkManager 주기 점검 + 4종 알림 + 종류별 ON/OFF |
| §1-7 저장 | Room 로컬 DB, 행동 시점마다 즉시 커밋, v1→v2 마이그레이션 |
| S-00 스플래시·온보딩 | 스플래시 1.5초, 이름 짓기 → 튜토리얼(코인 500) → 알림 권한 3스텝 |
| S-01 홈 | 레벨 카드, 재화, 스탯 패널, 캐릭터 무대, 말풍선, 미션 카드, 메뉴 타일, 하단 탭바, 복귀/출석 팝업 |
| S-02 밥주기 | 먹이 4종 바텀시트, 무료 사료 일일 제한, 재화 검증, 포만 상한 차단 |
| S-03 놀아주기 | 공놀이 / 숨바꼭질 / 비눗방울 3종 |
| S-04 재우기 | 30분·2시간·8시간 선택, 밤 모드, 카운트다운, 조기 기상 |
| S-05 씻기 | 드래그 문지르기 → 거품 진행도 100% → 보상 |
| S-06 미니게임 | 간식 잡기: 60초, 콤보 ×30, 폭탄 페널티, 3단계 난이도 곡선, 백그라운드 일시정지 |
| S-07 옷장 | 모자·옷·액세서리 탭, 실시간 프리뷰, 레벨 잠금, **세트 슬롯 5개**, **세트 보너스**, 미저장 이탈 확인 |
| S-08 방꾸미기 | 꾸미기/상점 토글, 구매·배치·드래그, **그리드 스냅**, **슬롯 제한**, **되돌리기**, **초기화**, 방 등급 일일 보너스 |
| S-09 산책 | 3분 진행(앱 종료해도 유지), 랜덤 이벤트 5종, 일일 3회 제한 |
| S-10 도감 | 5개 탭, 수집률, 실루엣 처리, 마일스톤 보상 4단계 |
| S-11 출석체크 | 7일 순환 캘린더, 연속 판정, 첫 실행 자동 팝업 |
| S-12 보관함·상점·설정 | 보관함 3탭, 상점 4탭(일일 특가 3종), 설정(사운드·알림·이름·데이터 초기화) |

### 아직 안 만든 것

- 보상형 광고(AdMob) 연동 — 광고 자리는 하트 결제로 대체해 두었습니다 (T-43)
- 사운드/BGM 재생 — 설정 토글과 볼륨 값만 저장하고 있습니다 (T-40)
- Lottie/Rive 캐릭터 에셋 — 현재는 Canvas 렌더러로 대체 (§2)
- 클라우드 세이브, 소셜, 시즌 이벤트 (기획서 3차 범위)

## 구조

```
domain/GameData.kt    밸런싱 테이블 (숫자는 전부 여기서 조정)
domain/Perks.kt       아이템·세트 효과의 수치 정의
domain/StatEngine.kt  감소·레벨업·보상 계산 (순수 함수, 테스트 대상)
data/local/           Room 엔티티 + DAO + 마이그레이션
data/prefs/           DataStore 설정
data/repo/            저장소
notify/               WorkManager 주기 점검 + 알림
ui/common/PetViewModel.kt  전 화면 공유 상태
ui/components/        캐릭터 렌더러 + 공통 UI + 하단 탭바
ui/onboarding, ui/home, ui/actions, ui/minigame, ui/wardrobe, ui/room,
ui/walk, ui/collection, ui/attendance, ui/inventory, ui/shop, ui/settings,
ui/pet, ui/more
```

밸런싱을 바꾸려면 `domain/GameData.kt` 의 `Balance`, `Foods`, `Wardrobe`, `Furnitures`,
`Walks`, `Collections`, `OutfitBonuses`, `RoomLayout` 만 수정하면 됩니다.
아이템 효과 수치는 `domain/Perks.kt` 한 곳에 모여 있습니다.

## 테스트

`app/src/test` 에 32개의 단위 테스트가 있습니다. 기획서 §10 QA 체크리스트 중
순수 계산으로 확인 가능한 항목(오프라인 상한, 시간 치트 방어, 재화 음수 방지,
수면 중 감소율, 퍽 합산, 배치 좌표 범위 등)을 덮습니다.

## 데이터베이스 마이그레이션

`app/schemas/` 에 Room 스키마가 버전별로 저장됩니다.
엔티티를 바꾸면 `AppDatabase` 의 version 을 올리고 `Migration` 을 추가하세요 —
`fallbackToDestructiveMigration` 은 쓰지 않습니다(플레이어 데이터가 날아갑니다).
