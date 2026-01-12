<div align="center">

# 📊 코스피 검색엔진
### Spring Boot · React · Oracle 기반 주식·뉴스 분석 플랫폼

<br>

<img src="https://img.shields.io/badge/Java-17-007396?logo=java">
<img src="https://img.shields.io/badge/SpringBoot-2.7-6DB33F?logo=springboot">
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

**코스피 검색엔진**은 
주식 종목 정보와 뉴스 데이터를 수집·분석하여  
**시장 흐름, 종목 이슈, 뉴스 감성 및 키워드 트렌드를 종합적으로 제공하는**  
웹 기반 주식·뉴스 분석 플랫폼입니다.

뉴스 크롤링을 통한 데이터 수집부터  
감성 분석, 키워드 트렌드 분석, 검색 엔진, 관리자 운영 기능까지  
**데이터 수집 → 분석 → 시각화 → 운영 관리** 흐름을 중심으로 설계되었습니다.

- 개발 기간 : 1차: `2025.12.02 ~ 2025.12.09`, 2차: `2025.12.09 ~ 2025.12.16`
- 개발 인원 : `5명` 
- 개발 환경 : Spring Boot + React 분리 구조
- 배포 URL : http://3.236.44.225/
---

### 👨‍💻 담당 역할

| 역할/영역 | 담당 내용 |
|---|---|
| 🧑‍🏫 팀원 |
| 🗂 DB 설계 | **주식·뉴스·감성 분석 관련 테이블(STOCK_INFO, STOCK_NEWS 등)** 구조 설계 및 관계 정의 |
| 📰 뉴스 수집 | **Python 기반 네이버 뉴스 크롤러 설계 및 구현**, **본문 정제·중복 제거 처리** |
| 🧠 감성 분석 | **뉴스 제목·본문 기반 감성 분석 로직(긍정/보통/부정)** 설계 및 **키워드 사전 확장** |
| 📊 감성 대시보드 | **종목별·기간별 감성 통계 대시보드** 설계 및 **그래프·요약 패널 시각화 구현** |
| 🔑 키워드 트렌드 | **뉴스 키워드 추출 및 빈도 기반 트렌드 분석 기능** 구현 |
| ⏱ 데이터 자동화 | **스케줄러 기반 뉴스 수집 및 감성 분석 자동화 구조** 설계 |
| 🔗 백엔드 API | **감성 통계·뉴스 조회 API(Spring Boot)** 설계 및 구현 |
| 🎨 화면 설계 | **감성 대시보드·키워드 트렌드 화면 UI 설계** 및 **데이터 시각화 구조 정의** |
| 🧪 데이터 검증 | **크롤링 실패·데이터 누락·감성 편향 문제 분석 및 개선 작업 수행** |

  
---

### ✨ 주요 특징 
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

## 🧩 기능 구성 (클릭해서 보기)

<details>
<summary><strong>📈 사용자 기능</strong></summary>

- 종목 검색 및 상세 조회  
- 종목 현재가, 등락률, 시가총액 정보 제공  
- 종목별 뉴스 조회  
- 뉴스 감성 분석 결과 확인  
- 키워드 기반 뉴스 요약 및 트렌드 분석  
- 시장 및 종목 차트 시각화  

</details>

<details>
<summary><strong>📰 데이터 수집 및 분석 기능</strong></summary>

- 주식 관련 뉴스 자동 수집(크롤링)  
- 뉴스 중복 제거 및 원문(CLOB) 저장  
- 뉴스–종목 자동 매칭  
- 배치/스케줄 기반 데이터 수집 구조  

</details>

<details>
<summary><strong>🧠 분석 및 인사이트 제공</strong></summary>

- 뉴스 본문 기반 감성 분석 (긍정 / 보통 / 부정)  
- 종목별 감성 비율 집계  
- 시장 전체 감성 흐름 요약  
- 키워드 빈도 기반 트렌드 분석  

</details>

<details>
<summary><strong>🔍 검색 엔진</strong></summary>

- 종목명 / 종목코드 검색  
- 뉴스 제목·키워드 검색  
- 종목·뉴스 통합 검색 결과 제공  

</details>

<details>
<summary><strong>🔐 회원 / 인증 기능</strong></summary>

- 회원가입 / 로그인 / 로그아웃  
- JWT 기반 인증 (Access / Refresh Token 분리)  
- 소셜 로그인 (Google / Kakao / Naver)  
- 비밀번호 찾기 및 재설정(이메일 인증)  
- 로그인 실패 횟수 제한 및 계정 잠금 처리  

</details>

<details>
<summary><strong>🛠 관리자 기능</strong></summary>

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

</details>

<details>
<summary><strong>🔍 로그 & 보안</strong></summary>

- 로그인 성공 / 실패 / 잠금 로그 기록  
- 관리자 모든 행위 로그 기록  
- IP / User-Agent 기반 접속 정보 저장  
- 운영·보안 감사 목적 로그 구조 설계  

</details>

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

<img width="1322" height="988" alt="image" src="https://github.com/user-attachments/assets/e0c61011-41fa-411f-88bb-625aa8cf5cdd" />

</details>

