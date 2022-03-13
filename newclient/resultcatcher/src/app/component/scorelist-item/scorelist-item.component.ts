import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { filter, map } from 'rxjs';
import { ScoreRow } from 'src/app/backend-types';

@Component({
  selector: 'scorelist-item',
  templateUrl: './scorelist-item.component.html',
  styleUrls: ['./scorelist-item.component.scss'],
})
export class ScorelistItemComponent implements OnInit {
  knownKeys: string[] = ["Rang", "Athlet", "Jahrgang", "Verein", "ø Gerät", "Total Punkte"];
  teilnehmerSubResults = '';

  @Input()
  teilnehmer: ScoreRow;

  @Output()
  selected: EventEmitter<ScoreRow>;

  constructor() { }

  ngOnInit() {
    this.teilnehmerSubResults = Object.keys(this.teilnehmer)
      .map(key => `${key}`)
      .filter((key: string) => this.knownKeys.indexOf(key) < 0)
      .map(key => `${key}: ${this.teilnehmer[key]['Endnote']} (${this.teilnehmer[key]['Rang']}.)`)
      .join(", ")
  }

}
