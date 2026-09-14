# CLAUDE.md – PairingList (Referenz-Repo, nur zum Import der Ausgaben)

Dieses Repo ist ein **flacher Klon eines fremden Java-Tools** (`gundramleifert/pairing-list`),
das Pairing-Listen für das "Liga-Format" (Segel-Bundesliga u. ä.) erzeugt.

**Wir portieren den Java-Code NICHT.** Ziel im SBL-Projekt (`/home/gundram/projects/sbl`,
React + FastAPI): die **Ausgabedateien** aus `events/*` ins Python-Backend importieren.
Diese Datei beschreibt das Datenformat so genau, dass ein Importer daraus gebaut werden kann,
und – als Messlatte für unseren einfachen Fallback-Generator – welche Gütekriterien das Tool
optimiert.

Alle Aussagen unten sind am Java-Quelltext in `src/main/java/gundramleifert/pairing_list/`
belegt (Klassennamen in Klammern).

---

## 1. Tool: Aufbau, Build, Einstiegspunkte

* **Sprache/Build:** Java 17, Maven. `mvn -B package` erzeugt
  `target/pairing-list-1.0-SNAPSHOT-jar-with-dependencies.jar`
  (Manifest-`mainClass` = `gundramleifert.pairing_list.Optimizer`, siehe `pom.xml`).
* **Aufruf:** immer **aus einem Event-Verzeichnis heraus**, z. B.
  `cd events/2026_DSBL-1 && java -cp .../jar-with-dependencies.jar gundramleifert.pairing_list.Optimizer`
  (siehe `.github/workflows/*.yml`).
* **Drei `main`-Klassen:**
  * `Optimizer` – erzeugt eine **neue** Pairing-Liste (2-Phasen-Optimierung, s. §4)
    und schreibt `pairing_list.yml` / `.csv` / `pairing_list*.pdf`.
  * `ReuseSchedule` – lädt eine **vorhandene** `pairing_list.yml`, validiert sie gegen
    `schedule_cfg.yml` und rendert nur die PDFs neu. Seine Prüfungen sind eine gute
    Import-Spezifikation (s. §3, "Invarianten").
  * `PdfExport` – rendert **genau eine** PDF aus `schedule_cfg.yml` + `pairing_list.yml`
    und schreibt sonst nichts: kein CSV, kein `*_teams.yml`, keine Debug-PDF, keine
    Titel-Schleife. Das ist der Weg, den das SBL-Backend nimmt
    (`api/app/pairing/pdf.py`, Story B-3) – Teams und Bootsfarben kommen aus der
    Datenbank, gebraucht wird eine Datei zum Herunterladen. Flags: `-s`, `-pli`, `-dc`
    (optional – ohne Datei gelten die Defaults), `-plp`, `-t <Titel>`, `-d` (Statistik-
    Seiten). Die Prüfung "passt Liste zu Konfiguration" liegt hier
    (`PdfExport.checkFits`), `ReuseSchedule` ruft sie auf.
* **CLI-Flags** (beide Klassen, alle optional; Defaults = die Dateinamen unten):
  `-s schedule_cfg.yml`, `-oc opt_cfg.yml`, `-dc display_cfg.yml`,
  `-pli` (Eingabe-Pairingliste), `-plo pairing_list.yml`, `-plc pairing_list.csv`,
  `-plp pairing_list.pdf`.
* **Konfig-Dateien pro Event** (Default-Namen, alle YAML, UTF-8):
  `schedule_cfg.yml` (Pflicht), `opt_cfg.yml`, `display_cfg.yml`.
* **Config-Klassen benutzen Jackson-YAML mit `FAIL_ON_UNKNOWN_PROPERTIES = false`**
  (`Yaml.dftMapper()`) – unbekannte/tippfehlerhafte Keys werden **stillschweigend ignoriert**
  (z. B. `tablewidth`, `show_schuttle_stat` in fast allen `display_cfg.yml` – die Java-Felder
  heißen `width` bzw. es gibt sie gar nicht).

---

## 2. Datenformat im Detail

### 2.1 `schedule_cfg.yml`  (Klasse `ScheduleConfig`)

Beispiel `events/2026_DSBL-1/schedule_cfg.yml`:

