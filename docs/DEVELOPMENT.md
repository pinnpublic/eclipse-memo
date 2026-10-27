# 개발과 빌드

Maven이나 네트워크 다운로드 없이 설치된 Eclipse의 플러그인 JAR와 JDK를 사용합니다. 저장소는 PDE 프로젝트로 가져올 수도 있습니다.

Windows PowerShell과 JDK 11 이상이 필요합니다. `JAVA_HOME`은 JDK 설치 폴더로, `PATH`는 해당 JDK의 `bin`을 포함하도록 설정하세요. 아래 Eclipse 경로는 자신의 설치 위치로 바꾸세요.

```powershell
$eclipseHome = 'C:\path\to\eclipse'
$jdkHome = $env:JAVA_HOME

powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 `
  -EclipseHome $eclipseHome `
  -JdkHome $jdkHome

powershell -NoProfile -ExecutionPolicy Bypass -File .\test-files.ps1

powershell -NoProfile -ExecutionPolicy Bypass -File .\build-update-site.ps1 `
  -EclipseHome $eclipseHome `
  -JdkHome $jdkHome

powershell -NoProfile -ExecutionPolicy Bypass -File .\test-runtime.ps1 `
  -EclipseHome $eclipseHome `
  -JdkHome $jdkHome `
  -Label eclipse
```

Eclipse 본체를 실행하는 `build-update-site.ps1`과 `test-runtime.ps1`에는 해당 Eclipse 제품이 지원하는 JDK를 사용하세요.

런타임 검사는 `.build/runtime-제품명-시간/` 안에 별도 구성과 워크스페이스를 만들고 실제 Eclipse Workbench를 실행합니다. 파일 저장, 재편집, 취소, Save All, 기본 Untitled 저장, 두 뷰 연동, 기존 Java 프로젝트 공존, 뷰 재열기를 확인합니다. 결과는 해당 폴더의 `results.txt`에 기록합니다. `-KeepOpen`은 검사 후 테스트 창을 유지합니다.

프로젝트가 네트워크 드라이브에 있다면 `-RuntimeBase 'C:\원하는\로컬\테스트폴더'`를 지정해 런타임 구성과 테스트 워크스페이스를 로컬 디스크에 둘 수 있습니다. 검사는 기본 IDE 애플리케이션(`org.eclipse.ui.ide.workbench`)을 실행한 뒤 테스트 전용 시작 확장으로 수행합니다.

빌드 산출물과 임시 테스트 결과는 `dist/`와 `.build/`에 생성되며 Git에서 제외합니다. `verification/`의 검증 요약과 제품별 결과는 저장소에 포함합니다. `.project`, `.classpath`, `.settings/`의 공통 컴파일·인코딩 설정도 PDE 프로젝트 가져오기에 필요하므로 유지합니다.

`build-update-site.ps1`은 오프라인 설치용 ZIP과 함께 웹에 게시할 `dist/update-site/` 폴더를 생성합니다. `-OutputDirectory`를 지정하면 ZIP과 `update-site/`를 다른 폴더에 생성할 수 있습니다. 빌드 작업용 파일은 `.build/`에 생성됩니다.

## 배포 파일 한 번에 만들기

저장소 루트에서 다음 명령을 실행합니다. `EclipseHome`에는 p2 게시 기능이 포함된 Eclipse 또는 STS를, `JdkHome`에는 해당 제품이 지원하는 JDK를 지정합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-release.ps1 `
  -EclipseHome 'C:\path\to\eclipse' `
  -JdkHome $env:JAVA_HOME
```

이 명령은 플러그인 빌드, p2 설치 목록 조회, 실제 아티팩트 수집 및 해시 검증을 마친 뒤 GitHub Releases에 올릴 파일을 `dist/release/1.0.1/`에 모읍니다. 이전에 개별 빌드로 생성한 `dist/` 루트의 ZIP 대신 이 폴더의 파일을 배포하세요.

| 파일 | 용도 |
| --- | --- |
| `dev.eclipsememo_1.0.1.jar` | dropins 설치 |
| `dev.eclipsememo_1.0.1.jar.sha256` | JAR 해시 |
| `eclipse-workspace-memo-1.0.1.zip` | 매뉴얼·MIT 원문을 포함한 dropins 배포 |
| `eclipse-workspace-memo-update-site-1.0.1.zip` | 오프라인 p2 설치 및 Pages 게시 |
| `SHA256SUMS` | 위 파일들의 SHA-256 목록 |

호스팅할 p2 폴더는 `dist/marketplace/update-site/`에 남습니다. 소스 파일만 Git에 커밋하고 `dist/`의 배포 파일은 GitHub Releases의 첨부 파일로 올립니다. 공개 URL 게시와 Marketplace 등록 순서는 [Marketplace 배포 문서](MARKETPLACE.md)에 있습니다.

## 새 버전 준비

`META-INF/MANIFEST.MF`, `feature/feature.xml`, `feature/category.xml`과 빌드·테스트 스크립트의 버전 및 파일 이름을 함께 변경하고 문서의 배포 버전도 갱신합니다. 이미 공개한 버전의 바이너리를 바꿀 때는 새 버전을 사용하세요.

검증 기록은 [verification/README.md](../verification/README.md)에 있습니다. 현재 UI 런타임 검증 기록은 1.0.0 기준이며, 1.0.1의 MIT 배포 검증과 구분해 기록합니다.

[사용 매뉴얼로 돌아가기](../README.md)
