### 다음 요구사항을 만족하는 배치 잡을 작성합니다.

Member 등급은 4개로 구분합니다.
- 일반(Normal)
- 실버
- 골드
- VIP

Member 등급 상향 조건은 총 주문 금액 기준으로 등급을 상향합니다.
- NORMAL(200_000)
- SILVER(300_000)
- GOLD(500_000)
- VIP(500_000)

JobExecutionListener를 이용하여 "총 데이터 처리 {}건 처리 시간 : {}millis"와 같은 로그 출력합니다.

Reader, Processor, Step, Job에 대한 테스트 코드를 작성합니다.

### 요구 사항 구현

- [배치 잡 작성](https://github.com/eastshine-high/spring-batch-practice/blob/main/src/main/java/com/eastshine/batch/task/membership/MemberLevelConfiguration.java)

- [테스트 코드 작성](https://github.com/eastshine-high/spring-batch-practice/blob/main/src/test/java/com/eastshine/batch/task/membership/MemberLevelConfigurationTest.java)
