import { Component, OnInit, ViewChild, HostListener } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf, ScoreBlock, ScoreRow, ScoreLink } from '../backend-types';
import { IonItemSliding, NavController, ActionSheetController } from '@ionic/angular';

import { BackendService } from '../services/backend.service';
import { debounceTime, distinctUntilChanged, filter, map, share, switchMap } from 'rxjs/operators';
import { backendUrl } from '../utils';
import { Observable } from 'rxjs/internal/Observable';
import { Subject, BehaviorSubject, of } from 'rxjs';

@Component({
  selector: 'app-last-results',
  templateUrl: './last-results.page.html',
  styleUrls: ['./last-results.page.scss'],
})
export class LastResultsPage implements OnInit {

  // @ViewChild(IonContent) content: IonContent;

  items: WertungContainer[] = [];
  lastItems: number[];
  geraete: Geraet[] = [];
  scorelinks: ScoreLink[] = [];
  defaultPath: string = undefined;
  scoreblocks: ScoreBlock[] = [];
  sFilteredScoreList: ScoreBlock[] = [];
  sMyQuery: string;

  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;

  private busy = new BehaviorSubject(false);


  // @HostListener('window:resize', ['$event'])
  // onResize(event: any) {
  //   if (this.content) {
  //     this.content.resize();
  //   }
  // }

  constructor(public navCtrl: NavController,
    public backendService: BackendService,
    public actionSheetController: ActionSheetController
    ) {
    if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  ngOnInit(): void {
    this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(geraete => {
      this.geraete = geraete || [];
      this.sortItems();
    });
    this.backendService.newLastResults.pipe(
      filter(r => !!r && !!r.results)
    ).subscribe(newLastRes => {
      this.lastItems = this.items.map(item => item.id * this.geraete.length + item.geraet);
      this.items = [];
      Object.keys(newLastRes.results).forEach(key => {
        this.items.push(newLastRes.results[key]);
      });
      this.sortItems();
      if (this.scorelistAvailable()) {
        this.backendService.getScoreLists().subscribe(scorelists => {
          const c = this.competitionContainer();
          const genericLink = `/api/scores/${c.uuid}/query?groupby=Kategorie:Geschlecht`;
          if (!!scorelists) {
            const values = Object.values(scorelists)
            this.scorelinks = [...values.filter(l => l.name != 'Zwischenresultate'), <ScoreLink>{
                name: 'Generische Rangliste',
                published: true,
                "published-date": '',
                "scores-href": genericLink,
                "scores-query": genericLink
              }];
            const publishedLists = this.scorelinks.filter(s => s.published)
            this.refreshScoreList(publishedLists[0]);
          }
        });
      }
    });
  }

  _title: string = 'Aktuelle Resultate';
  get title() {
    return this._title;
  }
  set title(title) {
    this._title = title;
  }

  refreshScoreList(link: ScoreLink) {
    let path = link['scores-href'];
    if (!link.published) {
      path = link['scores-query'];
    }
    if (path.startsWith('/')) {
      path = path.substring(1);
    }
    path = path.replace('html', '');
    this.title = link.name

    this.defaultPath = path;
    this.backendService.getScoreList(this.defaultPath).pipe(
      map(scorelist => {
        if (!!scorelist.title && !!scorelist.scoreblocks) {
          return scorelist.scoreblocks;
        } else {
          return [];
        }
      })).subscribe(scoreblocks => {
        this.scoreblocks = scoreblocks
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          filter(event => !!event && !!event.target && !!event.target.value),
          map(event => event.target.value),
          debounceTime(1000),
          distinctUntilChanged(),
          share()
        );
        pipeBeforeAction.subscribe(scoreSearchWith => {
          this.busy.next(true);
        });
        pipeBeforeAction.pipe(
          switchMap(this.runQuery(scoreblocks))
        ).subscribe(filteredList => {
          this.sFilteredScoreList = filteredList;
          this.busy.next(false);
        });
      });
  }

  sortItems() {
    this.items = this.items.sort((a, b) => {
      let p = a.programm.localeCompare(b.programm);
      if (p === 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    }).filter(w => w.wertung.endnote !== undefined);
  }

  isNew(item: WertungContainer): boolean {
    return this.lastItems.filter(id => id === item.id * this.geraete.length + item.geraet).length === 0;
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }
  set competition(competitionId: string) {
    if (!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(geraete => {
        this.geraete = geraete || [];
        this.sortItems();
      });
    }
  }
  get competition(): string {
    return this.backendService.competition || '';
  }
  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions || [];
  }

