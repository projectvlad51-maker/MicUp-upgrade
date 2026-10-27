#pragma once
#include <algorithm>
#include <cmath>

namespace micplugin {
// First-order low/high cuts for voice. State belongs to the audio thread.
class VoiceFilters {
public:
    void process(float* samples, int frames, bool highPass, float highHz,
                 bool lowPass, float lowHz, float sampleRate) {
        const float hp = std::exp(-6.283185307f * std::clamp(highHz, 20.0f, 300.0f) / sampleRate);
        const float lp = 1.0f - std::exp(-6.283185307f * std::clamp(lowHz, 3000.0f, 20000.0f) / sampleRate);
        if (highPass != highEnabled_) { highIn_ = highOut_ = 0; highEnabled_ = highPass; }
        if (lowPass != lowEnabled_) { lowOut_ = 0; lowEnabled_ = lowPass; }
        for (int i = 0; i < frames; ++i) {
            float x = samples[i];
            if (highPass) {
                highOut_ = hp * (highOut_ + x - highIn_);
                highIn_ = x;
                x = highOut_;
            }
            if (lowPass) { lowOut_ += lp * (x - lowOut_); x = lowOut_; }
            samples[i] = x;
        }
    }
private:
    float highIn_ = 0, highOut_ = 0, lowOut_ = 0;
    bool highEnabled_ = false, lowEnabled_ = false;
};
}
