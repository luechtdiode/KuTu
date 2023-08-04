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
  vereinregistration: ClubRegistration;

  @Input()
  status: string;

  @Input()
  programmlist: ProgrammRaw[];

  @Output()
  selected = new EventEmitter<AthletRegistration>();

  statusBadgeColor() {
    if (this.status === "in sync") {
      return "success";
    } else {
      return "warning"
    }
  }

  statusComment() {
    if (this.status === "in sync") {
      return "";
    } else {
      return this.status.substring(this.status.indexOf("(") + 1, this.status.length -1);
    }
  }

  statusBadgeText() {
    if (this.status === "in sync") {
      return "\u2714 " + this.status;
    } else {
      return "\u2757 " + this.status.substring(0, this.status.indexOf("(")-1);
    }
  }

  ngOnInit() {}

  getProgrammText() {
    const programraw = this.programmlist.find(pgm => pgm.id === this.athletregistration.programId);
    return  !!programraw ? programraw.name : '...';
  }

  getTeamText() {
    const team = this.athletregistration.team;
    const verein = this.vereinregistration;
    if (team > 0) {
      const pgmtext = this.getProgrammText();
      return 'Team ' + verein.vereinname + ' ' + this.athletregistration.team + " (bei " + this.athletregistration.geschlecht + "/" + pgmtext + ")";
    } else {
      return '';
    }
  }
}
