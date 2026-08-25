import { Component, OnInit, inject, signal } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf, Wertung } from '../backend-types';
import { NavController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { filter, map } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { backendUrl } from '../utils';
import { GroupBy } from '../component/result-display/result-display.component';


@Component({
    selector: 'app-athlet-view',
    templateUrl: './athlet-view.page.html',
    styleUrls: ['./athlet-view.page.scss'],
    standalone: false
})
export class AthletViewPage  implements OnInit {
  navCtrl = inject(NavController);
  private readonly route = inject(ActivatedRoute);
  backendService = inject(BackendService);

  /** signal views for zoneless template bindings */
  readonly competitionsList = this.backendService.competitionsList;

  groupBy = GroupBy;

  // @ViewChild(IonContent) content: IonContent;

  items = signal<WertungContainer[]>([]);
  lastItems = signal<number[]>([]);
  geraete = signal<Geraet[]>([]);
  athletId: number;
  wkId: string;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  makeItemHash(item: WertungContainer) {
    return item.id * this.geraete().length * 200 + item.geraet * 100 + (item.wertung.endnote || -1);
  }

  ngOnInit(): void {
    // tslint:disable-next-line:radix
    this.athletId = parseInt(this.route.snapshot.paramMap.get('athletId'));
    this.wkId = this.route.snapshot.paramMap.get('wkId');
    this.backendService.loadAthletWertungen(this.wkId, this.athletId).pipe(
      filter(r => !!r && r.length > 0)
    ).subscribe(athletWertungen => {
      this.items.set(athletWertungen);
      this.sortItems(this.athletId);
    });
    this.backendService.geraeteSubject.subscribe(geraete => {
      if (!this.backendService.captionmode()) {
        this.geraete.set(geraete);
      }
    });
    const changeHandler = (wcs: {string: WertungContainer}) => {
      const currentItems = this.items();
      this.lastItems.set(currentItems.map(item => this.makeItemHash(item)));
      this.items.set(currentItems.map(item => {
        const newItem: WertungContainer = wcs[item.wertung.wettkampfdisziplinId];
        if (newItem && newItem.id === item.id) {
          return newItem;
        } else {
          return item;
        }
      }));
    };
    this.backendService.newLastResults.pipe(
      filter(r => !!r),
      map(r => r.resultsPerWkDisz)
    ).subscribe(changeHandler);
  }

  sortItems(athletId: number) {
    this.items.update(items => [...items].sort((a, b) => {
      let p = a.programm.localeCompare(b.programm);
      if (p === 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    }));
  }

  isNew(item: WertungContainer): boolean {
    const itemhash = this.makeItemHash(item);
    return this.lastItems().filter(id => id === itemhash).length === 0;
  }

  isStroked(item: WertungContainer): boolean {
    return item.isStroked || false;
  }

  get total(): WertungContainer {
    const items = this.items();
    const currentItem = items && items.length > 0 ? <WertungContainer>{ programm: '', geraet: 0, id: 0, vorname: items[0].vorname, name: items[0].name, geschlecht: items[0].geschlecht, verein: items[0].verein, wertung: <Wertung>{ noteE: 0, noteD: 0, endnote: 0, wettkampfdisziplinId: 0 }, isDNoteUsed: items[0].isDNoteUsed} : <WertungContainer>{ vorname: '', name: '', geschlecht: '', verein: '', programm: '', geraet: 0, id: 0, wertung: <Wertung>{ noteE: 0, noteD: 0, endnote: 0, wettkampfdisziplinId: 0 }, isDNoteUsed: false };
    if (items && items.length > 0) {
      return items.reduce((acc, item) => {
        if (!item.isStroked) {
          currentItem.wertung.noteE += item.wertung.noteE || 0;
          currentItem.wertung.noteD += item.wertung.noteD || 0;
          currentItem.wertung.endnote += item.wertung.endnote || 0;
          currentItem.programm = item.programm;
        }
        return currentItem;
      }, currentItem);
    } else {
      return currentItem;
    }
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed();
  }
  get competition(): string {
    return this.backendService.competition || '';
  }
  getCompetitions(): Wettkampf[] {
    return this.competitionsList() || [];
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

    if (!this.competitionsList()) { return emptyCandidate; }
    const candidate = this.competitionsList()
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

  get athlet(): WertungContainer {
    const items = this.items();
    if (items && items.length > 0) {
      return items[0];
    } else {
      return <WertungContainer>{};
    }
  }

  getColumnSpec(): number {
    return this.geraete().length;
  }

  getMaxColumnSpec(): number {
    return Math.min(12, Math.max(1, Math.floor(12 / this.items().length + 0.5)));
  }

  geraetOrder(geraetId: number): number {
    if (!this.geraete()) {
      return 0;
    }
    const ret = this.geraete()
      .findIndex(c => c.id === geraetId);
    return ret;
  }

  geraetText(geraetId: number): string {
    if (!this.geraete()) { return ''; }
    const candidate = this.geraete()
      .filter(c => c.id === geraetId)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  getTitle(wertungContainer: WertungContainer): string {
    return wertungContainer ? wertungContainer.programm + ' - ' + this.geraetText(wertungContainer.geraet) : 'Total Punkte';
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
    let text = `${this.items()[0].vorname} ${this.items()[0].name}, ${this.items()[0].verein} #${sport}-${c.titel.replace(',', ' ').split(' ').join('_')}_${this.items()[0].programm} #${this.items()[0].verein.replace(',', ' ').split(' ').join('_')}`;

    if (new Date(c.datum).getTime() > Date.now()) {
      text = "Hoffe das Beste für " + text;
    } else {
      text = "Geschafft! Wettkampf Resultate von " + text;
    }
    if(this.isShareAvailable()) {
      navigator.share({
        title: this.competitionName(),
        text: text,
        url: `${backendUrl}athlet-view/${this.wkId}/${this.athletId}`
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
}
