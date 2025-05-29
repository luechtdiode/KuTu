#!/usr/bin/env bash

ranglisteuri=https://kutuapp.sharevic.net/api/scores/d9eb19ea-a69e-4562-8bc7-5174d5af4952/a676143a-8b69-41c1-884d-9895a20757ca
ranglistetitel=p1u9
curl $ranglisteuri | jq -r '.scoreblocks[].rows[] | [.Rang,.Athlet,.Jahrgang,.Verein,.["Total D"],.["Total E"],.["Total Punkte"]] | @tsv' > $ranglistetitel.tsv