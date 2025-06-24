# Template für Notenerfassung

## Variablen-Format:
`<Prefix><Name>[.<Kommastellen>]`

Prefix: `$D, $A, $E, $B, $P`

Name: Zeichenkette beginnend mit Buchstaben, enthaltend Buchstaben und Zahlen

Kommastellen 0-3 möglich

Regex: `\$([DEP]{1})([\w]+[\w\d]*)(.([0123]+))?`

### Beispiele:
D-Variablen: Difficulty `$Dname1.1, $Dname2.1`
E-Variablen: Execution  `$Ename1.3, $Ename2.3`
P-Variablen: Penalty    `$Pname.0`

## Math.-Funktionen:
```
Multiplikation *
Division /
Addition +
Subtraktion -
Durchschnitt avg(a,b,...)
Grösster Wert max(a,b,...)
Kleinster Wert min(a,b,...)
```

## Formel:

Mit `^` angehängt, werden die einzelnen Werte der Formel in der Rangliste angezeigt.

### Mehere Gesamtübungsbewertungen

Die drei Teilnoten können n-Mal (n-Übungen) erfasst werden.

Für die Berechnung der Endnote werden die jeweiligen geturnten Endnoten mit einer Aggregat-Funktion
zusammengerechnet (avg, sum, max min) auf Ebene (DNote, ENote oder Endnote).

#### min/max

bedeutet, dass die Teilnoten (D,P, und E) sowie die Endnote von der Übung mit der niedrigsten/höchsten 
Endnote übernommen wird.

#### sum/avg

bedeutet, dass von allen Noten der Durchschnitt aus den einzelnen Übungsbewertungen gerechnet wird.

```
DNote = max($Dname1.1, $Dname2.1)^
PNote = ($Pname.0 / 10)^
ENote = avg(10 - $Ename1.3, 10 - $Ename2.3)
Implizit: Endnote = max(0, min(30, DNote) + min(10, ENote) - PNote)

Aggregate = max (Werte der Übung mit der besten Endnote werden übernommen)

Implizit: Total DNote = Aggregate(max(0, min(30, DNote)), ...)
Implizit: Total ENote = Aggregate(max(0, min(30, ENote)), ...)
Implizit: Total Endnote = Aggregate(max(0, min(30, DNote) + min(10, ENote) - PNote), ...)
```

## Abgeleitet für generisches Eingabe-Formular

1) parsen der Variablen (in Wettkampfdisziplin / in Web-Formular)
2) hinterlegen der Genauigkeit (scale)
   ```
   1. Übung
      D-Noten:
      name1: 0.0
      name2: 0.0

      E-Noten:
      name1: 0.000
      name2: 0.000

      Penalty:
      name:  0
   
   2. Übung
      D-Noten:
      name1: 0.0
      name2: 0.0

      E-Noten:
      name1: 0.000
      name2: 0.000

      Penalty:
      name:  0
   ```
3) erfasste Werte mit Genauigkeit formatieren
4) formatierte Werte in Formel eintragen
5) Formel durch calculator ausrechnen lassen
6) ausgerechnete Teilwerte in JSON in Wertung speichern

Das Template kann im Description-Feld der Wettkampfdisziplin, oder in einem neuen Feld Disziplinübergeifend im Wettkampf 
hinterlegt werden. Wenn beides definiert ist, gewinnt das auf Wettkampfdisziplin-Ebene.

Die erfassten Werte müssen in einem neuen Feld an der Wertung gespeichert werden:
Fomat JSON:

    {
        1: {
            "variablename1": {
                "name": "name1",
                "wert": "0.000",
                "scale": "3"
            }      
            "variablename2": {
                "name": "name2",
                "wert": "0.0",
                "scale": "1"
            }
        }
        2: {
            "variablename1": {
                "name": "name1",
                "wert": "0.000",
                "scale": "3"
            }      
            "variablename2": {
                "name": "name2",
                "wert": "0.0",
                "scale": "1"
            }
        }
    }