```yaml
flights: 16
titles:
  - "Pairing List - 1. Segel-Bundesliga - MRSV/DTYC"
  - "Pairing List - 1. Segel-Bundesliga - BYC (BA)"
  - "Pairing List - 1. Segel-Bundesliga - Kieler Förde"
  - "Pairing List - 1. Segel-Bundesliga - NRV"
  - "Pairing List - 1. Segel-Bundesliga - PYC"
  - "Pairing List - 1. Segel-Bundesliga - VSaW"
teams:
  - BYC (BA)
  - BYC (BE)
  - BSC
  # ... 18 Einträge
  - WYC
boats:
- color: BLACK
- color: GREEN
- color: DARKBLUE
- color: RED
- color: GRAY
- color: ORANGE
```

| Feld | Typ | Bedeutung / zulässige Werte |
|---|---|---|
| `flights` | int | Anzahl Flights. Bestand: 6–20 (DSBL immer **16**). |
| `teams` | Liste von Strings | Teilnehmer. **Reihenfolge ist maßgeblich**: der Index (0-basiert, in Dateireihenfolge) ist die Team-ID in `pairing_list.yml`. DSBL: 18. Bestand: 7–33. |
| `boats` | Liste | Boote/Bootsfarben in fester Reihenfolge = Bootsnummer 1..N. Zwei Schreibweisen (s. u.). DSBL: 6. Bestand: 4–11. |
| `titles` | Liste von Strings | Eine PDF-Variante pro Titel (bei DSBL = ein Ausrichter/Standort je Eintrag). Bestimmt Anzahl `pairing_list_N.pdf` / `pairing_list_N_teams.yml`. **In Events ab 2024-04 vorhanden.** |
| `title` | String (Singular) | Alte Form (Events 2023 … 2024-03-15). `ScheduleConfig` hat heute **kein** `title`-Feld mehr → beim Import beide Keys akzeptieren, `title` ⇒ Liste mit einem Eintrag, fehlend ⇒ aus Verzeichnisnamen ableiten. |
| `fontsize` | float | Nur in Altformat, für PDF. Ignorierbar. |

**`boats`-Varianten:**
* Neu (2024+): Liste von Mappings `- color: NAME` mit optionalem `name: "..."`.
  Ein Eintrag kann `{}` sein oder nur `name:` haben → `color` = `null`
  (`events/0000-00-00_Test/schedule_cfg.yml`).
* Alt (2023): Liste von **reinen Strings**, z. B.
  ```yaml
  boats:
  - HELLBLAU
  - SCHWARZ
  - ROT
  ```
  (`events/2023-05-12_DSBL-1/schedule_cfg.yml`).
* **Seit 2026-09 auch Hex:** `#1a2b3c` oder kurz `#abc`, mit oder ohne `#`
  (`PdfCreator.parseHexColor`). Damit braucht ein Farbwähler keinen Umweg über
  `additional_colors` mehr. Ein Wert, der **weder** bekannter Name **noch** Hex ist, wird
  nicht mehr als Fehler abgebrochen, sondern in `headercolor_default` gedruckt — mit einer
  Warnung auf stdout, die Boot, Wert und die bekannten Namen nennt. Eine Pairing-Liste, die
  am Morgen einer Veranstaltung ausgegeben wird, darf nicht an einem Farbnamen scheitern.
* `color`-Werte sind ansonsten **freie GROSSBUCHSTABEN-Token**, deutsch/englisch gemischt und
  teils synonym: `PdfCreator.defaultColorMap()` kennt u. a. `BLACK`/`SCHWARZ`, `RED`/`ROT`,
  `GREEN`/`GRUEN`, `BLUE`/`BLAU`, `YELLOW`/`GELB`, `GRAY`/`GREY`/`GRAU`, `PINK`/`LILA`,
  `DARKBLUE`/`DUNKELBLAU`, `HELLBLAU`/`LIGHTBLUE`. Im Bestand vorkommend: BLACK, BLAU, BLUE,
  DARKBLUE, DARK_GRAY, GELB, GRAU, GRAY, GREEN, GREY, GRUEN, HELLBLAU, LIGHTBLUE, LILA,
  ORANGE, PINK, RED, ROT, SCHWARZ, WEISS, WHITE, YELLOW.
  Für den Import: **Rohstring speichern**, `null` zulassen. Farbe ist nur PDF-relevant.

