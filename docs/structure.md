# 분산 환경에서의 STT 다중 세션 관리 전략

> `app-debate`와 `infra-stt`를 **별도 배포 단위**로 분리한 아키텍처에서,
> STT 세션 관리와 서버 다중화(horizontal scaling) 전략을 정리한다.

---

## 1. 현재 구조와 문제 정의

### 1.1 배포 단위 분리 구조

```
Client ──WebSocket──► app-debate (JVM A)
                          │
                          │  HTTP POST (오디오 청크)
                          ▼
                      infra-stt (JVM B)
                          │
                          ├── ConcurrentHashMap<sessionId, TranscriberSession>
                          │       ├── session-A → Azure WS 연결 + PushAudioInputStream
                          │       └── session-B → Azure WS 연결 + PushAudioInputStream
                          │
                          └── AzureAdapter.sendAudioChunk(sessionId, pcmData)
                                  → sessions.get(sessionId).pushStream().write(pcmData)
                          │
                          │  Webhook / Message Queue (전사 결과)
                          ▼
                      app-debate (JVM A)
                          │
                          └── 클라이언트 WebSocket broadcast
```

`app-debate`와 `infra-stt`는 **별도 JVM / 별도 배포 단위**로 동작한다.

- `app-debate`: 세션 lifecycle, WebSocket broadcast, 발화·쟁점 도메인, 화자·찬반 라벨링 등 비즈니스 로직 전담.
- `infra-stt`: Azure Speech Service와의 stateful 연결 관리, 오디오 수신 및 전사 결과 전달 전담.

### 1.2 분리의 이점

| 관점 | 설명 |
|------|------|
| **독립 스케일링** | app-debate는 WebSocket 세션 수 기준, infra-stt는 Azure 동시 연결 수 기준으로 각각 스케일링. |
| **장애 격리** | infra-stt의 Azure 연결 문제(OOM, 메모리 릭)가 app-debate의 비즈니스 로직에 전파되지 않음. |
| **벤더 교체 용이** | infra-stt만 재배포하면 STT 벤더를 교체할 수 있다. app-debate는 무중단. |
| **자원 프로파일 분리** | infra-stt는 오디오 스트리밍으로 메모리·네트워크 집약적, app-debate는 CPU·DB I/O 집약적. 인스턴스 스펙을 독립 튜닝 가능. |

### 1.3 분리에 따른 새로운 문제

기존(같은 JVM)에서는 `ConcurrentHashMap`으로 충분하던 세션 조회가, 네트워크를 넘어야 하므로 다음 문제가 발생한다:

1. **라우팅 문제**: app-debate 인스턴스 A가 받은 오디오를, session-X를 담당하는 infra-stt 인스턴스 B로 정확히 보내야 한다.
2. **연결 생명주기 동기화**: app-debate에서 세션이 종료되면 infra-stt의 Azure 연결도 정리해야 한다. 네트워크 단절 시 orphan 연결이 생길 수 있다.
3. **전사 결과 전달**: 같은 JVM에서는 `Consumer<SttSegment>` 콜백으로 해결했으나, 네트워크를 넘기면 콜백이 동작하지 않는다. 별도 push 채널이 필요하다.
4. **부분 장애**: infra-stt는 살아있지만 app-debate가 죽거나, 그 반대 상황에서의 정합성 관리.

---

## 2. IPC 프로토콜 선택 — 양방향 스트리밍이 필요한가?

### 2.1 두 방향의 특성이 다르다

app-debate ↔ infra-stt 통신을 한 덩어리로 보면 "양방향 스트리밍"처럼 보이지만, 실제로 두 방향은 **특성이 완전히 다르다**.

| 방향 | 데이터 | 특성 | 요청-응답 가능? |
|------|--------|------|----------------|
| **오디오 전송** (app-debate → infra-stt) | PCM 바이너리 청크 (~200ms 단위) | 각 청크는 독립적. 순서만 보장되면 됨. 응답은 ACK 정도. | **O** — HTTP POST 충분 |
| **전사 결과** (infra-stt → app-debate) | `SttSegment` (텍스트, 화자, 타임스탬프) | 비동기. 오디오 청크와 1:1 대응 아님. Azure가 내부적으로 버퍼링 후 불규칙 타이밍에 이벤트 발생. | **X** — push 필요 |

