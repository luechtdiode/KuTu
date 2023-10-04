import { Component, OnDestroy, OnInit } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf, ScoreBlock, ScoreRow, ScoreLink } from '../backend-types';
import { IonItemSliding, NavController, ActionSheetController } from '@ionic/angular';

import { BackendService } from '../services/backend.service';
import { debounceTime, distinctUntilChanged, filter, map, share, switchMap } from 'rxjs/operators';
import { backendUrl } from '../utils';
import { Observable } from 'rxjs/internal/Observable';
import { Subject, BehaviorSubject, of, Subscription } from 'rxjs';
import { GroupBy } from '../component/result-display/result-display.component';

@Component({
  selector: 'app-last-results',
  templateUrl: './last-results.page.html',
  styleUrls: ['./last-results.page.scss'],
})
export class LastResultsPage implements OnInit, OnDestroy {
  groupBy = GroupBy;

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
  durchgangopen: boolean;

  subscriptions: Subscription[] = [];


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
    this.backendService.durchgangStarted.pipe(
      map(dgl => dgl.filter(dg => dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    )).subscribe(dg => {
      this.durchgangopen = dg;
    });
  }

  ngOnDestroy(): void {
      this.subscriptions.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {
    this.subscriptions.push(this.backendService.competitionSubject.subscribe(comps => {
      this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(geraete => {
        this.geraete = geraete || [];
        this.sortItems();
      });
      this.subscriptions.push(this.backendService.newLastResults.subscribe(newLastRes => {
        this.lastItems = this.items.map(item => item.id * this.geraete.length + item.geraet);
        this.items = [];
        if (!!newLastRes && !!newLastRes.results) {
          Object.keys(newLastRes.results).forEach(key => {
            this.items.push(newLastRes.results[key]);
          });
        }
        this.sortItems();
        if (this.scorelistAvailable()) {
          this.backendService.getScoreLists().subscribe(scorelists => {
            const c = this.competitionContainer();
            const genericLink = `/api/scores/${c.uuid}/query?groupby=Kategorie:Geschlecht`;
            if (!!scorelists) {
              const values = Object.values(scorelists)
              const lists = values.filter(l => (l as any).name != 'Zwischenresultate').sort((a,b)=> a.name.localeCompare(b.name));
              const einzelGeneric = <ScoreLink>{
                name: 'Generische Rangliste',
                published: true,
                "published-date": '',
                "scores-href": genericLink,
                "scores-query": genericLink
              };
              const teamGeneric = <ScoreLink>{
                name: 'Generische Team-Rangliste',
                published: true,
                "published-date": '',
                "scores-href": genericLink + '&kind=Teamrangliste',
                "scores-query": genericLink + '&kind=Teamrangliste'
              };
              this.scorelinks = c.teamrule?.trim.length > 0 ? [...lists, teamGeneric, einzelGeneric] : [...lists, einzelGeneric];
              const publishedLists = this.scorelinks.filter(s => ''+s.published === 'true')
              this.refreshScoreList(publishedLists[0]);
            }
          });
        } else {
          this.title = 'Aktuelle Resultate';
        }
      }));
    }));
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
        pipeBeforeAction.subscribe(() => {
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
    const compareItems = (a, b) => {
      let p = a.programm.localeCompare(b.programm);
      if (p === 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    };

    this.items = this.items
      .filter(w => w.wertung.endnote !== undefined)
      .sort(compareItems);
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
      programmId: 0,
      teamrule: ""
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
    return this.geraete
      .findIndex(c => c.id === geraetId);
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

  getColumnSpec(): number {
    return this.geraete?.length || 0;
  }

  getMaxColumnSpec(): number {
    return Math.min(12, Math.max(1, Math.floor(12 / this.geraete.length + 0.5)));
  }
  
  getTitle(wertungContainer: WertungContainer): string {
    return wertungContainer.programm + ' - ' + this.geraetText(wertungContainer.geraet);
  }

  onlyUnique(value, index, self) {
    return self.indexOf(value) === index;
  }

  getProgramme() {
    return this.items.map(wc => wc.programm).filter(this.onlyUnique);
  }

  getWertungen(programm) {
    return this.items.filter(wc => wc.programm === programm);
  }

  scorelistAvailable(): boolean {
    return !this.durchgangopen && (this.items?.length === 0) && new Date(this.competitionContainer().datum).getTime() <  new Date(Date.now() - 3600 * 1000 * 24).getTime();
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
    return (tnRoot: ScoreRow, block: ScoreBlock): boolean => {
      return queryTokens.filter(token => {
        return [tnRoot, ...tnRoot.rows].find(tn => {
          if (tn.Athlet?.toUpperCase().indexOf(token) > -1) {
            return true;
          }
          if (tn['Team/Athlet']?.toUpperCase().indexOf(token) > -1) {
            return true;
          }
          if (tn['Team']?.toUpperCase().indexOf(token) > -1) {
            return true;
          }
          if (tn.Verein?.toUpperCase().indexOf(token) > -1) {
            return true;
          }
          if (tn.Jahrgang?.toUpperCase().indexOf(token) > -1) {
            return true;
          }
          if (block.title.text.toUpperCase().replace('.', ' ').replace(',', ' ').split(' ').indexOf(token) > -1) {
            return true;
          }
        });
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
    if (item["athletID"]) {
      this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${item["athletID"]}`);
    } // else TODO provide team-view
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
      if (''+link.published === 'true') {
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
