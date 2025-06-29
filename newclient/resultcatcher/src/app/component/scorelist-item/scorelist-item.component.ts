import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
//import { filter, map } from 'rxjs/operators';
import { ScoreRow } from 'src/app/backend-types';

@Component({
    selector: 'scorelist-item',
    templateUrl: './scorelist-item.component.html',
    styleUrls: ['./scorelist-item.component.scss'],
    standalone: false
})
export class ScorelistItemComponent implements OnInit {
  teilnehmerSubResults = '';
  teamTeilnehmerSubResults = [];

  @Input()
  teilnehmer: ScoreRow;

  @Output()
  selected: EventEmitter<ScoreRow>;

  constructor() { }

  ngOnInit() {
    this.teilnehmerSubResults = renderTeilnehmerWertungen(this.teilnehmer);
    this.teamTeilnehmerSubResults = this.teilnehmer.rows?.length == 0
      ? []
      : this.teilnehmer.rows.map(renderTeilnehmer)
  }
}

const knownKeys: string[] = ["athletID", "rows", "Rang", "Athlet", "Team", "Team/Athlet", "Jahrgang", "Verein", "K", "ø Gerät", "Total D", "Total E", "Total A", "Total B", "Total Punkte"];

function renderTeilnehmer(tnRow: ScoreRow) {
  return `${tnRow['Team']} (${tnRow['K']}): ${renderTeilnehmerWertungen(tnRow)}`
}

function renderWertung(gearValues: any) {
  let best = "";
  let stroke = "";
  if (gearValues['Endnote-styles'].indexOf('best') >= 0) {
    best = "*";
  }
  if (gearValues['Endnote-styles'].indexOf('stroke') >= 0) {
    stroke = "!";
  }
  return `${best}${gearValues['Endnote-raw'].trim()}${stroke}`
}

function renderTeilnehmerWertungen(tnRow: ScoreRow) {
  return Object.keys(tnRow)
  .map(key => `${key}`)
  .filter((key: string) => knownKeys.indexOf(key) < 0 && tnRow[key]['Endnote'])
  .map(key => `${key}: ${renderWertung(tnRow[key])} (${tnRow[key]['Rang'].trim()})`)
  .join(", ")
}
