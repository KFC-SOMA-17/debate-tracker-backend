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

## US-004 - STOMP 제어 컨트롤러(start/stop)
- **고민했던 지점**: 서버→클라 전송을 컨트롤러 반환 + `@SendTo("/topic/debate/{debateId}")` 로 선언적으로 할지, US-002 의 `WebSocketMessageSender.broadcast(debateId, msg)` 를 명시 호출할지. 또 컨트롤러를 ws/controller 에 둘지 controller/debate 에 둘지. start/stop 의 비즈니스 로직을 컨트롤러에 둘지 서비스에 둘지.
- **트레이드오프**: broadcast 명시 호출을 택함 — @SendTo 도 destination 변수 치환을 지원하지만, US-006 의 ERROR(@MessageExceptionHandler)도 같은 토픽으로 broadcast 해야 하므로 **단일 broadcast 메커니즘**으로 통일하는 편이 경로 일관성·테스트 용이성에서 이득. 선언적 간결함은 약간 포기. 로직은 서비스(startDebate/stopDebateWithRemainingRefine)에 두고 컨트롤러는 위임+메시지 조립만 — 컨트롤러를 얇게 유지(통합 테스트로 커버), 서비스는 단위 테스트로 커버.
- **잠재 위험**: (1) 마이그레이션 중간 상태로 raw 경로(SttWebSocketHandler.handleControlMessage)와 STOMP 컨트롤러가 **동시에 startDebate/stop 을 호출 가능** — 같은 debateId 로 양쪽이 들어오면 중복 startStreaming/세션 저장. US-007 raw 제거 전까지 양 경로 공존 부채. (2) stop 은 세션 미존재여도 컨트롤러가 항상 DEBATE_END 를 broadcast — 구독자에게 "없던 토론 종료" 가 보일 수 있음(기존 handleControlMessage 와 동일 동작이라 회귀는 아님). (3) SimpleBroker 라 다중 인스턴스 확장 시 broadcast 가 인스턴스 로컬 — 추후 외부 브로커 전환 시 재검토.
- **검증/완화**: 통합 테스트(BaseStompTest)로 /topic 구독 후 start/stop SEND → DEBATE_START/DEBATE_END 수신을 future.get(3s)로 확인(broadcast 가시성 실증). 서비스 단위 테스트로 startDebate 의 세션 저장·startStreaming 위임 검증. 양 경로 공존/중복 호출 위험은 US-007(raw 제거)로 위임. /ws SockJS 미등록에 맞춰 plain WebSocketStompClient 로 접속해 transport 정합성 확보.
---

## US-005 - STOMP 바이너리 오디오 컨트롤러
- **고민했던 지점**: 통합 테스트에서 바이너리 페이로드를 어떻게 보내고 무엇을 단언할지. (1) octet-stream content-type 명시 → 클라 MappingJackson2 가 byte[]+octet-stream 미지원이라 send 단계에서 MessageConversionException. (2) 그냥 byte[] send → Jackson base64 JSON 직렬화되고 서버 ByteArrayMessageConverter 가 JSON 문자열 raw bytes 를 그대로 byte[] 에 주입(원본 PCM 과 불일치). 결국 바이트 정확 일치는 하네스 인코딩 아티팩트라 단언 불가/무의미하다고 판단.
- **트레이드오프**: 바이트 정확 round-trip 검증(captor.isEqualTo(pcm)) vs 소비 경로 검증(verify timeout + eq(debateId)). 전자는 BaseStompTest 의 Jackson 단일 컨버터 한계로 항상 깨지므로 포기하고, destination→debateId 추출+sendAudioChunk 위임이라는 컨트롤러 계약만 통합 테스트로 검증. 바이트 pass-through 정확성은 서비스 단위 테스트(SendAudioChunk)가 이미 보장하므로 커버리지 손실 없음. BaseStompTest 에 ByteArrayMessageConverter 를 섞는 composite 개조도 고려했으나, US-004 테스트까지 영향받는 공유 인프라 변경이라 이 스토리 범위 밖으로 미뤘다.
- **잠재 위험**: (1) audio 미-broadcast 검증이 FakeSttClient 를 그대로 쓰면 깨진다 — Fake 가 sendAudioChunk 에서 TranscribeEvent 를 publish→TRANSCRIPTION 이 /topic 으로 broadcast 되기 때문. @MockitoBean 으로 STT 를 격리하지 않으면 "토픽 미수신" 단언이 항상 실패. (2) 프로덕션 JS 클라이언트가 native binary STOMP 프레임을 보내야 ByteArrayMessageConverter 가 원본 PCM 을 받는다 — 만약 클라가 JSON/base64 로 보내면 서버가 깨진 바이트를 STT 에 흘린다(프론트 계약 의존성, 후속 검증 필요). (3) setMessageSizeLimit(64KB, US-001)이 ~6400B 청크를 수용하지만 base64 인코딩 시 ~1.33배 팽창 — JSON 경로로 보내면 한도 여유 축소(native binary 면 무관).
- **검증/완화**: @MockitoBean SttClient 로 다운스트림 전사 이벤트를 차단해 미-broadcast 를 결정적으로 단언. 소비는 Mockito timeout(3s) verify 로 비동기 처리 완료까지 대기 후 확정. 바이트 정확성은 서비스 단위 테스트에 위임. 하네스/프로덕션 인코딩 차이는 progress.txt Learnings 에 명시해 US-006(ERROR 경로)·향후 프론트 통합 시 참조하도록 남김.
---

