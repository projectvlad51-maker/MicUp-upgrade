#pragma once
#include <algorithm>
#include <cmath>

namespace micplugin {
class MicrophoneBoost {
public:
    void process(float* samples, int frames, float db, float sampleRate) {
        if (db <= 0.0f) { gain_ = 1.0f; limiter_ = 1.0f; return; }
        const float target = std::pow(10.0f, std::clamp(db, 0.0f, 36.0f) / 20.0f);
        const float rate = std::max(sampleRate, 1.0f);
        const float smoothing = 1.0f - std::exp(-1.0f / (0.01f * rate));
        float peak = 0.0f;
        for (int i = 0; i < frames; ++i) {
            gain_ += smoothing * (target - gain_);
            samples[i] *= gain_;
            peak = std::max(peak, std::abs(samples[i]));
        }
        const float needed = peak > 0.98f ? 0.98f / peak : 1.0f;
        if (needed < limiter_) limiter_ = needed;
        else limiter_ += (1.0f - std::exp(-frames / (rate * 0.08f))) * (needed - limiter_);
        for (int i = 0; i < frames; ++i) samples[i] *= limiter_;
    }
private:
    float gain_ = 1.0f, limiter_ = 1.0f;
};
}
