# TVM-Anforderungskatalog – Abgleich mit KuTu

Dieses Dokument fasst den in Issue #1349 beschriebenen Anforderungskatalog für eine Wettkampf-Software im Geräteturnen Männer zusammen und ordnet ihn dem aktuellen Funktionsumfang von KuTu zu.

Ziel ist eine praktische Einordnung:

* **Bereits in KuTu vorhanden**
* **Mit bestehendem Import-/Export-Weg abbildbar**
* **Noch offen für separate Folgetickets**

## Aktueller Abdeckungsgrad

| Anforderung | Stand in KuTu | Hinweise |
|-------------|---------------|----------|
| Anlage der Wettkämpfe mit WK-Nr., Geräten, 4-Kampf-Varianten | Bereits vorhanden | Wettkämpfe werden in der Desktop-App angelegt und konfiguriert. |
| Registrierung von Turnerinnen/Turnern | Bereits vorhanden | Direkt in der App, über Online-Anmeldungen oder per Import. |
| Registrierung von Mannschaften | Bereits vorhanden | Mannschafts- und Teamranglisten sind vorhanden; Teamregeln werden am Wettkampf hinterlegt. |
| Riegeneinteilung | Bereits vorhanden | Die Riegeneinteilung erfolgt in der Wettkampf-App. |
| Druck von Riegenlisten / Notenblättern | Bereits vorhanden | Die App erzeugt Teilnehmerlisten, Vereinslisten und Notenerfassungsblätter. |
| Eingabe per Handy/Tablet mit zentraler Kontrolle | Bereits vorhanden | Die mobile Resultaterfassung über Wertungsrichter ist als Online-Funktion vorhanden. |
| Ergebnislisten nach WK / Rang mit Detailwertungen | Bereits vorhanden | Einzel- und Teamranglisten, Gruppierungen und veröffentlichbare Online-Ranglisten sind vorhanden. |
| Import von Wettkampfdaten | Bereits vorhanden | Athleten können per Excel-Zwischenablage oder per CSV in den Wettkampf übernommen werden. |
| Export kompletter Wettkampfdaten | Bereits vorhanden | Ein Wettkampf kann als ZIP exportiert und wieder importiert werden. |
| Export für externe Weiterverarbeitung | Teilweise vorhanden | Ranglisten stehen als HTML/Druck und als publizierbare Online-Ausgabe zur Verfügung; für weitergehende Excel-/Word-Prozesse sind separate Exporttickets sinnvoll. |
| Überprüfung des Startmarkenstatus im GymNet | Offen | Dafür gibt es aktuell keine bekannte Schnittstellen-Integration in KuTu. |
| Ersatzturner bei größenlimitierten Teamregeln | Offen | Die Teamregeln steuern aktuell das Zustandekommen eines Teams; eine explizite Ersatzturner-Logik ist separat zu spezifizieren. |

## Bereits nutzbare Importwege

### Excel-Zwischenablage

Für die schnelle Übernahme in einen Wettkampf gibt es die Funktion **„Aus Excel einfügen ...“**.

### CSV-Import

Für den CSV-Import ist bereits ein konkretes Format hinterlegt. Verarbeitet werden aktuell insbesondere diese Spalten:

* `VEREIN`
* `VERBAND`
* `RLZ_TZ`
* `VERBAND_RLZ`
* `NAME_TURNER`
* `VORNAME_TURNER`
* `JG_TURNER`
* `WETTKAMPF_TEIL`

Die Riegeneinteilung bleibt dabei bewusst in der Wettkampf-App. Zusätzliche Einteilungsfelder wie Riege, Durchgang oder Startgerät sollten bei Bedarf in separaten Tickets beschrieben werden.

## Bereits nutzbare Online-Funktionen

Wenn ein Wettkampf auf den Server hochgeladen wurde, können bereits folgende Abläufe genutzt werden:

* Online-Anmeldungen für Vereine
* Mobile Resultaterfassung über Handy/Tablet
* Online-Publikation von Ranglisten
* Interaktive Teilnehmer- und Startlisten

## Sinnvolle Folgetickets

Für den TVM-Katalog bieten sich als klar getrennte Folge-Themen insbesondere an:

1. Export von Anmelde- und Resultatdaten in einem exakt definierten Excel-/CSV-Format
2. Fachliche Definition für Ersatzturner in Mannschaftswettkämpfen
3. Mögliche GymNet-Anbindung zur Startmarkenprüfung
4. Optionaler Import zusätzlicher Einteilungsinformationen wie Riege, Durchgang und Startgerät
