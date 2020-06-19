import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { ClubRegistration, AthletRegistration, ProgrammRaw } from '../../backend-types';

@Component({
  selector: 'app-reg-athlet-item',
  templateUrl: './reg-athlet-item.component.html',
  styleUrls: ['./reg-athlet-item.component.scss'],
})
export class RegAthletItemComponent implements OnInit {

  constructor() { }

  @Input()
  athletregistration: AthletRegistration;

  @Input()
  programmlist: ProgrammRaw[];

  @Output()
  selected = new EventEmitter<AthletRegistration>();

  ngOnInit() {}

  getProgrammText(id: number) {
    const programraw = this.programmlist.find(pgm => pgm.id === id);
    return  !!programraw ? programraw.name : '...';
  }
}
