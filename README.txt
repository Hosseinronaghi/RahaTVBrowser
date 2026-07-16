RahaTVBrowser 0.6.0 R2 cleanup

Delete this obsolete file from the repository:
app/src/main/res/drawable/app_icon.xml

Keep this file:
app/src/main/res/drawable/app_icon.png

Reason:
Android treats both app_icon.xml and app_icon.png as @drawable/app_icon,
so they cannot coexist in the same resource directory.

From the repository root, run:
bash apply-r2-cleanup.sh

Then commit the deletion:
git add -A
git commit -m "Remove obsolete duplicate app icon resource"
git push
