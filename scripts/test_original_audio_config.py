import subprocess
import unittest
from check_original_audio_config import ROOT, REFERENCE, NATIVE, validate

class OriginalConfigTests(unittest.TestCase):
    def setUp(self):
        self.original = subprocess.check_output(['git','show',REFERENCE+':'+NATIVE],cwd=ROOT).decode()
        self.current = (ROOT/NATIVE).read_text()
    def test_transport_matches_the_uploaded_original(self):
        validate(self.original,self.current)
    def test_forcing_an_effect_session_is_detected(self):
        changed = self.current.replace('->setInputPreset(oboe::InputPreset::VoiceCommunication)',
            '->setInputPreset(oboe::InputPreset::VoiceCommunication)\n->setSessionId(oboe::SessionId::Allocate)')
        with self.assertRaises(ValueError): validate(self.original, changed)
    def test_changing_capture_sharing_is_detected(self):
        changed = self.current.replace('SharingMode::Exclusive','SharingMode::Shared')
        with self.assertRaises(ValueError): validate(self.original, changed)
