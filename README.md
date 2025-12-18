<div align="center">

# 📊 K-Stock Insight  
### Spring Boot · React · Oracle 기반 주식·뉴스 분석 플랫폼

<br>

<img src="https://img.shields.io/badge/Java-17-007396?logo=java">
<img src="https://img.shields.io/badge/SpringBoot-3.x-6DB33F?logo=springboot">
<img src="https://img.shields.io/badge/React-18-61DAFB?logo=react">
<img src="https://img.shields.io/badge/Oracle-F80000?logo=oracle">
<img src="https://img.shields.io/badge/MyBatis-000000">

<br>

<img src="https://img.shields.io/badge/JWT-000000">
<img src="https://img.shields.io/badge/OAuth2-Google%20%7C%20Kakao%20%7C%20Naver-4285F4">
<img src="https://img.shields.io/badge/AWS%20EC2-Ubuntu-FF9900">
<img src="https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white">
<img src="https://img.shields.io/badge/GitHub-181717?logo=github&logoColor=white">

<br><br>
</div>

---

## 📖 프로젝트 개요

**K-Stock Insight**는  
주식 종목 정보와 뉴스 데이터를 수집·분석하여  
**시장 흐름, 종목 이슈, 뉴스 감성 및 키워드 트렌드를 종합적으로 제공하는**  
웹 기반 주식·뉴스 분석 플랫폼입니다.

뉴스 크롤링을 통한 데이터 수집부터  
감성 분석, 키워드 트렌드 분석, 검색 엔진, 관리자 운영 기능까지  
**데이터 수집 → 분석 → 시각화 → 운영 관리** 흐름을 중심으로 설계되었습니다.

- 개발 기간 : 1차: `2025.12.02 ~ 2025.12.09`, 2차: `2025.12.09 ~ 2025.12.16`
- 개발 인원 : `5명` 
- 개발 환경 : Spring Boot + React 분리 구조

---

### 👨‍💻 담당 역할

- 🧑‍🏫 **팀장** — 일정 관리, 업무 분배, 코드 리뷰 및 프로젝트 총괄
- 🔐 **인증/보안 기능** — JWT 기반 인증 구조(Access/Refresh) 설계 및 로그인/인증 필터 구현
- 🔄 **토큰 관리** — Refresh Token DB 저장, 재발급 로직 및 강제 만료 처리 구현
- 🔗 **소셜 로그인/계정 연동** — Kakao · Naver · Google OAuth2 로그인 구현 및 기존 계정 연동 로직 설계
- 📱 **QR 로그인** — QR 코드 생성/인증 토큰 발급, 모바일 인증 → 서버 검증 → PC 로그인 완료 흐름 구현(폴링/만료 포함)
- 🛡 **계정 상태 통제** — 로그인 실패 횟수 누적, 자동 잠금, 관리자 정지/해제 및 상태 기반 접근 제어 구현
- 🛠 **관리자 기능** — 회원 관리(정지·잠금·해제, 권한 변경), 토큰 강제 만료 등 운영 기능 구현
- 📊 **로그/보안 이벤트 관리** — 로그인 성공/실패 로그, 관리자 작업 로그(Admin Log) 설계 및 조회 기능 구현(필터/상세/CSV)
- 🚀 **서버 배포(부분)** — AWS EC2(Ubuntu) 기반 환경 구성 및 Spring Boot/Tomcat 배포 진행(부분 적용)

- 주요 특징  
  - 🔐 **JWT 기반 인증 시스템**  
    → Access / Refresh Token 분리, 재발급 및 만료 정책 적용으로 안정적인 인증 흐름 제공
  
  - 🔗 **소셜 로그인 통합 (Kakao · Naver · Google)**  
    → OAuth2 로그인 지원 및 사용자 계정 관리 정책(신규 가입/연동) 적용
  
  - 🛡 **계정 보안 통제 기능**  
    → 로그인 실패 누적, 계정 잠금/정지, 권한(ADMIN/USER) 기반 접근 제어
  
  - 🛠 **관리자 페이지 구축**  
    → 회원 상태/권한 관리, 토큰 관리, 게시판/운영 기능 등 서비스 운영 기능 제공
  
  - 📊 **로그 & 보안 이벤트 모니터링**  
    → 로그인 로그/관리자 작업 로그(Audit Log) 기록 및 조회
    → 필터/상세(IP·로그인 방식 등)/CSV 다운로드로 운영 추적 가능
    
  - 📈 **주식 데이터 검색 및 상세 정보 제공**  
    → 종목 검색, 상세 페이지, 관련 정보 조회 등 핵심 서비스 기능 제공
    
  - 📰 **주식 관련 뉴스 제공 및 분석 기능**  
    → 종목별 뉴스 수집/노출, 감성 분석(긍정/부정/중립) 및 요약 정보 제공
    
  - ⚡ **Redis + 스케줄러 기반 성능 최적화**  
    → Redis 캐싱, 스케줄링 기반 데이터 갱신 등 반복 조회·외부 호출 부담 완화
  
  - 📡 **Python 기반 데이터 수집 파이프라인 연계**  
    → 뉴스/데이터 크롤링 → DB 적재 → Spring Boot 서비스 조회 흐름 구성
    
  - 🚀 **AWS EC2 기반 서버 배포**  
    → Ubuntu 서버 환경에서 서비스 배포/운영 구성(지속적으로 보완 및 개선)