**왜 요청-응답만으로 안 되는가 (전사 결과 방향):**

Azure STT는 오디오를 받아서 내부적으로 축적한 뒤, 충분한 컨텍스트가 모이면 전사 결과를 발생시킨다. 이 타이밍은 오디오 청크 전송 타이밍과 무관하다.

```
오디오 청크:   |--1--|--2--|--3--|--4--|--5--|--6--|--7--|--8--|
Azure 내부:    [  버퍼링...  ][인식!]  [  버퍼링....  ][인식!]
전사 결과:                    ↑ SttSegment              ↑ SttSegment
```

- 청크 3개를 보냈을 때 결과가 올 수도 있고, 8개를 보내야 올 수도 있다.
- 하나의 오디오 청크 POST에 대한 HTTP 응답으로 전사 결과를 돌려줄 수 없다 — **아직 결과가 없을 수 있기 때문.**
- polling은 지연이 추가되고 자원 낭비.

### 2.2 결론: 방향별로 프로토콜을 분리한다

**gRPC bidi streaming이나 WebSocket 같은 양방향 스트리밍은 불필요하다.** 두 방향을 독립 채널로 분리하면 각각 가장 단순한 프로토콜을 쓸 수 있다.

```
┌─────────────────────────────────────────────────────────┐
│                     오디오 전송 (→)                       │
│   app-debate ──HTTP POST──► infra-stt                   │
│   - POST /api/stt/sessions/{id}/audio-chunks            │
│   - Body: binary PCM                                    │
│   - Response: 202 Accepted (ACK만)                      │
│                                                         │
│                     전사 결과 (←)                         │
│   infra-stt ──push──► app-debate                        │
│   - Webhook callback / Message Queue / SSE              │
│                                                         │
│                     세션 관리 (→)                         │
│   app-debate ──HTTP──► infra-stt                        │
│   - POST /api/stt/sessions/{id}/start                   │
│   - POST /api/stt/sessions/{id}/stop                    │
│   - GET  /api/stt/sessions/{id}/status                  │
└─────────────────────────────────────────────────────────┘
```

### 2.3 전사 결과 push 방식 후보 비교

| 방식 | 구현 복잡도 | 실시간성 | 인프라 의존 | Sprint 1-2 적합성 |
|------|-----------|---------|-----------|-------------------|
| **Webhook (HTTP callback)** | 낮음 | 높음 (즉시 POST) | 없음 | **O — 권장** |
| **Redis Pub/Sub** | 중간 | 높음 | Redis | O (Redis 이미 도입 시) |
| **Kafka / Redis Stream** | 높음 | 중간 (배치 가능) | Kafka/Redis | X (과설계) |
| **SSE (Server-Sent Events)** | 중간 | 높음 | 없음 | △ (연결 관리 필요) |

#### 방식 A: Webhook callback (권장 — Sprint 1-2)

세션 시작 시 app-debate가 콜백 URL을 등록한다. infra-stt는 전사 결과가 나올 때마다 해당 URL로 POST한다.

```
1. app-debate → infra-stt
   POST /api/stt/sessions/{id}/start
   Body: { "callbackUrl": "http://app-debate:8080/internal/stt/segments" }

2. (오디오 청크 전송 반복)
   app-debate → infra-stt
   POST /api/stt/sessions/{id}/audio-chunks
   Body: <binary PCM>
   Response: 202 Accepted

3. (Azure가 전사 결과 생성 시)
   infra-stt → app-debate
   POST http://app-debate:8080/internal/stt/segments
   Body: { "sessionId": "...", "text": "...", "speaker": "...", "start": 1.2, "end": 3.5 }
```

**이점**:
- 구현이 단순하다. 양쪽 모두 일반 HTTP 컨트롤러/클라이언트만 있으면 된다.
- 추가 인프라(Message Queue 등)가 필요 없다.
- 기존 Spring MVC 생태계 그대로 활용.

