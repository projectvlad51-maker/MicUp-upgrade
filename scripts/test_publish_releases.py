import json
import subprocess
import unittest
from unittest.mock import patch
import publish_releases as publisher

class PublicationTests(unittest.TestCase):
    def test_catalog_validates_all_fifteen_versions_and_fourteen_apks(self):
        catalog = list(publisher.releases())
        self.assertEqual(15, len(catalog))
        self.assertEqual(14, sum('artifact_ref' in r for r in catalog))

    @patch('publish_releases.subprocess.run')
    def test_forbidden_is_not_treated_as_missing_release(self, run):
        run.return_value = subprocess.CompletedProcess([],1,'','HTTP 403: Forbidden')
        with self.assertRaises(RuntimeError):
            publisher.api('repos/test/releases/tags/test',missing_ok=True)

    @patch('publish_releases.subprocess.run')
    def test_only_not_found_can_start_release_creation(self, run):
        run.return_value = subprocess.CompletedProcess([],1,'','HTTP 404: Not Found')
        self.assertIsNone(publisher.api('repos/test/releases/tags/test',missing_ok=True))
        with self.assertRaises(RuntimeError):
            publisher.api('repos/test/releases/tags/test')

    @patch('publish_releases.subprocess.run')
    def test_visibility_update_sends_boolean_json_and_returns_confirmed_state(self, run):
        run.return_value = subprocess.CompletedProcess([],0,'{"visibility":"public"}','')
        self.assertEqual('public',publisher.api('repos/test','PATCH',{'private':False})['visibility'])
        self.assertIs(False,json.loads(run.call_args.kwargs['input'])['private'])
        self.assertIn('--input',run.call_args.args[0])

if __name__ == '__main__':
    unittest.main()
