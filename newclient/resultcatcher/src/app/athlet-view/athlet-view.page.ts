import { Component, OnInit } from '@angular/core';
import { WertungContainer, Geraet, Wettkampf } from '../backend-types';
import { NavController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { filter, map } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { isArray } from 'util';

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

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              public backendService: BackendService) {
  }

  makeItemHash(item: WertungContainer) {
    return item.id * this.geraete.length * 200 + item.geraet * 100 + (item.wertung.endnote || -1);
  }

  ngOnInit(): void {
    // tslint:disable-next-line:radix
    const athletId = parseInt(this.route.snapshot.paramMap.get('athletId'));
    const wkId = this.route.snapshot.paramMap.get('wkId');
    this.backendService.loadAthletWertungen(wkId, athletId).pipe(
      filter(r => !!r && r.length > 0)
    ).subscribe(athletWertungen => {
      this.items = athletWertungen;
      this.sortItems(athletId);
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
}
