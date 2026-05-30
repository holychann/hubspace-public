# 허브스페이스 (HubSpace)

Google Forms로 신청을 받는 행사 및 모임에서 신청자가 "내가 제대로 신청됐나?"를 스스로 확인할 수 있게 해주는 서비스입니다. 폼 응답을 자동으로 수집 및 저장하고 신청자가 자신의 신청 결과를 조회하도록 지원해 운영자에게 반복되던 신청 확인 문의를 줄입니다.

학교 생활 중 이런 신청 확인 문의가 자주 반복되는 것을 직접 겪고, 이를 자동화로 해소하고자 만들었습니다. 현재 지인을 대상으로 운영 중이며 백엔드 전체를 단독으로 설계, 구현, 운영했습니다.

---

## 아키텍처 & 데이터 플로우

### 전체 아키텍처

<img width="4500" height="2791" alt="image" src="https://github.com/user-attachments/assets/2f0b655a-7c27-4619-9a48-717ee9768e25" />

Nginx 리버스 프록시가 요청을 EC2 위 Spring Boot 애플리케이션으로 전달합니다. 애플리케이션과 RabbitMQ 와 Prometheus / Loki / Grafana 모니터링 스택은 모두 Docker 컨테이너로 운영하며, 배포는 GitHub Actions -> ECR -> EC2 흐름으로 자동화했습니다.

데이터는 용도에 따라 저장소를 분리했습니다.

- **PostgreSQL (supabase)** — 메인 RDB이자 폴링 상태(폴링 대상, 다음 폴링 시간) 관리
- **DynamoDB** — 수집한 Google Forms 응답 관리
- **Redis** — 반복 조회 데이터 캐시

### 데이터 수집 플로우

<img width="2550" height="1526" alt="image" src="https://github.com/user-attachments/assets/180cd12a-4f2c-4028-aa6e-01fec94ed995" />

Scheduler가 폴링 대상을 조회해 작업을 RabbitMQ로 발행하면 Consumer가 이를 비동기로 처리합니다. Consumer는 Google Drive에서 Forms 응답을 조회해 DynamoDB에 batch 저장한 뒤에 다음 폴링 시간을 갱신합니다.

---

## 핵심 기능 & 기술 하이라이트

- **데이터 수집 파이프라인** — Google Forms 응답을 Scheduler로 주기적으로 폴링·수집해 DynamoDB에 batch 적재합니다. 폴링 상태(supabase)와 수집 데이터(DynamoDB)의 저장소 역할을 분리해 관리합니다.
- **비동기 처리 구조** — 외부 API 의존 구간을 RabbitMQ 기반 비동기로 분리해 처리 지연 병목을 완화하고, Consumer 확장으로 처리량을 확보합니다. 메시지 발행·소비를 인터페이스로 추상화해 Kafka 등 다른 브로커로 교체 가능하도록 설계했습니다. ([관련 글: RabbitMQ](https://velog.io/@east0323/%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98-RabbitMQ-%EA%B0%9C%EB%85%90))
- **Redis 캐싱 & 파이프라이닝** — Look-aside 캐시로 반복 조회 평균 응답 시간을 62~64% 단축하고, 파이프라이닝으로 다중 캐시 저장의 네트워크 왕복을 줄여 저장 처리 시간을 51% 단축했습니다. *(개선 전·후 각 5회 측정 평균)*
- **관측성 (Observability)** — AOP 공통 로깅과 TraceId 기반 요청 추적, Prometheus·Loki·Grafana 모니터링으로 운영 중 실행 흐름과 병목을 분석합니다. ([관련 글: Spring AOP](https://velog.io/@east0323/%EC%8A%A4%ED%94%84%EB%A7%81-AOP-%EA%B0%9C%EB%85%90%EA%B3%BC-Aspect))
- **무중단 배포** — Nginx Blue-Green 전략과 헬스체크로 배포 중 서비스 중단을 방지하고, 헬스체크 실패 시 기존 버전을 유지합니다.
- **인증** — OAuth2 기반 구글 로그인과 JWT 발급을 구성했으며, Google Forms·Drive 접근을 위한 OAuth scope와 refresh token 발급을 처리합니다.

> 각 항목의 문제 정의 · 해결 과정 · 트레이드오프는 [포트폴리오](<포트폴리오-링크>)에서 확인할 수 있습니다.

---

## 기술 스택

- **Backend** — Java, Spring Boot, Spring Data JPA, QueryDSL, Spring Security (OAuth2)
- **Data / Storage** — PostgreSQL (supabase), DynamoDB, Redis
- **Messaging** — RabbitMQ
- **Infra / DevOps** — Docker (Docker Compose), AWS EC2, AWS ECR, Nginx, Cloudflare, GitHub Actions
- **Monitoring** — Prometheus, Loki, Grafana

---

## 역할 & 향후 개선

### 역할

백엔드 전체를 단독으로 설계·구현·운영했습니다 (팀원은 프론트엔드 담당). 데이터 수집 파이프라인(Scheduler·RabbitMQ·DynamoDB), Redis 캐싱, OAuth2 인증, Blue-Green 무중단 배포, Prometheus·Loki·Grafana 모니터링 구성까지 백엔드 전 영역을 직접 다뤘습니다.

### 향후 개선

기능을 빠르게 추가하는 과정에서 테스트 작성을 미뤄둔 상태입니다. 데이터 수집·적재의 신뢰성을 보장하기 위해, 핵심 파이프라인부터 단위·통합 테스트를 우선 도입할 계획입니다.
