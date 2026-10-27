#pragma once
#include <cmath>

namespace micplugin {
// The original MicUp broadband gate, shared by the full and Lite native builds.
class NoiseGate {
public:
    void process(float* buf, int frames, float thresholdDb, float attackMs, float releaseMs, float sampleRate) {
        const float threshold = std::pow(10.0f, thresholdDb * 0.05f);
        const float attack = 1.0f - std::exp(-1.0f / (attackMs * 0.001f * sampleRate));
        const float release = 1.0f - std::exp(-1.0f / (releaseMs * 0.001f * sampleRate));
        for (int i = 0; i < frames; ++i) {
            rms_ = rms_ * 0.995f + buf[i] * buf[i] * 0.005f;
            const float target = std::sqrt(rms_) > threshold ? 1.0f : 0.0f;
            envelope_ += (target > envelope_ ? attack : release) * (target - envelope_);
            buf[i] *= envelope_;
        }
    }
private:
    float envelope_ = 0.0f;
    float rms_ = 0.0f;
};
}
