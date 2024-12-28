import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { ProgrammItem, Teilnehmer } from 'src/app/backend-types';

@Component({
    selector: 'startlist-item',
    templateUrl: './startlist-item.component.html',
    styleUrls: ['./startlist-item.component.scss'],
    standalone: false
})
export class StartlistItemComponent implements OnInit {
  @Input()
  programm: ProgrammItem;

  @Input()
  teilnehmer: Teilnehmer;

  @Output()
  selected: EventEmitter<Teilnehmer>;

  constructor() { }

  ngOnInit() {}

}
