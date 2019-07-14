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
  tMyQuery: string;

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
    if (!this.sMyQuery || this.sMyQuery.length < 2) {
      return [];
    }
    return this.sFilteredStartList || {
      programme : []
    } as StartList;
  }
  reloadList(event: any) {
    if (!event || !event.target.value || event.target.value.trim().length === 0) {
      return;
    }
    if (this.tMyQuery === event.target.value.trim()) {
      return;
    }
    this.tMyQuery = event.target.value.trim();

    if (this.sFilterTask) {
      return;
    }

    const finishedPromise = new Subject();
    this.sFilterTask = () => {
      let q: string;
      do {
        this.sFilteredStartList = {
          programme : []
        } as StartList;
        q = this.tMyQuery;
        if (event && this.sStartList) {
          this.sStartList.programme.filter(pgm => q === this.sMyQuery).forEach(programm => {
            const filter = this.filter(q);
            const tnFiltered = programm.teilnehmer
              .filter(pgm => q === this.sMyQuery)
              .filter(tn => filter(tn, programm.programm));
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
      } while (q !== this.tMyQuery);

      this.sFilterTask = undefined;
      this.sMyQuery = this.tMyQuery;
      finishedPromise.complete();
      this.backendService.resetLoading();
    };
    setTimeout(this.sFilterTask, 1500);

  }

  itemTapped(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 0) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  followAthlet(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.close();
    this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${item.athletid}`);
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