  competitionContainer(): Wettkampf {

    const emptyCandidate = <Wettkampf>{
      titel: "",
      datum: new Date(),
      auszeichnung: undefined,
      auszeichnungendnote: undefined,
      id: undefined,
      uuid: undefined,
      programmId: 0
    };

    if (!this.backendService.competitions) { return emptyCandidate; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return emptyCandidate;
    }
  }

  competitionName(): string {
    const c = this.competitionContainer();
    if (c.titel === '') {
      return '';
    }
    return c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-');
  }


  geraetOrder(geraetId: number): number {
    if (!this.geraete) {
      return 0;
    }
    const ret = this.geraete
      .findIndex(c => c.id === geraetId);
    return ret;
  }

  geraetText(geraetId: number): string {
    if (!this.geraete) { return ''; }
    const candidate = this.geraete
      .filter(c => c.id === geraetId)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  getTitle(wertungContainer: WertungContainer): string {
    return wertungContainer.programm + ' - ' + this.geraetText(wertungContainer.geraet);
  }

  scorelistAvailable(): boolean {
    return !!this.items && this.items.length === 0 && new Date(this.competitionContainer().datum).getTime() <  Date.now();
  }
  get filteredScoreList() {
    if (!!this.sFilteredScoreList && this.sFilteredScoreList.length > 0) {
      return this.sFilteredScoreList;
    } else {
      return this.getScoreListItems();
    }
  }

  get isBusy(): Observable<boolean> {
    return this.busy;
  }

  runQuery(scorelist: ScoreBlock[]) {
    return (query: string) => {
      const q = query.trim();
      let result: ScoreBlock[] = [];

      if (q && scorelist) {
        scorelist.forEach(scoreblock => {
          const filterFn = this.filter(q);
          const tnFiltered = scoreblock.rows.filter(tn => filterFn(tn, scoreblock));
          if (tnFiltered.length > 0) {
            const pgItem = {
              title : scoreblock.title,
              rows: tnFiltered
            } as ScoreBlock;
            result = [...result, pgItem];
          }
        });
      }

      return of(result);
    };
  }

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: ScoreRow, block: ScoreBlock): boolean => {
      return queryTokens.filter(token => {
        if (tn.Athlet.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.Verein.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.Jahrgang.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (block.title.text.toUpperCase().replace('.', ' ').replace(',', ' ').split(' ').indexOf(token) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }

  reloadList(event: any) {
    this.tMyQueryStream.next(event);
  }

  makeGenericScoreListLink(): string {
    return `${backendUrl}${this.defaultPath}&html`;
  }

  getScoreListItems(): ScoreBlock[] {
    return this.scoreblocks;
  }
  scoreItemTapped(item: ScoreRow, slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 0) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  followAthlet(item: ScoreRow, slidingItem: IonItemSliding) {
    slidingItem.close();
    this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${item["athletID"]}`);
  }

  open() {
    window.open(this.makeGenericScoreListLink(), '_blank');
  }

  isShareAvailable():boolean {
    return !!navigator && !!navigator.share;
  }

  share() {
    let sport = 'GeTu';
    const SPORT_MAPPING = {
      1: "Athletik",
      11: "KuTu",
      20: "GeTu",
      31: "KuTu"
    };
    const c = this.competitionContainer();
    if (c.titel !== '') {
      sport = SPORT_MAPPING[c.programmId];
    }    // API is fairly new, check if it is supported
    let text = `${this.competitionName()} #${sport}-${c.titel.replace(',', ' ').split(' ').join('_')}`;
    if(this.isShareAvailable()) {
      navigator.share({
        title: `${sport} Rangliste`,
        text: text,
        url: this.makeGenericScoreListLink()
      })
      .then(function() {
        // success
        console.log("Article shared");
      })
      .catch(function(e) {
        // error message
        console.log(e.message);
      });
    }
  }

  async presentActionSheet() {
    let buttons: any[] = [ ...this.scorelinks.map(link => {
      if (link.published) {
        return {
          text: `${link.name} anzeigen ...`,
          icon: 'document',
          handler: () => {
            this.refreshScoreList(link);
          }
        };
      } else {
        return {
          text: `${link.name} (unveröffentlicht)`,
          icon: 'today-outline',
          handler: () => {
            this.refreshScoreList(link);
          }
        };
      }
    }),
      {
        text: 'Rangliste öffnen ...',
        icon: 'open',
        handler: () => {
          this.open();
        }
      }
    ];
    if (this.isShareAvailable()) {
      buttons = [...buttons,
        {
          text: 'Teilen / Share ...',
          icon: 'share',
          handler: () => {
            this.share();
          }
        }
      ];
    }
    let actionconfig: any = {
      header: 'Aktionen',
      cssClass: 'my-actionsheet-class',
      buttons: buttons
    };
    const actionSheet = await this.actionSheetController.create(actionconfig);
    await actionSheet.present();
  }
}
