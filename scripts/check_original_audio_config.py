#!/usr/bin/env python3
"""Check the current transport against the user-supplied 1.0.6 source configuration."""
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REFERENCE = 'a6e87db3e12cbf3d79d695c1a28d5c2fcaed88a8'
NATIVE = 'app/src/main/cpp/AudioEngine.cpp'

def stream_settings(source):
    body = source.split('bool AudioEngine::openStreams()', 1)[1].split('void AudioEngine::closeStreams()', 1)[0]
    # Only an explicit device choice may override the original default device.
    for line in (
        'if (inputDeviceId_ != oboe::kUnspecified) builder.setDeviceId(inputDeviceId_);',
        'if (inputDeviceId_ != oboe::kUnspecified) builder.setDeviceId(oboe::kUnspecified);',
    ):
        body = body.replace(line, '')
    return [(name, re.sub(r'\s+', '', args)) for name, args in
            re.findall(r'(?:\.|->)(set[A-Z]\w*)\(([^;\n]*?)\)', body)]

def validate(reference, current):
    expected, actual = stream_settings(reference), stream_settings(current)
    if actual != expected:
        raise ValueError(f'Audio transport differs from original 1.0.6:\nexpected={expected}\nactual={actual}')

def main():
    original = subprocess.check_output(['git', 'show', REFERENCE+':'+NATIVE], cwd=ROOT).decode()
    validate(original, (ROOT/NATIVE).read_text())
    print('Oboe stream configuration matches original 1.0.6; explicit device selection is the only allowed override.')

if __name__ == '__main__':
    main()
