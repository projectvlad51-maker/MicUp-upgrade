#include "MicrophoneBoost.h"
#include <cassert>
#include <cmath>
#include <iostream>
#include <vector>
using micplugin::MicrophoneBoost;
int main() {
    { // Zero gain must be an exact bypass, including original full-scale peaks.
        MicrophoneBoost b;
        float x[] = {0,1,-1,0.25f,-2,2};
        b.process(x,6,0,48000);
        assert(x[0]==0 && x[1]==1 && x[2]==-1 && x[3]==0.25f && x[4]==-2 && x[5]==2);
    }
    {
        MicrophoneBoost b;
        std::vector<float> x(48000,0.001f);
        b.process(x.data(),x.size(),12,48000);
        assert(x.front()>0.001f && x.front()<0.00101f);
        assert(std::abs(x.back()-0.00398107f)<0.000001f);
    }
    {
        MicrophoneBoost b;
        float x[512];
        for(int n=0;n<100;n++) {
            for(int i=0;i<512;i++) x[i]=(i%2? -0.4f:0.4f);
            b.process(x,512,36,48000);
            for(float v:x) assert(std::isfinite(v) && std::abs(v)<=0.98001f);
        }
        x[0]=0.3f; x[1]=-0.4f;
        b.process(x,2,0,48000);
        assert(x[0]==0.3f && x[1]==-0.4f);
    }
    {
        auto ratio=[](float frequency) {
            MicrophoneBoost b;
            double input=0,output=0;
            for(int block=0;block<100;block++) {
                float x[512];
                for(int i=0;i<512;i++) {
                    x[i]=0.001f*std::sin(6.28318530718f*frequency*(block*512+i)/48000.0f);
                    if(block>50) input+=x[i]*x[i];
                }
                b.process(x,512,12,48000);
                if(block>50) for(float v:x) output+=v*v;
            }
            return std::sqrt(output/input);
        };
        assert(std::abs(ratio(400)-ratio(3200))<0.002);
        assert(std::abs(ratio(400)-std::pow(10.0,12.0/20.0))<0.002);
    }
    std::cout << "4 microphone boost checks passed\n";
}