📄 테이블 명세서  
👉 [table-definition.xls](https://github.com/user-attachments/files/24228696/table-definition.xls)

---

## 🔍 담당 기능

📰 코스피 데이터, 뉴스 수집 및 데이터 처리
<details> <summary><strong>뉴스 크롤링 및 정제</strong></summary>

📌 설명

Python 기반 크롤러를 이용해
네이버 뉴스에서 주식·경제 관련 기사를 자동 수집하도록 구현했습니다.

뉴스 제목, 본문, 작성일, 언론사 정보 수집

중복 뉴스 제거 및 본문 HTML 태그 정제

본문이 부족한 경우 제목 기반 보완 처리

종목명이 없는 뉴스도 시장(MARKET) 뉴스로 분류하여 저장

이를 통해 감성 분석 대상 데이터의 누락을 최소화하도록 설계했습니다.

</details>
🧠 감성 분석 기능
<details> <summary><strong>뉴스 감성 분석 로직</strong></summary>

📌 설명

뉴스 제목과 본문을 기반으로
긍정 / 보통 / 부정 감성을 분류하는 감성 분석 로직을 구현했습니다.

형태소 분석(Okt) 기반 키워드 추출

긍·부정 키워드 사전 직접 정의 및 확장

제목 가중치 적용을 통한 감성 판단 정확도 개선

부정 키워드 우선 판정 구조로 현실 기사 반영 강화

분석 결과는 DB에 저장되어
대시보드 및 통계 화면에서 활용됩니다.

📊 감성 대시보드
<details>
<summary><strong>감성 대시보드 및 패널 구성</strong></summary>

<div align="center">
  <img src="깃사진/감성 분석 대시보드 전체.png" width="900"/>
  <br>
  <sub>▲ 종목별 뉴스 감성 분석 대시보드 전체 화면</sub>
</div>

📌 설명  
수집·분석된 뉴스 데이터를 기반으로  
종목별 감성 흐름을 한눈에 확인할 수 있는 감성 대시보드를 구현했습니다.

- 전체 뉴스 수, 긍정/보통/부정 비율 요약 패널  
- 기간(7일/14일/30일)별 감성 변화 조회  
- 종목별 감성 비율 그래프 시각화  
- 감성 필터 기반 뉴스 목록 조회  

단순 뉴스 나열이 아닌,  
시장 분위기와 종목 이슈를 직관적으로 파악할 수 있도록 설계했습니다.

</details>


🔑 키워드 트렌드 분석
<details>
<summary><strong>키워드 트렌드 분석</strong></summary>

<div align="center">
  <img src="깃사진/1.png" width="45%"/>
  <img src="깃사진/2.png" width="45%"/>
  <br>
  <sub>▲ 뉴스 키워드 빈도 기반 트렌드 분석 화면</sub>
</div>

📌 설명  
뉴스 본문에서 주요 키워드를 추출하고  
빈도 기반으로 트렌드를 분석하는 기능을 구현했습니다.

- 최근 기간 내 키워드 TOP N 집계  
- 키워드 클릭 시 관련 뉴스 목록 조회  
- 키워드–종목–감성 데이터 연계 구조 설계  

이를 통해  
이슈 중심의 시장 흐름 분석이 가능하도록 구성했습니다.

</details>
⏱ 자동화 및 운영
<details> <summary><strong>스케줄링 기반 자동 처리</strong></summary>

📌 설명

뉴스 수집 및 감성 분석을
스케줄러 기반으로 자동 수행하도록 구성했습니다.

일정 주기 뉴스 크롤링 실행

수집 즉시 감성 분석 및 DB 저장

최신 데이터가 대시보드에 반영되도록 처리

운영 관점에서
수동 개입 없이 데이터가 지속적으로 갱신되는 구조를 목표로 설계했습니다.

</details>

---

## 📬 프로젝트 구조

```plaintext
📦 k-stock-insight
├─ FRONTEND/                              # Vite + React
│  ├─ node_modules/
│  ├─ public/
│  │  └─ vite.svg
│  ├─ src/
│  │  ├─ admin/                           # 관리자 화면/컴포넌트
│  │  ├─ api/                             # API 호출 모듈(axios 등)
│  │  ├─ assets/                          # 이미지/아이콘 등 정적 리소스
│  │  ├─ components/                      # 공용 컴포넌트
│  │  ├─ context/                         # 전역 상태/인증 컨텍스트
│  │  ├─ layouts/                         # 레이아웃(헤더/사이드바 등)
│  │  ├─ pages/                           # 페이지 단위 컴포넌트
│  │  ├─ routes/                          # 라우팅 설정
│  │  ├─ styles/                          # 스타일(CSS)
│  │  ├─ AboutPage.jsx
│  │  ├─ App.jsx
│  │  ├─ App.css
│  │  ├─ index.css
│  │  └─ main.jsx
│  ├─ .env.example
│  ├─ .gitignore
│  ├─ eslint.config.js
│  ├─ index.html
│  ├─ package.json
│  ├─ package-lock.json
│  ├─ README.md
│  └─ vite.config.js
│
├─ BACKEND/                               # Spring Boot + Gradle
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/
│  │  │  │  ├─ com.boot/                  # (루트 패키지)
│  │  │  │  ├─ com.boot.cache/
│  │  │  │  ├─ com.boot.config/
│  │  │  │  ├─ com.boot.controller/
│  │  │  │  ├─ com.boot.dao/
│  │  │  │  ├─ com.boot.dto/
│  │  │  │  ├─ com.boot.scheduler/
│  │  │  │  ├─ com.boot.security/
│  │  │  │  └─ com.boot.service/
│  │  │  └─ resources/                    # application.properties, mybatis mapper 등
│  │  └─ test/
│  │     └─ java/
│  ├─ bin/
│  ├─ gradle/
│  ├─ build.gradle
│  ├─ settings.gradle
│  ├─ gradlew
│  └─ gradlew.bat

```

---

## 🚀 시연 영상 & 데모

아래 영상은 코스피 검색엔진의 주요 기능을 실제 화면과 함께 보여줍니다. 
각 기능별 동작 방식과 흐름을 직관적으로 확인할 수 있습니다.

### 📌 전체 시연 영상
🔗 YouTube 링크: https://youtu.be/5spm6NijYE4 (사용자)<br>
🔗 YouTube 링크: https://youtu.be/cdFkztkbYDM (관리자)


---