**한계**:
- app-debate가 일시적으로 불능 상태면 전사 결과가 유실된다 → infra-stt 측에 재시도 로직 필요.
- app-debate 인스턴스가 여러 대일 때, 콜백 URL이 특정 인스턴스를 가리켜야 한다 (LB 뒤의 아무 인스턴스가 아닌, 해당 세션의 WS를 보유한 인스턴스).

#### 방식 B: Redis Pub/Sub (Scale-out 대비)

```
1. infra-stt: Azure 전사 결과 → Redis PUBLISH "stt:session:{sessionId}" 
2. app-debate: Redis SUBSCRIBE "stt:session:{sessionId}" → 클라이언트 WS broadcast
```

**이점**:
- app-debate가 여러 인스턴스여도 해당 세션의 WS를 가진 인스턴스만 subscribe하면 된다.
- infra-stt가 app-debate 주소를 알 필요 없다 (느슨한 결합).
- 일시적 네트워크 단절에도 Redis가 버퍼 역할.

**한계**:
- Redis 인프라 필요.
- Pub/Sub은 subscriber가 없으면 메시지 유실 (Redis Stream으로 대체 가능).

---

## 3. Consumer 콜백 → 이벤트 수신 구조로의 전환

### 3.1 현재 구조의 문제

현재 `SttClient` 인터페이스:

```java
public interface SttClient {
    void startStreaming(String sessionId, Consumer<SttSegment> onSegment);
    void sendAudioChunk(String sessionId, byte[] pcmData);
    void stopStreaming(String sessionId);
}
```

`Consumer<SttSegment> onSegment`는 **같은 JVM 안에서만 동작하는 콜백**이다. 서버가 분리되면:

- `Consumer`는 네트워크를 넘을 수 없다 (직렬화 불가).
- WebSocketHandler 안에서 `sttClient.startStreaming(id, segment -> session.sendMessage(...))` 같은 패턴을 쓸 수 없다.

### 3.2 분리 후 구조: 역할 분해

콜백 하나가 하던 일을 **3개의 독립 컴포넌트**로 분해한다.

```
[기존: 같은 JVM]
WebSocketHandler
  └── sttClient.startStreaming(id, segment -> {
          // 여기서 모든 걸 한다
          utteranceService.save(segment);      // DB 저장
          session.sendMessage(toJson(segment)); // WS 브로드캐스트
      });

[분리 후]
┌──────────────────────────────────────────────────────────────────┐
│ app-debate                                                       │
│                                                                  │
│  WebSocketHandler                                                │
│    ├── 클라이언트 오디오 수신                                       │
│    └── SttRemoteClient.sendAudioChunk(sessionId, pcmData)        │
│          └── HTTP POST → infra-stt                               │
│                                                                  │
│  SegmentReceiver (내부 HTTP 컨트롤러)                              │
│    ├── POST /internal/stt/segments  ← infra-stt가 호출           │
│    ├── utteranceService.save(segment)          // DB 저장         │
│    └── webSocketBroadcaster.send(sessionId, segment) // WS 전송  │
│                                                                  │
│  WebSocketBroadcaster                                            │
│    └── 해당 sessionId의 WS 세션들에 메시지 fan-out                  │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ infra-stt                                                        │
│                                                                  │
│  SttController (HTTP API)                                        │
│    ├── POST /api/stt/sessions/{id}/start   → startStreaming      │
│    ├── POST /api/stt/sessions/{id}/audio   → sendAudioChunk      │
│    └── POST /api/stt/sessions/{id}/stop    → stopStreaming       │
│                                                                  │
│  AzureAdapter (기존과 동일)                                       │
│    ├── ConcurrentHashMap<sessionId, TranscriberSession>           │
│    └── Azure transcribed 이벤트 발생 시:                           │
│          └── callbackClient.post(callbackUrl, sttSegment)        │
└──────────────────────────────────────────────────────────────────┘
```

### 3.3 변경되는 인터페이스

#### infra-stt 측: `SttClient` → HTTP API로 노출

`SttClient` 인터페이스 자체는 infra-stt 내부에서 그대로 사용한다. 다만 `Consumer` 콜백 대신 **콜백 URL**을 받도록 변경한다.

