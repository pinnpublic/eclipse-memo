# Eclipse Marketplace 배포

이 프로젝트는 MIT 라이선스로 배포합니다. 저작권자는 `pinn.public`이며, 원문은 [LICENSE](../LICENSE)에 있습니다. 설치 화면의 MIT 문구는 `feature/feature.xml`에 포함되어 있습니다.

배포 대상 GitHub 저장소는 [pinnpublic/eclipse-memo](https://github.com/pinnpublic/eclipse-memo)입니다. MIT 저작권 표기는 `pinn.public`을 사용합니다.

| 용도 | 주소 |
| --- | --- |
| 소스·문서 | https://github.com/pinnpublic/eclipse-memo |
| Git Clone | `https://github.com/pinnpublic/eclipse-memo.git` |
| 릴리스 | https://github.com/pinnpublic/eclipse-memo/releases |
| 지원·문제 제보 | https://github.com/pinnpublic/eclipse-memo/issues |
| 업데이트 사이트 예정 주소 | `https://pinnpublic.github.io/eclipse-memo/` |

업데이트 사이트 주소는 사용자 지정 도메인 없이 이 저장소의 Pages 루트에 게시할 경우의 기본 주소입니다. 실제 배포와 원격 설치 검증은 아직 수행하지 않았습니다. 배포 후에는 Actions의 `github-pages` 환경에 표시된 실제 주소를 확인합니다. [GitHub Pages 기본 주소 규칙](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages)

## 1. 소스 저장소 준비

이 폴더의 내용을 `pinnpublic/eclipse-memo` 저장소의 **루트**에 올립니다. `README.md`, `LICENSE`, `src/`, `docs/`, `scripts/`, `.github/`가 같은 루트에 있어야 합니다. Eclipse 프로젝트 설정인 `.project`, `.classpath`, `.settings/`의 공통 설정도 포함합니다. `.build/`, `bin/`, `dist/`는 `.gitignore`에 따라 제외합니다.

현재 폴더가 다른 상위 Git 저장소 안에 있다면, 이 폴더 내용만 별도 위치로 복사해 독립 저장소로 만들 수 있습니다. 상위 저장소 전체를 플러그인 저장소로 올리지 않도록 저장소 루트를 확인하세요.

## 2. GitHub Release 준비

[개발·빌드 안내](DEVELOPMENT.md)에 따라 `build-release.ps1`를 실행합니다. 완성된 `dist/release/1.0.1/`에는 배포용 JAR, ZIP 두 종류, 해시 파일이 모입니다. p2 게시·목록 조회·실제 파일 수집 검증을 통과한 업데이트 사이트 ZIP만 이 폴더에 복사됩니다.

[GitHub Releases](https://github.com/pinnpublic/eclipse-memo/releases)에서 `v1.0.1` 태그로 릴리스를 만들고 **이 폴더 안의 파일 다섯 개 모두**를 첨부합니다. GitHub가 자동으로 제공하는 Source code ZIP은 설치용 업데이트 사이트 ZIP과 다릅니다.

## 3. 업데이트 사이트를 GitHub Pages에 게시

GitHub Pages 배포에는 **Publish Eclipse update site** 워크플로를 사용합니다. 먼저 로컬의 `.github/workflows/publish-update-site.yml` 파일을 저장소에 등록합니다. 커밋이나 릴리스 생성만으로 자동 배포하지 않으며, Actions에서 수동 실행합니다.

1. 저장소의 `Settings > Pages > Build and deployment > Source`에서 **GitHub Actions**를 선택합니다.
2. 기본 브랜치에 워크플로가 포함되어 있는지 확인합니다.
3. `Actions > Publish Eclipse update site > Run workflow`를 엽니다.
4. 기본 브랜치를 선택하고 `release_tag`에 `v1.0.1`을 입력해 실행합니다.
5. 실행이 끝나면 `github-pages` 배포 환경에 표시된 **실제 공개 URL**을 확인합니다.

워크플로는 실행한 저장소의 릴리스에서 업데이트 사이트 ZIP과 `SHA256SUMS`를 받아 해시와 p2 파일 구성을 확인한 뒤 ZIP 내부의 업데이트 사이트를 Pages 루트에 게시합니다. 저장소 소유자나 이름을 소스에 직접 입력할 필요가 없습니다. 별도의 개인 액세스 토큰을 추가하지 않고 GitHub의 기본 `GITHUB_TOKEN`을 사용합니다.

직접 호스팅하려면 `dist/marketplace/update-site/`의 내용 전체를 HTTPS 서버의 업데이트 사이트 루트에 게시해도 됩니다. 이 경우 Pages 워크플로를 실행할 필요가 없습니다.

게시 폴더에는 다음 파일과 디렉터리가 포함됩니다. p2 게시기가 추가로 만든 파일도 함께 게시합니다.

```text
update-site/
├─ content.jar
├─ artifacts.jar
├─ category.xml
├─ LICENSE
├─ features/
│  └─ dev.eclipsememo.feature_1.0.1.jar
└─ plugins/
   └─ dev.eclipsememo_1.0.1.jar
```

GitHub Pages 사용자 지정 워크플로의 설정과 권한은 [GitHub 공식 문서](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)를 따릅니다.

## 4. 공개 주소로 설치 확인

Eclipse의 `Help > Install New Software… > Add…`에서 공개 업데이트 사이트 URL을 입력합니다. GitHub 소스 저장소 주소나 ZIP 다운로드 주소 대신 **Pages 배포 환경에 표시된 URL**을 사용합니다. 루트에 안내용 `index.html`은 없으므로 브라우저 첫 화면의 표시만으로 p2 사이트 동작을 판단하지 않습니다.

`Workspace Tools > Eclipse Workspace Memo`를 선택해 설치하고, 설치 과정의 MIT 라이선스 표시와 재시작 후 동작을 확인합니다. 프로젝트가 없는 상태와 기존 프로젝트가 있는 상태에서 Package Explorer 및 Project Explorer를 확인한 뒤, 공개할 스크린샷을 준비합니다.

공개 서버 설치는 로컬 ZIP의 p2 검사와 별도로 수행합니다. 로컬 검증 기록은 [verification/README.md](../verification/README.md)에 있습니다.

## 5. Marketplace 등록 정보

[Eclipse Marketplace](https://marketplace.eclipse.org/)에서 로그인해 플러그인 등록을 진행합니다. 공개 업데이트 사이트 설치를 확인한 뒤 아래 정보를 사용합니다.

| 항목 | 등록할 값 |
| --- | --- |
| 이름 | Eclipse Workspace Memo |
| 버전 | 1.0.1 |
| 라이선스 | MIT |
| Feature ID | `dev.eclipsememo.feature` |
| p2 설치 단위 ID | `dev.eclipsememo.feature.feature.group` |
| 소스·문서 주소 | https://github.com/pinnpublic/eclipse-memo |
| 지원 주소 | https://github.com/pinnpublic/eclipse-memo/issues |
| 업데이트 사이트 | `https://pinnpublic.github.io/eclipse-memo/` — Pages 배포 및 원격 설치 확인 후 등록 |
| 지원 환경 | 사용 매뉴얼 및 검증 기록에 있는 실제 검증 환경 |
| 스크린샷 | 프로젝트가 없는 화면과 기존 프로젝트와 함께 사용하는 화면 |

영문 소개:

> Create and edit text notes directly in your Eclipse workspace without creating a project. Workspace Memo adds a shared file area to Package Explorer and Project Explorer, with UTF-8 and CP949 text support, file and folder management, and automatic file list refresh.

Marketplace의 업데이트 주소와 feature 정보 구조는 [공식 Marketplace API 문서](https://webdev.eclipse.org/docs/api/marketplace-rest-api)에 설명되어 있습니다. 설치 라이선스는 [Eclipse Feature Manifest 형식](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/misc/feature_manifest.html)을 따릅니다.

## 6. 등록 이후

등록을 마치면 [README.md](../README.md)에 실제 Marketplace 페이지, GitHub Releases, 공개 업데이트 사이트 주소와 Marketplace 설치 방법을 추가합니다. 이후 버전은 새 태그와 배포 파일로 릴리스를 만들고 같은 Pages 워크플로를 실행합니다. 이미 배포한 버전의 파일을 수정해서 덮어쓰는 대신 새 버전을 사용합니다.

로컬 패키지 검증, 원격 Pages 배포, 원격 설치 확인, Marketplace 등록은 서로 다른 단계입니다. 현재 완료한 단계는 [검증 기록](../verification/README.md)에 명시합니다.

[사용 매뉴얼로 돌아가기](../README.md)