---

## 🛠 기술 스택

| 분야 | 기술 |
|------|------|
| **Frontend** | <img src="https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black"> <img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black"> |
| **Backend** | <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/Lombok-ED1C24?style=flat-square"> <img src="https://img.shields.io/badge/MyBatis-000000?style=flat-square"> |
| **Database** | <img src="https://img.shields.io/badge/Oracle%20Database-F80000?style=flat-square&logo=oracle&logoColor=white"> |
| **Security** | <img src="https://img.shields.io/badge/JWT%20(Access%20%2F%20Refresh%20Token)-000000?style=flat-square"> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"> |
| **Data / Crawling** | <img src="https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white"> <img src="https://img.shields.io/badge/Requests-000000?style=flat-square"> <img src="https://img.shields.io/badge/BeautifulSoup-4B8BBE?style=flat-square"> |
| **Cache / Scheduler** | <img src="https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Scheduler-6DB33F?style=flat-square&logo=spring&logoColor=white"> |
| **Infra / Server** | <img src="https://img.shields.io/badge/AWS%20EC2%20(Ubuntu)-FF9900?style=flat-square&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/Apache%20Tomcat-F8DC75?style=flat-square&logo=apachetomcat&logoColor=black"> |
| **Build Tool** | <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white"> |
| **Tools** | <img src="https://img.shields.io/badge/VS%20Code-007ACC?style=flat-square&logo=visualstudiocode&logoColor=white"> <img src="https://img.shields.io/badge/STS-6DB33F?style=flat-square&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/Postman-FF6C37?style=flat-square&logo=postman&logoColor=white"> <img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white"> <img src="https://img.shields.io/badge/SourceTree-0052CC?style=flat-square&logo=sourcetree&logoColor=white"> |


---

## ✨ 주요 기능

### 📈 사용자 기능
- 🔎 **종목 검색 및 상세 조회**
- 📊 **종목 현재가, 등락률, 시가총액 정보 제공**
- 📰 **종목별 뉴스 조회**
- 🧠 **뉴스 감성 분석 결과 확인**
- 🏷 **키워드 기반 뉴스 요약 및 트렌드 분석**
- 📈 **시장 및 종목 차트 시각화**


### 📰 데이터 수집 및 분석 기능
- 주식 관련 뉴스 자동 수집(크롤링)
- 뉴스 중복 제거 및 원문(CLOB) 저장
- 뉴스–종목 자동 매칭
- 배치/스케줄 기반 데이터 수집 구조


### 🧠 분석 및 인사이트 제공
- 뉴스 본문 기반 감성 분석 (긍정 / 보통 / 부정)
- 종목별 감성 비율 집계
- 시장 전체 감성 흐름 요약
- 키워드 빈도 기반 트렌드 분석


### 🔍 검색 엔진
- 종목명 / 종목코드 검색
- 뉴스 제목·키워드 검색
- 종목·뉴스 통합 검색 결과 제공


### 🔐 회원 / 인증 기능
- 회원가입 / 로그인 / 로그아웃
- JWT 기반 인증 (Access / Refresh Token 분리)
- 소셜 로그인 (Google / Kakao / Naver)
- 비밀번호 찾기 및 재설정(이메일 인증)
- 로그인 실패 횟수 제한 및 계정 잠금 처리


### 🛠 관리자 기능
- **관리자 대시보드**
  - 사용자 수, 로그인 현황, 뉴스 수집 상태 모니터링
- **회원 관리**
  - 계정 정지 / 해제
  - 권한 변경(USER / ADMIN)
