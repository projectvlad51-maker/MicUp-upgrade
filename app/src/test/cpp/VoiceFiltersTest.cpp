#include "VoiceFilters.h"
#include <cassert>
#include <cmath>
#include <iostream>
#include <vector>

std::vector<float> sine(float hz) {
    std::vector<float> x(48000);
    for (int i = 0; i < 48000; ++i) x[i] = 0.1f * std::sin(6.283185307f * hz * i / 48000);
    return x;
}
float rms(const std::vector<float>& x) {
    float sum = 0;
    for (int i = 24000; i < 48000; ++i) sum += x[i] * x[i];
    return std::sqrt(sum / 24000);
}
int main() {
    {
        auto x = sine(1000), original = x;
        micplugin::VoiceFilters filter;
        filter.process(x.data(), x.size(), false, 80, false, 12000, 48000);
        assert(x == original);
    }
    {
        auto low = sine(50), voice = sine(1000);
        micplugin::VoiceFilters a, b;
        a.process(low.data(), low.size(), true, 200, false, 12000, 48000);
        b.process(voice.data(), voice.size(), true, 200, false, 12000, 48000);
        assert(rms(low) < 0.025f && rms(voice) > 0.06f);
    }
    {
        auto high = sine(16000), voice = sine(1000);
        micplugin::VoiceFilters a, b;
        a.process(high.data(), high.size(), false, 80, true, 3000, 48000);
        b.process(voice.data(), voice.size(), false, 80, true, 3000, 48000);
        assert(rms(high) < 0.025f && rms(voice) > 0.06f);
    }
    {
        std::vector<float> dc(48000, 0.1f);
        micplugin::VoiceFilters filter;
        filter.process(dc.data(), dc.size(), true, 80, true, 12000, 48000);
        assert(rms(dc) < 0.00001f);
        auto voice = sine(1000), original = voice;
        filter.process(voice.data(), voice.size(), false, 80, false, 12000, 48000);
        assert(voice == original);
    }
    std::cout << "4 native voice filter checks passed\n";
}
