import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActionSheetController, IonItemSliding, NavController } from '@ionic/angular';
import { Geraet, NewLastResults, ScoreBlock, ScoreLink, ScoreRow, Wertung, WertungContainer, Wettkampf } from '../backend-types';

import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of, Subject, Subscription } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';
import { debounceTime, distinctUntilChanged, filter, map, share, switchMap } from 'rxjs/operators';
import { GroupBy } from '../component/result-display/result-display.component';
import { BackendService } from '../services/backend.service';
import { backendUrl } from '../utils';

@Component({
    selector: 'app-last-results',
    templateUrl: './last-results.page.html',
    styleUrls: ['./last-results.page.scss'],
    standalone: false
})
export class LastResultsPage implements OnInit, OnDestroy {
  groupBy = GroupBy;

  items: WertungContainer[] = [];
  mixeditems: WertungContainer[] = [];
  hasCountingItems: boolean = undefined;
  lastItems: number[];
  geraete: Geraet[] = [];
  scorelinks: ScoreLink[] = [];
  defaultPath: string = undefined;
  scoreblocks: ScoreBlock[] = [];
  sFilteredScoreList: ScoreBlock[] = [];
  sMyQuery: string;
  isDNoteUsed: boolean = true;

  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;

  private busy = new BehaviorSubject(false);
  
  durchgangopen: boolean;

  subscriptions: Subscription[] = [];

  constructor(public navCtrl: NavController,
    public backendService: BackendService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    public actionSheetController: ActionSheetController
    ) {
    if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
    this.durchgangopen = false;
    this.backendService.durchgangStarted.pipe(
      filter(dgl => dgl !== undefined),
      map(dgl => dgl.filter(dg => dg.wettkampfUUID === this.backendService.competition).length > 0
    )).subscribe(dg => {
      this.durchgangopen = dg;
      //console.log('durchgang new assigned: ' + dg);
    });
  }

  ngOnDestroy(): void {
      this.subscriptions.forEach(s => s.unsubscribe());
      this.cleanScoreListSubscriptions();
  }

  ngOnInit(): void {
    this.competition = this.route.snapshot.queryParamMap.get('c') || this.backendService.competition;
    this.kategorieFilter = this.route.snapshot.queryParamMap.get('k') || 'getrennt';
    this.durchgangFilter = this.route.snapshot.queryParamMap.get('d') || 'undefined';
    this.subscriptions.push(this.backendService.competitionSubject.subscribe(comps => {
      //console.log('refreshed competition subject ...');
      this.subscriptions.push(this.backendService.newLastResults.pipe(filter(nlr => nlr !== undefined)).subscribe(ni => this.refreshItems(ni)));
    }));
  }

  _optionsVisible: boolean = false;

  get optionsVisible(): boolean {
    return this._optionsVisible || !this.competition
  }
  set optionsVisible(value: boolean) {
    this._optionsVisible = value;
  }
  toggleOptions() {
    this._optionsVisible = !this._optionsVisible;
  }

  teamsAllowed(wk: Wettkampf): boolean {
    return wk.teamrule?.trim().length > 0 && wk.teamrule !== 'Keine Teams';
  }

