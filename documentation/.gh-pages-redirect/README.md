# GitHub Pages redirect

The canonical site lives on GitLab Pages: <https://podcast-server.davinkevin.fr/>.

This folder holds a one-page `index.html` that redirects visitors who land on the legacy URL <https://davinkevin.github.io/Podcast-Server/> to the new site.

## Deploy it once

GitHub Pages serves a single branch per repo. Push this `index.html` to the `gh-pages` branch of the GitHub mirror, then enable Pages from that branch:

```bash
# from a fresh clone or worktree of the repo
git worktree add -B gh-pages /tmp/gh-pages
cp documentation/.gh-pages-redirect/index.html /tmp/gh-pages/
cd /tmp/gh-pages
git add index.html
git commit -m "redirect legacy GitHub Pages URL to GitLab Pages"
git push -u origin gh-pages
cd -
git worktree remove /tmp/gh-pages
```

Then in the GitHub repo settings:

1. **Settings → Pages**
2. **Source**: `Deploy from a branch`
3. **Branch**: `gh-pages` / `(root)` → Save

The existing `task push-to-github` mirror keeps the GitHub `gh-pages` branch in sync with this one afterwards.

## Update the target URL

If the GitLab Pages URL ever changes (e.g. you move to a custom domain), edit `index.html` here, redeploy following the steps above, and update the canonical URL in the VitePress site's `description` if relevant.
