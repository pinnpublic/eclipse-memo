# Eclipse Workspace Memo

프로젝트 없이 현재 워크스페이스 폴더에 메모를 저장하고 편집하는 Eclipse 플러그인입니다. Package Explorer와 Project Explorer에 `워크스페이스 파일` 영역을 추가합니다.

## 설치

### 직접 설치

> **설치 주의 (STS3·Eclipse·STS5 공통):** `Contact all update sites during install to find required software` 체크를 **해제한 뒤** 진행하세요.
> 체크하면 다른 사이트의 Eclipse 구성 요소까지 변경될 수 있으며, STS3에서는 `Public key not found` 오류도 발생할 수 있습니다.

1. Eclipse에서 `Help > Install New Software… > Add…`를 선택합니다.
2. Name에 `Eclipse Workspace Memo`, Location에 `https://pinnpublic.github.io/eclipse-memo/`를 입력합니다.
3. `Workspace Tools > Eclipse Workspace Memo`를 선택합니다.
4. 라이선스에 동의한 뒤 설치하고 Eclipse를 재시작합니다.

ZIP 파일로 설치할 때는 [설치용 ZIP](https://github.com/pinnpublic/eclipse-memo/releases/download/v1.0.2/eclipse-workspace-memo-update-site-1.0.2.zip)을 받아 `Add… > Archive…`에서 선택합니다. 압축은 풀지 않습니다.

JAR로 설치하려면 Eclipse를 종료하고 [dev.eclipsememo_1.0.2.jar](https://github.com/pinnpublic/eclipse-memo/releases/download/v1.0.2/dev.eclipsememo_1.0.2.jar)를 **Eclipse 설치 폴더의 `dropins` 폴더**에 복사한 뒤 다시 실행합니다. 설치 방법 중 하나만 사용하세요.

### Eclipse Marketplace 설치

1. `Help > Eclipse Marketplace…`를 엽니다.
2. `Eclipse Workspace Memo`를 검색하고 `Install`을 누릅니다.
3. 라이선스에 동의한 뒤 설치하고 Eclipse를 재시작합니다.

## 사용법

1. Package Explorer 또는 Project Explorer를 엽니다. 보이지 않으면 `Window > Show View > Other…`에서 선택합니다.
2. `워크스페이스 파일` 영역의 **새 메모** 버튼 또는 `File > 새 워크스페이스 메모`를 선택합니다.
3. 내용을 입력하고 `Ctrl+S`를 누른 뒤 `메모.txt`처럼 파일 이름을 입력합니다. 파일은 **현재 워크스페이스의 최상위 폴더**에 저장됩니다.
4. 저장한 파일은 목록에서 더블클릭해 열고, 수정 후 `Ctrl+S`로 저장합니다.

- **새 폴더:** 폴더를 선택하고 **새 폴더** 버튼을 누릅니다. 선택하지 않으면 워크스페이스 최상위에 생성됩니다.
- **이름 변경:** 파일을 선택하고 `F2` 또는 우클릭 → `이름 변경…`을 선택합니다.
- **삭제:** 파일 또는 빈 폴더를 선택하고 `Delete`를 누릅니다. 확인하면 휴지통을 거치지 않고 삭제됩니다.
- **새로고침:** 목록에서 `F5` 또는 **새로고침** 버튼을 누릅니다. 외부 파일 변경도 자동 반영됩니다.

새 파일은 UTF-8로 저장하며, 기존 UTF-8·CP949 파일은 일반 저장 시 인코딩을 유지합니다. 프로젝트 파일은 기존 프로젝트 트리에서 관리합니다.

## 제거

- **Install New Software / Marketplace 설치:** `Help > About > Installation Details > Installed Software`에서 플러그인을 선택하고 `Uninstall…`을 진행합니다.
- **JAR 직접 설치:** Eclipse를 종료하고 `dropins`에 복사한 `dev.eclipsememo_*.jar`를 제거합니다.

제거 후 Eclipse를 재시작합니다. 저장한 메모 파일은 그대로 남습니다.

## 라이선스

[MIT License](LICENSE)
