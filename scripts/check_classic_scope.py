#!/usr/bin/env python3
"""Verify that the minimal app derives directly from the uploaded 1.0.6 tree."""
from pathlib import Path
import subprocess
from check_original_audio_config import validate

ROOT = Path(__file__).resolve().parents[1]
BASE = 'a6e87db3e12cbf3d79d695c1a28d5c2fcaed88a8'
ALLOWED = {
    'build.gradle.kts',
    'src/main/cpp/AudioEngine.cpp', 'src/main/cpp/AudioEngine.h',
    'src/main/java/com/micplugin/audio/AudioEngine.kt',
    'src/main/java/com/micplugin/service/AudioProcessingService.kt',
    'src/main/java/com/micplugin/service/SoftwareLoopback.kt',
    'src/main/java/com/micplugin/ui/AudioViewModel.kt',
    'src/main/java/com/micplugin/ui/Components.kt',
    'src/main/java/com/micplugin/ui/MainScreen.kt',
}

def baseline(name):
    return subprocess.check_output(['git','show',BASE+':app/'+name],cwd=ROOT)

def function(source, marker):
    start=source.index(marker)
    brace=source.index('{',start)
    depth=1; end=brace+1
    while depth:
        depth += (source[end]=='{') - (source[end]=='}');end+=1
    return source[start:end]

def main():
    files=subprocess.check_output(['git','ls-tree','-r','--name-only',BASE,'app'],cwd=ROOT,text=True).splitlines()
    changed=[]
    for original in files:
        relative=original.removeprefix('app/')
        current=ROOT/'classic'/relative
        if not current.is_file(): raise ValueError('Missing original file: '+relative)
        if current.read_bytes()!=baseline(relative): changed.append(relative)
    originals={str(Path(name).relative_to("app")) for name in files}
    extras={"src/main/cpp/MicrophoneBoost.h", "src/main/java/com/micplugin/service/BoostSettings.kt",
            "src/main/java/com/micplugin/service/SpeakerBoostController.kt"}
    licenses={"LICENSE-MicUp.txt", "LICENSE-Apache-2.0.txt", "LICENSE-CLAP.txt", "LICENSE-LV2.txt", "NOTICE.txt"}
    for path in (ROOT/"classic/src/main").rglob("*"):
        if not path.is_file(): continue
        relative=str(path.relative_to(ROOT/"classic"))
        if relative in originals or relative in extras: continue
        if path.parent == ROOT/"classic/src/main/assets" and path.name in licenses: continue
        raise ValueError("Unexpected runtime addition: "+relative)
    unexpected=set(changed)-ALLOWED
    if unexpected: raise ValueError('Unrelated changes to original app: '+str(sorted(unexpected)))
    original=baseline('src/main/cpp/AudioEngine.cpp').decode()
    current=(ROOT/'classic/src/main/cpp/AudioEngine.cpp').read_text()
    validate(original,current)
    for marker in ['bool AudioEngine::start()', 'void AudioEngine::stop()',
                   'void AudioEngine::closeStreams()', 'void AudioEngine::onErrorAfterClose(',
                   'void AudioEngine::processEffectsChain(', 'void AudioEngine::dsp_noiseGate(',
                   'void AudioEngine::dsp_eq(', 'void AudioEngine::dsp_compressor(',
                   'void AudioEngine::dsp_reverb(', 'void AudioEngine::dsp_pitchShift(']:
        if function(original,marker)!=function(current,marker): raise ValueError('Original method changed: '+marker)
    gradle=(ROOT/'classic/build.gradle.kts').read_text()
    if 'shared/audio' in gradle: raise ValueError('Minimal build must not import the newer shared audio implementation')
    print(f'1.0.6 baseline: {len(files)-len(changed)}/{len(files)} original files byte-identical; {len(changed)} scoped edits.')
    print('Stream configuration, recovery, effect algorithms, manifest, modes and original settings are preserved.')

if __name__=='__main__': main()
