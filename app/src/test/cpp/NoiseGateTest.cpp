#include "NoiseGate.h"
#include "MicrophoneGain.h"
#include <algorithm>
#include <cassert>
#include <cmath>
#include <iostream>
#include <vector>
using namespace micplugin;
int main() {
    { // Sub-threshold noise is removed without relying on a device AudioEffect.
        NoiseGate gate;
        float x[512]; std::fill_n(x,512,0.0001f);
        gate.process(x,512,-50,5,150,48000);
        for (float v:x) assert(v == 0.0f);
    }
    { // Steady speech opens the original gate and retains its waveform.
        NoiseGate gate;
        std::vector<float> x(48000,0.1f);
        gate.process(x.data(),x.size(),-50,5,150,48000);
        assert(x.front() > 0 && x.front() < 0.001f);
        assert(std::abs(x.back()-0.1f) < 0.00001f);
        std::fill(x.begin(),x.end(),0.0001f);
        gate.process(x.data(),x.size(),-50,5,150,48000);
        assert(x.back() < 0.000001f);
    }
    { // Turning the threshold down preserves a quiet source.
        NoiseGate gate;
        std::vector<float> x(48000,0.001f);
        gate.process(x.data(),x.size(),-70,5,150,48000);
        assert(std::abs(x.back()-0.001f) < 0.000001f);
    }
    { // Peak limiting preserves relative shape instead of clipping loud peaks flat.
        MicrophoneGain gain;
        float warmup[512];
        for(int j=0;j<100;j++) { std::fill_n(warmup,512,0.0001f); gain.process(warmup,512,36,48000); }
        float x[] = {0.1f,-0.2f,0.4f,-0.3f};
        gain.process(x,4,36,48000);
        assert(std::abs(x[2]-0.98f)<0.0001f);
        assert(std::abs(x[1]/x[2]+0.5f)<0.0001f);
        assert(std::abs(x[3]/x[2]+0.75f)<0.0001f);
    }
    std::cout << "4 native gate and peak-limiter checks passed\n";
}
