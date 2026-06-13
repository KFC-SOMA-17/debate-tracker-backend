## US-001 - STOMP 메시지 브로커 설정 추가
- **고민했던 지점**: (1) STOMP endpoint를 기존 raw처럼 @ConditionalOnProperty(stt.azure.enabled)로 묶을지. (2) setAllowedOrigins vs setAllowedOriginPatterns. (3) SockJS withSockJS() 호출 여부.
- **트레이드오프**:
  - 조건부 활성 포기 → 항상 활성. raw는 STT 핸들러 빈이 없으면 만들 수 없어 조건부였지만, STOMP는 핸들러 빈 없이 endpoint/broker만 등록하므로 조건이 불필요. 대신 STT 비활성 환경에서도 빈 broker가 뜨는 미세 비용을 감수(무해).
  - setAllowedOrigins 채택(AC 명시) — 정확한 origin 매칭. 와일드카드 패턴이 필요해지면 후속에서 setAllowedOriginPatterns로 전환.
  - SockJS는 명시 등록하지 않음 — AC 미요구. 단, 컨벤션 BaseStompTest는 SockJsClient로 접속하므로 US-004 통합 테스트 작성 시 endpoint에 .withSockJS()가 필요해질 수 있음(후속 빚으로 남김).
- **잠재 위험**:
  - 구독자 없는 /topic 발송 가시성: 아직 @MessageMapping/broadcast가 없어 이 스토리 자체는 무발송. US-002~006에서 SimpleBroker로 발송 시작.
  - SimpleBroker는 단일 인스턴스 인메모리 → 다중 인스턴스 스케일아웃 시 토픽 fan-out이 인스턴스 경계를 못 넘음(원래 계획상 Redis Pub/Sub 백플레인 필요). 1차 단일 인스턴스 전제로 보류.
  - 메시지 크기 64KB는 텍스트/바이너리 STOMP 프레임 한도. 분할 전송(partial)·sendBufferSizeLimit은 미설정 — 대용량 누적 시 재검토 필요.
- **검증/완화**: compileJava + 전체 test 통과로 새 broker 빈이 기존 @SpringBootTest 컨텍스트와 충돌 없음 확인(SimpleBrokerMessageHandler 기동 로그). endpoint 경로를 raw("/ws/stt")와 분리("/ws")해 마이그레이션 공존 중 충돌 차단. SockJS/백플레인 한계는 후속 스토리(US-004 통합 테스트, 운영 단계 Redis 백플레인)로 위임.
---

## US-002 - 서버→클라 메시지 /topic broadcast 전환
- **고민했던 지점**: broadcast 시그니처를 String debateId 로 받을지, DebateSession/long 으로 받을지. AC가 명시적으로 `broadcast(String debateId, WebSocketMessage)` + `convertAndSend("/topic/debate/"+debateId, ...)` 를 요구해 String 으로 통일. 또 broadcast 내부에 try/catch 로깅을 둘지 고민 — 기존 send 는 connection.sendMessage 의 IOException 을 삼켰지만, convertAndSend 는 in-memory SimpleBroker 큐 적재라 실패 양상이 다르고 US-006에서 예외→ERROR broadcast 를 별도로 다루므로 wrapping 없이 두었다.
- **트레이드오프**: send 오버로드(DebateSession/WebSocketSession)를 지금 지우지 않고 유지 — raw 핸들러/예외핸들러가 아직 그것에 의존하므로 컴파일·테스트 그린을 위해 데드코드를 잠시 남기는 비용을 감수(정리는 US-007). 대신 마이그레이션 중간 상태가 단순해지고 각 커밋이 독립적으로 그린.
- **잠재 위험**: (1) broadcast 가시성 — 구독자가 아직 /topic/debate/{id} 를 구독하지 않은 시점(START 직전)에 발송되면 메시지가 유실된다. 현재 전사/리파인은 START 이후 발생하므로 실사용 경로에선 안전하나, 토픽 구독 타이밍은 US-004(start 전 구독) 설계에 의존. (2) SimpleBroker 는 단일 인스턴스 in-memory — 다중 인스턴스 배포 시 다른 노드 구독자에게 broadcast 누락(STOMP relay/Redis 백플레인 필요). 1차 단일 인스턴스 전제. (3) 양 경로 공존 — 같은 메시지가 raw send 경로로는 더 이상 안 나가고 broadcast 로만 나감. raw 클라이언트(구 /ws/stt)는 이제 전사/리파인을 못 받는다(의도된 전환, US-007에서 raw 제거).
- **검증/완화**: WebSocketMessageSenderTest 로 convertAndSend("/topic/debate/1", message) 인자까지 verify. listener/refine 단위 테스트를 broadcast(eq(debateId),...) 로 갱신해 행위 회귀 차단. 전체 :app-debate:test 로 @SpringBootTest 컨텍스트가 SimpMessagingTemplate 빈과 함께 정상 기동함을 확인. 다중 인스턴스/구독 타이밍 위험은 후속 스토리(US-004 구독 선행)·운영 백플레인 결정으로 위임.
---