## US-006 - STOMP 예외를 ERROR로 broadcast
- **고민했던 지점**: 예외 핸들러에서 debateId 를 어떻게 얻나. (A) @DestinationVariable 을 @MessageExceptionHandler 에 그대로 쓸 수 있나, (B) 불가하면 simpDestination 헤더를 파싱(`/app/debate/{id}/start` 분해)해야 하나. Spring 이 매칭 시 template 변수를 동일 mutable 메시지 헤더에 set 하고 예외 처리 시 같은 메시지를 재사용한다고 판단해 (A) 채택, 통합 테스트로 실증. broadcast 메커니즘도 @SendTo 로 바꿀지 고민했으나 US-002~004 와 일관되게 WebSocketMessageSender.broadcast 단일 경로 유지.
- **트레이드오프**: @DestinationVariable(A) vs destination 파싱(B). A 는 코드가 짧고 라우팅 규칙 변경에 자동 추종하지만 Spring 내부 헤더 전파에 의존(버전 의존적). B 는 명시적이고 견고하나 destination 포맷 하드코딩·중복. → A 선택 + 통합 테스트로 회귀 가드(헤더 전파가 깨지면 통합 테스트가 즉시 실패). ErrorMessage 페이로드는 raw 핸들러와 동일 포맷 재사용 — 중복 ErrorMessage 타입을 새로 만들지 않아 클라이언트 스키마 일관.
- **잠재 위험**: (1) @DestinationVariable 해석이 Spring 마이너 버전 업에서 깨지면 debateId 가 null→Long.parseLong NPE/예외 핸들러 내부 2차 예외(무한루프는 아니나 ERROR 미전달). (2) 예외 핸들러 자체가 던지면(broadcast 실패 등) 클라이언트는 아무 응답도 못 받음. (3) SimpleBroker 단일 인스턴스 가정 — 다중 인스턴스/외부 브로커 전환 시 구독자가 다른 노드면 ERROR 가 도달 안 할 수 있음(이는 broadcast 전반 공통 한계, US 범위 밖). (4) 구독 타이밍: start 직전에 토픽 구독이 끝나야 시작단계 ERROR 를 받는다 — 통합 테스트는 subscribe 후 send 라 OK지만 실 클라이언트는 구독-후-start 순서 보장 필요.
- **검증/완화**: 단위 테스트로 DebateTrackerException 코드 보존·일반 예외 INTERNAL_SERVER_ERROR 매핑 검증(위험1의 매핑 로직). 통합 테스트로 @DestinationVariable 전파+ERROR 토픽 수신 end-to-end 검증(위험1의 헤더 전파 회귀 가드). logBySeverity 로 5xx/4xx 분리 로깅해 운영 가시성 확보(위험2 의 silent 실패 추적). 위험3/4 는 후속(외부 브로커 도입·클라이언트 구독 순서 컨벤션 docs)으로 위임.
---