  _title: string = 'Aktuelle Resultate';
  get title() {
    return this._title;
  }
  set title(title) {
    this._title = title;
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
      this.router.navigate(
        ['last-results'],
        {
          queryParams: {c: this.competition, k: this._kategorieFilter, d: this.backendService.durchgang}, // add query params to current url
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }
      );

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

  getColumnSpecs(): number[] {
    const columnSpec = this.getColumnSpec();
    if (columnSpec === 6) {
      return [ 12, 12, 6, 4, this.getMaxColumnSpec()];
    } else {
      return [ 12, 6, 6, 3, this.getMaxColumnSpec()];
    }
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
    if (this.kategorieFilter === 'getrennt') {
      return this.items.map(wc => wc.programm).filter(this.onlyUnique);
    } else {
      return [this.mixeditems.map(wc => wc.programm)[0]];
    }
  }

  getWertungen(programm) {
    if (this.kategorieFilter === 'getrennt') {
      return this.items.filter(wc => wc.programm === programm);
    } else {
      return this.getMixedWertungen();
    }
  }

  getMixedWertungen() {
    return this.mixeditems;
  }
  
  getDurchgaenge() {
    return [
      ...this.backendService.activeDurchgangList.map(d => d.durchgang), 
      ...this.backendService.durchgaenge.filter(d => !this.isDurchgangActiv(d))
    ];
  }
  
  isDurchgangActiv(durchgang: string): boolean {
    return this.backendService.activeDurchgangList.find(d => d.durchgang === durchgang) !== undefined
  }
  
  _durchgang: string = undefined;
  set durchgangFilter(durchgang: string) {
    if (durchgang === 'undefined') {
      //console.log("navigate to durchgang " + durchgang);
      this.router.navigate(
        ['last-results'],
        {
          queryParams: {c: this.competition, k: this._kategorieFilter, d: undefined}, // add query params to current url
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }
      );
      this.backendService.getGeraete(this.backendService.competition, undefined).subscribe(geraete => {
        this.geraete = geraete ?? [];
      });
      this.backendService.disconnectWS(true);
      this.backendService.initWebsocket();
      this._durchgang = undefined;
    } else {
      //console.log("navigate to durchgang defined: " + durchgang);
      this.router.navigate(
        ['last-results'],
        {
          queryParams: {c: this.competition, k: this._kategorieFilter, d: durchgang}, // add query params to current url
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }
      );
      this.backendService.getGeraete(this.backendService.competition, durchgang).subscribe(geraete => {
        this.geraete = geraete ?? [];
      });;
      this.backendService.disconnectWS(true);
      this.backendService.initWebsocket();
      this._durchgang = durchgang;
    }
  }
  get durchgangFilter(): string {
    return this._durchgang || 'undefined';
  }


  _kategorieFilter: string = "getrennt";

  set kategorieFilter(kategorieFilter: string) {
    this._kategorieFilter = kategorieFilter;
    if (this._kategorieFilter?.trim().length > 0) {
      this.router.navigate(
        ['last-results'],
        {
          queryParams: {c: this.competition, k: kategorieFilter, d: this.backendService.durchgang}, // add query params to current url
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }
      );
    } else {
      this.router.navigate(
        ['last-results'],
        {
          queryParams: {c: this.competition, d: this.backendService.durchgang}, // add query params to current url
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }
      );
    }
  }
  get kategorieFilter(): string {
    return this._kategorieFilter;
  }

  hideHeader: number = 0;

  toggleHeader() {
    if(!this.scorelistAvailable()) {
      this.hideHeader = this.hideHeader += 1;
      if (this.hideHeader > 2) {
        this.hideHeader = 0;
      }
    }
  }

  get nextToolbarPosition(): string {
    if (this.hideHeader === 0) {
      return 'von oben nach unten, und dann auf ausgeblendet';
    } else if (this.hideHeader === 1) {
      return 'von unten nach ausgeblendet';
    } else {
      return 'oben';
    }
  }
  isToolbarTop(): boolean {
    return this.hideHeader === 0 || this.scorelistAvailable();
  }

  isToolbarBottom(): boolean {
    return this.hideHeader === 1;
  }

  isFullscreen(): boolean {
    return this.hideHeader === 2;
  }

  get isBusy(): Observable<boolean> {
    return this.busy;
  }

  refreshItems(newLastRes: NewLastResults) {
    this.lastItems = this.items.map(item => item.id * this.geraete.length + item.geraet);
    this.items = [];
    this.mixeditems = [];
    this.hasCountingItems = false;
    //console.log('refreshing newLastRes...', newLastRes);
    if (!!newLastRes && !!newLastRes.resultsPerWkDisz) {
      //console.log('refreshing items ...');
      const newLastWKDiszValues = Object.values(newLastRes.resultsPerWkDisz);
      const pgms = newLastWKDiszValues.map(wc => wc.programm).filter(this.onlyUnique);
      const programme = pgms.length > 0 ? pgms : ['\u{00A0}'];
      if (newLastWKDiszValues.length > 0) {
        this.isDNoteUsed = newLastWKDiszValues.findIndex((v, i, obj) => v.isDNoteUsed) > -1;
      }
      const tmpItems = [];
      programme.forEach(p => {
        this.geraete.map(g => {
          return newLastWKDiszValues.find(d => d.geraet === g.id && d.wertung.endnote && (p === '\u{00A0}' || d.programm === p) ) 
          || <WertungContainer>{
            programm: p,
            geraet: g.id,
            id: 0,
            vorname: '\u{00A0}', name: '\u{00A0}', geschlecht: undefined, verein: '\u{00A0}',
            wertung: <Wertung>{
              noteE: 0, noteD: 0, endnote: 0,
              wettkampfdisziplinId: 0
            },
            isDNoteUsed:  this.isDNoteUsed
          }
        }).forEach(wc => {
            this.hasCountingItems = this.hasCountingItems || wc.wertung.endnote > 0;
            tmpItems.push(wc);
        });
      });
      this.items = tmpItems;
      this.mixeditems = this.geraete.map(g => newLastRes.resultsPerDisz[g.id] ?? <WertungContainer>{
          programm: '\u{00A0}',
          geraet: g.id,
          id: 0,
          vorname: '\u{00A0}', name: '\u{00A0}', geschlecht: undefined, verein: '\u{00A0}',
          wertung: <Wertung>{
            noteE: 0, noteD: 0, endnote: 0,
            wettkampfdisziplinId: 0
          },
          isDNoteUsed: this.items.length > 0 ?  this.items[0].isDNoteUsed : true
        }
      );
    }
    if (this.scorelistAvailable()) {
      this.loadScoreList();
    } else {
      this.title = 'Aktuelle Resultate';
    }
  }

  scorelistAvailable(): boolean {
    return this.hasCountingItems !== undefined && !this.durchgangopen && !this.hasCountingItems && new Date(this.competitionContainer().datum).getTime() <  new Date(Date.now() - 3600 * 1000 * 24).getTime();
  }

  get filteredScoreList() {
    if (this.sFilteredScoreList?.length > 0) {
      return this.sFilteredScoreList;
    } else {
      return this.getScoreListItems();
    }
  }

  loadScoreList() {
    //console.log('loadScoreLists');
    this.backendService.getScoreLists().subscribe(scorelists => {
      const c = this.competitionContainer();
      let genericLinkAddon = '';
      if (c.altersklassen && c.altersklassen.trim().length > 0) {
        genericLinkAddon = ':Wettkampf%20Altersklassen';
      }
      else if (c.jahrgangsklassen && c.jahrgangsklassen.trim().length > 0) {
        genericLinkAddon = ':Wettkampf%20JG-Altersklassen';
      }
      const genericLink = `/api/scores/${c.uuid}/query?groupby=Kategorie${genericLinkAddon}:Geschlecht`;
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
          "scores-href": `/api/scores/${c.uuid}/query?kind=Teamrangliste`,
          "scores-query": `/api/scores/${c.uuid}/query?kind=Teamrangliste`
        };
        this.scorelinks = this.teamsAllowed(c) ? [...lists, teamGeneric, einzelGeneric] : [...lists, einzelGeneric];
        const publishedLists = this.scorelinks.filter(s => ''+s.published === 'true')
        this.refreshScoreList(publishedLists[0]);
      }
    });
  }

  _scorelistSubscription: Subscription[] = [];
  cleanScoreListSubscriptions() {
    this._scorelistSubscription.forEach(s => s.unsubscribe());
    this._scorelistSubscription = [];
  }

  refreshScoreList(link: ScoreLink) {
    //console.log('refreshScoreList', link);
    let path = link['scores-href'];
    if (!link.published) {
      path = link['scores-query'];
    }
    if (path.startsWith('/')) {
      path = path.substring(1);
    }
    path = path.replace('html', '');
    this._optionsVisible = false;
    this.title = link.name;
    this.defaultPath = path;
    this._scorelistSubscription.push(this.backendService.getScoreList(this.defaultPath).pipe(
      map(scorelist => {
        if (!!scorelist.title && !!scorelist.scoreblocks) {
          return scorelist.scoreblocks;
        } else {
          return [];
        }
      })).subscribe(scoreblocks => {
        this.scoreblocks = scoreblocks;
        this.sFilteredScoreList = scoreblocks;
        this.cleanScoreListSubscriptions();
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          filter(event => !!event && !!event.target && !!event.target.value),
          map(event => event.target.value),
          debounceTime(1000),
          distinctUntilChanged(),
          share()
        );
        this._scorelistSubscription.push(pipeBeforeAction.subscribe(() => {
          this.busy.next(true);
        }));
        this._scorelistSubscription.push(pipeBeforeAction.pipe(
          switchMap(this.runQuery(this.scoreblocks))
        ).subscribe(filteredList => {
          //console.log('filteredList retrieved', filteredList);
          this.sFilteredScoreList = filteredList;
          this.busy.next(false);
        }));
      }));
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
    const upperQuery = query.toUpperCase();
    const queryTokens = upperQuery.split(' ');
    return (tnRoot: ScoreRow, block: ScoreBlock): boolean => {
      if (block.title.text.toUpperCase().indexOf(upperQuery) > -1) {
        return true;
      }
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
          if (tn.Jahrgang?.toUpperCase() === token) {
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