```java
// infra-stt 내부 — SttClient 인터페이스 변경
public interface SttClient {
    void startStreaming(String sessionId, String callbackUrl);  // Consumer → callbackUrl
    void sendAudioChunk(String sessionId, byte[] pcmData);
    void stopStreaming(String sessionId);
    boolean isConnected(String sessionId);
    String getVendorName();
}
```

```java
// infra-stt — HTTP API 컨트롤러 (신규)
@RestController
@RequestMapping("/api/stt/sessions")
public class SttController {

    private SttClient sttClient;

    @PostMapping("/{sessionId}/start")
    ResponseEntity<Void> startStreaming(@PathVariable String sessionId,
                                        @RequestBody StartRequest request) {
        sttClient.startStreaming(sessionId, request.callbackUrl());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{sessionId}/audio")
    ResponseEntity<Void> sendAudioChunk(@PathVariable String sessionId,
                                        @RequestBody byte[] pcmData) {
        sttClient.sendAudioChunk(sessionId, pcmData);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{sessionId}/stop")
    ResponseEntity<Void> stopStreaming(@PathVariable String sessionId) {
        sttClient.stopStreaming(sessionId);
        return ResponseEntity.ok().build();
    }
}
```

```java
// infra-stt — AzureAdapter 내부 콜백 처리 변경
// 기존: onSegment.accept(sttSegment);
// 변경: HTTP POST로 결과 전송
callbackClient.post(callbackUrl, sttSegment);
```

#### app-debate 측: `Consumer` 콜백 → `SegmentReceiver` 컨트롤러

```java
// app-debate — 전사 결과를 받는 내부 컨트롤러 (신규)
@RestController
@RequestMapping("/internal/stt")
public class SegmentReceiver {

    private UtteranceService utteranceService;
    private WebSocketBroadcaster broadcaster;

    @PostMapping("/segments")
    ResponseEntity<Void> receiveSegment(@RequestBody SttSegmentEvent event) {
        utteranceService.save(event.sessionId(), event.toSegment());
        broadcaster.send(event.sessionId(), event.toSegment());
        return ResponseEntity.ok().build();
    }
}
```

```java
// app-debate — infra-stt를 HTTP로 호출하는 클라이언트 (신규)
// Consumer 콜백 대신 원격 호출을 추상화
@Component
public class SttRemoteClient {

    private RestClient restClient;  // infra-stt 주소로 설정

    public void startStreaming(String sessionId, String callbackUrl) {
        restClient.post()
            .uri("/api/stt/sessions/{id}/start", sessionId)
            .body(new StartRequest(callbackUrl))
            .retrieve()
            .toBodilessEntity();
    }

    public void sendAudioChunk(String sessionId, byte[] pcmData) {
        restClient.post()
            .uri("/api/stt/sessions/{id}/audio", sessionId)
            .body(pcmData)
            .retrieve()
            .toBodilessEntity();
    }

    public void stopStreaming(String sessionId) {
        restClient.post()
            .uri("/api/stt/sessions/{id}/stop", sessionId)
            .retrieve()
            .toBodilessEntity();
    }
}
```

### 3.4 데이터 흐름 비교

```
[기존: 같은 JVM]
Client ──WS audio──► WebSocketHandler ──► SttClient.sendAudioChunk()
                                                    │
                                              Azure 이벤트
                                                    │
                                         Consumer<SttSegment>.accept()
                                                    │
                     WebSocketHandler ◄─────────────┘
                          │
Client ◄──WS message──────┘


[분리 후: 별도 JVM]
Client ──WS audio──► WebSocketHandler ──HTTP POST──► infra-stt SttController
                                                          │
                                                    Azure 이벤트
                                                          │
                                                    HTTP POST (webhook)
                                                          │
                     SegmentReceiver ◄────────────────────┘
                          │
                          ├── utteranceService.save()
                          │
                     WebSocketBroadcaster
                          │
Client ◄──WS message──────┘
```

핵심 차이: `Consumer` 콜백이 하던 역할이 **SegmentReceiver 컨트롤러 + WebSocketBroadcaster**로 분리된다. WebSocketHandler는 더 이상 전사 결과 처리를 직접 하지 않는다.

---

## 4. 분산 세션 관리 전략

### 4.1 전략 A: 1:1 고정 라우팅 (권장 — Sprint 1-2)