- **토큰 관리**
  - Refresh Token 강제 만료
- **로그 관리**
  - 로그인 로그
  - 관리자 작업 로그(Audit Log)


### 🔍 로그 & 보안
- 로그인 성공 / 실패 / 잠금 로그 기록
- 관리자 모든 행위 로그 기록
- IP / User-Agent 기반 접속 정보 저장
- 운영·보안 감사 목적 로그 구조 설계

---

## 🧭 메뉴 구조도 (PDF)

📄 메뉴 구조도 다운로드  
👉 [menu structure.pdf](https://github.com/user-attachments/files/24228651/menu.structure.pdf)

---

## 🖥 화면 설계서 (PDF)

📄 화면 설계서 보기  
👉 [ui-design.pdf](https://github.com/user-attachments/files/24228667/ui-design.pdf)

---

## 🗂 ERD 및 테이블 명세서

📄 ERD  
<details> <summary><strong>ERD 다이어그램</strong></summary>

<img width="568" height="843" alt="table" src="https://github.com/user-attachments/assets/db3ff890-d5fd-43aa-8861-5ef84e34b54c" />

</details>

📄 테이블 명세서  
👉 [table-definition.xls](https://github.com/user-attachments/files/24228696/table-definition.xls)

---

## 🔍 핵심 구현 내용 (내가 담당한 기능)

🔐 인증 / 보안
<details> <summary><strong>회원가입</strong></summary>


https://github.com/user-attachments/assets/684a2c0a-a9d8-4a2d-844f-fd342654ef77


https://github.com/user-attachments/assets/c0a16a21-42e8-4acb-aeda-10ff1b3ef3ec


📌 설명

사용자가 회원 정보를 입력하면
중복 여부를 확인한 후 계정을 생성하도록 구현했습니다.

회원 가입 이후에는 JWT 기반 인증 구조에 따라
로그인 시 Access Token을 발급받아 인증 상태를 유지하며,
계정 상태(활성 / 정지 / 잠금)에 따라
로그인 가능 여부를 판단하도록 구성했습니다.

</details> <details> <summary><strong>JWT 기반 인증 구조 설계 및 구현</strong></summary>

<img width="1909" height="913" alt="스크린샷 2025-12-18 170055" src="https://github.com/user-attachments/assets/fd782f1d-35ee-4c6e-9ad8-326daac11eeb" />


📌 설명

프론트엔드와 백엔드가 분리된 환경에서
인증 상태를 안전하게 관리하기 위해 JWT 기반 인증 구조를 설계하고 구현했습니다.

Access Token과 Refresh Token을 분리하여 발급하고,
Refresh Token은 DB에 저장하여 재발급 및 강제 만료가 가능하도록 처리했습니다.
이를 통해 로그아웃, 토큰 탈취 등 상황에서도
인증 상태를 제어할 수 있도록 했습니다.

요청 단위로 인증 필터에서 토큰을 검증하여
서버 상태에 의존하지 않는 Stateless 인증 흐름을 구성했습니다.

</details> <details> <summary><strong>소셜 로그인 및 계정 연동</strong></summary>


https://github.com/user-attachments/assets/ed9f6374-2057-4724-98cf-0e9992951766


📌 설명

Kakao, Naver, Google OAuth2 기반 소셜 로그인을 구현하여
다양한 로그인 수단을 제공했습니다.

인증 코드 발급 → 액세스 토큰 발급 → 사용자 정보 조회의
OAuth2 표준 흐름에 따라 인증을 처리했으며,
소셜 로그인 최초 시 기존 로컬 계정 존재 여부를 확인하여
계정 연동 여부를 판단하도록 설계했습니다.

이를 통해 로그인 방식과 관계없이
하나의 사용자 계정으로 일관된 인증 흐름을 유지했습니다.

</details> <details> <summary><strong>QR 로그인</strong></summary>


https://github.com/user-attachments/assets/20848a0b-46ad-4042-a773-6f836fe8186f


📌 설명

PC 화면에서 QR 코드를 생성하고, 모바일(또는 별도 브라우저 세션)에서 인증을 완료하면  
서버 검증을 거쳐 PC 로그인이 완료되는 QR 로그인 기능을 구현했습니다.

시연 영상에서는 **일반 창이 PC 화면**, **시크릿 창이 모바일 인증 화면(별도 세션)** 역할을 하며,  
두 세션을 분리해 실제 “PC–모바일” 환경과 동일한 흐름을 재현했습니다.

PC는 폴링 방식으로 승인 상태를 확인하고, 일정 시간 내 인증이 완료되지 않으면  
QR 인증 토큰이 자동 만료되도록 처리했습니다.

이를 통해 비밀번호 입력 없이도 안전하고 간편한 로그인 흐름을 제공했습니다.


</details> <details> <summary><strong>계정 상태 관리 및 보안 통제</strong></summary>


https://github.com/user-attachments/assets/3d49a768-9b44-408d-bc83-97debf2723a7


📌 설명

비정상적인 로그인 시도를 방지하기 위해
로그인 실패 횟수를 누적 관리하도록 구현했습니다.

일정 횟수 이상 실패할 경우 계정을 자동으로 잠금 처리하고,
관리자가 직접 계정 정지 및 해제를 수행할 수 있도록 구성했습니다.

계정 상태(활성 / 잠금 / 정지)에 따라
접근 가능 여부를 판단하여 보안 통제를 적용했습니다.

</details>
🛠 관리자 기능
<details> <summary><strong>회원 관리</strong></summary>


https://github.com/user-attachments/assets/6b74b333-4d16-4729-9e27-0464fc89176e


📌 설명

관리자 권한으로 사용자 계정 상태와 권한을 관리할 수 있도록
회원 관리 기능을 구현했습니다.

계정 정지, 잠금 해제, 권한 변경 등의 작업을
관리자 화면에서 수행할 수 있도록 구성했습니다.

</details> <details> <summary><strong>토큰 관리</strong></summary>


https://github.com/user-attachments/assets/6cfe6178-25e6-4608-bd28-93e23ae7e9fe


📌 설명

관리자가 특정 사용자에 대해
Refresh Token을 강제 만료할 수 있도록 구현했습니다.

이를 통해 로그아웃 처리, 계정 보안 이슈 발생 시
즉각적인 인증 차단이 가능하도록 구성했습니다.

</details> <details> <summary><strong>로그 관리 및 보안 이벤트 관리</strong></summary>


https://github.com/user-attachments/assets/5b1634fa-cac3-4f8b-8cbb-49ceed29105a


📌 설명

로그인 성공 및 실패 이력을 기록하여
비정상적인 로그인 시도를 모니터링할 수 있도록 구현했습니다.

로그 데이터는
로그인 결과(성공/실패) 기준으로
필터링이 가능하도록 구성했으며,
조회 결과를 CSV 파일로 다운로드할 수 있도록 구현했습니다.

각 로그 항목에 대해 상세 조회 기능을 제공하여
로그인 시도 시점의 IP 주소, 로그인 방식, 요청 정보 등을
확인할 수 있도록 구성했습니다.

</details> <details> <summary><strong>관리자 작업 로그 (Admin Log)</strong></summary>


https://github.com/user-attachments/assets/1cc578bb-f4b8-42b3-9ee5-80405dc552ce


📌 설명
관리자에 의해 수행된 주요 관리 작업에 대해
작업 로그(Audit Log)를 기록하도록 설계했습니다.

계정 상태 변경, 권한 변경, 토큰 강제 만료와 같은
중요한 관리자 작업을 로그로 남기고,
작업 유형 및 관리자 계정 기준으로
필터링 및 조회가 가능하도록 구성했습니다.

또한 관리자 작업 로그 역시
CSV 파일로 다운로드할 수 있도록 하여,
운영 이력 확인과 관리 작업 추적이 가능하도록 구현했습니다.

</details>

---

## 📬 프로젝트 구조

```plaintext
📦 k-stock-insight
├─ backend
│  ├─ controller
│  ├─ service
│  ├─ dao
│  ├─ dto
│  ├─ security
│  └─ mapper
│
├─ frontend
│  ├─ pages
│  ├─ components
│  └─ api
│
└─ database
   └─ ddl.sql
```

---

## 🚀 시연 영상 & 데모

아래 영상은 K-Stock Insight의 주요 기능을 실제 화면과 함께 보여줍니다. 
각 기능별 동작 방식과 흐름을 직관적으로 확인할 수 있습니다.

### 📌 전체 시연 영상
🔗 YouTube 링크: https://youtu.be/5spm6NijYE4 (사용자)<br>
🔗 YouTube 링크: https://youtu.be/cdFkztkbYDM (관리자)

또는  
🎥 EC2 배포 버전 직접 테스트: http://3.236.44.225/

---
