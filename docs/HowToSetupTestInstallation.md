Windows 10:
-----------

Menu start -> "PowerShell" eingeben, Windows PowerShell wählen:

Dann folgenden Inhalt einfügen:

```function set-shortcut {
param ( [string]$SourceLnk, [string]$DestinationPath )
    $WshShell = New-Object -comObject WScript.Shell
    $Shortcut = $WshShell.CreateShortcut($SourceLnk)
    $Shortcut.TargetPath = $DestinationPath
    $Shortcut.Save()
    }


$rmjob = Start-Job -ScriptBlock { rm $Env:USERPROFILE\TurnerWettkampf-App-v2r0 -Recurse }
Wait-Job $rmjob
Receive-Job $rmjob
$cpjob = Start-Job -ScriptBlock { cp $Env:ProgramFiles\TurnerWettkampf-App-v2r0 $Env:USERPROFILE -Recurse }
Wait-Job $cpjob
Receive-Job $cpjob

Set-Content $Env:USERPROFILE\TurnerWettkampf-App-v2r0\app\kutuapp.conf -Value 'app {
  majorversion = "latest-testversion"
  remote {
    schema = "https"
    hostname = "test-kutuapp.sharevic.net"
  }
}'

$DesktopPath = [Environment]::GetFolderPath("Desktop")
set-shortcut "$DesktopPath\Test-TurnerWettkampf-App-v2r0.lnk" "$Env:USERPROFILE\TurnerWettkampf-App-v2r0\TurnerWettkampf-App-v2r0.exe"
```

Linux (Terminal):
-----------------

```
rm -rf ./TurnerWettkampf-App-v2r0

cp -r /opt/TurnerWettkampf-App-v2r0 ./

echo 'app {
  majorversion = "latest-testversion"
  remote {
    schema = "https"
    hostname = "test-kutuapp.sharevic.net"
  }
}' > TurnerWettkampf-App-v2r0/app/kutuapp.conf
```

Dann starten mit:
```~/TurnerWettkampf-App-v2r0/TurnerWettkampf-App-v2r0```

Wenn die App oben rechts die URL `"https://test-kutuapp.sharevic.net:443"` anzeigt, dann hat alles geklappt.
