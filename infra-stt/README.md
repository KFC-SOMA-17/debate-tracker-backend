# infra-stt

벤더 중립 STT(Speech-to-Text) 클라이언트 라이브러리.
현재 Azure AI Speech 어댑터를 제공하며, `app-debate`에서 실시간 토론 음성을 전사할 때 사용한다.

## 모듈 구조

```
infra-stt/src/main/java/com/debatetracker/infra/stt/
├── client/
│   └── SttClient.java            # 벤더 중립 인터페이스
├── adapter/azure/
│   └── AzureAdapter.java         # Azure Speech SDK 구현체
├── config/
│   ├── AzureConfig.java          # Azure 연결 설정 (subscription-key, region 등)
│   ├── AudioProperties.java      # 오디오 포맷 설정 (sample-rate, channels 등)
│   └── SttAutoConfiguration.java # Spring auto-configuration
├── dto/
│   ├── SttSegment.java           # 전사 결과 DTO (시작시간, 종료시간, 화자, 텍스트)
│   └── TranscriberSession.java   # 세션별 Azure 리소스 묶음
└── package-info.java
```

## 핵심 개념

### SttClient 인터페이스

`app-debate`는 이 인터페이스만 바라본다. Azure SDK 타입은 일절 노출되지 않는다.

```java
public interface SttClient {
    void startStreaming(String sessionId, Consumer<SttSegment> onSegment);
    void sendAudioChunk(String sessionId, byte[] pcmData);
    void stopStreaming(String sessionId);
    boolean isConnected(String sessionId);
    String getVendorName();
}
```

### SttSegment

전사 결과 하나를 표현한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `start` | `BigDecimal` | 발화 시작 시각 (초 단위) |
| `end` | `BigDecimal` | 발화 종료 시각 (초 단위) |
| `speaker` | `String` | 화자 ID (Azure의 raw speaker label, 예: `Guest-1`) |
| `content` | `String` | 전사된 텍스트 |

## AzureAdapter 동작 원리

### 전체 흐름

```
app-debate                           AzureAdapter                        Azure Speech Service
    │                                     │                                      │
    │  1. startStreaming(sessionId, cb)    │                                      │
    │ ──────────────────────────────────►  │                                      │
    │                                     │  SpeechConfig 생성                    │
    │                                     │  PushAudioInputStream 생성            │
    │                                     │  ConversationTranscriber 생성         │
    │                                     │  이벤트 리스너 등록                     │
    │                                     │  startTranscribingAsync() ──────────► │  WebSocket 연결
    │                                     │                                      │
    │  2. sendAudioChunk(sessionId, pcm)  │                                      │
    │ ──────────────────────────────────►  │                                      │
    │                                     │  pushStream.write(pcm) ────────────► │  음성 인식 처리
    │       (200ms 간격으로 반복)            │                                      │
    │                                     │                                      │
    │                                     │  ◄──────── transcribed 이벤트 발생     │
    │                                     │  SttSegment 변환                      │
    │  ◄── cb.accept(SttSegment) ──────── │                                      │
    │       (콜백으로 결과 전달)              │                                      │
    │                                     │                                      │
    │  3. stopStreaming(sessionId)         │                                      │
    │ ──────────────────────────────────►  │                                      │
    │                                     │  pushStream.close()                  │
    │                                     │  stopTranscribingAsync() ───────────► │  WebSocket 종료
    │                                     │  리소스 정리 (transcriber, config)      │
```

### 단계별 상세

#### 1단계: startStreaming — 연결 수립

```java
adapter.startStreaming("session-123", segment -> {
    // 전사 결과가 올 때마다 호출됨
    System.out.println(segment.speaker() + ": " + segment.content());
});
```

내부에서 일어나는 일:

1. `AzureConfig`의 subscription-key/region으로 `SpeechConfig` 생성
2. `AudioProperties`의 sample-rate 기반으로 `PushAudioInputStream` 생성 (PCM 16kHz 16bit mono)
3. `ConversationTranscriber` 생성 — Azure의 화자분리(diarization) 기본 지원
4. 4개의 이벤트 리스너 등록:
   - `transcribed` — 최종 인식 결과 수신 시 `SttSegment`로 변환하여 콜백 호출
   - `sessionStarted` — Azure 세션 시작 로그
   - `sessionStopped` — Azure 세션 종료 시 세션 맵에서 제거
   - `canceled` — 인식 취소/에러 시 에러 로그