**`init()`-Logik (wichtig für nicht volle Events):**
`getRaces() = ceil(numTeams / numBoats) = (numTeams + numBoats - 1) / numBoats`
= Rennen pro Flight.
`isFull = getRaces()*numBoats == numTeams`. Ist es **nicht** voll, wird `teams` intern mit
leeren Strings `""` **aufgefüllt** bis `getRaces()*numBoats`. Diese Auffüll-Indizes tauchen
als "No-Show"-Plätze in den Pairing-Listen auf (Werte `>= numTeams_real`).
Beispiel: `events/2024-04-06_JSCL-Vilamoura` – 17 Teams, 6 Boote ⇒ 3 Rennen ⇒ 18 Plätze ⇒
1 No-Show mit Index 17 (in `pairing_list.csv` als `18`).

### 2.2 `pairing_list.yml`  (Klassen `Schedule` / `Flight` / `Race`, Ser./Deser. in `Yaml`)

Beispiel `events/2026_DSBL-1/pairing_list.yml` (Kopf):

```yaml
---
flights:
- races:
  - "2,0,15,12,1,13"
  - "7,10,16,14,11,3"
  - "17,4,5,8,6,9"
- races:
  - "0,11,5,13,6,16"
  - "8,1,10,2,17,14"
  - "4,9,7,3,15,12"
# ... insgesamt `flights` Flight-Blöcke
```

* Top-Level `flights:` = Liste; Länge == `schedule_cfg.flights`.
* Jeder Flight: `races:` = Liste von Strings; Länge == `getRaces()` (Rennen/Flight).
* Jeder Race-String: **komma-getrennte Ganzzahlen**, Länge == Anzahl Boote (`boats`).

**Semantik – am Code belegt:**

* **Deserialisierung** (`Yaml.RaceDeserializer`):
  `node.asText().trim().split(",")` → `Byte.parseByte(...)` je Element →
  `new Race(byte[] teams)`. **Keine Umrechnung.** Also: die Zahl ist direkt der
  **0-basierte Index in `schedule_cfg.teams`**.
* **Serialisierung** (`Yaml.RaceSerializer`): schreibt `race.teams` (byte[]) 1:1 als
  `"t0,t1,..."`. Bestätigt: Werte sind 0-basiert (Beispiele enthalten `0`, Maximum
  = `numTeams-1`, z. B. `17` bei 18 Teams).
* **Position im String = Bootsindex.** In `Schedule.writeCSV()` läuft `k` über
  `race.teams` und erzeugt die Spalte `Boat (k+1)`; in `BoatMatrix.add()`:
  `mat[i][race.teams[i]]++` – Index `i` ist das Boot, `race.teams[i]` das Team.
  Also: Position `p` (0..N-1) im String ⇒ Boot `p` ⇒ `boats[p]` aus `schedule_cfg.yml`
  (Bootsnummer `p+1`).
* **Team-Index-Reihenfolge:** `Optimizer.main` mischt `scheduleProps.teams` **nur** wenn
  eine Eingabeliste (`-pli`) geladen wird. Beim normalen Lauf werden die Indizes in
  `pairing_list.yml` **immer** in `schedule_cfg.teams`-Reihenfolge geschrieben.
  `Saver` schreibt `pairing_list.yml`/`.csv` **vor** der Titel-/Rotationsschleife (§2.5).
  ⇒ **Zum Dekodieren immer `schedule_cfg.teams` benutzen, niemals `*_teams.yml`.**
* Innerhalb eines Flights kommt jeder Team-Index (inkl. Auffüll-Indizes) **genau einmal** vor
  (`Util.getRandomFlight` partitioniert alle Plätze). Gute Import-Prüfung.

**Verifizierbares Beispiel (`events/2026_DSBL-1`):**
`teams` = [BYC (BA)=0, BYC (BE)=1, BSC=2, BOH-YC=3, BYCÜ=4, DYC=5, JSC=6, EnSFr=7,
KYC (SH)=8, KaR=9, MSC=10, MYC=11, NRV=12, PYC=13, SMCÜ=14, SVWu=15, SVI=16, WYC=17];
`boats` = [BLACK, GREEN, DARKBLUE, RED, GRAY, ORANGE].
`flights[0].races[0] = "2,0,15,12,1,13"` ⇒
BSC/BLACK, BYC (BA)/GREEN, SVWu/DARKBLUE, NRV/RED, BYC (BE)/GRAY, PYC/ORANGE.
`pairing_list.csv` Zeile 1: `1;1;3;1;16;13;2;14;` = (2+1,0+1,15+1,12+1,1+1,13+1). ✔

