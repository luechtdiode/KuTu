Spezifikation der Zusammenstellung der Turnerriegen in Geräteriegen und Durchgängen
===================================================================================

## Konzepte

### Turnerriege: 

Ist die kleinste Einheit, die im Wettkmapf bewegt werden kann. Sie besteht aus einem oder mehreren Turnern mit 
gemeinsamen Gruppierungsmerkmalen (z.B. Alter, Geschlecht, Verein, Kategorie).

### Geräteriege:

Ist eine Gruppe von Turnerriegen, die gemeinsam an einem Gerät starten.
Die Startgeräte ergeben sich aus den mitgegebenen Wertungen (Set referenzierter Diszipline) und den geschlechtsspezifischen Regeln in der WettkampfDisziplin hinterlegt.
Die Reihenfolge der Startgeräte ergibt sich aus den mitgegebenen Wertungen in der referenzierten WettkampfDisziplin hinterlegt.
Ein Sonderfall ist der Barren (siehe bestehende Sonderbehandlungen im Code). Die Barrenlogik soll beibehalten werden.

### Durchgang:
synonyme: Geräteriegen (Liste von Geräteriege)
Ist eine Phase des Wettkampfs, in der ausgehend vom Startgerät die Geräteriegen Gerät um Gerät traversieren und ihre Übungen turnen.

### Durchganggruppe

Ist eine Gruppe von Durchgängen, die parallel stattfinden. Alle Durchgänge einer Durchganggruppe starten gleichzeitig am Startgerät.

Es ist nicht zwingend notwendig, dass die zusammengefassten Durchgänge dieselben Startgeräte haben, da die Startgeräte sich aus 
den mitgegebenen Wertungen ergeben und nicht von der Gruppierung der Durchgänge abhängig sind. Es ist aber möglich, d
ass alle Durchgänge einer Durchganggruppe dieselben Startgeräte haben, wenn die zusammengefassten Kategorien dieselben 
Startgeräte haben.

Die in einer Durchganggruppe zusammengefassten Durchgänge müssen disjunkte Kategorien haben (z.B. 1 Durchgang mit Kategorie 
A, 1 Durchgang mit Kategorie B, etc.). Es dürfen also nicht mehrere Durchgänge mit derselben Kategorie in einer Durchganggruppe 
zusammengefasst werden.

Die Namensgebung soll sich an den zusammengefassten Kategorien orientieren 
(z.B. "Abteilung 1 (K1, K2, K3)", "Abteilung 2 (K1, K2, K3)", etc.).

Die Zuordnung eines Durchgangs in eine Durchgangsgruppe erfolg über das Feld `title` der Durchgangs-Entität. Durchgänge
mit dem selben Titel werden in die selbe Durchgangsgruppe eingeordnet. 

### Gruppierungsmerkmale

Sind die Merkmale, die zur Gruppierung der Turnerinnen und Turner in Turnerriegen verwendet werden. Dazu gehören:
- Geschlecht
- Alter/Jahrgang
- Kategorie/Programm
- Verein

### Einteilungsregeln

1) Gruppierungsregel: Es gibt Wettkampfspezifische Gruppierungsregeln, die anhand der Gruppierungsmerkmale der 
   Turner in Turnriegen zuteilen (implementiert in ATTGorouper, JGClubGrouper, SexDevideRule, RiegenGrouper, 
   KuTuGeTuGrouper). Zudem kann parametriert werden, ob die Gruppierung nach Geschlecht erfolgen soll oder nicht (SexDevideRule).
2) Handhabung der Geschlechtertrennung:
   * GemischteRiegen => Geräteriege darf sowohl männliche als auch weibliche Turner enthalten. Es soll versucht werden, die Turnerinnen und Turner möglichst gleichmäßig auf die Geräte zu verteilen, um eine ausgewogene Verteilung zu erreichen.
   * GemischterDurchgang => Pro Geräteriege immer nur ein Geschlecht vertreten. Im Durchgang dürfen aber beide Geschlechter vertreten sein.
   * GetrennteDurchgaenge => Die Geschlechtertrennung erfolgt auf Durchgangsebene.
3) Wenn eine parametrisierte Maximalgröße (Anzahl Turnerinnen und Turner, die pro Gerät maximal eingeteilt werden darf) überschritten wird,
   muss ein zusätzlicher Durchgang durchgeführt werden. Ist der Parameter 0 (nicht gesetzt), so ist die Maximalgrösse automatish so berechnet, dass alles in einem Durchgang eingeteilt werden kann.
4) Wenn es in einem Durchgang zu wenig Geräteriegen gibt, müssen leere Geräteriegen zum Startgerät hinzugefügt werden.

### Optimierungsregeln

1) Unter Berücksichtigung der vorherigen gruppierungs-/trennungs-Regeln, sollen die Turnerinnen und Turner aus dem selben Verein möglichst in der selben Geräteriege starten.
2) In einem Durchgang sollen möglichst an jedem Gerät gleich viele Turnerinnen und Turner starten.
3) Die Priorisierung ist Optimierung 1, dann Optimierung 2., kann aber auch iterativ optimiert werden, bis keine nennenswerte Qualitätsverbesserung mehr möglich ist.

## Vorgang der Einteilung

1) Es wird ein Set von WertungView übergeben, welche die Gerätewertungen verknüpft mit den Turnerinnen und Turner, die Gruppierungsmerkmale 
   (Alter, Geschlecht, Verein, Kategorie) und die mit den Kategorien verbundenen Startgeräte enthalten.
