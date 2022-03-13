import { Component, OnInit } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf } from '../backend-types';
import { NavController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { filter, map } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { backendUrl } from '../utils';


@Component({
  selector: 'app-athlet-view',
  templateUrl: './athlet-view.page.html',
  styleUrls: ['./athlet-view.page.scss'],
})
export class AthletViewPage  implements OnInit {

  // @ViewChild(IonContent) content: IonContent;

  items: WertungContainer[] = [];
  lastItems: number[] = [];
  geraete: Geraet[] = [];
  athletId: number;
  wkId: string;

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              public backendService: BackendService) {
  }

  makeItemHash(item: WertungContainer) {
    return item.id * this.geraete.length * 200 + item.geraet * 100 + (item.wertung.endnote || -1);
  }

  ngOnInit(): void {
    // tslint:disable-next-line:radix
    this.athletId = parseInt(this.route.snapshot.paramMap.get('athletId'));
    this.wkId = this.route.snapshot.paramMap.get('wkId');
    this.backendService.loadAthletWertungen(this.wkId, this.athletId).pipe(
      filter(r => !!r && r.length > 0)
    ).subscribe(athletWertungen => {
      this.items = athletWertungen;
      this.sortItems(this.athletId);
    });
    this.backendService.geraeteSubject.subscribe(geraete => {
      if (!this.backendService.captionmode) {
        this.geraete = geraete;
      }
    });
    const changeHandler = (wcs: {string: WertungContainer}) => {
      this.lastItems = this.items.map(item => this.makeItemHash(item));
      this.items = this.items.map(item => {
        const newItem: WertungContainer = wcs[item.wertung.wettkampfdisziplinId];
        if (newItem && newItem.id === item.id) {
          return newItem;
        } else {
          return item;
        }
      });
    };
    this.backendService.newLastResults.pipe(
      filter(r => !!r),
      map(r => r.results)
    ).subscribe(changeHandler);
  }

  sortItems(athletId: number) {
    this.items = this.items.sort((a, b) => {
      let p = a.programm.localeCompare(b.programm);
      if (p === 0) {
        p = this.geraetOrder(a.geraet) - this.geraetOrder(b.geraet);
      }
      return p;
    });
  }
  isNew(item: WertungContainer): boolean {
    const itemhash = this.makeItemHash(item);
    return this.lastItems.filter(id => id === itemhash).length === 0;
  }
  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
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

  isShareAvailable():boolean {
    return !!navigator && !!navigator.share && navigator.canShare();
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
    let text = `${this.items[0].vorname} ${this.items[0].name}, ${this.items[0].verein} #${sport}-${c.titel.split(' ').join('_')}} - #${this.items[0].verein} #${this.items[0].programm}`;

    if (c.datum.getDate > Date.now) {
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
