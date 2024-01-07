import { Component, OnInit } from '@angular/core';
import { BackendService } from '../services/backend.service';
import { ProgrammItem, Wettkampf } from '../backend-types';
import { NavController } from '@ionic/angular';
import { Subject, debounceTime, distinctUntilChanged, filter, map, of, share, switchMap, tap } from 'rxjs';

@Component({
  selector: 'app-competitions',
  templateUrl: './competitions.page.html',
  styleUrls: ['./competitions.page.scss'],
})
export class CompetitionsPage implements OnInit {
  pgmList;

  sFilteredWettkampfList: Wettkampf[];
  sMyQuery: string;

  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;

  constructor(private navCtrl: NavController, private backendService: BackendService,) {
    this.backendService.loadWKPrograms().subscribe(pgms => {
      this.pgmList = pgms.reduce(function (map, pgm) {
        map[pgm.id] = pgm.name;
        return map;
      }, {}
      )
    });

    this.backendService.competitionSubject.subscribe(wks => {
      this.sFilteredWettkampfList = wks;
      this.sMyQuery = '';
      console.log("Initializing with new wks: ", wks);
      const pipeBeforeAction = this.tMyQueryStream.pipe(
        map(event => event?.target?.value || ''),
        distinctUntilChanged(),
        switchMap(this.runQuery(wks)),
        share()
      );

      pipeBeforeAction.subscribe(filteredList => {
        this.sFilteredWettkampfList = filteredList;
      });
    });

    if (!this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  ngOnInit(): void {
  }

  runQuery(wettkampfliste: Wettkampf[]) {
    const wkl = [...wettkampfliste || []];
    return (query: string) => {
      const q = query.trim();
      let result: Wettkampf[];

      result = [];
      console.log(q, query, wkl);

      if (wkl.length > 0) {
        const filterFn = this.filter(q);
        result = [...wkl.filter(wettkampf => filterFn(wettkampf))];
      }

      return of(result);
    };
  }

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: Wettkampf): boolean => {
      return query.trim().length === 0 || query.trim() === '*' || queryTokens.length === 0 || queryTokens.filter(token => {
        if (this.competitionName(tn).toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (this.getProgrammname(tn.programmId).toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (this.getDetails(tn).toUpperCase().indexOf(token) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }

  reloadList(event: any) {
    this.tMyQueryStream.next(event);
  }

  getCompetitions(): Wettkampf[] {
    return this.sFilteredWettkampfList;
  }

  getProgrammname(id: number): string {
    if (this.pgmList) {
      return this.pgmList[id];
    }
    return '';
  }

  getDetails(wk: Wettkampf): string {
    let details = this.getProgrammname(wk.programmId);
    if (wk.teamrule && !(wk.teamrule === 'Keine Teams')) {
      details = details + ', Teams: ' + wk.teamrule;
    }
    if (wk.altersklassen?.length > 0) {
      details = details + ', Altersklassen';
    }
    if (wk.jahrgangsklassen?.length > 0) {
      details = details + ', Jahrgangs-Altersklassen';
    }
    return details;
  }

  competitionName(wk: Wettkampf): string {
    return wk.titel + ', am ' + (wk.datum + 'T').split('T')[0].split('-').reverse().join('-');
  }

  itemTapped(event: any, item: Wettkampf) {
    this.backendService.getDurchgaenge(item.uuid);
    this.navCtrl.navigateRoot('/home');
  }
}