2) Dann werden basierend auf den Einteilungs- und Optimierungsregeln:
   1) die Turnriegen anhand von Gruppierungskriterien zusammengestellt, 
   2) die Turnerriegen in Geräteriegen eingeteilt und 
   3) die Geräteriegen in Durchgänge eingeteilt.

Die Einteilungsregeln stehen über den Optimierungsregeln, da sie zwingend eingehalten werden müssen, während 
die Optimierungsregeln nur als Richtlinien dienen, um die Einteilung zu verbessern.


## Probleme

Der aktuelle Mechanismus hat Schwächen und ist zu komplex und nicht wartungsfreundlich.

Sobald aufgrund der Teilnehmerzahl mehrere Durchgänge notwendig werden, sollten die dadurch zusätzlich erstellten 
Durchgänge in Durchganggruppen zusammengefasst werden. Die Logik dazu wäre folgende:

1) In einer Durchganggruppe dürfen nur Durchgänge zusammengefasst werden, die disjunkte Kategorien 
   haben (z.B. 1 Durchgang mit Kategorie A, 1 Durchgang mit Kategorie B, etc.). Es dürfen also nicht mehrere Durchgänge 
   mit der selben Kategorie in einer Durchganggruppe zusammengefasst werden.
2) Die Namensgebung soll sich an den zusammengefassten Kategorien orientieren (z.B. "Durchganggruppe Kategorie A-B-C",
   "Durchganggruppe Kategorie D-E-F", etc.).

## Vorschlag

Input/Ouptutmapping: Liste von WertungView liefert den Hauptinput, welcher am Ende modifiziert wieder ausgegeben wird.
1. Phase: Einteilung der Turnerinnen und Turner in Turnerriegen, basierend auf den Gruppierungskriterien (Alter, Geschlecht, Verein, Kategorie).
2. Phase: Einteilung der Turnerriegen in Geräteriegen, basierend auf den Einteilungsregeln (1-3).
3. Phase: Einteilung der Geräteriegen in Durchgänge, basierend auf den Einteilungsregeln (4-5).
4. Phase: Einteilung der Durchgänge in Durchganggruppen, basierend auf den Einteilungsregeln (1-2).

Öffentliche Schnittstelle: `ch.seidel.kutu.squad.DurchgangBuilder.suggestDurchgaenge`, sowie die diversen Grouper 
Implementierungen müssen erhalten bleiben.

Die bisherige Schnittstelle soll die Phasen 1-3 abdecken. 
Die Phase 4 (Einteilung der Durchgänge in Durchganggruppen) soll unabhängig aber optional nachgelagert mit dem Output 
der Phase 3 durchgeführt werden können. Der Output der Phase 4 soll die Durchgänge mit einem zusätzlichen Feld `title` versehen, 
welches die Gruppierung der Durchgänge in Durchganggruppen ermöglicht. Wenn keine Gruppierung gesetzt werden muss, ist das Feld `title` gleich dem Durchgangnamen.

Beispiel Datensätze:

| Verein         | K1 Ti | K1 Tu | K2 Ti | K2 Tu | K3 Ti | K3 Tu | K4 Ti | K4 Tu | K5 Ti | K5 Tu | K6 Ti | K6 Tu | K7 Ti | K7 Tu | KD Ti | KD Tu | Total |
|---------------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|
| BTV Lustig    | 1     | 0     | 0     | 2     | 1     | 1     | 0     | 2     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 7     |
| DTV Schnell   | 7     | 1     | 6     | 0     | 5     | 0     | 3     | 0     | 3     | 0     | 2     | 0     | 0     | 0     | 0     | 0     | 27    |
| SV Laut       | 7     | 1     | 5     | 0     | 9     | 1     | 3     | 0     | 2     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 28    |
| TSV Schlau    | 0     | 0     | 1     | 1     | 0     | 0     | 2     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 4     |
| TV Gross      | 3     | 0     | 1     | 1     | 1     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 6     |
| TV Klein      | 6     | 1     | 11    | 2     | 3     | 1     | 3     | 1     | 3     | 0     | 1     | 0     | 0     | 0     | 0     | 0     | 32    |
| TV Dick       | 8     | 2     | 6     | 1     | 4     | 2     | 2     | 0     | 2     | 0     | 2     | 1     | 0     | 0     | 0     | 0     | 30    |
| TV Dünn       | 3     | 0     | 3     | 0     | 3     | 0     | 3     | 0     | 4     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 16    |
| TV Breit      | 7     | 0     | 6     | 0     | 6     | 0     | 2     | 0     | 0     | 0     | 0     | 0     | 1     | 0     | 1     | 0     | 23    |
| TV Schmal     | 3     | 2     | 12    | 0     | 3     | 1     | 5     | 1     | 2     | 0     | 2     | 1     | 0     | 0     | 0     | 0     | 32    |
| TV Hell       | 14    | 0     | 14    | 1     | 1     | 1     | 5     | 0     | 3     | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 39    |
| TV Dunkel     | 2     | 1     | 3     | 0     | 0     | 4     | 7     | 0     | 0     | 1     | 1     | 1     | 0     | 0     | 0     | 0     | 20    |
| TZ Freundlich | 4     | 0     | 6     | 0     | 7     | 0     | 0     | 0     | 2     | 1     | 0     | 1     | 0     | 0     | 0     | 0     | 21    |
| **Total**     | 65    | 8     | 74    | 8     | 43    | 11    | 35    | 4     | 21    | 2     | 8     | 4     | 1     | 0     | 1     | 0     | 285   |
