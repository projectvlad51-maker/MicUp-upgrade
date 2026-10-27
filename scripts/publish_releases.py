#!/usr/bin/env python3
"""Validate saved notes/assets; publish GitHub Releases only with --publish."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
REPO = 'projectvlad51-maker/12'

def run(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, **kwargs)

def api(path, method='GET', data=None, missing_ok=False):
    args = ['gh','api','--method',method,path]
    if data is not None:
        args += ['--input','-']
    response = subprocess.run(args,cwd=ROOT,input=json.dumps(data) if data is not None else None,
        capture_output=True,text=True)
    if response.returncode:
        if missing_ok and 'HTTP 404' in response.stderr:
            return None
        raise RuntimeError('GitHub API '+method+' '+path+' failed: '+response.stderr.strip())
    return json.loads(response.stdout) if response.stdout.strip() else {}

def releases():
    data = json.loads((ROOT / 'docs/release-catalog.json').read_text())
    if len({r['tag'] for r in data}) != len(data):
        raise ValueError('Duplicate release tags')
    for r in data:
        if not (ROOT / r['notes']).is_file():
            raise ValueError('Missing notes: ' + r['notes'])
        if 'artifact_ref' in r:
            blob = subprocess.check_output(['git','show',r['artifact_ref']+':'+r['artifact_name']],cwd=ROOT)
            if hashlib.sha256(blob).hexdigest() != r['sha256'] or len(blob) != r['size']:
                raise ValueError('Asset integrity failed: ' + r['artifact_name'])
        yield r

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--publish',action='store_true',help='Actually create/update GitHub Releases')
    parser.add_argument('--make-public',action='store_true',help='Explicitly make repository public before publication')
    parser.add_argument('--stage-private',action='store_true',help='Prepare releases inside a private repository without claiming public access')
    args = parser.parse_args()
    catalog = list(releases())
    for r in catalog:
        print(r['tag'],'—',r['title'],'—',r.get('artifact_name','source only'))
    if not args.publish:
        print('Validation complete; no network requests or GitHub changes made.')
        return
    if args.make_public:
        try:
            api('repos/'+REPO,'PATCH',{'private':False})
        except RuntimeError as error:
            if not args.stage_private:
                raise
            print('Public visibility update was not permitted:', error)
    visibility = api('repos/'+REPO)['visibility']
    if visibility != 'public' and not args.stage_private:
        raise RuntimeError('Repository is not public; use --make-public when authorized.')
    if visibility != 'public':
        print('Staging inside a private repository; public access is NOT enabled.')
    with tempfile.TemporaryDirectory(prefix='micup-release-') as temp:
        for r in catalog:
            commit = subprocess.check_output(['git','rev-parse','--verify','refs/tags/'+r['tag']+'^{commit}'],cwd=ROOT,text=True).strip()
            payload = {'name':r['title'],'body':(ROOT/r['notes']).read_text(),
                'make_latest':'true' if r['tag'].startswith('lite-') else 'false'}
            existing = api('repos/'+REPO+'/releases/tags/'+r['tag'],missing_ok=True)
            if existing:
                api('repos/'+REPO+'/releases/'+str(existing['id']),'PATCH',payload)
            else:
                # Tags already exist on GitHub. Do not request creation at an older workflow commit.
                payload.update(tag_name=r['tag'],draft=False,prerelease=False)
                api('repos/'+REPO+'/releases','POST',payload)
            if 'artifact_ref' in r:
                asset = Path(temp)/r['artifact_name']
                with asset.open('wb') as stream:
                    run(['git','show',r['artifact_ref']+':'+r['artifact_name']],stdout=stream)
                run(['gh','release','upload',r['tag'],str(asset),'--repo',REPO,'--clobber'])
    print('GitHub release publication complete; repository visibility:',visibility)

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        # No credentials are passed in arguments or included in this diagnostic.
        Path('publication-error.txt').write_text(type(error).__name__+': '+str(error))
        raise
