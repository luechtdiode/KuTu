import { Component, OnInit } from '@angular/core';
import { StartList, Wettkampf, Teilnehmer, ProgrammItem } from '../backend-types';
import { NavController, IonItemSliding } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { async } from 'rxjs/internal/scheduler/async';
import { BehaviorSubject, Subject } from 'rxjs';

@Component({
  selector: 'app-search-athlet',
  templateUrl: './search-athlet.page.html',
  styleUrls: ['./search-athlet.page.scss'],
})
export class SearchAthletPage implements OnInit {

  sStartList: StartList;
  sFilteredStartList: StartList;
  sMyQuery: string;
  sFilterTask: () => void = undefined;

  constructor(public navCtrl: NavController, public backendService: BackendService) {
    this.backendService.getCompetitions();
  }

  ngOnInit(): void {
    this.backendService.loadStartlist(undefined).subscribe(startlist => {
      this.startlist = startlist;
    });
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }

  set competition(competitionId: string) {
    if (!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.loadStartlist(this.sMyQuery).subscribe(startlist => {
        this.startlist = startlist;
      });
    }
  }
  get competition(): string {
    return this.backendService.competition || '';
  }
  set startlist(startlist: StartList) {
    this.sStartList = startlist;
    this.reloadList(this.sMyQuery);
  }
  get startlist() {
    return this.sStartList;
  }
  get filteredStartList() {
    return this.sFilteredStartList || {
      programme : []
    } as StartList;
  }
  reloadList(event: any) {
    if (!event || !event.target.value) {
      this.sFilteredStartList = this.sStartList;
      return;
    }
    if (this.sMyQuery === event.target.value.trim()) {
      return;
    }
    this.sMyQuery = event.target.value.trim();
    this.sFilteredStartList = {
      programme : []
    } as StartList;

    if (this.sFilterTask) {
      return;
    }

    const finishedPromise = new Subject();
    this.sFilterTask = () => {
/*      this.backendService.startLoading(
        'Filter wird angewendet ...',
        finishedPromise.asObservable() );*/
      let q;
      do {
        q = this.sMyQuery;
        if (event && this.sStartList) {
          this.sStartList.programme.forEach(programm => {
            const filter = this.filter(q);
            const tnFiltered = programm.teilnehmer.filter(tn => filter(tn, programm.programm));
            if (tnFiltered.length > 0) {
              const pgItem = {
                programm : programm.programm,
                teilnehmer: tnFiltered
              } as ProgrammItem;
              this.sFilteredStartList = Object.assign({}, this.sStartList, {
                programme : [...this.sFilteredStartList.programme, pgItem]
              });
            }
          });
        }
      } while (q !== this.sMyQuery);

      this.sFilterTask = undefined;
      finishedPromise.complete();
      this.backendService.resetLoading();
    };
    setTimeout(this.sFilterTask, 1800);

/*    this.backendService.loadStartlist(this.myQuery).subscribe(startlist => {
      console.log(startlist);
      this.startlist = startlist;
    });
    */
  }

  followAthlet(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.close();
  }

  followRiege(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.close();
    this.backendService.getGeraete(this.backendService.competition,
      item.durchgang).subscribe(geraete => {
      this.backendService.getSteps(this.backendService.competition,
        item.durchgang, geraete.find(gg => gg.name === item.start).id).subscribe(steps => {
        this.navCtrl.navigateForward('station');
      });
    });
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions || [];
  }
  competitionName(): string {
    if (!this.backendService.competitions) { return ''; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: Teilnehmer, pgm: string): boolean => {
      return queryTokens.filter(token => {
        if (tn.athletid + '' === token) {
          return true;
        }
        if (tn.athlet.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.verein.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.start.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.verein.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (pgm.indexOf(token) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }
}