**Nicht als Tool-Ausgabe behandeln:** `pairing_list_2021.yml` (in vielen 2023er-DSBL-Ordnern).
Das sind **Alt-Eingabedateien** aus einem Vorjahresformat: andere Einrückung (4 Spaces,
tiefer verschachtelt) und **1-basiert** (Werte `1..18`, keine `0`), z. B.
`events/2023-05-12_DSBL-1/pairing_list_2021.yml`:
```yaml
- races:
    - "2,1,3,4,5,7"
    - "8,9,10,11,6,12"
    - "13,14,15,16,17,18"
```
Beim Import **ignorieren** (Glob `pairing_list_[0-9][0-9][0-9][0-9].yml`).

### 2.3 `pairing_list.csv`  (`Schedule.writeCSV()`)

* **Header** (ohne abschließendes `;`):
  `Race;Flight;Boat 1;Boat 2;...;Boat N`  (N = Anzahl Boote).
* **Datenzeilen** (MIT abschließendem `;`, LF-Zeilenenden, UTF-8; in den Daten nur Ziffern):
  `<race>;<flight>;<t1>;...;<tN>;`
* Teamnummern **1-basiert** (`sb.append(team + 1)`), also `pairing_list.yml`-Index + 1.
  No-Show-Plätze erscheinen als `numTeams_real + 1 .. getRaces()*numBoats`.
* `race` = **globaler laufender Zähler** 1 .. `flights*getRaces()`.
* **`flight`-Spalte ist nicht überall gleich:**
  * Events **ab 2024-03-15**: echte Flight-Nummer, Zeilen flight-major sortiert.
    Beispiel `events/2026_DSBL-1/pairing_list.csv`: `4;2;1;12;6;14;7;17;`
    = `flights[1].races[0]` (+1). ✔
  * Events **mit Datum 2023-**: die 2. Spalte ist die **Rennnummer innerhalb des Flights**
    (1..getRaces(), zyklisch), nicht der Flight. Beispiel
    `events/2023-05-12_DSBL-1/pairing_list.csv`:
    ```
    1;1;8;10;18;13;1;4;
    2;2;14;2;11;3;6;15;
    3;3;9;7;12;5;16;17;
    4;1;15;14;12;8;16;1;   <- entspricht flights[1].races[0] (+1), nicht Flight 1
    ```
* **Empfehlung:** `pairing_list.yml` ist die Quelle der Wahrheit. Die CSV ist redundant;
  falls überhaupt genutzt, den Flight aus der Zeilenreihenfolge rekonstruieren
  (`flight = race_index // getRaces()`), nicht aus der `flight`-Spalte.

### 2.4 `opt_cfg.yml`  (`OptimizationConfig` / `OptMatchMatrixConfig` / `OptBoatConfig`)

Beispiel `events/2026_DSBL-1/opt_cfg.yml`:

```yaml
seed: 1240
optMatchMatrix:
  loops: 20000
  individuals: 600
  swapTeams: 200
  earlyStopping: 500
  maxBranches: 1
  saveEveryN: 250
optBoatUsage:
  loops: 20000
  individuals: 2000
  weightStayOnBoat: 2.03
  weightStayOnShuttle: 1.01
  weightChangeBetweenBoats: 20.1
  swapBoats: 400
  swapRaces: 200
  earlyStopping: 1000
  saveEveryN: 250
```

Reine Optimierer-Steuerung (**für den Import irrelevant**, nur als Kontext):

* `seed` – RNG-Seed.
* `optMatchMatrix` (Phase 1 – Gegner-/Begegnungsausgleich):
  `loops`, `individuals` (Populationsgröße), `swapTeams` (Mutationen/Iteration),
  `earlyStopping`, `maxBranches` (Beam-Breite, meist 1), `saveEveryN`, `showEveryN`,
  `factorLessParticipants` (Default 3.01), `factorTeamMissing` (Default 20.01).
