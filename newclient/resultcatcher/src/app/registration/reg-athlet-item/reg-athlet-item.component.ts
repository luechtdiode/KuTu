import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { ClubRegistration, AthletRegistration, ProgrammRaw, TeamItem } from '../../backend-types';

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

  @Input()
  teams: TeamItem[];

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

  mapTeam(teamId: number): string {
    return [...this.teams.filter(tm => tm.name?.trim().length > 0 && tm.index == teamId).map(tm => {
      if (tm.index > 0) {
        return tm.name + ' ' + tm.index;
      } else return tm.name;
    }), ''][0];
  }

  getTeamText() {
    const team = this.athletregistration.team;
    if (team) {
      const pgmtext = this.getProgrammText();
      return 'Team ' + this.mapTeam(this.athletregistration.team) + " (bei " + this.athletregistration.geschlecht + "/" + pgmtext + ")";
    } else {
      return '';
    }
  }
}