## US-003 - 세션 lifecycle을 debateId 키로 전환
- **고민했던 지점**: (1) ATTR_DEBATE_ID 상수를 어디에 둘지 — 축소된 순수 record DebateSession 에 남길지, raw 핸들러로 옮길지. (2) 서비스(refine/agenda)가 받는 DebateSession 파라미터를 String debateId 로 바꿀지. (3) DebateSession 의 WS 생성자를 쓰던 WebSocketExceptionHandler 를 어느 커밋에서 손볼지. (4) handleControlMessage 가 session 을 계속 받게 둘지(START 시 attribute set 책임을 어디에).
- **트레이드오프**:
  - ATTR_DEBATE_ID → SttWebSocketHandler 이전: DebateSession 을 진짜 순수 도메인 record 로 유지(얻음) vs WebSocketExceptionHandler 가 ws.handler 패키지를 참조하는 결합 발생(포기). 둘 다 raw 경로라 US-007 에서 동반 삭제되므로 일시적 결합으로 판단.
  - 서비스 시그니처 유지(DebateSession 그대로 받음): 변경 범위 최소화(얻음) vs debateId 기반 일관성 미완(포기). 서비스가 session.debateId() 만 쓰므로 record 축소만으로 충분, 불필요한 시그니처 파급 회피.
  - 핸들러가 attribute set 책임을 가져감: startDebate 에서 WebSocketSession 을 완전히 들어냄(얻음) vs raw 핸들러에 WS 상태관리 로직이 남음(포기). 어차피 US-007 에서 핸들러째 삭제.
  - 커밋 분리: WebSocketExceptionHandler attribute 추출을 선행 독립 커밋(현행 DebateSession 으로 컴파일 가능)으로 떼어내 reshape 커밋 크기를 줄임. reshape 자체는 prod+test 원자 커밋(분해 불가).
- **잠재 위험**:
  - 마이그레이션 중간 상태: raw 경로(SttWebSocketHandler·WebSocketExceptionHandler)와 STOMP broadcast 가 여전히 공존. raw 핸들러는 send(WebSocketSession,...) 단건 전송, STOMP 는 /topic broadcast — 두 전송 방식이 동시 존재(US-007 에서 raw 제거로 해소).
  - DebateSession 이 debateId 만 들고 있어 "연결 정보"가 사라짐 → 서버가 특정 연결로 직접 push 하던 능력 상실. 이는 의도된 방향(STOMP SimpleBroker 가 구독 기반 fan-out 담당)이나, raw 핸들러가 살아있는 동안은 attribute 에 의존한 단건 전송이 남아있는 이중 모델.
  - afterConnectionClosed/exceptionHandler 가 attribute 미존재 시: closed 는 Optional 로 null 안전, exceptionHandler 는 String.valueOf(null)="null" → Long.parseLong 실패 가능(원본도 new DebateSession(session) 가 throw 했으므로 동등 수준). START 이전 예외라는 희귀 경로.
  - SimpleBroker 한계(다중 인스턴스 미지원)는 이 스토리 범위 밖 — debateId 키 전환으로 외부 브로커(STOMP relay) 이전 시 자연스럽게 흡수 가능한 형태가 됨.
- **검증/완화**: 손댄 모든 소비처의 단위 테스트를 새 시그니처로 갱신하고 :app-debate:test 전체 그린 확인. reshape 를 prod+test 원자 커밋으로 묶어 중간 커밋도 테스트 그린 유지. 선행 독립 커밋(exceptionHandler)은 단독 컴파일+해당 테스트 그린으로 검증. 양 경로 공존 위험은 US-007(raw 제거)로 명시 위임.
---
