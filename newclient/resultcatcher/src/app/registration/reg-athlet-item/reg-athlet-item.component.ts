import { Component, OnInit, input, output } from '@angular/core';
import { ClubRegistration, AthletRegistration, ProgrammRaw, TeamItem } from '../../backend-types';

@Component({
    selector: 'app-reg-athlet-item',
    templateUrl: './reg-athlet-item.component.html',
    styleUrls: ['./reg-athlet-item.component.scss'],
    standalone: false
})
export class RegAthletItemComponent implements OnInit {

  constructor() { }

  readonly athletregistration = input<AthletRegistration>(undefined);

  readonly vereinregistration = input<ClubRegistration>(undefined);

  readonly status = input<string>(undefined);

  readonly programmlist = input<ProgrammRaw[]>(undefined);

  readonly teams = input<TeamItem[]>(undefined);

  readonly selected = output<AthletRegistration>();

  statusBadgeColor() {
    if (this.status() === "in sync") {
      return "success";
    } else {
      return "warning"
    }
  }

  statusComment() {
    const status = this.status();
    if (status === "in sync") {
      return "";
    } else {
      return status.substring(status.indexOf("(") + 1, status.length -1);
    }
  }

  statusBadgeText() {
    const status = this.status();
    if (status === "in sync") {
      return "\u2714 " + status;
    } else {
      return "\u2757 " + status.substring(0, status.indexOf("(")-1);
    }
  }

  ngOnInit() {}

  getProgrammText() {
    const programraw = this.programmlist().find(pgm => pgm.id === this.athletregistration().programId);
    return  !!programraw ? programraw.name : '...';
  }

  mapTeam(teamId: number): string {
    return [...this.teams().filter(tm => tm.name?.trim().length > 0 && tm.index == teamId).map(tm => {
      if (tm.index > 0) {
        return tm.name + ' ' + tm.index;
      } else return tm.name;
    }), ''][0];
  }

  getTeamText() {
    const team = this.athletregistration().team;
    if (team) {
      const pgmtext = this.getProgrammText();
      return 'Team ' + this.mapTeam(this.athletregistration().team) + " (bei " + this.athletregistration().geschlecht + "/" + pgmtext + ")";
    } else {
      return '';
    }
  }
}
