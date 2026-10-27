# 검증 결과

검증일: 2026-10-26

1.0.0의 동일한 Java 11용 플러그인 JAR를 각 제품의 설치 플러그인과 함께, 별도 구성·워크스페이스에서 기본 IDE 애플리케이션으로 실행했습니다.

| 제품 | Eclipse 플랫폼 | 실행 Java | 결과 |
|---|---|---|---|
| STS 3.9.18.RELEASE | 4.21.0 | 11.0.32.1 | 29개 통과 |
| Eclipse IDE 2026-06 | 4.40.0 | 21.0.11 | 29개 통과 |
| Spring Tools 5.3.0.RELEASE | 4.40.0 | 21.0.11 | 29개 통과 |

프로젝트 없는 상태의 두 탐색기 표시, UTF-8 저장·재편집, CP949 유지, 저장 취소, 새 메모 Save All, 기본 Untitled 저장 명령, 외부 파일 변경 감시, 기존 Java 프로젝트 공존, 탐색기 닫기·재열기를 확인했습니다. 원시 결과는 이 폴더의 제품별 TXT에 있습니다. 경로·파일 이름·인코딩 검사는 별도로 23개를 통과했습니다.

제품에 포함된 Gradle·Oomph 등의 업데이트 조회는 테스트 환경의 네트워크 제한으로 경고를 기록했습니다. 최종 실행 로그에서 플러그인 오류 및 편집기 초기화·undo context 오류는 발생하지 않았습니다.

1.0.1의 UI 변경은 파일 트리의 `SWT.BORDER`와 바깥 여백 제거입니다. 위 기능 검사 29개는 1.0.0 결과입니다.

## MIT 배포 구성 검증 (2026-10-27)

1.0.1에 MIT 라이선스와 Marketplace용 업데이트 사이트 출력을 추가한 뒤 다음을 확인했습니다. 이 검증에서는 UI 기능 검사 29개를 다시 실행하지 않았습니다.

- Java 11 바이트코드 컴파일과 플러그인 JAR·dropins ZIP 빌드 성공
- 플러그인 JAR, feature JAR, 배포 ZIP, 업데이트 사이트에 MIT 원문 포함
- p2 설치 단위 `dev.eclipsememo.feature.feature.group`의 설치 라이선스가 루트 `LICENSE`와 일치
- Spring Tools 5.3.0의 p2 및 Java 21로 ZIP 설치 목록 조회와 실제 아티팩트 수집 성공
- 수집한 플러그인·feature JAR의 SHA-256이 게시 원본과 일치
- HTTPS 호스팅용 `dist/marketplace/update-site/` 출력 확인

## 공개 업데이트 사이트 검증 (2026-10-27)

업데이트 사이트: https://pinnpublic.github.io/eclipse-memo/

- 공개 사이트 루트 HTTP 200 확인
- 메타데이터, 라이선스, feature·플러그인 파일 6개를 HTTPS로 다운로드하고 게시 원본과 SHA-256 일치 확인
- Eclipse p2 director로 공개 주소에서 `dev.eclipsememo.feature.feature.group=1.0.1` 조회 성공
- Eclipse p2로 공개 주소의 feature·플러그인 JAR 수집 성공 및 SHA-256 일치 확인

이 검증은 공개 p2 저장소의 조회와 파일 수집을 확인한 것입니다. 실제 GUI 설치 후 기능 검증과 Marketplace 등록은 별도 단계입니다.
## STS3 설치 검증 (2026-10-27)

STS 3.9.18.RELEASE / Eclipse 4.21 / Java 11.0.32.1의 별도 복사본에서 공개 업데이트 사이트만 지정해 p2 director 설치를 실행했습니다.

- `dev.eclipsememo.feature.feature.group` 1.0.1 설치 성공, 종료 코드 0
- 추가된 설치 단위는 메모 플러그인과 feature 관련 3개이며, 기존 Eclipse 구성 요소의 교체·삭제는 없음
- 사용자 STS3에서도 `Contact all update sites during install to find required software` 체크 해제 후 설치 가능 확인

다른 업데이트 사이트를 함께 조회하면 구형 p2에서 외부 패키지의 PGP 공개 키 검증 오류가 발생할 수 있어, 해당 오류의 해결 방법을 설치 매뉴얼에 추가했습니다.