## US-007 - raw WebSocket 핸들러/설정 제거
- **고민했던 지점**: 한 번에 큰 삭제 vs 컴파일-세이프한 순차 삭제. 삭제 대상이 서로 참조(WebSocketConfig→핸들러→서비스 메서드→ControlMessage→sender 오버로드)해서, 어느 순서로 잘라야 매 커밋이 단독 컴파일되는지가 핵심. 또 DebateStreamingServiceTest 가 handleControlMessage 를 START 셋업 헬퍼로 쓰고 있어, 메서드 삭제 시 테스트 셋업을 startDebate 직접 호출로 갈아끼울지/별도 헬퍼로 둘지 고민.
- **트레이드오프**: 소비처(상위)부터 잘라 내려가는 순서를 택해 매 커밋 compileJava 그린을 보장(5커밋). 대안인 단일 대형 커밋은 리뷰/이분 탐색이 어렵고 중간 그린 보장이 안 됨. 핸들러와 그 테스트는 같은 커밋으로 묶어 compileTestJava 도 그린 유지(테스트를 뒤로 미루면 중간 커밋이 깨짐) — 잘게 쪼개되 테스트-구현 원자성은 지키는 절충.
- **잠재 위험**: (1) ServletServerContainerFactoryBean(servlet 컨테이너 64KB 버퍼)까지 WebSocketConfig 와 함께 제거 → STOMP 대용량(오디오) 메시지가 servlet 레벨 기본 버퍼에 걸릴 가능성. (2) raw 경로 제거로 stt.azure.enabled=true 실환경에서 /ws/stt 로 붙던 기존 클라이언트가 있다면 즉시 단절(롤백 창 없음). (3) DebateStreamingService 가 이제 STOMP 컨트롤러에만 의존 — 컨트롤러 우회 호출 경로가 사라져 서비스 단위 테스트가 유일한 진입 검증.
- **검증/완화**: (1) STOMP 메시지 한도는 US-001 StompConfig.configureWebSocketTransport.setMessageSizeLimit(64KB)가 별도로 책임 — servlet 버스 빈 제거와 무관하게 STOMP transport 레벨에서 보장됨을 확인(통합 테스트 컨텍스트 정상 기동). (2) raw 경로는 본래 stt.azure.enabled 조건부라 테스트(조건 미설정)·현행 STOMP 클라이언트에 영향 없음 — 양 경로 공존 중간 상태를 이 스토리로 의도적으로 종료. 실 배포 전환은 클라이언트의 STOMP 이전 완료가 선행 조건(문서/PR로 위임). (3) :app-debate:test 전체 그린 + grep 으로 삭제 심볼 잔존 0 확인. @SpringBootTest 컨텍스트가 STOMP endpoint 만으로 정상 부팅됨을 통합 테스트 로드로 검증(별도 bootRun 불요).
---

