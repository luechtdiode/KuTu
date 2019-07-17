import { Component, OnInit } from '@angular/core';
import { StartList, Wettkampf, Teilnehmer, ProgrammItem } from '../backend-types';
import { NavController, IonItemSliding } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { async } from 'rxjs/internal/scheduler/async';
import { BehaviorSubject, Subject, of, Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { debounceTime, distinctUntilChanged, map, filter, switchMap, tap, share } from 'rxjs/operators';

@Component({
  selector: 'app-search-athlet',
  templateUrl: './search-athlet.page.html',
  styleUrls: ['./search-athlet.page.scss'],
})
export class SearchAthletPage implements OnInit {

  sStartList: StartList;
  sFilteredStartList: StartList;
  sMyQuery: string;

  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;

  private busy = new BehaviorSubject(false);

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              public backendService: BackendService) {
    this.backendService.getCompetitions();
  }

  ngOnInit(): void {
    this.busy.next(true);
    const wkId = this.route.snapshot.paramMap.get('wkId');
    if (wkId) {
      this.competition = wkId;
    }
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }

  set competition(competitionId: string) {
    if (!this.startlist || competitionId !== this.backendService.competition) {
      this.busy.next(true);
      this.startlist = {} as StartList;
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.loadStartlist(/*this.sMyQuery*/undefined).subscribe(startlist => {
        this.startlist = startlist;
        this.busy.next(false);
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          filter(event => !!event && !!event.target && !!event.target.value),
          map(event => event.target.value),
          debounceTime(1000),
          distinctUntilChanged(),
          share()
        );
        pipeBeforeAction.subscribe(startSearchWith => {
          this.busy.next(true);
        });
        pipeBeforeAction.pipe(
          switchMap(this.runQuery(startlist))
        ).subscribe(filteredList => {
          this.sFilteredStartList = filteredList;
          this.busy.next(false);
        });
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

  get isBusy(): Observable<boolean> {
    return this.busy;
  }

  runQuery(startlist: StartList) {
    return (query: string) => {
      const q = query.trim();
      let result: StartList;

      result = {
        programme : []
      } as StartList;

      if (q && startlist) {

        startlist.programme.forEach(programm => {
          const filterFn = this.filter(q);
          const tnFiltered = programm.teilnehmer.filter(tn => filterFn(tn, programm.programm));
          if (tnFiltered.length > 0) {
            const pgItem = {
              programm : programm.programm,
              teilnehmer: tnFiltered
            } as ProgrammItem;
            result = Object.assign({}, startlist, {
              programme : [...result.programme, pgItem]
            });
          }
        });
      }

      return of(result);
    };
  }

  reloadList(event: any) {
    this.tMyQueryStream.next(event);
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
      if (!this.startlist) {
        this.competition = this.backendService.competition;
      }
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
