#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <cmath>
#include "llama.h"

struct Engine {
    llama_model * model = nullptr;
    llama_context * ctx = nullptr;
    const llama_vocab * vocab = nullptr;
    std::mutex mutex;
};

static std::string jstr(JNIEnv * env, jstring s) {
    if (!s) return {};
    const char * p = env->GetStringUTFChars(s, nullptr);
    std::string out = p ? p : "";
    if (p) env->ReleaseStringUTFChars(s, p);
    return out;
}

static void cleanup(Engine * e) {
    if (!e) return;
    if (e->ctx) llama_free(e->ctx);
    if (e->model) llama_model_free(e->model);
    delete e;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_mgai_app_LocalInferenceBridge_createEngine(JNIEnv * env, jclass, jstring modelPath, jint contextSize, jint threads) {
    std::string path = jstr(env, modelPath);
    if (path.empty()) return 0;

    ggml_backend_load_all();
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    llama_model * model = llama_model_load_from_file(path.c_str(), mp);
    if (!model) return 0;

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = contextSize > 256 ? (uint32_t) contextSize : 2048;
    cp.n_batch = 512;
    cp.n_threads = threads > 0 ? threads : 4;
    cp.n_threads_batch = cp.n_threads;
    cp.no_perf = false;

    llama_context * ctx = llama_init_from_model(model, cp);
    if (!ctx) {
        llama_model_free(model);
        return 0;
    }

    Engine * e = new Engine();
    e->model = model;
    e->ctx = ctx;
    e->vocab = llama_model_get_vocab(model);
    return reinterpret_cast<jlong>(e);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_mgai_app_LocalInferenceBridge_generateNative(JNIEnv * env, jclass, jlong handle, jstring promptJ, jint maxTokens, jfloat temperature) {
    Engine * e = reinterpret_cast<Engine *>(handle);
    if (!e || !e->model || !e->ctx || !e->vocab) return env->NewStringUTF("Engine hazır değil.");
    std::lock_guard<std::mutex> lock(e->mutex);

    std::string prompt = jstr(env, promptJ);
    if (prompt.empty()) return env->NewStringUTF("");

    llama_memory_clear(llama_get_memory(e->ctx), true);

    int n_prompt = -llama_tokenize(e->vocab, prompt.c_str(), (int32_t)prompt.size(), nullptr, 0, true, true);
    if (n_prompt <= 0) return env->NewStringUTF("Prompt tokenize edilemedi.");
    std::vector<llama_token> tokens((size_t)n_prompt);
    if (llama_tokenize(e->vocab, prompt.c_str(), (int32_t)prompt.size(), tokens.data(), (int32_t)tokens.size(), true, true) < 0)
        return env->NewStringUTF("Prompt tokenize edilemedi.");

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t)tokens.size());
    if (llama_decode(e->ctx, batch) != 0) return env->NewStringUTF("Prompt decode hatası.");

    llama_sampler_chain_params scp = llama_sampler_chain_default_params();
    llama_sampler * sampler = llama_sampler_chain_init(scp);
    float temp = temperature > 0.01f ? temperature : 0.7f;
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temp));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(0xC0FFEEu));

    std::string output;
    int limit = maxTokens > 0 ? maxTokens : 256;
    for (int i = 0; i < limit; ++i) {
        llama_token tok = llama_sampler_sample(sampler, e->ctx, -1);
        if (llama_vocab_is_eog(e->vocab, tok)) break;

        char buf[512];
        int n = llama_token_to_piece(e->vocab, tok, buf, sizeof(buf), 0, true);
        if (n > 0) output.append(buf, (size_t)n);

        llama_batch next = llama_batch_get_one(&tok, 1);
        if (llama_decode(e->ctx, next) != 0) break;
    }

    llama_sampler_free(sampler);
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_mgai_app_LocalInferenceBridge_destroyEngine(JNIEnv *, jclass, jlong handle) {
    cleanup(reinterpret_cast<Engine *>(handle));
}
