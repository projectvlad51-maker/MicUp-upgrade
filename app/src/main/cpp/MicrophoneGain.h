#pragma once

#include <algorithm>
#include <cmath>

namespace micplugin {

// Output boost independent of the compressor. No allocation or locking on the audio thread.
class MicrophoneGain {
public:
    static constexpr float kMaxBoostDb = 36.0f;

    void process(float* samples, int frames, float boostDb, float sampleRate) {
        const float target = std::pow(10.0f, std::clamp(boostDb, 0.0f, kMaxBoostDb) / 20.0f);
        const float smoothing = 1.0f - std::exp(-1.0f / (0.01f * std::max(sampleRate, 1.0f)));
        for (int i = 0; i < frames; ++i) {
            gain_ += smoothing * (target - gain_);
            // Keep the final PCM signal within full scale, including effect/plugin peaks.
            samples[i] = std::clamp(samples[i] * gain_, -1.0f, 1.0f);
        }
    }

private:
    float gain_ = 1.0f;
};

} // namespace micplugin
