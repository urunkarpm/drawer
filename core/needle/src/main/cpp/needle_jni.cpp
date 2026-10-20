#include <jni.h>
#include <string>
#include <mutex>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <android/log.h>
#include "needle.h"

#define TAG "NeedleNativeBridge"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::mutex g_mutex;
static void* g_mmap_ptr = nullptr;
static size_t g_mmap_size = 0;
static std::string g_loaded_path;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_urunkarpm_drawer_core_needle_engine_NeedleNativeBridge_nativeLoad(
    JNIEnv* env,
    jobject /* thiz */,
    jstring model_path_str
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (model_path_str == nullptr) {
        LOGE("nativeLoad: model_path is null");
        return JNI_FALSE;
    }

    const char* path_chars = env->GetStringUTFChars(model_path_str, nullptr);
    if (!path_chars) {
        return JNI_FALSE;
    }

    std::string path(path_chars);
    env->ReleaseStringUTFChars(model_path_str, path_chars);

    if (g_mmap_ptr != nullptr && g_loaded_path == path) {
        LOGD("nativeLoad: Model already loaded from %s", path.c_str());
        return JNI_TRUE;
    }

    int fd = open(path.c_str(), O_RDONLY);
    if (fd < 0) {
        LOGE("nativeLoad: Failed to open %s", path.c_str());
        return JNI_FALSE;
    }

    struct stat st{};
    if (fstat(fd, &st) < 0) {
        LOGE("nativeLoad: Failed to fstat %s", path.c_str());
        close(fd);
        return JNI_FALSE;
    }

    size_t file_size = static_cast<size_t>(st.st_size);
    if (file_size == 0) {
        LOGE("nativeLoad: Model file is empty");
        close(fd);
        return JNI_FALSE;
    }

    void* mapped = mmap(nullptr, file_size, PROT_READ, MAP_SHARED, fd, 0);
    close(fd);

    if (mapped == MAP_FAILED) {
        LOGE("nativeLoad: Failed to mmap model file of size %zu", file_size);
        return JNI_FALSE;
    }

    // Unmap previous model if one was mapped
    if (g_mmap_ptr != nullptr && g_mmap_size > 0) {
        munmap(g_mmap_ptr, g_mmap_size);
        g_mmap_ptr = nullptr;
        g_mmap_size = 0;
        g_loaded_path.clear();
    }

    g_mmap_ptr = mapped;
    g_mmap_size = file_size;

    int rc = needle_load(static_cast<const unsigned char*>(g_mmap_ptr), g_mmap_size);
    if (rc < 0) {
        const char* err = needle_last_error();
        LOGE("nativeLoad: needle_load failed (code %d): %s", rc, err ? err : "unknown error");
        munmap(g_mmap_ptr, g_mmap_size);
        g_mmap_ptr = nullptr;
        g_mmap_size = 0;
        return JNI_FALSE;
    }

    g_loaded_path = path;
    LOGI("nativeLoad: Successfully loaded Needle model from %s (size: %zu bytes)", path.c_str(), g_mmap_size);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_urunkarpm_drawer_core_needle_engine_NeedleNativeBridge_nativeInit(
    JNIEnv* env,
    jobject /* thiz */,
    jstring system_prompt_str,
    jstring tools_json_str
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    const char* sys_chars = system_prompt_str ? env->GetStringUTFChars(system_prompt_str, nullptr) : "";
    const char* tools_chars = tools_json_str ? env->GetStringUTFChars(tools_json_str, nullptr) : "[]";

    int rc = needle_init(sys_chars, tools_chars, nullptr);

    if (system_prompt_str) env->ReleaseStringUTFChars(system_prompt_str, sys_chars);
    if (tools_json_str) env->ReleaseStringUTFChars(tools_json_str, tools_chars);

    if (rc < 0) {
        const char* err = needle_last_error();
        LOGE("nativeInit: needle_init failed (code %d): %s", rc, err ? err : "unknown error");
        return JNI_FALSE;
    }

    LOGD("nativeInit: Initialized Needle session with prefix tokens: %d", rc);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_urunkarpm_drawer_core_needle_engine_NeedleNativeBridge_nativeComplete(
    JNIEnv* env,
    jobject /* thiz */,
    jstring prompt_str,
    jint max_tokens
) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (!prompt_str) {
        LOGE("nativeComplete: prompt is null");
        return nullptr;
    }

    const char* prompt_chars = env->GetStringUTFChars(prompt_str, nullptr);
    if (!prompt_chars) {
        return nullptr;
    }

    constexpr int kBufferSize = 65536;
    char buffer[kBufferSize];
    buffer[0] = '\0';

    int rc = needle_complete(
        prompt_chars,
        nullptr, // pcm
        0,       // samples
        max_tokens > 0 ? max_tokens : 128,
        buffer,
        kBufferSize
    );

    env->ReleaseStringUTFChars(prompt_str, prompt_chars);

    if (rc < 0) {
        const char* err = needle_last_error();
        LOGE("nativeComplete: needle_complete failed (code %d): %s", rc, err ? err : "unknown error");
        return nullptr;
    }

    return env->NewStringUTF(buffer);
}

JNIEXPORT void JNICALL
Java_com_urunkarpm_drawer_core_needle_engine_NeedleNativeBridge_nativeReset(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    needle_reset();
    LOGD("nativeReset: Session reset complete");
}

JNIEXPORT jstring JNICALL
Java_com_urunkarpm_drawer_core_needle_engine_NeedleNativeBridge_nativeGetLastError(
    JNIEnv* env,
    jobject /* thiz */
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    const char* err = needle_last_error();
    return env->NewStringUTF(err ? err : "");
}

} // extern "C"
