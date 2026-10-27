#pragma once
#include <algorithm>
#include <cmath>

namespace micplugin {

// Shared amplitude-only boost and peak limiter; no EQ, pitch shift or allocations.
class MicrophoneGain {
public:
    static constexpr float kMaxBoostDb = 36.0f;
    static constexpr float kPeakLimit = 0.98f;
    void process(float* samples, int frames, float boostDb, float sampleRate) {
        if (frames <= 0) return;
        const float rate = std::max(sampleRate, 1.0f);
        const float target = std::pow(10.0f, std::clamp(boostDb, 0.0f, kMaxBoostDb) / 20.0f);
        const float smoothing = 1.0f - std::exp(-1.0f / (0.01f * rate));
        float peak = 0.0f;
        for (int i = 0; i < frames; ++i) {
            gain_ += smoothing * (target - gain_);
            samples[i] *= gain_;
            peak = std::max(peak, std::abs(samples[i]));
        }
        // The previous Lite limiter now protects both versions instead of hard clipping.
        const float required = peak > kPeakLimit ? kPeakLimit / peak : 1.0f;
        if (required < limiter_) limiter_ = required;
        else limiter_ += (1.0f - std::exp(-frames / (rate * 0.08f))) * (required - limiter_);
        for (int i = 0; i < frames; ++i) samples[i] *= limiter_;
    }
private:
    float gain_ = 1.0f;
    float limiter_ = 1.0f;
};
}