* `optBoatUsage` (Phase 2 – Bootszuteilung):
  `loops`, `individuals`, `swapBoats`, `swapRaces`, `earlyStopping`, `saveEveryN`,
  `showEveryN` **plus die Zielgewichte**
  `weightStayOnBoat`, `weightStayOnShuttle`, `weightChangeBetweenBoats`.
* Altformat (2023, `events/2023-05-12_DSBL-1/opt_cfg.yml`): teils identische Struktur,
  in der mitgelieferten Default-Ressource `src/main/resources/optimization_properties_default.yml`
  sind `optMatchMatrix`/`optBoatUsage` **Listen** (`- loops: ...`) und es gibt ein Feld
  `merges`. Beim Import nicht auf eine feste Form verlassen.

### 2.5 `pairing_list_N.pdf`, `pairing_list_N_debug.pdf`, `pairing_list_N_teams.yml`

* Es gibt **eine Variante `N` pro `titles`-Eintrag** (DSBL: `N` = 0..5, ein Ausrichter je `N`).
  Bei nur einem/keinem Titel: `pairing_list.pdf` ohne Suffix.
* `pairing_list_N_debug.pdf` – dasselbe mit zusätzlichen Statistik-Seiten
  (Begegnungs-, Boots-, Shuttle-Verteilung).
* `pairing_list_N_teams.yml` – die **Team-Liste, zyklisch nach links rotiert** ausgegeben,
  z. B. `events/2026_DSBL-1/pairing_list_0_teams.yml` beginnt mit `KYC (SH)` (Index 8),
  `_1_teams.yml` mit `KaR` (Index 9), `_2_teams.yml` mit `MSC` (Index 10) …
  In `Saver.accept()`: für `i>0` wird `scheduleProps.teams` um 1 nach links rotiert und dann
  geschrieben (`_k` = `_0` um weitere `k` rotiert). Da `Saver` das **geteilte Array mutiert**
  und während des Optimierens periodisch (`saveEveryN`) speichert, akkumuliert der
  Rotations-Offset über die Läufe – der **absolute Startpunkt ist nicht an
  `schedule_cfg`-Reihenfolge gekoppelt**, nur der Schritt +1 zwischen aufeinanderfolgenden `N`.
* **Nur ein Druck-/Anzeige-Artefakt.** Nicht importieren, nicht zum Index-Dekodieren nutzen.

### 2.6 `display_cfg.yml`  (`DisplayConfig`) – **nur PDF, für den Import ignorierbar**

**`fontsize` ist seit 2026-09 optional.** Fehlt der Wert, wählt `DisplayConfig.fontsize()`
ihn aus der Zeilenzahl des Blatts (`flights * getRaces()`): bis 42 Zeilen 10, bis 56 8,
bis 64 7, darüber 6. Die Tabelle ist an den 43 Event-Verzeichnissen dieses Repos abgelesen —
ein DSBL-Spieltag (16 × 3 = 48 Zeilen) kommt damit auf 8, genau das, was diese Events von
Hand gesetzt haben. Vorher war der Default fest 10, womit 48 Zeilen nicht auf eine Seite
passen; jeder Aufrufer musste die Größe selbst kennen.

Beispiel `events/2026_DSBL-1/display_cfg.yml`: `fontsize`, `tablewidth` (wird ignoriert,
Java-Feld heißt `width`), `factor_flight_race_width`, `show_match_stat`, `show_boat_stat`,
`show_schuttle_stat` (kein Java-Feld – ignoriert), `teamwise_list`,
`additional_colors:` (Map `NAME -> [r,g,b]` oder `[grau]` oder `[r,g,b,a]`, Name muss
GROSS sein, sonst `RuntimeException` in `PdfCreator.createColorMap`).
Ergänzt/überschreibt die Default-Farbtabelle für die `boats[].color`-Namen.

---

## 3. Invarianten (Import-Validierung, aus `ReuseSchedule.main`)

Für jedes Event mit `pairing_list.yml`:

1. `len(flights) == schedule_cfg.flights`.
2. `max(len(race)) == len(boats)` – jedes Rennen hat genau so viele Einträge wie Boote.
3. `sum(len(race) for race in flights[0].races) == getRaces()*numBoats`
   `== len(teams)` **nach Auffüllung** (`getRaces() = ceil(numTeams_real/numBoats)`).