5. `startTranscribingAsync()` — Azure와 WebSocket 연결 수립
6. 세션을 `ConcurrentHashMap`에 저장

#### 2단계: sendAudioChunk — 오디오 전송

```java
adapter.sendAudioChunk("session-123", pcmBytes);
```

- `PushAudioInputStream.write()`로 PCM 바이트를 Azure에 push
- 호출 주기: `chunkDurationMs` 간격 (기본 200ms)
- 청크 크기: `AudioProperties.chunkSizeBytes()` = sampleRate x 2bytes x channels x chunkDurationMs / 1000
  - 기본값: 16000 x 2 x 1 x 200 / 1000 = **6,400 bytes**

#### 3단계: stopStreaming — 연결 종료

```java
adapter.stopStreaming("session-123");
```

`TranscriberSession.close()`가 다음 순서로 리소스를 정리한다:

1. `pushStream.close()` — 오디오 스트림 닫기
2. `transcriber.stopTranscribingAsync().get()` — Azure 전사 중지 대기
3. `transcriber.close()` — SDK 리소스 해제
4. `audioConfig.close()`
5. `speechConfig.close()`

각 단계에서 예외가 발생해도 다음 단계로 진행한다 (리소스 누수 방지).

### 다중 세션

`ConcurrentHashMap<String, TranscriberSession>`으로 세션별 독립 리소스를 관리한다.
여러 토론 세션을 동시에 처리할 수 있으며, 한 세션의 종료가 다른 세션에 영향을 주지 않는다.

```
sessions = {
    "session-A" → TranscriberSession(transcriber_A, pushStream_A, ...),
    "session-B" → TranscriberSession(transcriber_B, pushStream_B, ...),
}
```

## 설정

`application.yml`에 다음 설정을 추가한다. `SttAutoConfiguration`이 auto-configuration으로 등록되어 있어 `app-debate`에서 별도 `@Import` 없이 자동 로드된다.

```yaml
stt:
  azure:
    enabled: true                          # false면 빈 생성 안 됨
    subscription-key: ${AZURE_STT_KEY}     # Azure Portal에서 발급
    region: koreacentral                   # Azure 리전
    language: ko-KR                        # 인식 언어
    profanity: raw                         # 비속어 필터 (raw = 필터 없음)
    silence-timeout-ms: 500                # 묵음 감지 임계값 (ms)
  audio:
    sample-rate: 16000                     # PCM 샘플레이트 (Hz)
    channels: 1                            # 모노
    encoding: LINEAR16                     # 16-bit PCM
    chunk-duration-ms: 200                 # 청크 전송 간격 (ms)
```

## 테스트

```bash
# 단위 테스트 (Mock 기반, Azure 연결 없음)
./gradlew :infra-stt:test

# 통합 테스트 실행 (실제 Azure 연동)
# 1. application-test.yml에 subscription-key 설정
# 2. src/test/resources/test-audio.pcm 준비 (16kHz 16bit mono PCM)
# 3. @Disabled 주석 해제 후 실행
./gradlew :infra-stt:test --tests "*.AzureAdapterIntegrationTest"
```

PCM 파일 변환 (ffmpeg):

```bash
ffmpeg -i speech.wav -f s16le -acodec pcm_s16le -ar 16000 -ac 1 test-audio.pcm
```

## app-debate에서 사용하기

`SttClient`가 빈으로 등록되므로 주입만 하면 된다.

```java
@Service
public class DebateStreamingService {

    private final SttClient sttClient;

    public DebateStreamingService(SttClient sttClient) {
        this.sttClient = sttClient;
    }

    public void startTranscription(String sessionId) {
        sttClient.startStreaming(sessionId, segment -> {
            // segment.speaker()  → "Guest-1" (raw speaker label)
            // segment.content()  → "저는 이 의견에 반대합니다"
            // segment.start()    → 12.34 (초)
            // segment.end()      → 15.67 (초)
            // TODO: 화자 → 찬/반 매핑은 app-debate의 labeling 영역에서 처리
        });
    }
}
```
