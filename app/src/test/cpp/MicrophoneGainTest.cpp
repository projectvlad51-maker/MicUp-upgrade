// Run from the repository root:
// g++ -std=c++17 -O3 -ffast-math -Iapp/src/main/cpp app/src/test/cpp/MicrophoneGainTest.cpp -o /tmp/micup-gain-test && /tmp/micup-gain-test
#include "MicrophoneGain.h"
#include <cassert>
#include <iostream>
#include <vector>

using micplugin::MicrophoneGain;

int main() {
    {
        MicrophoneGain gain;
        float samples[] = {0.0f, 0.01f, -0.5f, 1.0f, -1.0f};
        gain.process(samples, 5, 0.0f, 48000.0f);
        assert(samples[0] == 0 && samples[1] == 0.01f && samples[2] == -0.5f);
        assert(samples[3] == 1 && samples[4] == -1);
    }
    {
        MicrophoneGain gain;
        std::vector<float> samples(48000, 0.01f);
        gain.process(samples.data(), samples.size(), 6.0f, 48000);
        assert(std::abs(samples.back() - 0.0199526f) < 0.000001f);
        // A gain change ramps across callback blocks instead of jumping in one sample.
        assert(samples.front() > 0.01f && samples.front() < 0.0101f);
    }
    {
        MicrophoneGain gain;
        std::vector<float> samples(48000, 0.001f);
        gain.process(samples.data(), samples.size(), 100.0f, 48000);
        assert(std::abs(samples.back() - 0.0630957f) < 0.00001f);
        samples.assign(48000, 0.001f);
        gain.process(samples.data(), samples.size(), 0.0f, 48000);
        assert(std::abs(samples.back() - 0.001f) < 0.000001f);
    }
    {
        MicrophoneGain gain;
        std::vector<float> samples(48000);
        for (int i = 0; i < 48000; ++i) samples[i] = i % 2 ? -0.5f : 0.5f;
        gain.process(samples.data(), samples.size(), 36.0f, 48000);
        for (float sample : samples) assert(sample >= -1.0f && sample <= 1.0f);
        assert(samples[47998] == 1.0f && samples[47999] == -1.0f);
    }
    {
        MicrophoneGain gain;
        float last = 0.0f;
        for (int block = 0; block < 100; ++block) {
            float samples[128];
            std::fill_n(samples, 128, 0.001f);
            gain.process(samples, 128, 24.0f, 48000);
            assert(samples[0] >= last);
            last = samples[127];
        }
        assert(std::abs(last - 0.0158489f) < 0.00001f);
    }
    std::cout << "5 native microphone boost checks passed\n";
}