4. (implizit) je Flight jeder Team-Index genau einmal.
5. Werte `>= numTeams_real` sind No-Shows (nur wenn `!isFull`) → Team-Slot leer (`""`).

`0000-00-00_Test` hat **keine** `pairing_list.yml`/`.csv` (nur Configs) – solche Events
beim Import überspringen.

---

## 4. Gütekriterien (Messlatte für unseren Fallback-Generator)

Zwei getrennte Phasen (`Optimizer.optimizeMatchMatrix` → `optimizeBoatSchedule`),
je ein evolutionärer (µ+λ)-artiger Optimierer.

### Phase 1 – Begegnungsausgleich (`CostCalculatorMatchMatrix`, `MatchMatrix`)

* `MatchMatrix.mat[i][j]` (untere Dreiecksmatrix) = wie oft Team `i` und `j` **im selben
  Rennen** waren. `add()` zählt für jedes Paar in jedem Rennen +1.
* Kosten:
  `Σ_{i>j} |c_ij − avg|³`  (kubisch ⇒ bestraft Ausreißer stark; `avg` = Mittel aller Paar-Zähler).
* Zusätzlich für nicht volle Events:
  * `lowerParticipants[t]` = wie oft Team `t` in einem Rennen mit reduzierter Teilnehmerzahl
    (mit No-Show) war → `Σ |avg − lp_t|³ · factorLessParticipants` (Default 3.01).
  * pro Flight: `(max − min` Anzahl No-Shows je Rennen`) · factorTeamMissing` (Default 20.01)
    → No-Shows gleichmäßig über die Rennen eines Flights verteilen.
* **Kein** eigener Term für "komplette 6er-Gruppe wiederholt sich" – das fällt indirekt über
  die paarweise Matrix an.

### Phase 2 – Bootszuteilung (`CostCalculatorBoatSchedule`, `BoatMatrix`, `InterFlightStat`)

Mutiert **nur die Position der Teams innerhalb der Rennen** (= Boot) und tauscht Rennen
(`swapBoats`, `swapRaces`). Kosten (über alle Flight-Präfixe aufsummiert, frühe Flights
gehen dadurch mehrfach ein):

* **Bootsgleichverteilung:** `BoatMatrix.mat[boot][team]` = wie oft Team `team` Boot `boot`
  benutzt hat. Kosten `Σ |v − avg|` (linear).
* **Bootswechsel zwischen Flights** (`InterFlightStat`, letztes Rennen Flight *f* ↔ erstes
  Rennen Flight *f+1*):
  * Team in beiden Rennen, **andere** Position ⇒ `teamsChangeBoats`
    → `count · weightChangeBetweenBoats` (~20).
  * `weightStayOnShuttle` (~1.0) auf `shuttleFirstRace` / `shuttleLastRace`,
    `weightStayOnBoat` (~2.0) auf `shuttleBetweenFlight`; `shuttlesPerTeams(n) = (n+1)/2`
    (Teams teilen sich zu zweit ein Begleitboot/Shuttle; Ziel: Paare bleiben über die
    Flight-Grenze zusammen).
* Debug-PDF-Statistiken heißen dort "number of matches" (Begegnungen je Flight/Häufigkeit),
  "number of boat usages", "saved Shuttles: in habour / at sea – boat changes".

**Kurz für unseren Generator:** anzustreben sind (a) möglichst gleiche Begegnungszahl für
alle Teampaare, (b) jedes Team möglichst gleich oft auf jeder Bootsfarbe, (c) möglichst
wenige Bootswechsel eines Teams zwischen Ende eines Flights und Anfang des nächsten,
(d) bei ungerader Teamzahl No-Shows gleichmäßig streuen.

---

## 5. Bestandsvarianten (der Importer muss alle vertragen)

41 Event-Ordner mit `pairing_list.yml` (+ `0000-00-00_Test` ohne). Spannbreiten:

| Kennzahl | Werte im Bestand | DSBL |
|---|---|---|
| `flights` | 6, 7, 12, 14, 15, 16, 18, 20 | 16 |
| Teams (`teams`, real) | 7 … 33 | 18 |
| Boote (`boats`) | 4 … 11 | 6 |
| Rennen/Flight `= ceil(Teams/Boote)` | 2 … 4 | 3 |
| `titles` | 0 (Alt: `title`), 1, oder 5–6 (DSBL) | 6 (DSBL-1) / 5 (DSBL-2) |

