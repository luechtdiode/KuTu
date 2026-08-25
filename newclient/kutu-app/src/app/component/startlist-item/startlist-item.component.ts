import { Component, OnInit, output, input } from '@angular/core';
import { ProgrammItem, Teilnehmer } from 'src/app/backend-types';

@Component({
    selector: 'startlist-item',
    templateUrl: './startlist-item.component.html',
    styleUrls: ['./startlist-item.component.scss'],
    standalone: false
})
export class StartlistItemComponent implements OnInit {
  readonly programm = input<ProgrammItem>(undefined);

  readonly teilnehmer = input<Teilnehmer>(undefined);

  readonly selected = output<Teilnehmer>();

  constructor() { }

  ngOnInit() {}

}