**핵심 아이디어**: app-debate 인스턴스와 infra-stt 인스턴스 간에 세션 단위로 고정 라우팅을 설정한다.

```
Client ──WS──► LB ──► app-debate (JVM A1)
                          │
                          │  세션 시작 시 infra-stt 인스턴스 배정
                          │  이후 해당 인스턴스로 고정 라우팅
                          ▼
                   LB / Service Discovery
                          │
                      infra-stt (JVM B1)  ◄── session-X 담당
                      infra-stt (JVM B2)  ◄── session-Y 담당
```

**흐름**:
1. 클라이언트가 app-debate에 WebSocket 연결 + 세션 생성 요청.
2. app-debate가 infra-stt에 `POST /sessions/{id}/start` + callbackUrl 전달 → infra-stt 내부 LB가 least-connection 기준으로 인스턴스 배정.
3. 배정된 infra-stt 인스턴스 ID를 app-debate가 **세션 메타데이터로 기록** (in-memory 또는 Redis).
4. 이후 오디오 청크 HTTP POST는 기록된 인스턴스로 직접 라우팅.
5. 전사 결과는 infra-stt가 callbackUrl로 HTTP POST — 역방향은 별도 채널.

**왜 자연스러운가**:
- 클라이언트 → app-debate: WebSocket 자체가 sticky.
- app-debate → infra-stt: 세션 시작 시 한 번 배정하고, 해당 세션 동안 고정.
- infra-stt → app-debate: callbackUrl로 특정 인스턴스에 직접 전달.

**한계**:
- infra-stt 인스턴스 장애 시 해당 인스턴스의 모든 세션 유실. Azure 연결 자체가 소멸하므로 **재시작**이 필요하다.
- app-debate에서 infra-stt 인스턴스 주소를 알아야 하므로 Service Discovery가 필요하다.

### 4.2 전략 B: Redis 세션 레지스트리 + 고정 라우팅 (Scale-out 대비)

인스턴스 수가 늘어나고 장애 복구 요구가 강해지면, 세션-인스턴스 매핑을 Redis에 저장한다.

```
┌─────────────────────────────────────────────────────────────┐
│                          Redis                              │
│  session-A → { sttInstanceId: "B1", debateInstanceId: "A1", │
│               startedAt: ..., lastActiveAt: ... }           │
│  session-B → { sttInstanceId: "B2", debateInstanceId: "A2", │
│               startedAt: ..., lastActiveAt: ... }           │
└─────────────────────────────────────────────────────────────┘
        ▲               ▲               ▲              ▲
        │               │               │              │
   app-debate A1   app-debate A2   infra-stt B1   infra-stt B2
```

전사 결과 전달도 Redis Pub/Sub으로 전환:
- infra-stt: `PUBLISH "stt:session:{id}"` → app-debate가 해당 채널 `SUBSCRIBE`.
- callbackUrl 방식보다 느슨한 결합. infra-stt가 app-debate 주소를 몰라도 된다.

### 4.3 전략 C: Consistent Hashing 기반 자동 라우팅 (장기)

세션 수가 수백~수천 단위로 커지면, Service Mesh(Envoy/Istio)의 consistent hashing으로 sessionId 기반 자동 라우팅을 적용한다.

```
app-debate ──► Envoy Sidecar ──hash(sessionId)──► infra-stt 인스턴스
```

- 별도 레지스트리 없이 sessionId만으로 라우팅이 결정된다.
- infra-stt 인스턴스 추가/제거 시 최소한의 세션만 재배치된다.
- 인프라 복잡도가 높으므로 규모가 커진 후 도입한다.

### 전략 선택 가이드

| 기준 | A: 1:1 고정 라우팅 | B: Redis 레지스트리 | C: Consistent Hashing |
|------|-------------------|--------------------|-----------------------|
| 동시 세션 수 | ~수십 | ~수백 | 수백~수천 |
| 인프라 복잡도 | 낮음 (Service Discovery) | 중간 (Redis 추가) | 높음 (Service Mesh) |
| 전사 결과 전달 | Webhook callback | Redis Pub/Sub | Pub/Sub + Mesh |
| 장애 복구 | 클라이언트 재연결 | 자동 재시작 트리거 | 자동 재배치 |
| Sprint 1-2 적합성 | O | 부분적 (Redis 이미 도입 시) | X |

