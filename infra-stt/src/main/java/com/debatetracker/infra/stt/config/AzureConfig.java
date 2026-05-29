package com.debatetracker.infra.stt.config;

import com.microsoft.cognitiveservices.speech.OutputFormat;
import com.microsoft.cognitiveservices.speech.ProfanityOption;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.SpeechConfig;

public record AzureConfig(
        boolean enabled,
        String subscriptionKey,
        String region,
        String language,
        String profanity,
        int silenceTimeoutMs
) {

    public SpeechConfig toSpeechConfig() {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);
        speechConfig.setSpeechRecognitionLanguage(language);
        speechConfig.setProfanity(ProfanityOption.Raw);
        speechConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, String.valueOf(silenceTimeoutMs));
        speechConfig.enableDictation();
        speechConfig.setOutputFormat(OutputFormat.Detailed);
        return speechConfig;
    }

}
