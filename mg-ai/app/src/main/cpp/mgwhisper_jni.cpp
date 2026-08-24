#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <cstdint>
#include <algorithm>
#include "whisper.h"

struct MgWhisperEngine {
    whisper_context * ctx = nullptr;
};

static bool read_pcm16_wav(const std::string & path, std::vector<float> & pcm) {
    std::ifstream in(path, std::ios::binary);
    if (!in) return false;
    char header[44];
    in.read(header, 44);
    if (in.gcount() != 44) return false;
    if (std::string(header, header + 4) != "RIFF" || std::string(header + 8, header + 12) != "WAVE") return false;
    auto le16 = [](const char * p) -> uint16_t {
        return (uint16_t)(uint8_t)p[0] | ((uint16_t)(uint8_t)p[1] << 8);
    };
    auto le32 = [](const char * p) -> uint32_t {
        return (uint32_t)(uint8_t)p[0] | ((uint32_t)(uint8_t)p[1] << 8) |
               ((uint32_t)(uint8_t)p[2] << 16) | ((uint32_t)(uint8_t)p[3] << 24);
    };
    const uint16_t format = le16(header + 20);
    const uint16_t channels = le16(header + 22);
    const uint32_t sampleRate = le32(header + 24);
    const uint16_t bits = le16(header + 34);
    const uint32_t dataSize = le32(header + 40);
    if (format != 1 || channels != 1 || sampleRate != WHISPER_SAMPLE_RATE || bits != 16) return false;
    std::vector<int16_t> raw(dataSize / 2);
    in.read(reinterpret_cast<char *>(raw.data()), dataSize);
    const size_t got = (size_t)in.gcount() / 2;
    pcm.resize(got);
    for (size_t i = 0; i < got; ++i) pcm[i] = std::max(-1.0f, std::min(1.0f, raw[i] / 32768.0f));
    return !pcm.empty();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mgai_app_WhisperInferenceBridge_create(JNIEnv * env, jclass, jstring modelPath) {
    if (!modelPath) return 0;
    const char * p = env->GetStringUTFChars(modelPath, nullptr);
    whisper_context_params cp = whisper_context_default_params();
    cp.use_gpu = false;
    whisper_context * ctx = whisper_init_from_file_with_params(p, cp);
    env->ReleaseStringUTFChars(modelPath, p);
    if (!ctx) return 0;
    auto * engine = new MgWhisperEngine();
    engine->ctx = ctx;
    return reinterpret_cast<jlong>(engine);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mgai_app_WhisperInferenceBridge_transcribe(JNIEnv * env, jclass, jlong handle, jstring wavPath, jstring language) {
    auto * engine = reinterpret_cast<MgWhisperEngine *>(handle);
    if (!engine || !engine->ctx || !wavPath) return env->NewStringUTF("");
    const char * wp = env->GetStringUTFChars(wavPath, nullptr);
    std::vector<float> pcm;
    bool ok = read_pcm16_wav(wp, pcm);
    env->ReleaseStringUTFChars(wavPath, wp);
    if (!ok) return env->NewStringUTF("");

    std::string lang = "tr";
    if (language) {
        const char * lp = env->GetStringUTFChars(language, nullptr);
        if (lp && *lp) lang = lp;
        env->ReleaseStringUTFChars(language, lp);
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.no_context = true;
    params.single_segment = false;
    params.language = lang.c_str();
    params.n_threads = 4;

    if (whisper_full(engine->ctx, params, pcm.data(), (int)pcm.size()) != 0) return env->NewStringUTF("");
    std::string out;
    const int n = whisper_full_n_segments(engine->ctx);
    for (int i = 0; i < n; ++i) {
        const char * t = whisper_full_get_segment_text(engine->ctx, i);
        if (t) out += t;
    }
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mgai_app_WhisperInferenceBridge_destroy(JNIEnv *, jclass, jlong handle) {
    auto * engine = reinterpret_cast<MgWhisperEngine *>(handle);
    if (!engine) return;
    if (engine->ctx) whisper_free(engine->ctx);
    delete engine;
}