---

## 5. 스트리밍 시작 여부에 따른 분기

세션의 생명주기에서 **스트리밍 시작 전후**로 관리 전략이 달라진다.

### 5.1 스트리밍 시작 전 (세션 생성 ~ startStreaming 호출 전)

- 아직 Azure 연결이 없으므로, infra-stt에 대한 라우팅이 불필요하다.
- app-debate 내부에서 세션 메타데이터만 관리하면 된다.
- 어떤 app-debate 인스턴스가 처리해도 무방하다.

### 5.2 스트리밍 중 (startStreaming ~ stopStreaming)

- infra-stt의 특정 인스턴스에 Azure WebSocket 연결이 바인딩된 상태.
- **app-debate → infra-stt 오디오 라우팅이 고정되어야 한다**.
- infra-stt → app-debate 결과 전달은 callbackUrl로 직접 전달되므로 라우팅 문제 없음.

### 5.3 스트리밍 종료 후 (stopStreaming 이후)

- infra-stt의 Azure 연결이 정리된 상태.
- 전사 결과는 이미 DB에 저장되어 있으므로 인스턴스 무관.
- 후속 처리(LLM 보정, 쟁점 추출)는 app-debate 어떤 인스턴스에서든 가능.

```
[세션 생성]──────[startStreaming]──────────────[stopStreaming]──────[후처리]
   │                  │                            │                  │
   │  app-debate만    │  오디오: app-debate→infra-stt 고정            │
   │  (infra-stt 무관)│  결과: infra-stt→app-debate callbackUrl      │
   │                  │  (양쪽 인스턴스 고정)        │  app-debate만    │
   │                  │                            │  (infra-stt 무관)│
```

---

## 6. 장애 시나리오별 대응

분리 배포에서는 장애 시나리오가 더 다양하다. 각 케이스별 대응을 정리한다.

### 6.1 infra-stt 인스턴스 장애

```
infra-stt B1 사망
  → B1이 담당하던 Azure 연결 모두 소멸
  → app-debate의 다음 HTTP POST가 connection refused / timeout
  → app-debate가 클라이언트에 "전사 일시 중단" 알림
  → 새 infra-stt 인스턴스(B3)에 세션 재시작 요청
  → 클라이언트에 "전사 재개" 알림
```

- 이미 DB에 저장된 `Utterance`는 보존된다.
- 재시작 시 마지막 저장 타임스탬프 이후부터 오디오를 다시 보내는 **resume 프로토콜**로 갭을 최소화한다.

### 6.2 app-debate 인스턴스 장애

```
app-debate A1 사망
  → 클라이언트 WS 끊김
  → infra-stt B1의 webhook POST가 connection refused
  → infra-stt B1이 해당 세션의 Azure 연결을 일정 시간 유지 (grace period)
  → 클라이언트 재연결 → LB가 app-debate A2로 라우팅
  → A2가 infra-stt에 콜백 URL 변경 요청 (PUT /sessions/{id}/callback)
  → B1의 기존 Azure 세션에 재연결 (grace period 내라면 Azure 연결 재활용 가능)
```

- grace period 내 재연결 시, Azure 세션을 살릴 수 있어 전사 연속성이 유지된다.
- grace period 초과 시, infra-stt도 Azure 연결을 정리하고 새 세션으로 재시작.

### 6.3 네트워크 단절 (양쪽 서비스 모두 정상)

```
app-debate A1 ──X──► infra-stt B1  (네트워크 단절)
  → app-debate: HTTP POST timeout → 재시도 (exponential backoff)
  → infra-stt: webhook POST timeout → 결과 임시 큐잉 → 재시도
  → 재연결 성공 시: 큐잉된 결과 일괄 전송, 기존 Azure 세션 재활용
  → 재연결 실패 시: 6.1과 동일하게 새 인스턴스에 세션 재시작
```

---

## 7. 분산 환경 처리율 제한 (단일 Azure 계정)

### 7.1 Azure Speech Service 제한 사항

단일 Azure 구독(subscription)에서의 주요 제한:

