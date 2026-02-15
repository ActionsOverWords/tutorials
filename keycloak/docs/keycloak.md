# Keycloak 학습 가이드

이 문서는 특정 프로젝트 설정과 무관하게, Keycloak을 이해하고 설계/운영할 때 필요한 핵심을 한 번에 정리한 문서다.

## 1. Keycloak이 해결하는 문제
Keycloak은 인증(Authentication)과 인가(Authorization) 기능을 애플리케이션에서 분리해 중앙에서 관리하게 해주는 IAM(Identity and Access Management) 서버다.

- 로그인 화면, 세션, 토큰 발급/검증 규칙을 표준 기반으로 통합
- 여러 서비스에서 SSO(Single Sign-On) 제공
- 사용자/역할/권한/로그인 정책을 중앙화
- 외부 IdP(예: Google, GitHub, 기업 SSO) 연동 지원

## 2. Keycloak을 쓸 때와 안 쓸 때
쓸 때:
- 서비스가 2개 이상이고 로그인 체계를 공통화해야 할 때
- 역할 기반 접근제어(RBAC), SSO, 외부 IdP 연동이 필요할 때
- OAuth 2.0 / OIDC 표준 준수가 필요한 B2B/B2C 시스템

안 맞을 수 있는 때:
- 단일 내부 도구 + 단순 계정 인증만 필요한 경우
- 운영 인프라(백업, 모니터링, 업그레이드)를 감당하기 어려운 경우

## 3. 핵심 용어
Keycloak을 이해할 때 가장 중요한 객체는 아래 순서다.

- Realm
  - 사용자, 클라이언트, 역할, 인증 정책이 분리되는 최상위 보안 경계
  - 보통 회사/서비스/테넌트 단위로 분리
- Client
  - Keycloak과 연동하는 애플리케이션(웹 앱, 모바일 앱, API 등)
  - `client_id`로 식별
  - Public Client: 비밀키를 안전하게 보관할 수 없는 앱(브라우저/모바일)
  - Confidential Client: 서버 애플리케이션처럼 비밀키 보관 가능한 앱
- User / Group
  - User는 로그인 주체
  - Group은 조직 구조/권한 묶음 표현에 유리
- Role
  - Realm Role: Realm 전역 권한
  - Client Role: 특정 Client 전용 권한
- Scope / Claim
  - Scope: 토큰에 어떤 권한/정보를 포함할지 범위 정의
  - Claim: 토큰 내부의 실제 데이터(예: `sub`, `email`, `roles`)
- Identity Provider (IdP)
  - Keycloak 외부 인증원(소셜 로그인, 사내 SSO)
- User Federation
  - LDAP/AD 같은 외부 사용자 저장소와 연동

## 4. 표준 프로토콜 관점
- OAuth 2.0
  - "권한 위임" 프레임워크
  - Access Token 중심으로 API 접근 제어
- OpenID Connect (OIDC)
  - OAuth 2.0 위에 "사용자 인증" 계층 추가
  - ID Token 개념 제공
- SAML 2.0
  - 엔터프라이즈 SSO에서 많이 쓰는 XML 기반 표준

실무에서는 신규 시스템 기준으로 OIDC를 우선 고려하는 경우가 많다.

## 5. 인증 플로우 선택 기준
### 5.1 Authorization Code + PKCE (권장)
- 대상: 브라우저 SPA, 모바일, 일반 웹 로그인
- 장점: 보안성과 표준성 균형이 가장 좋음
- 권장 기본값으로 생각하면 된다

### 5.2 Client Credentials
- 대상: 사용자 없는 서버 간 통신(M2M)
- 특징: 사용자 계정 없이 client 자체 권한으로 토큰 발급

### 5.3 Device Authorization
- 대상: TV/콘솔/입력 제한 장치
- 특징: 다른 기기에서 인증 완료 후 토큰 획득

### 5.4 Resource Owner Password Credentials (Password Grant)
- 대상: 레거시 전환/호환 시 제한적 사용
- 주의: 신규 설계에는 비권장, 가능하면 Code + PKCE로 대체

## 6. 토큰 이해하기
### 6.1 토큰 종류
- Access Token: API 호출 권한 증명
- Refresh Token: Access Token 재발급
- ID Token: 로그인한 사용자 식별 정보(OIDC)

### 6.2 Access Token에서 자주 보는 클레임
- `iss`: 발급자(issuer)
- `sub`: 사용자 고유 식별자
- `aud`: 대상 수신자(audience)
- `exp`, `iat`, `nbf`: 만료/발급/유효시작 시각
- `scope`: 허용 범위
- `realm_access.roles`, `resource_access`: 역할 정보

API(Resource Server)는 토큰을 받을 때 최소한 아래를 검증해야 한다.
- 서명 유효성(JWK 기반)
- 만료 시간(`exp`)
- 발급자(`iss`)
- 수신 대상(`aud`) 및 필요한 권한/역할

## 7. Keycloak 구성 요소 간 관계
1. 사용자가 Client에서 로그인 시작
2. Client가 Keycloak으로 리다이렉트
3. Keycloak이 인증 수행(비밀번호, OTP, 외부 IdP 등)
4. Keycloak이 토큰 발급
5. Client가 Access Token으로 API 호출
6. API가 토큰 검증 후 인가 판단

핵심은 "로그인/토큰 발급은 Keycloak, 비즈니스 권한 판단은 애플리케이션"으로 역할을 분리하는 것이다.

## 8. 자주 발생하는 문제와 원인
- `invalid_redirect_uri`
  - Client 설정의 Redirect URI 불일치
- `unauthorized_client`
  - 해당 grant type이 Client에서 비활성화됨
- `invalid_grant`
  - 코드/리프레시 토큰 만료, PKCE 값 불일치, 사용자 상태 문제
- `401 invalid_token`
  - API에서 issuer/audience/서명키 검증 실패
- 로그아웃 후 재로그인 이상
  - 프론트 세션, Keycloak 세션, 백엔드 세션 정리 불일치
