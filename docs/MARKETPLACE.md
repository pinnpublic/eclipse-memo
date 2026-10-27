# Eclipse Marketplace 등록 안내

이 저장소는 Eclipse Marketplace에 등록할 플러그인의 소스와 사용 매뉴얼을 보관합니다. 공개 업데이트 사이트를 제공하며, Marketplace 등록은 아직 완료하지 않았습니다.

| 항목 | 현재 내용 |
| --- | --- |
| 소스·문서 | https://github.com/pinnpublic/eclipse-memo |
| 지원·문제 제보 | https://github.com/pinnpublic/eclipse-memo/issues |
| 라이선스 | [MIT License](../LICENSE) |
| 저작권자 | pinn.public |
| 업데이트 사이트 URL | https://pinnpublic.github.io/eclipse-memo/ |

## 등록할 때 준비할 것

### 1. 배포 파일 생성

[개발·빌드 안내](DEVELOPMENT.md)에 따라 `build-release.ps1`을 실행합니다. 설치용 JAR, ZIP, 체크섬은 `dist/release/1.0.1/`에, 호스팅할 p2 업데이트 사이트는 `dist/marketplace/update-site/`에 생성됩니다.

배포 파일은 Git 소스에 포함하지 않습니다. 배포 시점에 필요하면 [GitHub Releases](https://github.com/pinnpublic/eclipse-memo/releases) 등으로 제공할 수 있습니다.

### 2. 업데이트 사이트 게시와 설치 확인

업데이트 사이트는 `gh-pages` 브랜치의 p2 파일을 GitHub Pages로 게시합니다. 등록 폼의 **Update Site URL**에는 `https://pinnpublic.github.io/eclipse-memo/`를 입력합니다. 별도의 사용자 워크플로 파일은 사용하지 않습니다.

공개 주소를 Eclipse의 `Help > Install New Software… > Add…`에 입력해 설치하고, MIT 라이선스 표시와 재시작 후 동작을 확인합니다. 소스 저장소 주소나 ZIP 다운로드 주소를 업데이트 사이트 URL로 넣지 않습니다.

공개 서버 설치 확인은 로컬 ZIP의 p2 검증과 별도로 수행합니다. 현재 검증 범위는 [검증 기록](../verification/README.md)에 있습니다.

### 3. Marketplace 등록

[Eclipse Marketplace](https://marketplace.eclipse.org/)에서 로그인해 등록을 진행합니다. 실제 배포 버전에 맞춰 다음 정보를 준비합니다.

| 항목 | 준비할 내용 |
| --- | --- |
| 이름 | Eclipse Workspace Memo |
| 버전 | 등록할 배포 버전 — 현재 소스는 1.0.1 |
| 라이선스 | MIT |
| Feature ID | `dev.eclipsememo.feature` |
| p2 설치 단위 ID | `dev.eclipsememo.feature.feature.group` |
| 소스·문서 주소 | https://github.com/pinnpublic/eclipse-memo |
| 지원 주소 | https://github.com/pinnpublic/eclipse-memo/issues |
| 업데이트 사이트 | https://pinnpublic.github.io/eclipse-memo/ |
| 지원 환경 | 실제 검증한 Eclipse·STS·Java 버전 |
| 소개·스크린샷 | 기능 소개와 실제 탐색기 사용 화면 |

영문 소개 예시:

> Create and edit text notes directly in your Eclipse workspace without creating a project. Workspace Memo adds a shared file area to Package Explorer and Project Explorer, with UTF-8 and CP949 text support, file and folder management, and automatic file list refresh.

등록이 완료되면 [사용 매뉴얼](../README.md)에 실제 Marketplace 페이지, 업데이트 사이트 주소, 설치 방법을 추가합니다.

Marketplace의 업데이트 주소와 feature 정보는 [공식 Marketplace API 문서](https://webdev.eclipse.org/docs/api/marketplace-rest-api)에 설명되어 있습니다. 설치 화면의 MIT 문구는 `feature/feature.xml`에 있으며 [Eclipse Feature Manifest 형식](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/feature_manifest.html)을 따릅니다.

[사용 매뉴얼로 돌아가기](../README.md)