## US-008 - STOMP /ws SockJS fallback 추가
- **고민했던 지점**: /ws 를 어떻게 native 와 SockJS 둘 다로 노출할지. (A) addEndpoint("/ws").withSockJS() 단일 등록 — 그러면 native ws:// 정확매칭 클라이언트(BaseStompTest)가 깨질 위험. (B) 경로를 /ws(native)·/ws-sockjs(SockJS)로 분리 — STOMP_SPEC 2.1의 "동일 /ws" 요구 위반. (C) addEndpoint("/ws") 를 native·withSockJS 두 번 등록 → 채택. 테스트도 BaseStompTest 를 상속해 SockJS 세션을 추가로 열지(포트 private 노출 필요), 독립 테스트로 SockJS 클라이언트를 자체 구성할지 고민 → 후자(BaseStompTest 변경 회피, native 전용 책임 유지).
- **트레이드오프**: 이중 등록은 SockJsClient 와 native 클라이언트가 같은 /ws 를 공유(스펙 충족)하지만 SockJS 가 /ws/** 하위 경로(info·xhr 등)를 추가로 점유한다. 독립 테스트 선택으로 BaseStompTest 를 건드리지 않아 native 회귀 위험은 0이 됐지만, 컨버터 셋업(MappingJackson2+JavaTimeModule)이 BaseStompTest 와 중복됐다(향후 SockJS 테스트 증가 시 공통 베이스 추출 부채).
- **잠재 위험**: (1) native 정확매칭 핸들러와 SockJS /ws/** 핸들러의 매핑 우선순위가 꼬이면 한쪽이 다른쪽을 가릴 수 있음 — Spring 은 정확매칭(/ws)과 패턴(/ws/**)을 분리 처리해 실제론 충돌 없음. (2) SockJS 활성화로 /ws/info 등 추가 HTTP endpoint 가 노출 → CORS(setAllowedOrigins)를 양쪽 등록 모두에 적용해 동일 출처 정책 유지. (3) setAllowedOrigins 에 "*" 가 아닌 명시 origin 만 있어 SockJS 의 origin 검사도 동일 화이트리스트로 동작(와일드카드였다면 SockJS info 요청에서 제약 가능성).
- **검증/완화**: 신규 SockJS 통합 테스트로 핸드셰이크뿐 아니라 start→DEBATE_START broadcast 왕복까지 단언(SockJS transport 위 STOMP 정상 동작 증명). 동시에 기존 native DebateStompControllerTest 를 같은 실행에서 통과시켜 이중 등록이 native 경로를 깨지 않음을 확인. 전체 :app-debate:test 그린으로 회귀 없음 확정.
---

## US-009 - 테스트 verify를 assertAll 안으로 통합 (감사형)
- **고민했던 지점**: 이 스토리는 transport 전환이 아니라 테스트 컨벤션 정리다. verify( 13개 파일 전수 조사 결과 위반 0건 — 멀티 검증은 모두 assertAll 람다 안, 단발 verify는 미포장, capturing verify는 assertAll 앞. "구현"으로 억지 변경을 만들지(예: 단발 verify를 1개짜리 assertAll로 감싸기), 아니면 감사 결과(0건)를 그대로 인정하고 passes:true로 둘지 망설였다.
- **트레이드오프**: (A) 억지 수정 — diff는 생기지만 CLAUDE.local.md '람다 1개짜리 assertAll 금지'·'capturing verify는 assertAll 앞' 규칙을 오히려 위반하고 노이즈만 추가. (B) 무변경 + 감사 근거 기록 — diff 0이지만 추적성(파일:라인)을 notes/progress에 남겨 "정말 다 봤다"를 증명. (B) 선택: 컨벤션의 목적(가독성·soft assertion)을 이미 달성한 코드를 건드리지 않는 게 규칙 정신에 부합.
- **잠재 위험**: 코드 변경이 없어 "스토리를 실제로 했나"가 불투명할 수 있다(감사형 스토리의 본질적 리스크). 또 향후 새 테스트가 규칙을 어겨 유입될 여지는 남는다(자동 강제 장치 부재 — ArchUnit/커스텀 린트 없음).
- **검증/완화**: verify( grep 전수 + 각 라인을 단발/멀티/capturing 3분류로 판정해 근거를 prd.json notes·progress.txt에 파일:라인까지 기록. :app-debate:test BUILD SUCCESSFUL로 그린 유지 확인. 향후 강제는 코드 리뷰 체크리스트에 위임(자동화는 별도 과제).
---

## US-010 - Ralph 런타임 산출물 .gitignore
- **고민했던 지점**: 무시 범위를 어디까지 잡을지. ralph/ 디렉터리 전체를 ignore하고 산출물만 `!` 예외처리하는 방식 vs 런타임 파일만 개별 명시하는 방식. 후자를 택함 — 전체 ignore + negation은 새 산출물(향후 추가될 .md 등)이 생길 때마다 negation을 빠뜨리면 조용히 추적 누락되는 위험이 있고, AC가 "런타임 파일 패턴 추가"를 명시했으므로 화이트리스트가 아닌 블랙리스트가 의도에 맞음.
- **트레이드오프**: 개별 명시(블랙리스트)는 새 런타임 파일이 생기면 .gitignore를 또 손봐야 함(놓치면 잡파일이 커밋될 수 있음). 대신 커밋해야 할 산출물이 실수로 무시될 위험은 0 — Ralph 워크플로에서 prd.json/progress.txt 추적 유지가 산출물 누락보다 훨씬 중요하므로 안전한 쪽을 택함.
- **잠재 위험**: 이미 인덱스에 추적 중인 파일이 있었다면 .gitignore만으로는 무시되지 않아(추적 우선) 잡파일이 계속 커밋됐을 것. 이번엔 세 파일 모두 untracked였어 무해했지만, 과거 어느 커밋에 들어갔다면 git rm --cached가 필수였음.
- **검증/완화**: git ls-files로 무시 대상이 인덱스에 없음을 먼저 확인(→ rm --cached 스킵이 정당). git check-ignore로 런타임 4패턴 무시·산출물 4개 비무시(exit 1)를 양방향 검증해 "잡파일은 막고 산출물은 추적"을 동시에 보장. transport 마이그레이션 자체와 무관한 운영 위생 스토리라 프로덕션 경로 위험은 없음.
---
