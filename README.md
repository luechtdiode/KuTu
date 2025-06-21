# KuTu
Wettkampf-App für Kunst- und Geräteturnen

[![Build Status Github](https://github.com/luechtdiode/KuTu/actions/workflows/build-pipeline.yml/badge.svg)](https://github.com/luechtdiode/KuTu/actions/workflows/build-pipeline.yml)
[![codecov](https://codecov.io/gh/luechtdiode/KuTu/branch/master/graph/badge.svg?token=MHS3C84GA5)](https://codecov.io/gh/luechtdiode/KuTu)

[![Uptime sharevic.net](https://app.statuscake.com/button/index.php?Track=3358506&Days=1000&Design=2)](http://status.sharevic.net)

[![GitBook](https://img.shields.io/static/v1?message=Documented%20on%20GitBook&logo=gitbook&logoColor=ffffff&label=%20&labelColor=5c5c5c&color=3F89A1)](https://luechtdiode.gitbook.io/turner-wettkampf-app/v/v2r3/)

# Features:

## Desktop-Installation
* Linux
* Mac
* Windows

_[siehe Releases](https://github.com/luechtdiode/KuTu/releases)_
## Wettkampf-Vorbereitung
* Erstellen von Wettkämpfen (KuTu, GeTu und Athletiktest)
* Zuweisen von Turner in eine Kategorie/ein Programm eines Wettkampfs
* Riegeneinteilung (mit Vorbelegungs-Vorschlag)
* Durchgangs-Plaung mit Berechnung der Durchlaufzeiten
* Berechnung des Medallienbedarfs

_[siehe detailierte Dokumentation](https://luechtdiode.gitbook.io/turner-wettkampf-app/wettkampf-vorbereitung)_

## Wettkampf-Durchführung
* Resultat-Erfassung
* Funktionen für eine Vollständigkeits-Kontrolle
* Erstellung von Bestenlisten
* Einzelranglisten Erstellung mit div. Gruppierungskriterien
* Teamranglisten
  
_[siehe detailierte Dokumentation](https://luechtdiode.gitbook.io/turner-wettkampf-app/wettkampf-durchfuhrung)_

## Resultat-Analysen
* Wettkampf-, Vereins- oder Jahresübergreifende Auswertungen
* Speichern von Ranglisten-Einstellungen
* Export (HTML oder Drucker)

_[siehe detailierte Dokumentation](https://luechtdiode.gitbook.io/turner-wettkampf-app/resultatanalysen)_
 
## Pflege der Stammdaten
* Verwaltung von Turnerinnen/Turner mit ihren Vereinen
* Finden und bereinigen von doppelt erfassten Turnerinnen/Turner
* Import und Export von Wettkampfdaten

# Online-Features:
  Die App kann in der Desktop-Version selbst als Server betrieben werden (nützlich in privaten Netzwerken ohne Internet-Zugang).
  
  Einfacher ist die Nutzung des [zentral bereitgestellten Servers](https://kutuapp.sharevic.net), mit dem sich die Desktop-Version standardmässig verbinden kann. Davon gibt es auch eine [Test-/Spiel-Plattform](https://test-kutuapp.sharevic.net) und eine [Beschreibung, wie die Test-/Spiel-Plattform benutzt werden kann](https://github.com/luechtdiode/KuTu/blob/master/docs/HowToSetupTestInstallation.md).

  Es steht ein [Docker-Image](https://hub.docker.com/r/luechtdiode/kutuapp) zur Verfügung, um den Server unter einer eigenen Domäne zu betreiben.
  * Docu [Detailierte Beschreibung zur Installation des Dockerimages](docs/KuTuAppDockerImageDocu.md)
  * Feature [Wettkampfanmeldungen für Vereine](https://luechtdiode.gitbook.io/turner-wettkampf-app/wettkampf-vorbereitung/wettkampf_uebersicht/turneranmeldungen_verarbeiten_online)
  * Feature [Mobile-App für die Resultaterfassung](https://luechtdiode.gitbook.io/turner-wettkampf-app/wettkampf-durchfuhrung/wettkampf-netzwerk-wertungsrichter)
  * Feature Mobile-App für die Resultat-Anzeige
  * Feature API für die Abfrage diverser Listen und Reports zu einem Wettkampf, für die Integration in eigene Plattformen (Homepage, etc)

[Benutzeranleitung (online)](https://luechtdiode.gitbook.io/turner-wettkampf-app/)

# Contributions / Beiträge an der Weiterentwicklung

Die Software ist Opensource und lebt von Ideen und Beiträgen, die von Interessierten beigesteuert werden können.

Folgende Beiträge sind explizit erwünscht. Siehe [CONTRIBUTING.md](CONTRIBUTING.md).