* **DSBL** (`2023-05-12_DSBL-1` … `2026_DSBL-2`): immer 18 Teams / 6 Boote / 16 Flights / 3 Rennen,
  volle Belegung (`isFull`), 5–6 `titles` (Ausrichter). Ältere DSBL-Ordner (`2023-*_DSBL-*`)
  im **Altformat** (`title:` Singular, `boats:` als Stringliste, CSV mit
  Rennnummer-statt-Flight-Spalte, Zusatzdatei `pairing_list_2021.yml`).
* **Nicht volle Events** (`teams` nicht durch `boats` teilbar): u. a.
  `2024-04-06_JSCL-Vilamoura` (17/6), `2024-06-06_San-Francisco` (8/4),
  `2026-05-30_US-Sailing-League-West` (7/4), `2025-05-17_WoW-BYC` (17/6),
  `2024-07-06_SCL-Warnemuende` (17/6). → No-Show-Indizes vorhanden.
* **Große Flotten:** `2025-07-11_SCL-Kiel` (33 Teams / 11 Boote),
  `2023-09-15_WSCL-Final` (32/8), `2024-06-01_WSCL-Berlin` (25/9).
* **Wenige Flights:** `2024-06-19_NRW-SchulCup` / `2024-06-23_NRW-Pokal` (7 Flights).
* **Viele Flights:** `2026-05-30_US-Sailing-League-West` (20).

---

## 6. Fallstricke

* **Kodierung:** alle Dateien UTF-8. Teamnamen mit Umlauten: `BYCÜ`, `SMCÜ`, Titel
  `... Kieler Förde`. Nicht als Latin-1 lesen.
* **Teamnamen mit Leerzeichen/Klammern:** `BYC (BA)`, `BYC (BE)`, `KYC (SH)`, `BSC(HH)`,
  `CN Altea`, `StFYC (JR)`, `LBYC (w)`. In `schedule_cfg.yml` meist **unquotiert**
  (`  - BYC (BA)`), in `*_teams.yml` re-emittiert **mit** Quotes (`- "BYC (BA)"`).
  Der Klammer-Suffix disambiguiert gleichnamige Clubs (`BSC(HH)` vs `BSC(BA)`,
  `BYC (BA)` vs `BYC (BE)`) – Name **nicht** normalisieren/trimmen, sonst Kollision.
* **Doppelte reale Namen ohne Suffix** kommen ebenfalls vor (`GREY`-Boote in San Francisco,
  `titles` mit gleichem Prefix). Team-Identität = Index, nicht Name.
* **`titles` mit eingebettetem Zeilenumbruch:** `events/2026-05-30_US-Sailing-League-West/schedule_cfg.yml`:
  ```yaml
  titles:
    - "Pairing List
    2026 US Sailing League West Coast Championship"
  ```
  Ein YAML-Skalar über zwei Zeilen (Newline wird zu Space). Nach dem Parsen normalisieren.
* **Inkonsistente Verzeichnisnamen:** `2026_DSBL-1` (nur Jahr, kein Datum) vs
  `2024-03-15_SCL-1` (ISO-Datum) vs `0000-00-00_Test`. Muster: `<YYYY[-MM-DD]>_<slug>`,
  Slug = alles nach dem ersten `_`. Datum ist **nicht** zuverlässig parsebar
  (`2026_DSBL-1` → nur Jahr; `0000-00-00` → Platzhalter).
* **CSV `flight`-Spalte** je nach Event-Alter unterschiedlich (§2.3) – nicht darauf verlassen.
* **`pairing_list_2021.yml`** = 1-basierte Alt-Eingabe, kein Output (§2.2).
* **`*_teams.yml`** = rotierte Anzeigereihenfolge, nicht zum Index-Dekodieren (§2.5).
* **`display_cfg.yml`** enthält oft ignorierte Keys (`tablewidth`, `show_schuttle_stat`) –
  Jackson wirft dabei nicht, das Tool nutzt still die Defaults.
* **`schedule_cfg.yml` kann `{}`- oder farb-lose `boats`-Einträge haben** → `color = null`.
* Ältere `schedule_cfg.yml` haben Felder, die es in der heutigen `ScheduleConfig` nicht mehr
  gibt (`title`, `fontsize`) – tolerant parsen.