| 리소스 | 기본 제한 | 비고 |
|--------|----------|------|
| 동시 연결 수 | 100 / region | ConversationTranscriber 기준 |
| 초당 요청 수 | 별도 throttle 없음 | 스트리밍은 연결 기반이므로 RPS 개념이 다름 |
| 오디오 길이 | 연속 전사 무제한 | 단, 15초 무음 시 자동 끊김 가능 |

**infra-stt가 별도 배포 단위이므로**, 처리율 제한은 infra-stt 내부에서 중앙 관리할 수 있다.

### 7.2 분리 배포에서의 처리율 제한 전략

#### 방법 1: infra-stt 내부 집계 (단일 인스턴스 — Sprint 1-2)

infra-stt가 단일 인스턴스라면, `ConcurrentHashMap.size()`로 충분하다.

```java
private static final int MAX_CONCURRENT_SESSIONS = 90;

@Override
public void startStreaming(String sessionId, String callbackUrl) {
    if (sessions.size() >= MAX_CONCURRENT_SESSIONS) {
        throw new CapacityExceededException("동시 세션 한도 초과");
    }
    // ...
}
```

app-debate는 처리율 제한을 알 필요 없다. infra-stt가 거부하면 HTTP 429를 받아 클라이언트에 전달하면 된다.

#### 방법 2: Redis 분산 카운터 (infra-stt 다중화 시)

```
┌─────────────────────────────────────────┐
│              Redis                      │
│  azure:stt:connections = 47  (INCR/DECR)│
│  azure:stt:limit = 90       (여유분 10) │
└─────────────────────────────────────────┘
       ▲               ▲
       │               │
  infra-stt B1 (23)   infra-stt B2 (24)
```

- 실제 한도(100)보다 **여유분을 두고 설정** (90). 네트워크 지연으로 카운터와 실제 연결 수에 순간 차이가 생길 수 있다.
- 인스턴스 비정상 종료 시 `DECR`이 호출되지 않을 수 있다 → **TTL 기반 자동 만료** 또는 주기적 reconciliation 필요.

### 7.3 권장 조합 (Sprint 단계별)

**Sprint 1-2**: infra-stt 단일 인스턴스 → 로컬 카운터로 충분.

**Sprint 3+**: infra-stt 다중화 시 → Redis 분산 카운터 + heartbeat 기반 stale 카운터 정리.

**장기**: Azure 구독을 region별로 분리하거나, 멀티 구독으로 한도 자체를 확장.

---

## 8. 정리 및 의사결정 로그

| 항목 | Sprint 1-2 결정 | Scale-out 시 전환 |
|------|-----------------|-------------------|
| 배포 단위 | app-debate / infra-stt **별도 JVM** | 동일 |
| 오디오 전송 (→) | **HTTP POST** (청크 단위, 202 ACK) | 동일 |
| 전사 결과 (←) | **Webhook callback** (infra-stt → app-debate HTTP POST) | Redis Pub/Sub |
| 세션 관리 (→) | **HTTP REST** (start/stop/status) | 동일 |
| Consumer 콜백 대체 | `SegmentReceiver` 내부 컨트롤러 + `WebSocketBroadcaster` | 동일 |
| 세션 매핑 저장소 | app-debate 내 in-memory (infra-stt 인스턴스 주소 기록) | Redis 세션 레지스트리 |
| 라우팅 (Client→app-debate) | WebSocket 자체 sticky | LB sticky session |
| 라우팅 (app-debate→infra-stt) | 세션 시작 시 1:1 고정 | Redis lookup + consistent hashing |
| 장애 복구 | 클라이언트 재연결 → 새 세션 | Redis 기반 세션 재시작 트리거 + grace period 재활용 |
| 처리율 제한 | infra-stt 로컬 `sessions.size()` 체크 | Redis 분산 카운터 (INCR/DECR + TTL) |
| Azure 연결 관리 | infra-stt 인스턴스 로컬 (불변) | infra-stt 인스턴스 로컬 (불변) |
| 스케일링 정책 | app-debate: WS 세션 수 기준 / infra-stt: Azure 연결 수 기준 | 동일 (각각 독립 Auto Scaling) |
