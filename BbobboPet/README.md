# 뽀뽀 키우기 (BBOBBO PET)

기획서 v1.0 의 MVP 범위를 구현한 Android 프로젝트입니다.
Kotlin + Jetpack Compose + Room 으로 작성했고, **외부 이미지 에셋 없이 실행됩니다.**
캐릭터는 `ui/components/PetCharacter.kt` 에서 Compose Canvas로 직접 그립니다.

## 실행 방법

1. Android Studio (Koala 이상) 에서 이 폴더를 엽니다.
2. Gradle Sync 시 wrapper JAR 이 없다는 경고가 뜨면, 터미널에서 `gradle wrapper` 를 한 번 실행하거나
   Android Studio 의 안내에 따라 wrapper 를 생성하세요. (`gradle-wrapper.properties` 는 포함되어 있습니다.)
3. Run ▶ 으로 실기기/에뮬레이터에 설치합니다. minSdk 26.

## 구현된 범위

| 기획서 | 구현 |
|---|---|
| §1 공통 시스템 | 스탯 5종 자연 감소, 오프라인 12시간 상한 정산, 시간 되돌리기 방어, 레벨/경험치, 코인·하트, 에너지 자동 회복 |
| S-01 홈 | 레벨 카드, 재화, 스탯 패널, 캐릭터 무대, 말풍선 로테이션, 미션 카드, 메뉴 타일, 복귀/출석 팝업, 토스트 |
| S-02 밥주기 | 먹이 4종 바텀시트, 무료 사료 일일 제한, 재화 검증, 포만 상한 차단, eating 모션 |
| S-03 놀아주기 | 공놀이 / 숨바꼭질 / 비눗방울 3종 |
| S-04 재우기 | 30분·2시간·8시간 선택, 밤 모드, 카운트다운, 조기 기상 |
| S-05 씻기 | 드래그 문지르기 → 거품 진행도 100% → 보상 |
| S-06 미니게임 | 간식 잡기: 60초, 콤보 ×30, 폭탄 페널티, 3단계 난이도 곡선, 결과/보상 |
| S-07 옷장 | 모자·옷·액세서리 탭, 실시간 프리뷰, 레벨 잠금, 구매 후 착용 |
| S-08 방꾸미기 | 꾸미기/상점 토글, 카테고리 탭, 구매·배치·드래그 이동·삭제, 방 등급(cozy) |
| S-11 출석체크 | 7일 순환, 연속 판정, 첫 실행 자동 팝업 |
| 미션 | 일일 3종 랜덤(오전 5시 갱신), 진행도 추적, 보상 수령 |

## 아직 안 만든 것 (기획서 Phase 6~7)

- 도감, 산책 이벤트 테이블 (홈의 산책 버튼은 임시로 놀아주기 로직 사용)
- 푸시 알림 (WorkManager), 사운드/BGM, 보상형 광고, 온보딩·튜토리얼
- 세트 코디 저장 슬롯, 아이템 퍽(perk) 실제 적용 — 현재는 데이터에 정의만 되어 있음

## 구조

```
domain/GameData.kt    밸런싱 테이블 (숫자는 전부 여기서 조정)
domain/StatEngine.kt  감소·레벨업·보상 계산 (순수 함수, 테스트 용이)
data/local/           Room 엔티티 + DAO
data/repo/            저장소
ui/common/PetViewModel.kt  전 화면 공유 상태
ui/components/        캐릭터 렌더러 + 공통 UI
ui/home, ui/actions, ui/minigame, ui/wardrobe, ui/room
```

밸런싱을 바꾸려면 `domain/GameData.kt` 의 `Balance`, `Foods`, `Wardrobe`, `Furnitures` 만 수정하면 됩니다.
